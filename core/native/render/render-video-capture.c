#include "ogl-loader.h"
#include "render.h"
#include "render-video-capture.h"
#include "video-capture.h"
#include "render-pixel-unpack-buffer.h"

static int src_render, src_width, src_height, src_enabled;
static int dst_width, dst_height;
static render_pixel_unpack_buffer_instance* buffer_instance;
static GLuint texture_id;

void render_video_capture_initialize() {
	
}

void render_video_capture_set_device(char* device, int width, int height) {
	// TODO: Free old device name
	video_capture_set_device(device, width, height);
    src_width = width;
    src_height = height;
}

void render_video_capture_set_enabled(int enabled) {
	if (enabled) 
	{
		video_capture_open_device();
        src_enabled = 1;
	}
	else 
	{
        // TODO: Need a lock for this case
        src_enabled = 0;
		video_capture_close();
	}
}

void render_video_capture_set_render(int render) {
	src_render = render;
}

void render_video_capture_download_preview(int* data);

void render_video_capture_create_buffers()
{
	render_pixel_unpack_buffer_create(&buffer_instance);
}

void render_video_capture_update_buffers()
{
    if (src_enabled == 0) {
        return;
    }

    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_write(buffer_instance);

    if (buffer) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);

        if (buffer->width != src_width || buffer->height != src_height) {
            glBufferData(GL_PIXEL_UNPACK_BUFFER, src_width * src_height * 4, 0, GL_DYNAMIC_DRAW);
            buffer->width = src_width;
            buffer->height = src_height;
        }

        void* data = glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY);
        video_capture_print_frame(data);
        glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    render_pixel_unpack_buffer_enqueue_for_read(buffer_instance, buffer);
}

void render_video_capture_flush_buffers()
{
	render_pixel_unpack_buffer_flush(buffer_instance);
}

void render_video_capture_deallocate_buffers()
{
	render_pixel_unpack_buffer_deallocate(buffer_instance);
	buffer_instance = NULL;
}

void render_video_capture_create_assets()
{
    glGenTextures(1, &texture_id);
}

void render_video_capture_update_assets()
{
    render_pixel_unpack_buffer_node* buffer = render_pixel_unpack_buffer_dequeue_for_read(buffer_instance);

    // TODO: Add the fader
    if (buffer) {
        dst_width = buffer->width;
        dst_height = buffer->height;

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer->gl_buffer);
        glBindTexture(GL_TEXTURE_2D, texture_id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, dst_width, dst_height, 0, GL_BGRA, GL_UNSIGNED_BYTE, 0);
        tex_set_default_params();

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
    }

    render_pixel_unpack_buffer_enqueue_for_flush(buffer_instance, buffer);
}

void render_video_capture_deallocate_assets()
{
    glDeleteTextures(1, &texture_id);
}

void render_video_capture_render(render_layer* layer)
{
    // Fix this, check correctly for src_render
    if (src_enabled == 0 || src_render == 0) {
        return;
    }

    glColor4f(1.0, 1.0, 1.0, 1.0);
    glBindTexture(GL_TEXTURE_2D, texture_id);

    double x, y, w, h;

    x = 0;
    y = 0;
    w = layer->config.w;
    h = layer->config.h;

    glBegin(GL_QUADS);
    glTexCoord2f(0.0, 0.0); glVertex2d(x, y);
    glTexCoord2f(0.0, 1.0); glVertex2d(x, y + h);
    glTexCoord2f(1.0, 1.0); glVertex2d(x + w, y + h);
    glTexCoord2f(1.0, 0.0); glVertex2d(x + w, y);
    glEnd();

    glBindTexture(GL_TEXTURE_2D, 0);
}

void render_video_capture_shutdown()
{

}
