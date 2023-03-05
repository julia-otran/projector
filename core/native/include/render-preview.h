#include "ogl-loader.h"

#ifndef _RENDER_PREVIEW_H_
#define _RENDER_PREVIEW_H_

void render_preview_initialize();
void render_preview_set_size(int width, int height);

void render_preview_download_buffer(void *buffer);

void render_preview_create_buffers();
void render_preview_update_buffers();
void render_preview_deallocate_buffers();

void render_preview_create_assets();
void render_preview_update_assets();
void render_preview_deallocate_assets();

void render_preview_cycle();

void render_preview_shutdown();

#endif
