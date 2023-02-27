#include <pthread.h>
#include <string.h>
#include <stdlib.h>

#include "debug.h"
#include "ogl-loader.h"
#include "render.h"
#include "render-text.h"
#include "render-video.h"

static int count_renders;
static render_layer *renders;
static render_output *output = NULL;

static int run;

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

void lock_renders() {
    for (int i=0; i < count_renders; i++) {
        pthread_mutex_lock(&renders[i].thread_mutex);
    }
}

void unlock_renders() {
    for (int i=0; i < count_renders; i++) {
        pthread_cond_signal(&renders[i].thread_cond);
        pthread_mutex_unlock(&renders[i].thread_mutex);
    }
}

void lock_renders_for_asset_upload() {
    for (int i=0; i < count_renders; i++) {
        pthread_mutex_lock(&renders[i].asset_thread_mutex);
    }
}

void unlock_renders_for_asset_upload() {
    for (int i=0; i < count_renders; i++) {
        pthread_mutex_unlock(&renders[i].asset_thread_mutex);
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
    run = 0;

    // Send signal so threads wont be locked waiting for signal
    lock_renders();
    unlock_renders();

    for (int i=0; i<count_renders; i++) {
        pthread_join(renders[i].thread_id, NULL);
    }

    render_video_shutdown();
    render_text_shutdown();

    for (int i=0; i<count_renders; i++) {
        glfwDestroyWindow(renders[i].window);
    }

    free(output);
    free(renders);
}

void* renderer_loop(void *arg) {
    render_layer *render = (render_layer*) arg;
    config_color_factor *background_clear_color = &render->config.background_clear_color;

    if (!render->window) {
        log_debug("Renderer loop window is null\n");
    }

    glfwMakeContextCurrent(render->window);
    glewInit();

    int width = render->config.w;
    int height = render->config.h;

    glViewport(0, 0, width, height);

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

//    // Set the list of draw buffers.
//    GLenum DrawBuffers[1] = {GL_COLOR_ATTACHMENT0};
//    glDrawBuffers(1, DrawBuffers); // "1" is the size of DrawBuffers

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    for (int i=0; i < count_renders; i++) {
        if (output[i].render_id == render->config.render_id) {
            output[i].rendered_texture = renderedTexture;
        }
    }

    if (render->config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {
        lock_renders_for_asset_upload();

        render_video_generate_assets();

        unlock_renders_for_asset_upload();
    }

    while (run) {
        if (render->config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {
            register_render_frame();

            lock_renders_for_asset_upload();

            render_video_upload_texes();
            render_text_upload_texes();

            unlock_renders_for_asset_upload();
        }

        pthread_mutex_lock(&render->thread_mutex);
        pthread_cond_wait(&render->thread_cond, &render->thread_mutex);

        pthread_mutex_lock(&render->asset_thread_mutex);

        glBindFramebuffer(GL_FRAMEBUFFER, FramebufferName);
        glViewport(0, 0, width, height);

        glPushMatrix();
        glOrtho(0.f, render->config.w, render->config.h, 0.f, 0.f, 1.f );

        glClearColor(background_clear_color->r, background_clear_color->g, background_clear_color->b, 1.0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        render_text_render_cycle(render);
        render_video_render(render);

        glPopMatrix();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        pthread_mutex_unlock(&render->asset_thread_mutex);
        pthread_mutex_unlock(&render->thread_mutex);
    }

    if (render->config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {

        render_text_deallocate_assets();
        render_video_deallocate_assets();

    }

    for (int i=0; i < count_renders; i++) {
        if (output[i].render_id == render->config.render_id) {
            output[i].rendered_texture = 0;
        }
    }

    glDeleteTextures(1, &renderedTexture);

    return NULL;
}

void create_render(GLFWwindow *shared_context, config_render *render_conf, render_layer *render) {
    memcpy(&render->config, render_conf, sizeof(config_render));

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    render->window = glfwCreateWindow(render_conf->w, render_conf->h, "Projector Render Layer", NULL, shared_context);

    if (!render->window) {
        log_debug("Failed to create render window\n");
    }

    if (render->config.text_render_mode & CONFIG_RENDER_MODE_MAIN) {
        render_text_set_size(render->config.text_area.w, render->config.text_area.h);
    }

    pthread_mutex_init(&render->asset_thread_mutex, NULL);
    pthread_mutex_init(&render->thread_mutex, NULL);
    pthread_cond_init(&render->thread_cond, NULL);

    pthread_create(&render->thread_id, NULL, renderer_loop, (void*)render);
}

void activate_renders(GLFWwindow *shared_context, projection_config *config) {
    count_renders = config->count_renders;
    renders = calloc(config->count_renders, sizeof(render_layer));
    run = 1;

    output = (render_output*) calloc(count_renders, sizeof(render_output));

    for (int i=0; i < config->count_renders; i++) {
        output[i].render_id = config->renders[i].render_id;
        create_render(shared_context, &config->renders[i], &renders[i]);
    }
}