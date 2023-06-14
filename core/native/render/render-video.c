#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <string.h>

#include "tinycthread.h"
#include "debug.h"
#include "ogl-loader.h"
#include "vlc-loader.h"
#include "render-fader.h"
#include "render-video.h"
#include "render-pixel-unpack-buffer.h"

#define BYTES_PER_PIXEL 4

static int running;

static int src_crop;
static int src_render;
static int src_update_buffer;

static int dst_width = 0;
static int dst_height = 0;
static int dst_render = 0;

static mtx_t thread_mutex;

static render_fader_instance *fader_instance;
static render_pixel_unpack_buffer_instance *buffer_instance;

static struct libvlc_media_player_t* current_player;

static GLuint texture_id;
static int texture_loaded;
static int should_clear;

typedef struct {
    struct libvlc_media_player_t* player;
    int width, height, buffer_size, glew_initialized;
    void* buffer;
    void* raw_buffer;
} render_video_opaque;

typedef struct {
    render_video_opaque *data;
    void *next;
} render_video_opaque_node;

static render_video_opaque_node *opaque_list = NULL;
static GLFWwindow* transfer_window;

void render_video_create_mtx() {
    // I rly don't care about this leak, it only leaks when program closes, so, it does not leaks
    mtx_init(&thread_mutex, 0);
}

void render_video_create_window(GLFWwindow *shared_context) {
    mtx_lock(&thread_mutex);
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_SAMPLES, 0);

    transfer_window = glfwCreateWindow(800, 600, "Projector VLC4j", NULL, shared_context);
    
    mtx_unlock(&thread_mutex);
}

void render_video_destroy_window() {
    mtx_lock(&thread_mutex);
    glfwDestroyWindow(transfer_window);
    transfer_window = NULL;

    for (render_video_opaque_node* node = opaque_list; node; node = node->next) {
        node->data->glew_initialized = 0;
    }

    mtx_unlock(&thread_mutex);
}

void render_video_initialize() {
    src_crop = 0;
    src_update_buffer = 0;
    src_render = 0;
    texture_loaded = 0;
    should_clear = 0;
    dst_width = 0;
    dst_height = 0;

    render_fader_init(&fader_instance);
}

unsigned render_video_format_callback_alloc(
    void** opaque,
    char* chroma,
    unsigned* width,
    unsigned* height,
    unsigned* pitches,
    unsigned* lines
) {
    render_video_opaque* data = (render_video_opaque*)(*opaque);

    chroma[0] = (char)'R';
    chroma[1] = (char)'V';
    chroma[2] = (char)'3';
    chroma[3] = (char)'2';

    mtx_lock(&thread_mutex);

    data->width = (*width);
    data->height = (*height);
    data->buffer_size = ((data->width * data->height * BYTES_PER_PIXEL) + 511) & ~255;

    data->raw_buffer = malloc(data->buffer_size);
    data->buffer = (void*) (((unsigned long long)data->raw_buffer + 255) & ~255);
    data->glew_initialized = 0;

    if (transfer_window != NULL) {
        glfwMakeContextCurrent(transfer_window);
        glewInit();
        data->glew_initialized = 1;
    }

    mtx_unlock(&thread_mutex);

    (*pitches) = (*width) * BYTES_PER_PIXEL;
    (*lines) = (*height);

    return 1;
}

void render_video_format_callback_dealoc(void* opaque) {
    render_video_opaque* data = (render_video_opaque*)opaque;

    if (data->raw_buffer) {
        mtx_lock(&thread_mutex);

        free(data->raw_buffer);

        data->raw_buffer = NULL;
        data->buffer = NULL;
        data->buffer_size = 0;

        mtx_unlock(&thread_mutex);
    }
}

static void* render_video_lock(void* opaque, void** p_pixels)
{
    render_video_opaque* data = (render_video_opaque*)opaque;

    mtx_lock(&thread_mutex);

    if (!running || transfer_window == NULL || data->player != current_player) {
        (*p_pixels) = data->buffer;

        mtx_unlock(&thread_mutex);

        return NULL;
    }

    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instance);

    if (buffer == NULL) {
        (*p_pixels) = data->buffer;

        mtx_unlock(&thread_mutex);

        return NULL;
    }

    glfwMakeContextCurrent(transfer_window);

    if (data->glew_initialized == 0) {
        glewInit();
        data->glew_initialized = 1;
    }

    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

    if (buffer->width != data->width || buffer->height != data->height) {
        glBufferData(GL_PIXEL_UNPACK_BUFFER, data->width * data->height * BYTES_PER_PIXEL, 0, GL_DYNAMIC_DRAW);
        buffer->width = data->width;
        buffer->height = data->height;
    }

    void *pdata = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);
    (*p_pixels) = pdata;

    return (void*)buffer;
}

static void render_video_unlock(void* opaque, void* id, void* const* p_pixels)
{
    if (id != NULL) {
        glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        glfwMakeContextCurrent(NULL);
        mtx_unlock(&thread_mutex);
    }
}

static void render_video_display(void* opaque, void* id)
{
    if (id) {
        mtx_lock(&thread_mutex);
        if (buffer_instance) {
            render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, id);
        }
        mtx_unlock(&thread_mutex);
    }
}

void render_video_attach_player(void *player) {
    render_video_opaque* data = (render_video_opaque*)calloc(1, sizeof(render_video_opaque));
    data->player = player;
    data->buffer = 0;

    libvlc_video_set_callbacks(data->player, render_video_lock, render_video_unlock, render_video_display, (void*)data);
    libvlc_video_set_format_callbacks(data->player, render_video_format_callback_alloc, render_video_format_callback_dealoc);

    render_video_opaque_node *node = (render_video_opaque_node*) malloc(sizeof(render_video_opaque_node));
    node->next = (void*) opaque_list;
    node->data = data;

    opaque_list = node;
}

void render_video_download_preview(void* player, void* data, long buffer_capacity, int *out_width, int *out_height) {
    (*out_width) = 0;
    (*out_height) = 0;

    if (current_player != player) {
        return;
    }

    mtx_lock(&thread_mutex);

    if (current_player == player) {
        mtx_unlock(&thread_mutex);
        return;
    }

    for (render_video_opaque_node *node = opaque_list; node; node = node->next) {
        if (node->data->player == player) {
            if (node->data->buffer && node->data->width && node->data->height) {
                (*out_width) = node->data->width;
                (*out_height) = node->data->height;

                if (node->data->width * node->data->height * BYTES_PER_PIXEL <= buffer_capacity) {
                    memcpy(data, node->data->buffer, node->data->width * node->data->height * BYTES_PER_PIXEL);
                }
            }

            break;
        }
    }

    mtx_unlock(&thread_mutex);
}

void render_video_src_set_render(void *player, int render) {
    mtx_lock(&thread_mutex);

    if (render) {
        current_player = player;
    }

    mtx_unlock(&thread_mutex);

    src_render = render;
}

void render_video_src_set_crop_video(int in_crop) {
    src_crop = in_crop;
}

void render_video_create_buffers() {
    mtx_lock(&thread_mutex);
    render_pixel_unpack_buffer_create(&buffer_instance);
    running = 1;
    mtx_unlock(&thread_mutex);
}

void render_video_deallocate_buffers() {
    mtx_lock(&thread_mutex);
    render_pixel_unpack_buffer_deallocate(buffer_instance);
    buffer_instance = NULL;
    running = 0;
    mtx_unlock(&thread_mutex);
}

void render_video_update_buffers() {

}

void render_video_create_assets() {
    glGenTextures(1, &texture_id);
}

void render_video_update_assets() {
    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instance);

    if (buffer) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

        glBindTexture(GL_TEXTURE_2D, texture_id);

        if (dst_width != buffer->width || dst_height != buffer->height) {
            dst_width = buffer->width;
            dst_height = buffer->height;

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, dst_width, dst_height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, NULL);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        } else {
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, dst_width, dst_height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, NULL);
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        texture_loaded = 1;
    }

    render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);

    if (src_render) {
        dst_render = src_render;

        if (texture_loaded) {
            should_clear = 1;
            render_fader_fade_in(fader_instance, 1, RENDER_FADER_DEFAULT_TIME_MS);
        }
    } else {
        if (should_clear) {
            should_clear = 0;
            texture_loaded = 0;
            render_fader_fade_out(fader_instance, 1, RENDER_FADER_DEFAULT_TIME_MS);
        }

        if (current_player != NULL) {
            render_fader_for_each(fader_instance) {
                if (render_fader_is_hidden(node)) {
                    current_player = NULL;
                }
            }
        }
    }
}

void render_video_render(render_layer *layer) {
    if (dst_width <= 0 || dst_height <= 0) {
        return;
    }

    if (!(dst_render & (1 << layer->config.render_id))) {
        return;
    }

    double x, y, w, h;

    double w_scale = (dst_width / (double)dst_height);
    double h_scale = (dst_height / (double)dst_width);

    double w_sz = layer->config.h * w_scale;
    double h_sz = layer->config.w * h_scale;

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

    render_fader_for_each(fader_instance) {
        float alpha = render_fader_get_alpha(node);

        glColor4f(alpha, alpha, alpha, alpha);

        glBindTexture(GL_TEXTURE_2D, texture_id);

        glBegin(GL_QUADS);

        glTexCoord2d(0.0, 0.0); glVertex2d(x, y);
        glTexCoord2d(0.0, 1.0); glVertex2d(x, y + h);
        glTexCoord2d(1.0, 1.0); glVertex2d(x + w, y + h);
        glTexCoord2d(1.0, 0.0); glVertex2d(x + w, y);

        glEnd();

        glBindTexture(GL_TEXTURE_2D, 0);
    }
}

void render_video_deallocate_assets() {
    glDeleteTextures(1, &texture_id);
}

void render_video_shutdown() {
    render_fader_terminate(fader_instance);
}
