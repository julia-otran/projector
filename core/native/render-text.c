#include <pthread.h>
#include <string.h>

#include "render.h"
#include "debug.h"
#include "render-text.h"
#include "ogl-loader.h"
#include "render-fader.h"

static int initialized = 0;
static int width, height;

#define CHECK_INIT {\
    if (!initialized) {\
        log_debug("FreeType Lib not initialized!");\
        return;\
    }\
}

static pthread_mutex_t thread_mutex;

static void *share_pixel_data;
static int pixel_data_changed;
static int clear_text;

static GLuint render_tex_id;
static render_fader_instance *fader_instance;

void render_text_initialize(int width_in, int height_in) {
    if (initialized) {
        return;
    }

    width = width_in;
    height = height_in;
    share_pixel_data = malloc(width_in * height_in * 4);
    pixel_data_changed = 0;
    clear_text = 0;
    render_tex_id = 0;

    pthread_mutex_init(&thread_mutex, 0);

    render_fader_init(&fader_instance);

    initialized = 1;
}

void render_text_set_image(void *pixel_data) {
    pthread_mutex_lock(&thread_mutex);

    if (pixel_data) {
        memcpy(share_pixel_data, pixel_data, width * height * 4);
        pixel_data_changed = 1;
    } else {
        clear_text = 1;
        pixel_data_changed = 1;
    }

    pthread_mutex_unlock(&thread_mutex);
}

void render_text_upload_texes() {
    pthread_mutex_lock(&thread_mutex);

    if (pixel_data_changed && !clear_text) {
        GLuint renderedTexture;

        glGenTextures(1, &renderedTexture);

        glBindTexture(GL_TEXTURE_2D, renderedTexture);
        // Check if we r using rgba in java too.
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, share_pixel_data);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        if (render_tex_id) {
            render_fader_fade_out(fader_instance, render_tex_id, 1000);
        }

        render_tex_id = renderedTexture;
        render_fader_fade_in(fader_instance, render_tex_id, 1000);
    }

    if (pixel_data_changed && clear_text && render_tex_id) {
        render_tex_id = 0;
        render_fader_fade_out(fader_instance, render_tex_id, 1000);
    }

    pthread_mutex_unlock(&thread_mutex);

    render_fader_for_each(fader_instance) {
        if (render_fader_is_hidden(node)) {
            GLuint tex_id = (unsigned int) node->fade_id;

            glDeleteTextures(1, &tex_id);
        }
    }

    render_fader_cleanup(fader_instance);
}

void render_text_render_cycle(render_layer *layer) {
    glEnable(GL_TEXTURE_2D);

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

    glBindTexture(GL_TEXTURE_2D, 0);
}
