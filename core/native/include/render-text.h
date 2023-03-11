#include "render.h"

#ifndef _RENDER_TEXT_H_
#define _RENDER_TEXT_H_

void render_text_initialize();

void render_text_set_size(int width, int height);
void render_text_set_image(void *pixel_data);

void render_text_create_buffers();
void render_text_update_buffers();
void render_text_deallocate_buffers();

void render_text_create_assets();
void render_text_deallocate_assets();
void render_text_update_assets();

void render_text_start(render_layer *layer);
void render_text_render(render_layer *layer);
void render_text_stop(render_layer *layer);

void render_text_shutdown();

#endif
