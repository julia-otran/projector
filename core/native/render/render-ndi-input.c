#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "ogl-loader.h"
#include "render.h"
#include "render-ndi-input.h"
#include "render-pixel-unpack-buffer.h"
#include "render-fader.h"
#include "ndi-input.h"
#include "Processing.NDI.Lib.h"

static int src_render, src_enabled, src_crop;

static int dst_width, dst_height, dst_enabled, dst_render;

static render_pixel_unpack_buffer_instance* buffer_instance;
static render_fader_instance* fader_instance;

static GLuint texture_id;

static mtx_t thread_mutex;

typedef struct {
    int bytesPerPixel;
    GLuint pixelFormat;
} render_pixel_unpack_buffer_ndi_extra_data;

void render_ndi_input_initialize() {
    mtx_init(&thread_mutex, 0);
}

void render_ndi_input_set_enabled(int enabled) {
    mtx_lock(&thread_mutex);

	if (enabled) 
	{
        src_enabled = 1;
	}
	else 
	{
        src_enabled = 0;
	}

    mtx_unlock(&thread_mutex);
}

void render_ndi_input_set_render(int render) {
	src_render = render;
}

void render_ndi_input_download_preview(int* data, int dataMaxSize, int* width, int* height, int *bytesPerPixel, GLuint *pixelFormat)
{
    if (dst_render != 0 || dst_enabled == 0 || src_enabled == 0)
    {
        return;
    }

    mtx_lock(&thread_mutex);

    if (dst_render != 0 || dst_enabled == 0 || src_enabled == 0)
    {
        mtx_unlock(&thread_mutex);
        return;
    }

    ndi_input_lock();
    ndi_input_get_frame_size(width, height, bytesPerPixel, pixelFormat);
    
    if ((*width) * (*height) * (*bytesPerPixel) <= dataMaxSize) {
        ndi_input_download_frame((void*)data);
    }

    ndi_input_unlock();

    mtx_unlock(&thread_mutex);
}

void render_ndi_input_set_crop(int crop)
{
    src_crop = crop;
}

void render_ndi_input_create_buffers()
{
	render_pixel_unpack_buffer_create(&buffer_instance);
    render_pixel_unpack_buffer_allocate_extra_data(buffer_instance, sizeof(render_pixel_unpack_buffer_ndi_extra_data));
    render_fader_init(&fader_instance);
}

void render_ndi_input_update_buffers()
{
    mtx_lock(&thread_mutex);

    if (src_enabled != dst_enabled || src_render != dst_render) {
        dst_enabled = src_enabled;

        if (src_render != 0) {
            dst_render = src_render;
        }
    }

    if (src_enabled == 0 || src_render == 0) {
        mtx_unlock(&thread_mutex);
        return;
    }

    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instance);

    if (buffer) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

        render_pixel_unpack_buffer_ndi_extra_data *extra_data = (render_pixel_unpack_buffer_ndi_extra_data*) buffer->extra_data;

        ndi_input_lock();

        int src_width, src_height, bytesPerPixel;
        GLuint pixelFormat;

        ndi_input_get_frame_size(&src_width, &src_height, &bytesPerPixel, &pixelFormat);

        if (src_width > 0 && src_height > 0) {
            if (buffer->width != src_width || buffer->height != src_height || extra_data->bytesPerPixel != bytesPerPixel) {
                glBufferData(GL_PIXEL_UNPACK_BUFFER, src_width * src_height * bytesPerPixel, 0, GL_DYNAMIC_DRAW);
                buffer->width = src_width;
                buffer->height = src_height;
                extra_data->bytesPerPixel = bytesPerPixel;
            }

            extra_data->pixelFormat = pixelFormat;

            void* data = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);
            ndi_input_download_frame(data);
            glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
        }

        ndi_input_unlock();

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, buffer);
    mtx_unlock(&thread_mutex);
}

void render_ndi_input_flush_buffers()
{
	render_pixel_unpack_buffer_flush(buffer_instance);
}

void render_ndi_input_deallocate_buffers()
{
    render_pixel_unpack_buffer_free_extra_data(buffer_instance);
	render_pixel_unpack_buffer_deallocate(buffer_instance);
	buffer_instance = NULL;

    render_fader_terminate(fader_instance);
    fader_instance = NULL;
}

void render_ndi_input_create_assets()
{
    glGenTextures(1, &texture_id);
}

void render_ndi_input_update_assets()
{
    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instance);

    if (buffer) {
        dst_width = buffer->width;
        dst_height = buffer->height;

        render_pixel_unpack_buffer_ndi_extra_data *extra_data = (render_pixel_unpack_buffer_ndi_extra_data*) buffer->extra_data;

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);
        glBindTexture(GL_TEXTURE_2D, texture_id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, dst_width, dst_height, 0, extra_data->pixelFormat, GL_UNSIGNED_BYTE, 0);
        tex_set_default_params();

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    render_pixel_unpack_buffer_enqueue_for_flush(buffer_instance, buffer);

    if (dst_enabled && dst_render && src_render)
    {
        render_fader_fade_in(fader_instance, texture_id, RENDER_FADER_DEFAULT_TIME_MS);
    }
    else
    {
        render_fader_fade_out(fader_instance, texture_id, RENDER_FADER_DEFAULT_TIME_MS);
    }

    if (src_render == 0 && dst_render != 0)
    {
        render_fader_for_each(fader_instance) {
            if (render_fader_is_hidden(node))
            {
                dst_render = 0;
            }
        }
    }
}

void render_ndi_input_deallocate_assets()
{
    glDeleteTextures(1, &texture_id);
}

void render_ndi_input_render(render_layer* layer)
{
    if (dst_enabled == 0) {
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

    if ((w_sz > layer->config.w) == src_crop)
    {
        w = w_sz;
        h = layer->config.h;
    }
    else 
    {
        h = h_sz;
        w = layer->config.w;
    }

    x = (layer->config.w - w) / 2;
    y = (layer->config.h - h) / 2;

    render_fader_for_each(fader_instance) {
        float alpha = render_fader_get_alpha(node);

        glColor4f(1.0, 1.0, 1.0, alpha);

        glBindTexture(GL_TEXTURE_2D, node->fade_id);

        glBegin(GL_QUADS);
        glTexCoord2f(0.0, 0.0); glVertex2d(x, y);
        glTexCoord2f(0.0, 1.0); glVertex2d(x, y + h);
        glTexCoord2f(1.0, 1.0); glVertex2d(x + w, y + h);
        glTexCoord2f(1.0, 0.0); glVertex2d(x + w, y);
        glEnd();
    }

    glBindTexture(GL_TEXTURE_2D, 0);
}

void render_ndi_input_shutdown()
{
    mtx_destroy(&thread_mutex);
}
