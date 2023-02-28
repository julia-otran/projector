#include <pthread.h>
#include "ogl-loader.h"

#ifndef _RENDER_PIXEL_UNPACK_BUFFER_H_
#define _RENDER_PIXEL_UNPACK_BUFFER_H_

#define RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT 3

typedef struct {
    int width;
    int height;
    int updated;

    GLuint gl_buffer;
} render_pixel_unpack_buffer_node;

typedef struct {
    pthread_mutex_t thread_mutex;
    render_pixel_unpack_buffer_node buffers[RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT];
    render_pixel_unpack_buffer_node* read_buffers[RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT];
    render_pixel_unpack_buffer_node* write_buffers[RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT];
} render_pixel_unpack_buffer_instance;

void render_pixel_unpack_buffer_create(render_pixel_unpack_buffer_instance **instance_ptr);
void render_pixel_unpack_buffer_deallocate(render_pixel_unpack_buffer_instance *instance);

render_pixel_unpack_buffer_node* render_pixel_unpack_buffer_dequeue_for_read(render_pixel_unpack_buffer_instance *instance);
void render_pixel_unpack_buffer_enqueue_for_write(render_pixel_unpack_buffer_instance *instance, render_pixel_unpack_buffer_node *buffer_node);

render_pixel_unpack_buffer_node* render_pixel_unpack_buffer_dequeue_for_write(render_pixel_unpack_buffer_instance *instance);
void render_pixel_unpack_buffer_enqueue_for_read(render_pixel_unpack_buffer_instance *instance, render_pixel_unpack_buffer_node *buffer_node);

#endif
