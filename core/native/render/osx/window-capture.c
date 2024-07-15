#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "debug.h"
#include "window-capture.h"

#define ACTIVE_WINDOWS "_NET_CLIENT_LIST"

typedef struct {
    
} window_capture_extra_data;

void window_capture_init() {

}

window_node_list* window_capture_get_window_list() {
    window_node_list *result = calloc(1, sizeof(window_node_list));

    result->list_size = 0;

    return result;
}

void window_capture_free_window_list(window_node_list* list) {
    for (unsigned int i = 0; i < list->list_size; i++) {
        window_node *window_node_instance = &list->list[i];

        free(window_node_instance->extra_data);
        free(window_node_instance->window_name);
    }

    free(list->list);
    free(list);
}

void* window_capture_get_handler(char *window_name) {
    window_node_list* list = window_capture_get_window_list();
    void *handle = NULL;
    void *handle_copy = NULL;

    for (unsigned int i = 0; i < list->list_size; i++) {
        if (strcmp(list->list[i].window_name, window_name) == 0) {
            handle = list->list[i].extra_data;
            break;
        }
    }

    if (handle) {
        handle_copy = (void*) calloc(1, sizeof(window_capture_extra_data));
        memcpy(handle_copy, handle, sizeof(window_capture_extra_data));
    }

    window_capture_free_window_list(list);

    return handle_copy;
}

void window_capture_free_handler(void *handler) {
    free(handler);
}

void window_capture_get_window_size(void *handler, int *out_width, int *out_height) {
    window_capture_extra_data* handler_data = (window_capture_extra_data*) handler;

}

void window_capture_get_image(void *handler, int width, int height, void *out_data) {
    window_capture_extra_data* handler_data = (window_capture_extra_data*) handler;
}

void window_capture_terminate() {
}
