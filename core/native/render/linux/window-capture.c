#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <X11/Xlib.h>
#include <X11/Xutil.h>

#include "debug.h"
#include "window-capture.h"

#define ACTIVE_WINDOWS "_NET_CLIENT_LIST"

typedef struct {
    Window window;
} window_capture_extra_data;

static Display *display;
static Window root_window;

void window_capture_init() {
    display = XOpenDisplay(NULL);
    root_window = RootWindow(display, DefaultScreen(display));
}

window_node_list* window_capture_get_window_list() {
    Atom atom = XInternAtom(display, ACTIVE_WINDOWS, 1);

    Atom actualType;
    int format;
    unsigned long numItems;
    unsigned long bytesAfter;

    unsigned char *data = '\0';

    Window *list;
    char *windowName;

    int status = XGetWindowProperty(display, root_window, atom, 0L, (~0L), 0,
        AnyPropertyType, &actualType, &format, &numItems, &bytesAfter, &data);

    list = (Window *)data;

    unsigned int valid_item_count = 0;

    char **names = NULL;
    window_capture_extra_data *extra_datum = NULL;

    if (status >= Success && numItems) {
        names = (char**) calloc(numItems, sizeof(char*));
        extra_datum = (window_capture_extra_data*) calloc(numItems, sizeof(window_capture_extra_data));

        for (unsigned int i = 0; i < numItems; ++i) {
            status = XFetchName(display, list[i], &windowName);

            if (status >= Success && windowName) {
                valid_item_count++;

                window_capture_extra_data *extra_data = &extra_datum[i];
                extra_data->window = list[i];

                unsigned int window_name_size = strlen(windowName);
                names[i] = (char*) calloc(1, window_name_size + 1);
                memcpy(names[i], windowName, window_name_size);
            }

            XFree(windowName);
        }
    }

    XFree(data);

    window_node_list *result = calloc(1, sizeof(window_node_list));
    window_node *window_nodes = calloc(valid_item_count, sizeof(window_node));

    int valid_index = 0;

    for (unsigned int i = 0; i < numItems; i++) {
        if (names && names[i] && extra_datum) {
            window_nodes[valid_index].window_name = names[i];

            window_nodes[valid_index].extra_data = (window_capture_extra_data*) calloc(1, sizeof(window_capture_extra_data));
            memcpy(window_nodes[valid_index].extra_data, &extra_datum[i], sizeof(window_capture_extra_data));
            valid_index++;
        }
    }

    result->list = window_nodes;
    result->list_size = valid_item_count;

    free(names);
    free(extra_datum);

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

    Window window = handler_data->window;

    XWindowAttributes window_attributes;
    XGetWindowAttributes(display, window, &window_attributes);

    (*out_width) = window_attributes.width;
    (*out_height) = window_attributes.height;
}

void window_capture_get_image(void *handler, int width, int height, void *out_data) {
    window_capture_extra_data* handler_data = (window_capture_extra_data*) handler;

    Window window = handler_data->window;

    XImage *x_img = XGetImage(display, window, 0, 0, width, height, AllPlanes, ZPixmap);

    memcpy(out_data, x_img->data, width * height * 4);

    XFree(x_img);
}

void window_capture_terminate() {
}
