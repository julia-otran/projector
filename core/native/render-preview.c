#include <pthread.h>
#include <string.h>
#include <stdlib.h>
#include "ogl-loader.h"
#include "render-preview.h"
#include "render-pixel-unpack-buffer.h"

static pthread_mutex_t thread_mutex;
static void *data_buffer;
static int width;
static int height;
static render_pixel_unpack_buffer_instance *buffer_instance;

void render_preview_initialize() {
    pthread_mutex_init(&thread_mutex, 0);
}

void render_preview_set_size(int in_width, int in_height) {
    if (data_buffer) {
        free(data_buffer);
    }

    width = in_width;
    height = in_height;

    data_buffer = calloc(width * height, 3);
}

void render_preview_download_buffer(void *buffer) {
    pthread_mutex_lock(&thread_mutex);
    memcpy(buffer, data_buffer, width * height * 3);
    pthread_mutex_unlock(&thread_mutex);
}

void render_preview_create_buffers() {
    render_pixel_unpack_buffer_create(&buffer_instance);

    render_pixel_unpack_buffer_node *buffers = render_pixel_unpack_buffer_get_all_buffers(buffer_instance);

    for (int i = 0; i < RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT; i++) {
        render_pixel_unpack_buffer_node *buffer = &buffers[i];

        glBindBuffer(GL_PIXEL_PACK_BUFFER, buffer->gl_buffer);
        glBufferData(GL_PIXEL_PACK_BUFFER, width * height * 3, NULL, GL_STREAM_READ);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    }
}

void render_preview_update_buffers() {
    render_pixel_unpack_buffer_node *buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instance);

    if (buffer) {
        glBindBuffer(GL_PIXEL_PACK_BUFFER, buffer->gl_buffer);
        void *data = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY);

        pthread_mutex_lock(&thread_mutex);
        memcpy(data_buffer, data, width * height * 3);
        pthread_mutex_unlock(&thread_mutex);

        glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    }

    render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);
}

void render_preview_deallocate_buffers() {
    render_pixel_unpack_buffer_deallocate(buffer_instance);
}

void render_preview_create_assets() {}
void render_preview_update_assets() {}
void render_preview_deallocate_assets() {}

void render_preview_cycle() {
    render_pixel_unpack_buffer_node *buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instance);

    if (buffer) {
        glBindBuffer(GL_PIXEL_PACK_BUFFER, buffer->gl_buffer);
        glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, 0L);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    }

    render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, buffer);
}

void render_preview_shutdown() {
    pthread_mutex_destroy(&thread_mutex);

    if (data_buffer) {
        free(data_buffer);
        data_buffer = NULL;
    }
}
