#include <pthread.h>
#include <stdlib.h>
#include "loop.h"

static int run;
static pthread_t thread_id;

void* loop(void*) {
    render_output *output;
    int render_output_count;

    prepare_monitors();

    while (run) {
        render_cycle(&output, &render_output_count);
        render_monitors(output, render_output_count);

        glfwPollEvents();
        run = !window_should_close();

        free(output);
    }

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