#include "ogl-loader.h"
#include "render.h"

#ifndef _RENDER_TEXT_OUTLINE_SHADER_H_
#define _RENDER_TEXT_OUTLINE_SHADER_H_

void render_text_outline_shader_initialize(int width, int height);

void render_text_outline_shader_start(render_layer *render);
void render_text_outline_shader_render(render_layer *render);
void render_text_outline_shader_stop(render_layer *render);

void render_text_outline_shader_shutdown();

#endif