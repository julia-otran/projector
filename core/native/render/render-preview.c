#include "tinycthread.h"
#include <string.h>
#include <stdlib.h>
#include "ogl-loader.h"
#include "render-preview.h"
#include "render-pixel-unpack-buffer.h"

#define BYTES_PER_PIXEL 4

static mtx_t thread_mutex;
static render_preview* previews;
static int count_previews;

static render_preview_buffer* buffers;
static int count_buffers;

void render_preview_initialize() {
    mtx_init(&thread_mutex, 0);

    previews = NULL;
    count_previews = 0;

    buffers = NULL;
    count_buffers = 0;
}

void render_preview_free() {
    for (int i = 0; i < count_previews; i++) {
        render_preview* preview = &previews[i];
        free(preview->data_buffer);
    }

    free(previews);
    count_previews = 0;
}

void render_preview_set_renders(render_layer* renders, int count_renders) {
    mtx_lock(&thread_mutex);

    if (previews) {
        render_preview_free();
    }

    count_previews = count_renders;
    previews = (render_preview*) calloc(count_previews, sizeof(render_preview));

    for (int i = 0; i < count_previews; i++) {
        render_preview* preview = &previews[i];
        render_layer* render = &renders[i];

        preview->render_id = render->config.render_id;
        preview->width = render->config.w;
        preview->height = render->config.h;

        int required_size = render->config.w * render->config.h * BYTES_PER_PIXEL;
        preview->data_buffer = malloc((required_size + 511) & ~255);
        preview->data_buffer_aligned = (void*)(((unsigned long long)preview->data_buffer + 255) & ~255);
    }

    mtx_unlock(&thread_mutex);
}

void render_preview_download_buffer(int render_id, void* buffer) {
    mtx_lock(&thread_mutex);

    for (int i = 0; i < count_previews; i++) {
        render_preview* preview = &previews[i];

        if (preview->render_id == render_id && preview->buffer_read == 0) {
            memcpy(buffer, preview->data_buffer_aligned, preview->width * preview->height * BYTES_PER_PIXEL);
            preview->buffer_read = 1;
        }
    }
    
    mtx_unlock(&thread_mutex);
}

void render_preview_create_buffers() {
    count_buffers = count_previews;
    
    if (buffers) {
        free(buffers);
    }

    buffers = (render_preview_buffer*) calloc(count_buffers, sizeof(render_preview_buffer));

    for (int i = 0; i < count_buffers; i++) {
        render_preview_buffer* buffer = &buffers[i];
        
        buffer->render_id = previews[i].render_id;
        buffer->readed = 0;

        glGenBuffers(1, &buffer->buffer);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, buffer->buffer);
        glBufferData(GL_PIXEL_PACK_BUFFER, previews[i].width * previews[i].height * BYTES_PER_PIXEL, NULL, GL_STREAM_READ);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    }
}

void render_preview_update_buffers() {
    if (mtx_trylock(&thread_mutex) == thrd_success) {

        for (int i = 0; i < count_previews; i++) {
            render_preview* preview = &previews[i];
            render_preview_buffer* buffer = &buffers[i];

            if (preview->buffer_read == 0) {
                continue;
            }

            glBindBuffer(GL_PIXEL_PACK_BUFFER, buffer->buffer);
            void* data = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY);
            memcpy(preview->data_buffer_aligned, data, preview->width * preview->height * BYTES_PER_PIXEL);
            glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

            buffer->readed = 1;
            preview->buffer_read = 0;
        }

        mtx_unlock(&thread_mutex);
    }
}

void render_preview_deallocate_buffers() {
    count_buffers = 0;

    for (int i = 0; i < count_buffers; i++) {
        glDeleteBuffers(1, &buffers[i].buffer);
    };

    free(buffers);
    buffers = NULL;    
}

void render_preview_create_assets() {}
void render_preview_update_assets() {}
void render_preview_deallocate_assets() {}

void render_preview_cycle(render_layer* render) {
    for (int i = 0; i < count_buffers; i++) {
        render_preview_buffer* buffer = &buffers[i];

        if (buffer->render_id != render->config.render_id) {
            continue;
        }

        if (buffer->readed == 0) {
            break;
        }

        glBindBuffer(GL_PIXEL_PACK_BUFFER, buffer->buffer);
        glReadPixels(0, 0, render->config.w, render->config.h, GL_BGRA, GL_UNSIGNED_BYTE, 0L);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

        buffer->readed = 0;
    }
}

void render_preview_shutdown() {
    render_preview_free();
    mtx_destroy(&thread_mutex);
}
