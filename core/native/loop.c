#include <pthread.h>
#include <stdlib.h>

#include "debug.h"
#include "loop.h"
#include "ogl-loader.h"

static int run;
static pthread_t thread_id;

void* loop(void*) {

    render_output *output;
    int render_output_count;

    get_render_output(&output, &render_output_count);
    prepare_monitors(output, render_output_count);

    while (run) {
        lock_renders();
        render_monitors();
        unlock_renders();

        glfwPollEvents();

        if (window_should_close()) {
            run = 0;
        }
    }

    deallocate_monitors();

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