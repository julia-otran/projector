#include "tinycthread.h"
#include <stdlib.h>

#include "debug.h"
#include "loop.h"
#include "ogl-loader.h"
#include "render.h"
#include "clock.h"

static int run;
static int waiting;

static thrd_t thread_id;
static mtx_t thread_mutex;
static cnd_t thread_cond;

static projection_config *pending_config_reload;

static struct timespec last_frame_completed_at = {
    .tv_nsec = 0,
    .tv_sec = 0
};

int loop(void *_) {
    struct timespec current_time;
    struct timespec sleep_interval = {
        .tv_sec = 0
    };

    render_output *output;
    int render_output_count;

    get_render_output(&output, &render_output_count);
    monitors_init(output, render_output_count);

    monitors_config_hot_reload(pending_config_reload);
    int milisecs_per_frame = 1000.0 / (monitors_get_minor_refresh_rate() - 10.0);
    pending_config_reload = NULL;

    monitor_prepare_renders_context();
    renders_init();

    get_time(&last_frame_completed_at);

    while (run) {
        mtx_lock(&thread_mutex);

        if (pending_config_reload) {
            monitors_config_hot_reload(pending_config_reload);
            milisecs_per_frame = 1000.0 / (monitors_get_minor_refresh_rate() - 2.0);
            pending_config_reload = NULL;
        }

        if (waiting) {
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
        waiting = 1;
    }

    pending_config_reload = config;

    if (run) {
        cnd_wait(&thread_cond, &thread_mutex);
        waiting = 0;
        mtx_unlock(&thread_mutex);
    }
}

void main_loop_start() {
    mtx_init(&thread_mutex, 0);
    cnd_init(&thread_cond);

    run = 1;

    thrd_create(&thread_id, loop, NULL);

    mtx_lock(&thread_mutex);
    waiting = 1;
    cnd_wait(&thread_cond, &thread_mutex);
    waiting = 0;
    mtx_unlock(&thread_mutex);

}

void main_loop_terminate() {
    run = 0;

    thrd_join(thread_id, NULL);
    cnd_destroy(&thread_cond);
    mtx_destroy(&thread_mutex);
}