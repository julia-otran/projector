#include <string.h>
#include <stdlib.h>

#include "tinycthread.h"
#include "render.h"
#include "debug.h"
#include "render-image.h"
#include "ogl-loader.h"
#include "render-fader.h"
#include "render-pixel-unpack-buffer.h"

typedef struct {
    int render_id, width, height, updated, cleared, buffer_size, crop;
    void *pixel_data;
} image_info;

typedef struct {
    int width, height, crop;
} simple_image_info;

static int initialized = 0;
static int src_crop;

static mtx_t thread_mutex;

static int renders_count;

static render_layer *renders;
static image_info *input_images;

static render_fader_instance **fader_instances;
static render_pixel_unpack_buffer_instance **buffer_instances;

void render_image_initialize() {
    mtx_init(&thread_mutex, 0);
    initialized = 1;
}

void render_image_set_config(render_layer *in_renders, int count) {
    renders = in_renders;
    renders_count = count;

    fader_instances = (render_fader_instance**) calloc(count, sizeof(render_fader_instance*));
    input_images = (image_info*) calloc(count, sizeof(image_info));

    for (int i = 0; i < count; i++) {
        render_fader_init(&fader_instances[i]);
        input_images[i].render_id = renders[i].config.render_id;
    }
}

void render_image_set_image_multi(void *pixel_data, int width, int height, int render_id, int crop) {
    mtx_lock(&thread_mutex);

    for (int i = 0; i < renders_count; i++) {
        if (input_images[i].render_id == render_id) {
            int min_size = width * height * 4;

            if (input_images[i].buffer_size < min_size) {
                if (input_images[i].pixel_data) {
                    free(input_images[i].pixel_data);
                }

                input_images[i].pixel_data = malloc(min_size);
            }

            input_images[i].width = width;
            input_images[i].height = height;
            input_images[i].crop = crop;

            if (pixel_data != NULL) {
                memcpy(input_images[i].pixel_data, pixel_data, min_size);
                input_images[i].updated = 1;
                input_images[i].cleared = 0;
            } else {
                input_images[i].updated = 0;
                input_images[i].cleared = 1;
            }
        }
    }

    mtx_unlock(&thread_mutex);
}

void render_image_create_buffers() {
    buffer_instances = (render_pixel_unpack_buffer_instance**) calloc(renders_count, sizeof(render_pixel_unpack_buffer_instance*));

    for (int i = 0; i < renders_count; i++) {
        render_pixel_unpack_buffer_create(&buffer_instances[i]);
        render_pixel_unpack_buffer_allocate_extra_data(buffer_instances[i], sizeof(simple_image_info));
    }
}

void render_image_flush_buffers() {
    for (int i = 0; i < renders_count; i++) {
        render_pixel_unpack_buffer_flush(buffer_instances[i]);
    }
}

void render_image_deallocate_buffers() {
    for (int i = 0; i < renders_count; i++) {
        render_pixel_unpack_buffer_free_extra_data(buffer_instances[i]);
        render_pixel_unpack_buffer_deallocate(buffer_instances[i]);
    }

    free(buffer_instances);
    buffer_instances = NULL;
}

void render_image_update_buffers() {
    for (int i = 0; i < renders_count; i++) {
        int buffer_updated = 0;

        render_pixel_unpack_buffer_node *buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instances[i]);

        mtx_lock(&thread_mutex);

        image_info *info = &input_images[i];

        if (info->updated > 0) {
            int width = info->width;
            int height = info->height;

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

            if (buffer->width != width || buffer->height != height) {
                glBufferData(GL_PIXEL_UNPACK_BUFFER, width * height * 4, 0, GL_DYNAMIC_DRAW);
                buffer->width = width;
                buffer->height = height;
            }

            void *data = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);

            // At the present moment, text_data indexes matches renders indexes
            memcpy(data, info->pixel_data, width * height * 4);

            glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

            simple_image_info *simple_info = (simple_image_info*) buffer->extra_data;

            simple_info->width = width;
            simple_info->height = height;
            simple_info->crop = info->crop;

            buffer_updated = 1;
            info->updated = 0;
        }

        mtx_unlock(&thread_mutex);

        if (buffer_updated) {
            render_pixel_unpack_buffer_enqueue_for_read(buffer_instances[i], buffer);
        } else {
            render_pixel_unpack_buffer_enqueue_for_write(buffer_instances[i], buffer);
        }
    }
}

void render_image_create_assets() {
}

void render_image_update_assets() {
    for (int i = 0; i < renders_count; i++) {
        if (input_images[i].cleared) {
            render_fader_fade_in_out(fader_instances[i], 0, RENDER_FADER_DEFAULT_TIME_MS);
            input_images[i].cleared = 0;
        } else {
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

                simple_image_info *info = (simple_image_info*) malloc(sizeof(simple_image_info));
                info->width = buffer->width;
                info->height = buffer->height;
                info->crop = ((simple_image_info*)buffer->extra_data)->crop;

                render_fader_fade_in_out_data(fader_instances[i], texture_id, RENDER_FADER_DEFAULT_TIME_MS, (void*)info);
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

                if (node->extra_data) {
                    free(node->extra_data);
                    node->extra_data = NULL;
                }

                node->mode = RENDER_FADER_MODE_DELETE;
            }
        }

        render_fader_cleanup(fader_instances[i]);
    }
}

void render_image_render(render_layer *layer) {
    float x, y, w, h;

    glEnableClientState(GL_VERTEX_ARRAY);

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    for (int i = 0; i < renders_count; i++) {
        if (renders[i].config.render_id == layer->config.render_id) {
            render_fader_for_each(fader_instances[i]) {
                if (node->fade_id) {
                    simple_image_info *info = (simple_image_info*) node->extra_data;

                    int dst_width = info->width;
                    int dst_height = info->height;

                    float w_scale = (dst_width / (float)dst_height);
                    float h_scale = (dst_height / (float)dst_width);

                    float w_sz = layer->config.h * w_scale;
                    float h_sz = layer->config.w * h_scale;

                    if (info->crop) {
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

                    glBindTexture(GL_TEXTURE_2D, node->fade_id);

                    float alpha = render_fader_get_alpha(node);

                    alpha = (-1.0f * alpha * alpha) + 2.0f * alpha;

                    glColor4f(1.0, 1.0, 1.0, alpha);

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

    glDisableClientState(GL_VERTEX_ARRAY);
    glBindTexture(GL_TEXTURE_2D, 0);
}

void render_image_deallocate_assets() {
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

void render_image_shutdown() {
    for (int i = 0; i < renders_count; i++) {
        render_fader_terminate(fader_instances[i]);

        if (input_images[i].pixel_data) {
            free(input_images[i].pixel_data);
            input_images[i].pixel_data = NULL;
        }
    }

    free(input_images);
    free(fader_instances);
    fader_instances = NULL;
}
