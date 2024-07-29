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
    char* name;
} window_capture_extra_data;

static window_capture_extra_data *window_capture_extra_datum = NULL;
static unsigned int window_capture_extra_datum_count = 0;

static Display *display;
static Window root_window;

static window_capture_list_callback window_capture_callback_function;

void window_capture_init(window_capture_list_callback fn) {
    window_capture_callback_function = fn;
    display = XOpenDisplay(NULL);
    root_window = RootWindow(display, DefaultScreen(display));
}

void window_capture_get_window_list() {
    for (unsigned int i = 0; i < window_capture_extra_datum_count; i++) {
        window_capture_extra_data *extra_data = &window_capture_extra_datum[i];
        free(extra_data->name);
    }

    free(window_capture_extra_datum);
    window_capture_extra_datum_count = 0;

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

    if (status >= Success && numItems) {
        names = (char**) calloc(numItems, sizeof(char*));
        window_capture_extra_datum = (window_capture_extra_data*) calloc(numItems, sizeof(window_capture_extra_data));

        for (unsigned int i = 0; i < numItems; ++i) {
            status = XFetchName(display, list[i], &windowName);

            if (status >= Success && windowName) {
                window_capture_extra_data *extra_data = &window_capture_extra_datum[valid_item_count];
                extra_data->window = list[i];

                unsigned int window_name_size = strlen(windowName);
                names[valid_item_count] = (char*) calloc(1, window_name_size + 1);
                memcpy(names[valid_item_count], windowName, window_name_size);

                extra_data->name = names[valid_item_count];
                valid_item_count++;
            }

            XFree(windowName);
        }

        window_capture_extra_datum_count = valid_item_count;
    }

    XFree(data);

    window_capture_callback_function(names, valid_item_count);

    free(names);
}

void* window_capture_get_handler(char *window_name) {
    void *handle = NULL;
    void *handle_copy = NULL;

    for (unsigned int i = 0; i < window_capture_extra_datum_count; i++) {
        window_capture_extra_data *extra_data = &window_capture_extra_datum[i];

        if (strcmp(extra_data->name, window_name) == 0) {
            handle = extra_data;
            break;
        }
    }

    if (handle) {
        handle_copy = (void*) calloc(1, sizeof(window_capture_extra_data));
        memcpy(handle_copy, handle, sizeof(window_capture_extra_data));
    }

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
