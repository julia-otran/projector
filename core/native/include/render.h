#include <pthread.h>
#include "ogl-loader.h"
#include "config-structs.h"

#ifndef _RENDER_H_
#define _RENDER_H_

typedef struct {
    config_render config;
    GLFWwindow *window;
    pthread_t thread_id;

    pthread_mutex_t thread_mutex;
    pthread_cond_t thread_cond;

    GLuint rendered_texture;
} render_layer;

typedef struct {
    int render_id;
    GLuint rendered_texture;
} render_output;

void activate_renders(GLFWwindow *shared_context, projection_config *config);
void shutdown_renders();
void render_cycle(render_output **out, int *render_output_count);

#endif