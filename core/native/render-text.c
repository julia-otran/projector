#include <pthread.h>
#include <string.h>

#include "render.h"
#include "debug.h"
#include "render-text.h"
#include "ogl-loader.h"
#include "render-fader.h"
#include "render-pixel-unpack-buffer.h"

static int initialized = 0;
static int width, height;

static pthread_mutex_t thread_mutex;

static void *share_pixel_data;
static int pixel_data_changed;
static int clear_text;

static render_fader_instance *fader_instance;
static render_pixel_unpack_buffer_instance *buffer_instance;

void render_text_initialize() {
    pthread_mutex_init(&thread_mutex, 0);

    render_fader_init(&fader_instance);

    initialized = 1;
}

void render_text_set_size(int width_in, int height_in) {
    width = width_in;
    height = height_in;
    share_pixel_data = malloc(width_in * height_in * 4);
    pixel_data_changed = 0;
    clear_text = 0;
}

void render_text_set_image(void *pixel_data) {
    pthread_mutex_lock(&thread_mutex);

    if (pixel_data) {
        // memcpy(share_pixel_data, pixel_data, width * height * 4);
        // Adjust ARGB to RGBA
        // Hope this works faster for 64bit systems.
        // Also, I don't think this sw could run on 32bit
        int pointer_count = width * height * 4 / sizeof(unsigned long long*);
        unsigned long long *src_ptr = (unsigned long long*) pixel_data;
        unsigned long long *dst_ptr = (unsigned long long*) share_pixel_data;
        unsigned long long aux;

        for (int i=0; i<pointer_count; i++) {
            // ARGB ARGB << 8
            // RGB0 RGB0
            // ARGB ARGB >> 24
            // 000A RGBA
            aux = (src_ptr[i] << 8) & 0xFFFFFF00FFFFFF00;
            aux = aux | ((src_ptr[i] >> 24) & 0x000000FF000000FF);
            dst_ptr[i] = aux;
        }

        clear_text = 0;
        pixel_data_changed = 1;
    } else {
        clear_text = 1;
    }

    pthread_mutex_unlock(&thread_mutex);
}

void render_text_create_buffers() {
    render_pixel_unpack_buffer_create(&buffer_instance);
}

void render_text_deallocate_buffers() {
    render_pixel_unpack_buffer_deallocate(buffer_instance);
    buffer_instance = NULL;
}

void render_text_update_buffers() {
    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instance);

    pthread_mutex_lock(&thread_mutex);

    if (pixel_data_changed) {
        if (buffer) {
            buffer->updated = 1;

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);
            glBufferData(GL_PIXEL_UNPACK_BUFFER, width * height * 4, share_pixel_data, GL_STREAM_DRAW);
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

            pixel_data_changed = 0;
        }
    }

    pthread_mutex_unlock(&thread_mutex);

    if (buffer && buffer->updated) {
        render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, buffer);
    } else {
        render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);
    }
}

void render_text_create_assets() {
}

void render_text_render(render_layer *layer) {
    if (clear_text) {
        render_fader_fade_in_out(fader_instance, 0, 1000);
    } else {
        // TODO: add a debouce. if text changes too fast we may allocate too many textures
        render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instance);

        if (buffer && buffer->updated) {
            buffer->updated = 0;

            GLuint texture_id = 0;
            glGenTextures(1, &texture_id);

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);
            glBindTexture(GL_TEXTURE_2D, texture_id);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            glBindTexture(GL_TEXTURE_2D, 0);
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

            render_fader_fade_in_out(fader_instance, texture_id, 1000);
        }

        render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);
    }

    float x, y, w, h, xs, ys, ws, hs;

    x = layer->config.text_area.x;
    y = layer->config.text_area.y;
    w = layer->config.text_area.w;
    h = layer->config.text_area.h;

    ws = w * 1.01;
    hs = h * 1.01;
    xs = x - ((ws - w) / 2);
    ys = y - ((hs - h) / 2);

    render_fader_for_each(fader_instance) {
        if (node->fade_id) {
            glBindTexture(GL_TEXTURE_2D, node->fade_id);

            render_fader_set_alpha(node);

            // TODO: Need shader to invert the tex color to create the border of chars
            // Also consider replacing color for the subtitles
            glBegin(GL_QUADS);

            glTexCoord2i(0,0); glVertex2d(xs, ys);
            glTexCoord2i(0, 1); glVertex2d(xs, ys + hs);
            glTexCoord2i(1, 1); glVertex2d(xs + ws, ys + hs);
            glTexCoord2i(1, 0); glVertex2d(xs + ws, ys);

            glEnd();

            glBegin(GL_QUADS);

            glTexCoord2i(0,0); glVertex2d(x, y);
            glTexCoord2i(0, 1); glVertex2d(x, y + h);
            glTexCoord2i(1, 1); glVertex2d(x + w, y + h);
            glTexCoord2i(1, 0); glVertex2d(x + w, y);

            glEnd();
        }
    }

    glBindTexture(GL_TEXTURE_2D, 0);

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

void render_text_deallocate_assets() {
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
