#include <pthread.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

#include "debug.h"
#include "ogl-loader.h"
#include "render.h"
#include "render-text.h"
#include "render-video.h"

static int count_renders;
static render_layer *renders;
static render_output *output = NULL;

static int transfer_window_thread_run;
static int transfer_window_initialized;
static GLFWwindow *transfer_window;
static pthread_t transfer_window_thread;
static pthread_mutex_t transfer_window_thread_mutex;
static pthread_cond_t transfer_window_thread_cond;

void get_main_output_size(render_output_size *output_size) {
    for (int i = 0; i < count_renders; i++) {
        if (renders[i].config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {
            output_size->render_width = renders[i].config.w;
            output_size->render_height = renders[i].config.h;
        }
    }
}

void get_main_text_area(config_bounds *text_area) {
    for (int i = 0; i < count_renders; i++) {
        if (renders[i].config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {
            memcpy(text_area, &renders[i].config.text_area, sizeof(config_bounds));
        }
    }
}

void get_render_output(render_output **out, int *render_output_count) {
   (*out) = output;
   (*render_output_count) = count_renders;
}

void initialize_renders() {
    render_video_initialize();
    render_text_initialize();
}

void shutdown_renders() {
    transfer_window_thread_run = 0;
    pthread_join(transfer_window_thread, NULL);

    render_video_shutdown();
    render_text_shutdown();

    free(output);
    free(renders);
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
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,GL_RGBA, GL_UNSIGNED_BYTE, 0);

    // Poor filtering. Needed !
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

    glBindTexture(GL_TEXTURE_2D, 0);

    GLuint glDepthRenderBuffer;
    glGenRenderbuffers(1, &glDepthRenderBuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, glDepthRenderBuffer);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
    glBindRenderbuffer(GL_RENDERBUFFER, 0);

    GLuint FramebufferName = 0;
    glGenFramebuffers(1, &FramebufferName);
    glBindFramebuffer(GL_FRAMEBUFFER, FramebufferName);

    // Set "renderedTexture" as our colour attachement #0
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderedTexture, 0);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, glDepthRenderBuffer);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    render->framebuffer_name = FramebufferName;

    for (int i=0; i < count_renders; i++) {
        if (output[i].render_id == render->config.render_id) {
            output[i].rendered_texture = renderedTexture;
        }
    }

    if (render->config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {
        render_video_create_assets();
        render_text_create_assets();
    }
}

void render_cycle(render_layer *render) {
    if (!transfer_window_initialized) {
        return;
    }

    config_color_factor *background_clear_color = &render->config.background_clear_color;

    int width = render->config.w;
    int height = render->config.h;

    glBindFramebuffer(GL_FRAMEBUFFER, render->framebuffer_name);

    // Point shaders output to correct buffer
    GLenum DrawBuffers[1] = {GL_COLOR_ATTACHMENT0};
    glDrawBuffers(1, DrawBuffers);

    glViewport(0, 0, width, height);

    glClearColor(background_clear_color->r, background_clear_color->g, background_clear_color->b, 1.0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glPushMatrix();
    glOrtho(0.0, width, height, 0.0, 0.0, 1.0);

    render_video_render(render);
    render_text_render(render);

    glPopMatrix();

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
}

void render_terminate(render_layer *render) {
    if (render->config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {
        render_text_deallocate_assets();
        render_video_deallocate_assets();
    }

    for (int i=0; i < count_renders; i++) {
        if (output[i].render_id == render->config.render_id) {
            output[i].rendered_texture = 0;
        }
    }

    glDeleteTextures(1, &render->rendered_texture);
}

void renders_init() {
    for (int i=0; i<count_renders; i++) {
        render_layer *render = (render_layer*) &renders[i];
        render_init(render);
    }
}

void renders_cycle() {
    for (int i=0; i<count_renders; i++) {
        render_layer *render = (render_layer*) &renders[i];
        render_cycle(render);
    }
}

void renders_terminate() {
    for (int i=0; i<count_renders; i++) {
        render_layer *render = (render_layer*) &renders[i];
        render_terminate(render);
    }
}

void* transfer_window_loop(void*) {
    glfwMakeContextCurrent(transfer_window);
    glewInit();

    render_text_create_buffers();
    render_video_create_buffers();

    transfer_window_initialized = 1;

    while (transfer_window_thread_run) {
        render_video_update_buffers();
        render_text_update_buffers();
        usleep(1000);
    }

    render_video_deallocate_buffers();
    render_text_deallocate_buffers();

    return NULL;
}

void create_render(config_render *render_conf, render_layer *render) {
    memcpy(&render->config, render_conf, sizeof(config_render));

    if (render->config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {
        render_text_set_size(render->config.text_area.w, render->config.text_area.h);
    }
}

void activate_renders(GLFWwindow *shared_context, projection_config *config) {
    count_renders = config->count_renders;
    renders = calloc(config->count_renders, sizeof(render_layer));

    output = (render_output*) calloc(count_renders, sizeof(render_output));

    for (int i=0; i < config->count_renders; i++) {
        output[i].render_id = config->renders[i].render_id;
        create_render(&config->renders[i], &renders[i]);
    }

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    transfer_window = glfwCreateWindow(800, 600, "Projector Render Transfer Window", NULL, shared_context);

    pthread_mutex_init(&transfer_window_thread_mutex, 0);
    pthread_cond_init(&transfer_window_thread_cond, 0);

    transfer_window_thread_run = 1;
    transfer_window_initialized = 0;
    pthread_create(&transfer_window_thread, 0, transfer_window_loop, NULL);
    log_debug("renders activated\n");
}