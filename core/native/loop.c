#include <pthread.h>
#include <stdlib.h>

#include "debug.h"
#include "loop.h"
#include "ogl-loader.h"
#include "render.h"

static int run;
static pthread_t thread_id;

void* loop(void*) {

    render_output *output;
    int render_output_count;

    get_render_output(&output, &render_output_count);
    monitors_init(output, render_output_count);

    monitor_prepare_renders_context();
    renders_init();

    while (run) {
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

void start_main_loop() {
    run = 1;
    pthread_create(&thread_id, NULL, loop, NULL);
}

void terminate_main_loop() {
    run = 0;
    pthread_join(thread_id, NULL);
}