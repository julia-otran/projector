#include <pthread.h>
#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>

#include "debug.h"
#include "ogl-loader.h"
#include "render-fader.h"
#include "render-video.h"
#include "render-pixel-unpack-buffer.h"

static int src_crop;
static void *src_buffer;
static int src_width;
static int src_height;
static int src_render;
static int src_update_buffer;

static int dst_width = 0;
static int dst_height = 0;

static pthread_mutex_t thread_mutex;
static pthread_cond_t thread_cond;

static render_fader_instance *fader_instance;
static render_pixel_unpack_buffer_instance *buffer_instance;

static GLuint texture_id;

void render_video_initialize() {
    src_crop = 0;
    src_update_buffer = 0;
    src_render = 0;

    pthread_mutex_init(&thread_mutex, 0);
    pthread_cond_init(&thread_cond, 0);

    render_fader_init(&fader_instance);
}

void render_video_src_set_crop_video(int in_crop) {
    src_crop = in_crop;
}

void render_video_src_set_buffer(void *buffer, int width, int height) {
    src_buffer = buffer;
    src_width = width;
    src_height = height;
}

void render_video_src_set_render(int render) {
    src_render = render;
}

void render_video_src_buffer_update() {
    pthread_mutex_lock(&thread_mutex);

    if (src_update_buffer == 0) {
        src_update_buffer = 1;
        pthread_cond_wait(&thread_cond, &thread_mutex);
    }

    pthread_mutex_unlock(&thread_mutex);
}

void render_video_create_buffers() {
    render_pixel_unpack_buffer_create(&buffer_instance);
}

void render_video_deallocate_buffers() {
    render_pixel_unpack_buffer_deallocate(buffer_instance);
    buffer_instance = NULL;
}

void render_video_update_buffers() {
    int buffer_updated = 0;

    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instance);

    pthread_mutex_lock(&thread_mutex);

    if (src_update_buffer) {
        src_update_buffer = 0;

        if (buffer) {
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

            if (buffer->width != src_width || buffer->height != src_height) {
                glBufferData(GL_PIXEL_UNPACK_BUFFER, src_width * src_height * 4, 0, GL_DYNAMIC_DRAW);
                buffer->width = src_width;
                buffer->height = src_height;
            }

            buffer_updated = 1;

            void *data = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);
            memcpy(data, src_buffer, src_width * src_height * 4);
            glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        }

        pthread_cond_signal(&thread_cond);
    }

    pthread_mutex_unlock(&thread_mutex);

    if (buffer_updated) {
        render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, buffer);
    } else {
        render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);
    }
}

void render_video_create_assets() {
    glGenTextures(1, &texture_id);
}

void render_video_render(render_layer *layer) {
    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instance);

    if (buffer) {
        dst_width = buffer->width;
        dst_height = buffer->height;

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);
        glBindTexture(GL_TEXTURE_2D, texture_id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, dst_width, dst_height, 0, GL_BGRA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);

    if (dst_width <= 0 || dst_height <= 0) {
        return;
    }

    float x, y, w, h;

    float w_scale = (dst_width / (float)dst_height);
    float h_scale = (dst_height / (float)dst_width);

    float w_sz = layer->config.h * w_scale;
    float h_sz = layer->config.w * h_scale;

    if (src_crop) {
        if (w_sz > layer->config.w) {
            w = w_sz;
            h = h_scale * w;
        } else {
            h = h_sz;
            w = w_scale * h;
        }
    } else {
        if (w_sz < layer->config.w) {
            w = w_sz;
            h = h_scale * w;
        } else {
            h = h_sz;
            w = w_scale * h;
        }
    }

    x = (layer->config.w - w) / 2;
    y = (layer->config.h - h) / 2;

    glEnableClientState(GL_VERTEX_ARRAY);
    glEnable(GL_TEXTURE_2D);
    glColor4f(1.0, 1.0, 1.0, 1.0);

    glBindTexture(GL_TEXTURE_2D, texture_id);

    glBegin(GL_QUADS);

    // TODO: Fader
    // render_fader_set_alpha(node);

    glTexCoord2f(0.0, 0.0); glVertex2d(x, y);
    glTexCoord2f(0.0, 1.0); glVertex2d(x, y + h);
    glTexCoord2f(1.0, 1.0); glVertex2d(x + w, y + h);
    glTexCoord2f(1.0, 0.0); glVertex2d(x + w, y);

    glEnd();

    glBindTexture(GL_TEXTURE_2D, 0);
}
void render_video_deallocate_assets() {
    glDeleteTextures(1, &texture_id);
}

void render_video_shutdown() {
    render_fader_terminate(fader_instance);
}
