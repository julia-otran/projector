#include <string.h>
#include <math.h>

#include "tinycthread.h"
#include "render.h"
#include "debug.h"
#include "render-text.h"
#include "ogl-loader.h"
#include "render-fader.h"
#include "render-pixel-unpack-buffer.h"
#include "clock.h"

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
    int x, y, w, h, dark_background;
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
            extra_data->dark_background = text_datum[i].dark_background;

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

void render_text_flush_buffers() {
    for (int i = 0; i < renders_count; i++) {
        render_pixel_unpack_buffer_flush(buffer_instances[i]);
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
                tex_set_default_params();

                glBindTexture(GL_TEXTURE_2D, 0);
                glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

                render_fader_fade_in_out_data(fader_instances[i], texture_id, RENDER_FADER_TEXT_TIME_MS, buffer->extra_data);
            }

            render_pixel_unpack_buffer_enqueue_for_flush(buffer_instances[i], buffer);
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
    struct timespec spec;
    get_time(&spec);

    glEnableClientState(GL_VERTEX_ARRAY);

    glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE);

    for (int i = 0; i < renders_count; i++) {
        if (renders[i].config.render_id == layer->config.render_id) {
            float max_darken_bg = 0.0;

            render_fader_for_each(fader_instances[i]) {
                if (node->fade_id) {
                    render_text_extra_data* extra_data = (render_text_extra_data*)node->extra_data;
                    if (extra_data->dark_background) {
                        float light_amount = render_fader_get_alpha_with_time(node, &spec);
                        max_darken_bg += light_amount;
                    }
                }
            }

            if (max_darken_bg > 1.0) {
                max_darken_bg = 1.0;
            }

            if (max_darken_bg > 0.01) {
                glColor4f(
                    .0f,
                    .0f,
                    .0f,
                    max_darken_bg * 0.75f
                );

                glBegin(GL_QUADS);

                glTexCoord2i(0, 0); glVertex2d(0, 0);
                glTexCoord2i(0, 1); glVertex2d(0, layer->config.h);
                glTexCoord2i(1, 1); glVertex2d(layer->config.w, layer->config.h);
                glTexCoord2i(1, 0); glVertex2d(layer->config.w, 0);

                glEnd();
            }

            render_fader_for_each(fader_instances[i]) {
                if (node->fade_id) {
                    render_text_extra_data *extra_data = (render_text_extra_data*) node->extra_data;

                    float x, y, w, h;

                    x = extra_data->x;
                    y = extra_data->y;
                    w = extra_data->w;
                    h = extra_data->h;

                    glBindTexture(GL_TEXTURE_2D, node->fade_id);

                    float light_amount = render_fader_get_alpha_with_time(node, &spec);
                    float text_multiply = (-1.0f * light_amount * light_amount) + 2.0f * light_amount;
                    float alpha = (-0.35f * light_amount * light_amount) + 1.35f * light_amount;

                    glColor4f(
                        layer->config.text_color.r * text_multiply,
                        layer->config.text_color.g * text_multiply,
                        layer->config.text_color.b * text_multiply,
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
    glDisableClientState(GL_VERTEX_ARRAY);
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
