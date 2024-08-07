#include <string.h>
#include <stdlib.h>
#include <time.h>

#include "clock.h"
#include "tinycthread.h"
#include "debug.h"
#include "ogl-loader.h"
#include "render.h"
#include "render-text.h"
#include "render-video.h"
#include "render-web-view.h"
#include "render-window-capture.h"
#include "render-image.h"
#include "render-preview.h"
#include "render-tex-blur.h"
#include "render-video-capture.h"

static int count_renders;
static render_layer *renders;
static render_output *output = NULL;

static int transfer_window_thread_run;
static int transfer_window_initialized;
static GLFWwindow *transfer_window;
static thrd_t transfer_window_thread;

void get_main_output_size(render_output_size *output_size) {
    for (int i = 0; i < count_renders; i++) {
        if (renders[i].config.render_mode & CONFIG_RENDER_MODE_MAIN) {
            output_size->render_width = renders[i].config.w;
            output_size->render_height = renders[i].config.h;
            return;
        }
    }

    log_debug("BUG: No main render found! Be sure to have created one!\n");
}

void get_main_text_area(config_bounds *text_area) {
    for (int i = 0; i < count_renders; i++) {
        if (renders[i].config.render_mode & CONFIG_RENDER_MODE_MAIN) {
            memcpy(text_area, &renders[i].config.text_area, sizeof(config_bounds));
            return;
        }
    }

    log_debug("BUG: No main render found! Be sure to have created one!\n");
}

void get_render_output(render_output **out, int *render_output_count) {
   (*out) = output;
   (*render_output_count) = count_renders;
}

void initialize_renders() {
    render_video_initialize();
    render_text_initialize();
    render_web_view_initialize();
    render_window_capture_initialize();
    render_image_initialize();
    render_preview_initialize();
    render_video_capture_initialize();
}

void shutdown_renders() {
    transfer_window_thread_run = 0;
    thrd_join(transfer_window_thread, NULL);

    render_video_destroy_window();

    render_video_shutdown();
    render_text_shutdown();
    render_web_view_shutdown();
    render_window_capture_shutdown();
    render_image_shutdown();
    render_preview_shutdown();
    render_video_capture_shutdown();

    free(output);
    free(renders);

    glfwDestroyWindow(transfer_window);
}

void render_init(render_layer *render) {
    int width = render->config.w;
    int height = render->config.h;

    GLuint renderedTexture;
    glGenTextures(1, &renderedTexture);

    render->rendered_texture = renderedTexture;

    // "Bind" the newly created texture : all future texture functions will modify this texture
    glBindTexture(GL_TEXTURE_2D, renderedTexture);

    // Give an empty image to OpenGL ( the last "0" )
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, 0);

    tex_set_default_params();

    glBindTexture(GL_TEXTURE_2D, 0);

    GLuint FramebufferName = 0;
    glGenFramebuffers(1, &FramebufferName);
    glBindFramebuffer(GL_FRAMEBUFFER, FramebufferName);

    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderedTexture, 0);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    render->framebuffer_name = FramebufferName;

    for (int i=0; i < count_renders; i++) {
        if (output[i].render_id == render->config.render_id) {
            output[i].rendered_texture = renderedTexture;
        }
    }

    if (render->config.render_mode & CONFIG_RENDER_MODE_MAIN) {
        render_video_create_assets();
        render_text_create_assets();
        render_web_view_create_assets();
        render_window_capture_create_assets();
        render_image_create_assets();
        render_preview_create_assets();
        render_tex_blur_create_assets();
        render_video_capture_create_assets();
    }

    render_text_start(render);
}

void render_cycle(render_layer *render) {
    if (!transfer_window_initialized) {
        return;
    }

    config_color_factor *background_clear_color = &render->config.background_clear_color;

    int width = render->config.w;
    int height = render->config.h;

    glBindFramebuffer(GL_FRAMEBUFFER, render->framebuffer_name);

    glViewport(0, 0, width, height);

    glPushMatrix();
    glLoadIdentity();
    glOrtho(0.0, width, height, 0.0, 0.0, 1.0);

    glClearColor(background_clear_color->r, background_clear_color->g, background_clear_color->b, background_clear_color->a);
    glClear(GL_COLOR_BUFFER_BIT);

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    render_image_render(render);
    render_video_render(render);
    render_web_view_render(render);
    render_window_capture_render(render);
    render_video_capture_render(render);
    render_text_render(render);

    render_preview_cycle(render);

    glPopMatrix();

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
}

void render_terminate(render_layer *render) {
    render_text_stop(render);

    if (render->config.render_mode & CONFIG_RENDER_MODE_MAIN) {
        render_text_deallocate_assets();
        render_video_deallocate_assets();
        render_web_view_deallocate_assets();
        render_window_capture_deallocate_assets();
        render_image_deallocate_assets();
        render_preview_deallocate_assets();
        render_tex_blur_deallocate_assets();
        render_video_capture_deallocate_assets();
    }

    for (int i=0; i < count_renders; i++) {
        if (output[i].render_id == render->config.render_id) {
            output[i].rendered_texture = 0;
        }
    }

    glDeleteFramebuffers(1, &render->framebuffer_name);
    glDeleteTextures(1, &render->rendered_texture);
}

void renders_init() {
    for (int i=0; i<count_renders; i++) {
        render_layer *render = (render_layer*) &renders[i];
        render_init(render);
    }
}

void renders_update_assets()
{
    if (!transfer_window_initialized) {
        return;
    }

    render_text_update_assets();
    render_video_update_assets();
    render_web_view_update_assets();
    render_window_capture_update_assets();
    render_image_update_assets();
    render_preview_update_assets();
    render_video_capture_update_assets();
}

void renders_cycle() {
    glEnable(GL_TEXTURE_2D);
    
    for (int i=0; i<count_renders; i++) {
        render_layer *render = (render_layer*) &renders[i];
        render_cycle(render);
    }

    glDisable(GL_TEXTURE_2D);
}

void renders_flush_buffers() {
    if (!transfer_window_initialized) {
        return;
    }

    render_text_flush_buffers();
    render_video_flush_buffers();
    render_web_view_flush_buffers();
    render_window_capture_flush_buffers();
    render_image_flush_buffers();
    render_video_capture_flush_buffers();
    render_preview_flush_buffers();
}

void renders_terminate() {
    for (int i=0; i<count_renders; i++) {
        render_layer *render = (render_layer*) &renders[i];
        render_terminate(render);
    }
}

int transfer_window_loop(void *_) {
    struct timespec ts_prev, ts_curr;
    struct timespec sleep_time = {
        .tv_sec = 0,
        .tv_nsec = 5 * 1000 * 1000
    };

    glfwMakeContextCurrent(transfer_window);
    
#ifdef _GLEW_ENABLED_
    glewInit();
#endif
    
    render_text_create_buffers();
    render_video_create_buffers();
    render_web_view_create_buffers();
    render_window_capture_create_buffers();
    render_image_create_buffers();
    render_preview_create_buffers();
    render_video_capture_create_buffers();

    transfer_window_initialized = 1;

    glClearColor(0, 0, 0, 1.0);

    get_time(&ts_prev);

    while (transfer_window_thread_run) {
        glClear(GL_COLOR_BUFFER_BIT);

        render_video_update_buffers();
        render_text_update_buffers();
        render_web_view_update_buffers();
        render_window_capture_update_buffers();
        render_image_update_buffers();
        render_preview_update_buffers();
        render_video_capture_update_buffers();

        glFinish();
        
        get_time(&ts_curr);

        while (get_delta_time_ms(&ts_curr, &ts_prev) < 15)
        {
            thrd_sleep(&sleep_time, NULL);
            get_time(&ts_curr);
        }

        copy_time(&ts_prev, &ts_curr);

        register_stream_frame();
    }

    render_video_deallocate_buffers();
    render_text_deallocate_buffers();
    render_web_view_deallocate_buffers();
    render_window_capture_deallocate_buffers();
    render_image_deallocate_buffers();
    render_preview_deallocate_buffers();
    render_video_capture_deallocate_buffers();

    return 0;
}


void renders_config_hot_reload(projection_config *config) {
    for (int i=0; i < config->count_renders; i++) {
        config_render* render_conf = &config->renders[i];
        render_layer* render = &renders[i];
        memcpy(&render->config, render_conf, sizeof(config_render));
    }

    render_preview_set_renders(renders, config->count_renders);
}

void activate_renders(GLFWwindow *shared_context, projection_config *config) {
    count_renders = config->count_renders;
    renders = calloc(config->count_renders, sizeof(render_layer));

    output = (render_output*) calloc(count_renders, sizeof(render_output));

    for (int i=0; i < config->count_renders; i++) {
        output[i].render_id = config->renders[i].render_id;
        output[i].size.render_width = config->renders[i].w;
        output[i].size.render_height = config->renders[i].h;
    }

    renders_config_hot_reload(config);

    render_text_set_config(renders, count_renders);
    render_image_set_config(renders, count_renders);

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_SAMPLES, 0);
    transfer_window = glfwCreateWindow(800, 600, "Projector Stream Window", NULL, shared_context);

    render_video_create_window(shared_context);

    transfer_window_thread_run = 1;
    transfer_window_initialized = 0;
    thrd_create(&transfer_window_thread, transfer_window_loop, NULL);
    log_debug("renders activated\n");
}
