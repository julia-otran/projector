#ifndef _WINDOW_CAPTURE_H
#define _WINDOW_CAPTURE_H

typedef struct {
    char *window_name;
    void *extra_data;
} window_node;

typedef struct {
    window_node *list;
    unsigned int list_size;
} window_node_list;

void window_capture_init();

window_node_list* window_capture_get_window_list();
void window_capture_free_window_list(window_node_list* list);

void window_capture_terminate();

#endif
