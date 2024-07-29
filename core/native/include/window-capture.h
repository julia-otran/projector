#ifndef _WINDOW_CAPTURE_H
#define _WINDOW_CAPTURE_H

typedef void (*window_capture_list_callback)(char**, int size);

void window_capture_init(window_capture_list_callback fn);

void window_capture_get_window_list();

void* window_capture_get_handler(char *window_name);
void window_capture_free_handler(void *handler);

void window_capture_get_window_size(void *handler, int *out_width, int *out_height);
void window_capture_get_image(void *handler, int width, int height, void *out_data);

void window_capture_terminate();

#endif
