#include "debug.h"
#include "window-capture.h"

typedef struct {
} window_capture_extra_data;

void window_capture_init() {
}

window_node_list* window_capture_get_window_list() {
    window_node_list *result = calloc(1, sizeof(window_node_list));

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

void window_capture_terminate() {
}
