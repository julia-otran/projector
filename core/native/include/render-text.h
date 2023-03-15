#include "render.h"

#ifndef _RENDER_TEXT_H_
#define _RENDER_TEXT_H_

typedef struct {
    int render_id;

    void *image_data;
    int position_x, position_y;
    int image_w, image_h;

    double x, y, w, h;
} render_text_data;

void render_text_initialize();

void render_text_set_config(render_layer *renders, int count);
void render_text_set_data(render_text_data *data, int count);

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
