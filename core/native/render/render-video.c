#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <string.h>

#include "tinycthread.h"
#include "debug.h"
#include "ogl-loader.h"
#include "render-fader.h"
#include "render-video.h"
#include "render-pixel-unpack-buffer.h"

#ifdef _WIN32
#define ssize_t SSIZE_T
#endif

#ifdef __gnu_linux__
#define ssize_t size_t
#endif

#include "vlc/vlc.h"

#define BYTES_PER_PIXEL 4

static int src_crop;
static void *src_buffer;
static int src_width;
static int src_height;
static int src_render;
static int src_update_buffer;

static int dst_width = 0;
static int dst_height = 0;
static int dst_render = 0;

static mtx_t thread_mutex;
static cnd_t thread_cond;

static render_fader_instance *fader_instance;
static render_pixel_unpack_buffer_instance *buffer_instance;

static struct libvlc_media_player_t* current_player;

static GLuint texture_id;
static int texture_loaded;
static int should_clear;

typedef struct {
    struct libvlc_media_player_t* player;
    int width, height, buffer_size;
    void* buffer;
    void* raw_buffer;
} render_video_opaque;

void render_video_initialize() {
    src_crop = 0;
    src_update_buffer = 0;
    src_render = 0;
    texture_loaded = 0;
    should_clear = 0;

    mtx_init(&thread_mutex, 0);
    cnd_init(&thread_cond);

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

    data->width = (*width);
    data->height = (*height);
    data->buffer_size = ((data->width * data->height * BYTES_PER_PIXEL) + 63) & ~63;
    
    data->raw_buffer = malloc(data->buffer_size + 63);
    data->buffer = ((unsigned long long)data->raw_buffer) & ~63;

    // VirtualLock(data->buffer, data->buffer_size);

    (*pitches) = (*width) * BYTES_PER_PIXEL;
    (*lines) = (*height);

    return 1;
}

void render_video_format_callback_dealoc(void* opaque) {
    render_video_opaque* data = (render_video_opaque*)opaque;

    if (data->raw_buffer) {
        // VirtualUnlock(data->buffer, data->buffer_size);
        free(data->raw_buffer);
    }
}

static void* render_video_lock(void* opaque, void** p_pixels)
{
    render_video_opaque* data = (render_video_opaque*)opaque;

    (*p_pixels) = data->buffer;
    
    mtx_lock(&thread_mutex);
    src_update_buffer = 0;
    mtx_unlock(&thread_mutex);

    return NULL;
}

static void render_video_unlock(void* opaque, void* id, void* const* p_pixels)
{
    render_video_opaque* data = (render_video_opaque*)opaque;
    
    mtx_lock(&thread_mutex);

    if (data->player == current_player) {
        src_buffer = data->buffer;
        src_width = data->width;
        src_height = data->height;
        src_update_buffer = 1;
    }

    mtx_unlock(&thread_mutex);
}

static void render_video_display(void* opaque, void* id)
{
}

void render_video_attach_player(void *player) {
    render_video_opaque* data = (render_video_opaque*)calloc(1, sizeof(render_video_opaque));
    data->player = player;
    data->buffer = 0;

    libvlc_video_set_callbacks(data->player, render_video_lock, render_video_unlock, render_video_display, (void*)data);
    libvlc_video_set_format_callbacks(data->player, render_video_format_callback_alloc, render_video_format_callback_dealoc);
}

void render_video_src_set_render(void *player, int render) {
    mtx_lock(&thread_mutex);
    current_player = player;
    src_render = render;
    mtx_unlock(&thread_mutex);
}

void render_video_src_set_crop_video(int in_crop) {
    src_crop = in_crop;
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

    mtx_lock(&thread_mutex);

    if (src_update_buffer) {
        src_update_buffer = 0;

        if (buffer) {
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

            if (buffer->width != src_width || buffer->height != src_height) {
                glBufferData(GL_PIXEL_UNPACK_BUFFER, src_width * src_height * BYTES_PER_PIXEL, 0, GL_DYNAMIC_DRAW);
                buffer->width = src_width;
                buffer->height = src_height;
            }

            buffer_updated = 1;

            void *data = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);
            memcpy(data, src_buffer, src_width * src_height * BYTES_PER_PIXEL);
            glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);

            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        }
    }

    mtx_unlock(&thread_mutex);

    if (buffer_updated) {
        render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, buffer);
    } else {
        render_pixel_unpack_buffer_enqueue_for_write(buffer_instance, buffer);
    }
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
