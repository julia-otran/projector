#include "render.h"

#ifndef _RENDER_IMAGE_H_
#define _RENDER_IMAGE_H_

void render_image_initialize();

void render_image_set_image(void *pixel_data, int width, int height, int crop);

void render_image_create_buffers();
void render_image_update_buffers();
void render_image_deallocate_buffers();

void render_image_create_assets();
void render_image_deallocate_assets();
void render_image_update_assets();

void render_image_render(render_layer *layer);

void render_image_shutdown();

#endif
