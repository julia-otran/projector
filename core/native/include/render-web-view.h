#include "ogl-loader.h"
#include "render.h"

#ifndef _RENDER_WEB_VIEW_H_
#define _RENDER_WEB_VIEW_H_

void render_web_view_initialize();

void render_web_view_src_set_buffer(void *buffer, int width, int height);
void render_web_view_src_set_render(int render);
void render_web_view_src_buffer_update();

void render_web_view_create_buffers();
void render_web_view_update_buffers();
void render_web_view_deallocate_buffers();

void render_web_view_create_assets();
void render_web_view_update_assets();
void render_web_view_deallocate_assets();

void render_web_view_render(render_layer *layer);

void render_web_view_shutdown();

#endif
