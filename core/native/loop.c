#include <pthread.h>
#include <stdlib.h>

#include "debug.h"
#include "loop.h"
#include "ogl-loader.h"
#include "render.h"

static int run;

static pthread_t thread_id;
static pthread_mutex_t thread_mutex;
static pthread_cond_t thread_cond;

static projection_config *pending_config_reload;

void* loop(void*) {

    render_output *output;
    int render_output_count;

    get_render_output(&output, &render_output_count);
    monitors_init(output, render_output_count);

    monitors_config_hot_reload(pending_config_reload);
    pending_config_reload = NULL;

    monitor_prepare_renders_context();
    renders_init();

    while (run) {
        pthread_mutex_lock(&thread_mutex);

        if (pending_config_reload) {
            monitors_config_hot_reload(pending_config_reload);
            pending_config_reload = NULL;
            pthread_cond_signal(&thread_cond);
        }

        pthread_mutex_unlock(&thread_mutex);

        monitor_prepare_renders_context();
        renders_cycle();
        monitors_cycle();

        glfwPollEvents();

        if (window_should_close()) {
            run = 0;
        }
    }

    renders_terminate();
    monitors_terminate();

    return NULL;
}

void main_loop_schedule_config_reload(projection_config *config) {
    if (run) {
        pthread_mutex_lock(&thread_mutex);
    }

    pending_config_reload = config;

    if (run) {
        pthread_cond_wait(&thread_cond, &thread_mutex);
        pthread_mutex_unlock(&thread_mutex);
    }
}

void main_loop_start() {
    pthread_mutex_init(&thread_mutex, 0);

    run = 1;

    pthread_create(&thread_id, NULL, loop, NULL);
}

void main_loop_terminate() {
    run = 0;

    pthread_join(thread_id, NULL);
    pthread_mutex_destroy(&thread_mutex);
}