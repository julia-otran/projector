#include <pthread.h>
#include <string.h>
#include <stdlib.h>

#include "render.h"

static int count_renders;
static render_layer *renders;

static int run;

void render_cycle(render_output **out, int *render_output_count) {
    render_output *output = NULL;

    if (out) {
        output = (render_output*) calloc(count_renders, sizeof(render_output));
        (*out) = output;
    }

    for (int i=0; i < count_renders; i++) {
        pthread_mutex_lock(&renders[i].thread_mutex);
    }

    for (int i=0; i < count_renders; i++) {
        pthread_cond_signal(&renders[i].thread_cond);
        pthread_mutex_unlock(&renders[i].thread_mutex);

        if (output) {
            output[i].render_id = renders[i].config.render_id;
            output[i].rendered_texture = renders[i].rendered_texture;
        }
    }

    if (render_output_count) {
        (*render_output_count) = count_renders;
    }
}

void shutdown_renders() {
    run = 0;
    render_cycle(NULL, NULL);

    for (int i=0; i<count_renders; i++) {
        pthread_join(renders[i].thread_id, NULL);
        glfwDestroyWindow(renders[i].window);
    }

    free(renders);
}

void* renderer_loop(void *arg) {
    render_layer *render = (render_layer*) arg;
    config_color_factor *background_clear_color = &render->config.background_clear_color;

    glfwMakeContextCurrent(render->window);
    gladLoadGL(glfwGetProcAddress);

    int width, height;

    glfwGetFramebufferSize(render->window, &width, &height);

    GLuint FramebufferName = 0;
    glGenFramebuffers(1, &FramebufferName);
    glBindFramebuffer(GL_FRAMEBUFFER, FramebufferName);

    GLuint renderedTexture;
    glGenTextures(1, &renderedTexture);

    render->rendered_texture = renderedTexture;

    // "Bind" the newly created texture : all future texture functions will modify this texture
    glBindTexture(GL_TEXTURE_2D, renderedTexture);

    // Give an empty image to OpenGL ( the last "0" )
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0,GL_RGB, GL_UNSIGNED_BYTE, 0);

    // Poor filtering. Needed !
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

    // Set "renderedTexture" as our colour attachement #0
    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderedTexture, 0);

    // Set the list of draw buffers.
    GLenum DrawBuffers[1] = {GL_COLOR_ATTACHMENT0};
    glDrawBuffers(1, DrawBuffers); // "1" is the size of DrawBuffers

    while (run) {
        pthread_mutex_lock(&render->thread_mutex);

        glBindFramebuffer(GL_FRAMEBUFFER, FramebufferName);
        glViewport(0, 0, width, height);

        glClearColor(background_clear_color->r, background_clear_color->g, background_clear_color->b, 1.0);
        glClear(GL_COLOR_BUFFER_BIT);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glfwSwapBuffers(render->window);

        pthread_cond_wait(&render->thread_cond ,&render->thread_mutex);
        pthread_mutex_unlock(&render->thread_mutex) ;
    }

    return NULL;
}

void create_render(GLFWwindow *shared_context, config_render *render_conf, render_layer *render) {
    memcpy(&render->config, render_conf, sizeof(config_render));

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    render->window = glfwCreateWindow(render_conf->w, render_conf->h, "Projector Render Layer", NULL, shared_context);

    pthread_mutex_init(&render->thread_mutex, NULL);
    pthread_cond_init(&render->thread_cond, NULL);

    pthread_create(&render->thread_id, NULL, renderer_loop, (void*)render);
}

void activate_renders(GLFWwindow *shared_context, projection_config *config) {
    count_renders = config->count_renders;
    renders = calloc(config->count_renders, sizeof(render_layer));
    run = 1;

    for (int i=0; i < config->count_renders; i++) {
        create_render(shared_context, &config->renders[i], &renders[i]);
    }
}