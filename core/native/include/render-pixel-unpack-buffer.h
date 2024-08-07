#include "tinycthread.h"
#include "ogl-loader.h"

#ifndef _RENDER_PIXEL_UNPACK_BUFFER_H_
#define _RENDER_PIXEL_UNPACK_BUFFER_H_

#define RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT 3

typedef struct {
    int width;
    int height;
    int updated;

    void *extra_data;

    GLuint gl_buffer;
    int gl_alloc_size;
} render_pixel_unpack_buffer_node;

typedef struct {
    mtx_t thread_mutex;
    render_pixel_unpack_buffer_node buffers[RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT];
    render_pixel_unpack_buffer_node* read_buffers[RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT];
    render_pixel_unpack_buffer_node* flush_buffers[RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT];
    render_pixel_unpack_buffer_node* write_buffers[RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT];
} render_pixel_unpack_buffer_instance;

void render_pixel_unpack_buffer_create(render_pixel_unpack_buffer_instance **instance_ptr);
void render_pixel_unpack_buffer_deallocate(render_pixel_unpack_buffer_instance *instance);

void render_pixel_unpack_buffer_allocate_extra_data(render_pixel_unpack_buffer_instance *instance, int size);
void render_pixel_unpack_buffer_free_extra_data(render_pixel_unpack_buffer_instance *instance);

render_pixel_unpack_buffer_node* render_pixel_unpack_buffer_get_all_buffers(render_pixel_unpack_buffer_instance *instance);

render_pixel_unpack_buffer_node* render_pixel_unpack_buffer_dequeue_for_read(render_pixel_unpack_buffer_instance *instance);
void render_pixel_unpack_buffer_enqueue_for_flush(render_pixel_unpack_buffer_instance *instance, render_pixel_unpack_buffer_node *buffer_node);
void render_pixel_unpack_buffer_enqueue_for_write(render_pixel_unpack_buffer_instance* instance, render_pixel_unpack_buffer_node* buffer_node);

render_pixel_unpack_buffer_node* render_pixel_unpack_buffer_dequeue_for_write(render_pixel_unpack_buffer_instance *instance);
void render_pixel_unpack_buffer_enqueue_for_read(render_pixel_unpack_buffer_instance *instance, render_pixel_unpack_buffer_node *buffer_node);

void render_pixel_unpack_buffer_flush(render_pixel_unpack_buffer_instance* instance);

#endif
