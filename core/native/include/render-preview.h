#include "ogl-loader.h"
#include "render.h"

#ifndef _RENDER_PREVIEW_H_
#define _RENDER_PREVIEW_H_

typedef struct {
	int render_id, readed;
	GLuint buffer;
} render_preview_buffer;

typedef struct {
	int render_id;
	int width;
	int height;

	void* data_buffer;
	void* data_buffer_aligned;

	int buffer_read;
} render_preview;

void render_preview_initialize();
void render_preview_set_renders(render_layer* renders, int count_renders);

void render_preview_download_buffer(int render_id, void *buffer);

void render_preview_create_buffers();
void render_preview_update_buffers();
void render_preview_flush_buffers();
void render_preview_deallocate_buffers();

void render_preview_create_assets();
void render_preview_update_assets();
void render_preview_deallocate_assets();

void render_preview_cycle(render_layer* render);

void render_preview_shutdown();

#endif
