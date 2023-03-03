#include <pthread.h>
#include <string.h>
#include <stdlib.h>

#include "render.h"
#include "debug.h"
#include "render-image.h"
#include "ogl-loader.h"
#include "render-fader.h"
#include "render-pixel-unpack-buffer.h"

static int initialized = 0;
static int width, height, crop;
static int dst_width, dst_height;

static pthread_mutex_t thread_mutex;

static void *share_pixel_data;
static int pixel_data_changed;
static int clear_image;

static render_fader_instance *fader_instance;
static render_pixel_unpack_buffer_instance *buffer_instance;

void render_image_initialize() {
    pthread_mutex_init(&thread_mutex, 0);

    render_fader_init(&fader_instance);

    initialized = 1;
}

void render_image_set_image(void *pixel_data, int width_in, int height_in, int crop_in) {
    pthread_mutex_lock(&thread_mutex);

    crop = crop_in;

    if (pixel_data) {
        if (width != width_in || height != height_in) {
            width = width_in;
            height = height_in;

            if (share_pixel_data) {
                free(share_pixel_data);
            }

            share_pixel_data = malloc(width * height * 4);
        }

        memcpy(share_pixel_data, pixel_data, width * height * 4);

        clear_image = 0;
        pixel_data_changed = 1;
    } else {
        clear_image = 1;
    }

    pthread_mutex_unlock(&thread_mutex);
}

void render_image_create_buffers() {
    render_pixel_unpack_buffer_create(&buffer_instance);
}

void render_image_deallocate_buffers() {
    render_pixel_unpack_buffer_deallocate(buffer_instance);
    buffer_instance = NULL;
}

void render_image_update_buffers() {
    int buffer_updated = 0;

    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instance);

    pthread_mutex_lock(&thread_mutex);

    if (pixel_data_changed && buffer) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

        if (buffer->width != width || buffer->height != height) {
            glBufferData(GL_PIXEL_UNPACK_BUFFER, width * height * 4, 0, GL_DYNAMIC_DRAW);
            buffer->width = width;
            buffer->height = height;
        }

        void *data = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);
        memcpy(data, share_pixel_data, width * height * 4);
        glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        buffer_updated = 1;
        pixel_data_changed = 0;
    }

    pthread_mutex_unlock(&thread_mutex);

    if (buffer_updated) {
        render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, buffer);
    } else {
        render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);
    }
}

void render_image_create_assets() {
}

void render_image_update_assets() {
    if (clear_image) {
        render_fader_fade_in_out(fader_instance, 0, RENDER_FADER_DEFAULT_TIME_MS);
    } else {
        // TODO: add a debouce. if image changes too fast we may allocate too many imageures
        render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instance);

        if (buffer) {
            GLuint image_texture_id = 0;

            dst_width = buffer->width;
            dst_height = buffer->height;

            glEnable(GL_TEXTURE_2D);

            glGenTextures(1, &image_texture_id);

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);
            glBindTexture(GL_TEXTURE_2D, image_texture_id);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, buffer->width, buffer->height, 0, GL_BGRA, GL_UNSIGNED_BYTE, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

            glBindTexture(GL_TEXTURE_2D, 0);
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

            render_fader_fade_in_out(fader_instance, image_texture_id, RENDER_FADER_DEFAULT_TIME_MS);
        }

        render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);
    }

    render_fader_for_each(fader_instance) {
        if (render_fader_is_hidden(node)) {
            GLuint tex_id = (unsigned int) node->fade_id;

            if (tex_id) {
                glDeleteTextures(1, &tex_id);
            }
        }
    }

    render_fader_cleanup(fader_instance);
}

void render_image_render(render_layer *layer) {
    if (dst_width <= 0 || dst_height <= 0) {
        return;
    }

    float x, y, w, h;

    float w_scale = (dst_width / (float)dst_height);
    float h_scale = (dst_height / (float)dst_width);

    float w_sz = layer->config.h * w_scale;
    float h_sz = layer->config.w * h_scale;

    if (crop) {
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

    render_fader_for_each(fader_instance) {
        if (node->fade_id) {
            glBindTexture(GL_TEXTURE_2D, node->fade_id);

            float alpha = render_fader_get_alpha(node);

            glColor4f(alpha, alpha, alpha, alpha);

            glBegin(GL_QUADS);

            glTexCoord2i(0,0); glVertex2d(x, y);
            glTexCoord2i(0, 1); glVertex2d(x, y + h);
            glTexCoord2i(1, 1); glVertex2d(x + w, y + h);
            glTexCoord2i(1, 0); glVertex2d(x + w, y);

            glEnd();
        }
    }

    glBindTexture(GL_TEXTURE_2D, 0);
}

void render_image_deallocate_assets() {
    render_fader_for_each(fader_instance) {
        GLuint tex_id = (unsigned int) node->fade_id;

        if (tex_id) {
            glDeleteTextures(1, &tex_id);
        }
    }

    render_fader_cleanup(fader_instance);
}

void render_image_shutdown() {
    if (share_pixel_data) {
        free(share_pixel_data);
    }

    render_fader_terminate(fader_instance);
}
