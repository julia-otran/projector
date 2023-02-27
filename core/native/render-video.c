#include <pthread.h>
#include <math.h>

#include "ogl-loader.h"
#include "render-fader.h"
#include "render-video.h"

static int src_crop;
static void *src_buffer;
static int src_width;
static int src_height;
static int src_render;
static int src_update_buffer;

static int dst_width;
static int dst_height;
static int dst_render;
static int dst_crop;

static pthread_mutex_t thread_mutex;
static pthread_cond_t thread_cond;

static render_fader_instance *fader_instance;

static GLuint texture_id;

void render_video_initialize() {
    src_crop = 0;
    src_update_buffer = 0;
    src_render = 0;

    pthread_mutex_init(&thread_mutex, 0);
    pthread_cond_init(&thread_cond, 0);

    render_fader_init(&fader_instance);
}

void render_video_set_crop_video(int in_crop) {
    pthread_mutex_lock(&thread_mutex);
    src_crop = in_crop;
    pthread_mutex_unlock(&thread_mutex);
}

void render_video_set_buffer(void *buffer, int width, int height) {
    pthread_mutex_lock(&thread_mutex);

    src_buffer = buffer;
    src_width = width;
    src_height = height;

    pthread_mutex_unlock(&thread_mutex);
}

void render_video_set_render(int render) {
    pthread_mutex_lock(&thread_mutex);
    src_render = render;
    pthread_mutex_unlock(&thread_mutex);
}

void render_video_update_buffer() {
    pthread_mutex_lock(&thread_mutex);

    src_update_buffer = 1;

    pthread_cond_wait(&thread_cond, &thread_mutex);
    pthread_mutex_unlock(&thread_mutex);
}

void render_video_generate_assets() {
    glGenTextures(1, &texture_id);
    dst_render = 0;
}

void render_video_upload_texes() {
    pthread_mutex_lock(&thread_mutex);

    dst_crop = src_crop;

    if (src_update_buffer) {
        dst_width = src_width;
        dst_height = src_height;

        glBindTexture(GL_TEXTURE_2D, texture_id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, src_width, src_height, 0, GL_RGB, GL_UNSIGNED_BYTE, src_buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glBindTexture(GL_TEXTURE_2D, 0);

        src_update_buffer = 0;
        pthread_cond_signal(&thread_cond);
    }

    if (dst_render != src_render) {
        if (src_render && src_buffer) {
            dst_render = src_render;
            render_fader_fade_in(fader_instance, 1, 1000);
        } else {
            dst_render = src_render;
            render_fader_fade_out(fader_instance, 1, 1000);
        }
    }

    pthread_mutex_unlock(&thread_mutex);
}

void render_video_render(render_layer *layer) {
    if (dst_width <= 0 || dst_height <= 0) {
        return;
    }

    float x, y, w, h;

    float w_scale = (dst_width / dst_height);
    float h_scale = (dst_height / dst_width);

    float w_sz = layer->config.h * w_scale;
    float h_sz = layer->config.w * h_scale;

    if (dst_crop) {
        if (w_sz > h_sz) {
            w = w_sz;
            h = h_scale * w;
        } else {
            h = h_sz;
            w = w_scale * h;
        }
    } else {
        if (w_sz < h_sz) {
            w = w_sz;
            h = h_scale * w;
        } else {
            h = h_sz;
            w = w_scale * h;
        }
    }

    x = (layer->config.w - w) / 2;
    y = (layer->config.h - h) / 2;

    render_fader_for_each(fader_instance) {
        glBindTexture(GL_TEXTURE_2D, texture_id);

        glBegin(GL_QUADS);

        render_fader_set_alpha(node);

        glTexCoord2i(0,0); glVertex2d(x, y);
        glTexCoord2i(0, 1); glVertex2d(x, y + h);
        glTexCoord2i(1, 1); glVertex2d(x + w, y + h);
        glTexCoord2i(1, 0); glVertex2d(x + w, y);

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
