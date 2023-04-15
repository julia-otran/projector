#include "render.h"

#ifndef _RENDER_WINDOW_CAPTURE_H_
#define _RENDER_WINDOW_CAPTURE_H_

void render_window_capture_initialize();

void render_window_capture_src_set_window_name(char *window_name);
void render_window_capture_src_set_render(int render);
void render_window_capture_src_set_crop(int crop);

void render_window_capture_create_buffers();
void render_window_capture_update_buffers();
void render_window_capture_deallocate_buffers();

void render_window_capture_create_assets();
void render_window_capture_update_assets();
void render_window_capture_deallocate_assets();

void render_window_capture_render(render_layer *layer);

void render_window_capture_shutdown();

#endif
