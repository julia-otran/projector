#include "tinycthread.h"
#include "render-pixel-unpack-buffer.h"
#include "debug.h"

void render_pixel_unpack_buffer_create(render_pixel_unpack_buffer_instance **instance_ptr) {
    render_pixel_unpack_buffer_instance *instance = calloc(1, sizeof(render_pixel_unpack_buffer_instance));
    (*instance_ptr) = instance;

    mtx_init(&instance->thread_mutex, 0);

    glGenBuffers(1, &instance->buffers[0].gl_buffer);
    glGenBuffers(1, &instance->buffers[1].gl_buffer);
    glGenBuffers(1, &instance->buffers[2].gl_buffer);

    instance->write_buffers[0] = &instance->buffers[0];
    instance->write_buffers[1] = &instance->buffers[1];
    instance->write_buffers[2] = &instance->buffers[2];
}

void render_pixel_unpack_buffer_deallocate(render_pixel_unpack_buffer_instance *instance) {
    glDeleteBuffers(1, &instance->buffers[0].gl_buffer);
    glDeleteBuffers(1, &instance->buffers[1].gl_buffer);
    glDeleteBuffers(1, &instance->buffers[2].gl_buffer);

    // TODO: destroy all other mutexes in the project. the r never deallocated
    mtx_destroy(&instance->thread_mutex);

    free(instance);
}

void render_pixel_unpack_buffer_allocate_extra_data(render_pixel_unpack_buffer_instance *instance, int size) {
    instance->buffers[0].extra_data = malloc(size);
    instance->buffers[1].extra_data = malloc(size);
    instance->buffers[2].extra_data = malloc(size);
}

void render_pixel_unpack_buffer_free_extra_data(render_pixel_unpack_buffer_instance *instance) {
    free(instance->buffers[0].extra_data);
    free(instance->buffers[1].extra_data);
    free(instance->buffers[2].extra_data);
}

render_pixel_unpack_buffer_node* render_pixel_unpack_buffer_get_all_buffers(render_pixel_unpack_buffer_instance *instance) {
    return (render_pixel_unpack_buffer_node*) &instance->buffers;
}

render_pixel_unpack_buffer_node* render_pixel_unpack_buffer_dequeue_for_read(render_pixel_unpack_buffer_instance *instance) {
    mtx_lock(&instance->thread_mutex);

    render_pixel_unpack_buffer_node *free_buffer = instance->read_buffers[0];
    instance->read_buffers[0] = instance->read_buffers[1];
    instance->read_buffers[1] = instance->read_buffers[2];
    instance->read_buffers[2] = NULL;

    mtx_unlock(&instance->thread_mutex);

    return free_buffer;
}

void render_pixel_unpack_buffer_enqueue_for_flush(render_pixel_unpack_buffer_instance *instance, render_pixel_unpack_buffer_node* buffer_node) {
    if (buffer_node == NULL) {
        return;
    }

    mtx_lock(&instance->thread_mutex);

    if (instance->flush_buffers[0] == NULL) {
        instance->flush_buffers[0] = buffer_node;
    } else if (instance->flush_buffers[1] == NULL) {
        instance->flush_buffers[1] = buffer_node;
    } else if (instance->flush_buffers[2] == NULL) {
        instance->flush_buffers[2] = buffer_node;
    } else {
        log_debug("Pixel pack flush buffers is full. This is not expected. Check for duplicated enqueue for flush calls.\n");
    }

    mtx_unlock(&instance->thread_mutex);
}

render_pixel_unpack_buffer_node* render_pixel_unpack_buffer_dequeue_for_write(render_pixel_unpack_buffer_instance *instance) {
    mtx_lock(&instance->thread_mutex);

    render_pixel_unpack_buffer_node *free_buffer = instance->write_buffers[0];
    instance->write_buffers[0] = instance->write_buffers[1];
    instance->write_buffers[1] = instance->write_buffers[2];
    instance->write_buffers[2] = NULL;

    mtx_unlock(&instance->thread_mutex);

    return free_buffer;
}

void render_pixel_unpack_buffer_enqueue_for_read(render_pixel_unpack_buffer_instance *instance, render_pixel_unpack_buffer_node* buffer_node) {
    if (buffer_node == NULL) {
        return;
    }

    mtx_lock(&instance->thread_mutex);

    if (instance->read_buffers[0] == NULL) {
        instance->read_buffers[0] = buffer_node;
    } else if (instance->read_buffers[1] == NULL) {
        instance->read_buffers[1] = buffer_node;
    } else if (instance->read_buffers[2] == NULL) {
        instance->read_buffers[2] = buffer_node;
    } else {
        log_debug("Pixel pack read buffer is full. This is not expected. Check for duplicated enqueue calls.\n");
    }

    mtx_unlock(&instance->thread_mutex);
}

void render_pixel_unpack_buffer_enqueue_for_write_int(render_pixel_unpack_buffer_instance* instance, render_pixel_unpack_buffer_node* buffer_node) {
    if (buffer_node == NULL) {
        return;
    }

    if (instance->write_buffers[0] == NULL) {
        instance->write_buffers[0] = buffer_node;
    }
    else if (instance->write_buffers[1] == NULL) {
        instance->write_buffers[1] = buffer_node;
    }
    else if (instance->write_buffers[2] == NULL) {
        instance->write_buffers[2] = buffer_node;
    }
    else {
        log_debug("Pixel pack flush buffers is full. This is not expected. Check for duplicated enqueue for flush calls.\n");
    }
}

void render_pixel_unpack_buffer_enqueue_for_write(render_pixel_unpack_buffer_instance* instance, render_pixel_unpack_buffer_node* buffer_node) {
    if (buffer_node == NULL) {
        return;
    }

    mtx_lock(&instance->thread_mutex);
    render_pixel_unpack_buffer_enqueue_for_write_int(instance, buffer_node);
    mtx_unlock(&instance->thread_mutex);
}

void render_pixel_unpack_buffer_flush(render_pixel_unpack_buffer_instance* instance) {
    mtx_lock(&instance->thread_mutex);

    render_pixel_unpack_buffer_enqueue_for_write_int(instance, instance->flush_buffers[0]);
    instance->flush_buffers[0] = NULL;

    render_pixel_unpack_buffer_enqueue_for_write_int(instance, instance->flush_buffers[1]);
    instance->flush_buffers[1] = NULL;

    render_pixel_unpack_buffer_enqueue_for_write_int(instance, instance->flush_buffers[2]);
    instance->flush_buffers[2] = NULL;

    mtx_unlock(&instance->thread_mutex);
}
