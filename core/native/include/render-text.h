#include "render.h"

#ifndef _RENDER_TEXT_H_
#define _RENDER_TEXT_H_

void render_text_initialize(int width, int height);
void render_text_set_image(void *pixel_data);
void render_text_upload_texes();
void render_text_render_cycle(render_layer *layer);

#endif
