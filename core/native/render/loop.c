#include "tinycthread.h"
#include <stdlib.h>

#include "debug.h"
#include "loop.h"
#include "ogl-loader.h"
#include "render.h"

static int run;

static thrd_t thread_id;
static mtx_t thread_mutex;
static cnd_t thread_cond;

static projection_config *pending_config_reload;

int loop(void *_) {

    render_output *output;
    int render_output_count;

    get_render_output(&output, &render_output_count);
    monitors_init(output, render_output_count);

    monitors_config_hot_reload(pending_config_reload);
    pending_config_reload = NULL;

    monitor_prepare_renders_context();
    renders_init();

    while (run) {
        mtx_lock(&thread_mutex);

        if (pending_config_reload) {
            monitors_config_hot_reload(pending_config_reload);
            pending_config_reload = NULL;
            cnd_signal(&thread_cond);
        }

        mtx_unlock(&thread_mutex);

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

    return 0;
}

void main_loop_schedule_config_reload(projection_config *config) {
    if (run) {
        mtx_lock(&thread_mutex);
    }

    pending_config_reload = config;

    if (run) {
        cnd_wait(&thread_cond, &thread_mutex);
        mtx_unlock(&thread_mutex);
    }
}

void main_loop_start() {
    mtx_init(&thread_mutex, 0);

    run = 1;

    thrd_create(&thread_id, loop, NULL);
}

void main_loop_terminate() {
    run = 0;

    thrd_join(thread_id, NULL);
    mtx_destroy(&thread_mutex);
}