#include <string.h>
#include <math.h>

#include "tinycthread.h"
#include "render.h"
#include "debug.h"
#include "render-text.h"
#include "ogl-loader.h"
#include "render-fader.h"
#include "render-pixel-unpack-buffer.h"
#include "render-text-outline-shader.h"

static int initialized = 0;
static int width, height;

static mtx_t thread_mutex;

static void *share_pixel_data;
static int pixel_data_changed;
static int clear_text;

static render_fader_instance *fader_instance;
static render_pixel_unpack_buffer_instance *buffer_instance;

void render_text_initialize() {
    mtx_init(&thread_mutex, 0);

    render_fader_init(&fader_instance);

    initialized = 1;
}

void render_text_set_size(int width_in, int height_in) {
    if (width != width_in || height != height_in) {
        mtx_lock(&thread_mutex);

        width = width_in;
        height = height_in;

        if (share_pixel_data) {
            free(share_pixel_data);
        }

        share_pixel_data = malloc(width_in * height_in * 4);
        pixel_data_changed = 0;
        clear_text = 0;

        mtx_unlock(&thread_mutex);
    }
}

void render_text_set_image(void *pixel_data) {
    mtx_lock(&thread_mutex);

    if (pixel_data) {
        memcpy(share_pixel_data, pixel_data, width * height * 4);
        clear_text = 0;
        pixel_data_changed = 1;
    } else {
        log_debug("Clear text received\n");
        clear_text = 1;
    }

    mtx_unlock(&thread_mutex);
}

void render_text_create_buffers() {
    render_pixel_unpack_buffer_create(&buffer_instance);
}

void render_text_deallocate_buffers() {
    render_pixel_unpack_buffer_deallocate(buffer_instance);
    buffer_instance = NULL;
}

void render_text_update_buffers() {
    int buffer_updated = 0;

    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instance);

    mtx_lock(&thread_mutex);

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

    mtx_unlock(&thread_mutex);

    if (buffer_updated) {
        render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, buffer);
    } else {
        render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);
    }
}

void render_text_create_assets() {
    render_text_outline_shader_initialize(width, height);
}

void render_text_update_assets() {
    if (clear_text) {
        render_fader_fade_in_out(fader_instance, 0, RENDER_FADER_DEFAULT_TIME_MS);
    } else {
        // TODO: add a debouce. if text changes too fast we may allocate too many textures
        render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instance);

        if (buffer) {
            GLuint texture_id = 0;
            glGenTextures(1, &texture_id);

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);
            glBindTexture(GL_TEXTURE_2D, texture_id);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_BGRA, GL_UNSIGNED_BYTE, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            glBindTexture(GL_TEXTURE_2D, 0);
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

            render_fader_fade_in_out(fader_instance, texture_id, RENDER_FADER_DEFAULT_TIME_MS);
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

void render_text_start(render_layer *layer) {
    render_text_outline_shader_start(layer);
}

void render_text_render(render_layer *layer) {
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnable(GL_TEXTURE_2D);

    render_fader_for_each(fader_instance) {
        if (node->fade_id) {
            glBindTexture(GL_TEXTURE_2D, node->fade_id);
            render_text_outline_shader_render(layer);
        }
    }

    glBindTexture(GL_TEXTURE_2D, 0);
}

void render_text_stop(render_layer *layer) {
    render_text_outline_shader_stop(layer);
}

void render_text_deallocate_assets() {
    render_text_outline_shader_shutdown();

    render_fader_for_each(fader_instance) {
        GLuint tex_id = (unsigned int) node->fade_id;

        if (tex_id) {
            glDeleteTextures(1, &tex_id);
        }
    }

    render_fader_cleanup(fader_instance);
}

void render_text_shutdown() {
    if (share_pixel_data) {
        free(share_pixel_data);
    }

    render_fader_terminate(fader_instance);
}
