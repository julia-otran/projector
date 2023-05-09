#include <string.h>
#include <math.h>

#include "tinycthread.h"
#include "render.h"
#include "debug.h"
#include "render-text.h"
#include "ogl-loader.h"
#include "render-fader.h"
#include "render-pixel-unpack-buffer.h"

static mtx_t thread_mutex;

static int text_datum_count;
static render_text_data *text_datum;

static int pixel_data_changed;
static int clear_text;

static int renders_count;
static render_layer *renders;

static render_fader_instance **fader_instances;
static render_pixel_unpack_buffer_instance **buffer_instances;

typedef struct {
    int x, y, w, h;
} render_text_extra_data;

void render_text_initialize() {
    mtx_init(&thread_mutex, 0);
}

void render_text_set_config(render_layer *in_renders, int count) {
    renders = in_renders;
    renders_count = count;

    fader_instances = (render_fader_instance**) calloc(count, sizeof(render_fader_instance*));

    for (int i = 0; i < count; i++) {
        render_fader_init(&fader_instances[i]);
    }
}

void render_text_set_data(render_text_data *data, int count) {
    mtx_lock(&thread_mutex);

    for (int i = 0; i < text_datum_count; i++) {
        free(text_datum[i].image_data);
    }

    if (text_datum) {
        free(text_datum);
    }

    text_datum = data;
    text_datum_count = count;

    if (data) {
        if (count != renders_count) {
            log_debug("[BUG] Text data set does not match renders quantity.\n Engine will crash soon.\n");
        }

        clear_text = 0;
        pixel_data_changed = count;
    } else {
        pixel_data_changed = 0;
        clear_text = 1;
    }

    mtx_unlock(&thread_mutex);
}

void render_text_create_buffers() {
    buffer_instances = (render_pixel_unpack_buffer_instance**) calloc(renders_count, sizeof(render_pixel_unpack_buffer_instance*));

    for (int i = 0; i < renders_count; i++) {
        render_pixel_unpack_buffer_create(&buffer_instances[i]);

        render_pixel_unpack_buffer_node *buffers = render_pixel_unpack_buffer_get_all_buffers(buffer_instances[i]);

        for (int i = 0; i < RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT; i++) {
            buffers[i].extra_data = (void*) calloc(1, sizeof(render_text_extra_data));
        }
    }
}

void render_text_deallocate_buffers() {
    for (int i = 0; i < renders_count; i++) {
        render_pixel_unpack_buffer_node *buffers = render_pixel_unpack_buffer_get_all_buffers(buffer_instances[i]);

        for (int i = 0; i < RENDER_PIXEL_UNPACK_BUFFER_BUFFER_COUNT; i++) {
            free(buffers[i].extra_data);
        }

        render_pixel_unpack_buffer_deallocate(buffer_instances[i]);
    }

    free(buffer_instances);
    buffer_instances = NULL;
}

void render_text_update_buffers() {
    for (int i = 0; i < renders_count; i++) {
        int buffer_updated = 0;

        render_pixel_unpack_buffer_node *buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instances[i]);

        mtx_lock(&thread_mutex);

        if (pixel_data_changed > 0) {
            render_text_extra_data *extra_data = (render_text_extra_data*) buffer->extra_data;

            int width = text_datum[i].image_w;
            int height = text_datum[i].image_h;

            extra_data->x = text_datum[i].position_x;
            extra_data->y = text_datum[i].position_y;
            extra_data->w = width;
            extra_data->h = height;

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

            if (buffer->width != width || buffer->height != height) {
                glBufferData(GL_PIXEL_UNPACK_BUFFER, width * height * 4, 0, GL_DYNAMIC_DRAW);
                buffer->width = width;
                buffer->height = height;
            }

            void *data = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);

            // At the present moment, text_data indexes matches renders indexes
            memcpy(data, text_datum[i].image_data, width * height * 4);

            glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

            buffer_updated = 1;
            pixel_data_changed = pixel_data_changed - 1;
        }

        mtx_unlock(&thread_mutex);

        if (buffer_updated) {
            render_pixel_unpack_buffer_enqueue_for_read(buffer_instances[i], buffer);
        } else {
            render_pixel_unpack_buffer_enqueue_for_write(buffer_instances[i], buffer);
        }
    }
}

void render_text_create_assets() {
}

void render_text_do_clear() {
    for (int i = 0; i < renders_count; i++) {
        render_fader_fade_in_out(fader_instances[i], 0, RENDER_FADER_DEFAULT_TIME_MS);
    }
}

void render_text_update_assets() {
    if (clear_text) {
        render_text_do_clear();
    } else {
        for (int i = 0; i < renders_count; i++) {
            // TODO: add a debouce. if text changes too fast we may allocate too many textures
            render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instances[i]);

            if (buffer) {
                GLuint texture_id = 0;
                glGenTextures(1, &texture_id);

                glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);
                glBindTexture(GL_TEXTURE_2D, texture_id);

                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, buffer->width, buffer->height, 0, GL_BGRA, GL_UNSIGNED_BYTE, 0);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

                glBindTexture(GL_TEXTURE_2D, 0);
                glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

                render_fader_fade_in_out_data(fader_instances[i], texture_id, RENDER_FADER_DEFAULT_TIME_MS, buffer->extra_data);
            }

            render_pixel_unpack_buffer_enqueue_for_write(buffer_instances[i], buffer);
        }
    }

    for (int i = 0; i < renders_count; i++) {
        render_fader_for_each(fader_instances[i]) {
            if (render_fader_is_hidden(node)) {
                GLuint tex_id = (unsigned int) node->fade_id;

                if (tex_id) {
                    glDeleteTextures(1, &tex_id);
                }

                node->mode = RENDER_FADER_MODE_DELETE;
            }
        }

        render_fader_cleanup(fader_instances[i]);
    }
}

void render_text_start(render_layer *layer) {
}

void render_text_render(render_layer *layer) {
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnable(GL_TEXTURE_2D);

    glBlendFunc(GL_ONE_MINUS_CONSTANT_COLOR, GL_ONE_MINUS_SRC_ALPHA);

    for (int i = 0; i < renders_count; i++) {
        if (renders[i].config.render_id == layer->config.render_id) {
            render_fader_for_each(fader_instances[i]) {
                if (node->fade_id) {
                    render_text_extra_data *extra_data = (render_text_extra_data*) node->extra_data;

                    float x, y, w, h;

                    x = extra_data->x;
                    y = extra_data->y;
                    w = extra_data->w;
                    h = extra_data->h;

                    glBindTexture(GL_TEXTURE_2D, node->fade_id);

                    float alpha = render_fader_get_alpha(node);

                    alpha = (-1.0f * alpha * alpha) + 2.0f * alpha;

                    glColor4f(
                        layer->config.text_color.r * alpha,
                        layer->config.text_color.g * alpha,
                        layer->config.text_color.b * alpha,
                        alpha
                    );

                    glBegin(GL_QUADS);

                    glTexCoord2i(0, 0); glVertex2d(x, y);
                    glTexCoord2i(0, 1); glVertex2d(x, y + h);
                    glTexCoord2i(1, 1); glVertex2d(x + w, y + h);
                    glTexCoord2i(1, 0); glVertex2d(x + w, y);

                    glEnd();
                }
            }
        }
    }

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void render_text_stop(render_layer *layer) {
}

void render_text_deallocate_assets() {
    for (int i = 0; i < renders_count; i++) {
        render_fader_for_each(fader_instances[i]) {
            GLuint tex_id = (unsigned int) node->fade_id;

            if (tex_id) {
                glDeleteTextures(1, &tex_id);
            }

            node->mode = RENDER_FADER_MODE_DELETE;
        }

        render_fader_cleanup(fader_instances[i]);
    }
}

void render_text_shutdown() {
    for (int i = 0; i < renders_count; i++) {
        render_fader_terminate(fader_instances[i]);
    }

    free(fader_instances);
    fader_instances = NULL;
}
