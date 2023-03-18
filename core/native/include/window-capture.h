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

typedef struct {
    int width, height;
    void *data;
} window_image;

void window_capture_init();

window_node_list* window_capture_get_window_list();
void window_capture_free_window_list(window_node_list* list);

void* window_capture_get_handler(char *window_name);
void window_capture_free_handler(void *handler);

void window_capture_get_window_size(void *handler, int *out_width, int *out_height);
void window_capture_get_image(void *handler, int width, int height, void *out_data);

void window_capture_terminate();

#endif
