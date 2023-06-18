#include "ogl-loader.h"

#ifndef _RENDER_TEX_BLUR_H_
#define _RENDER_TEX_BLUR_H_

typedef struct {
	GLuint vertexarray;
	GLuint vertexbuffer;
	GLuint uvbuffer;
	GLuint tex;
	int tex_w, tex_h;
} render_tex_blur_instance;

void render_tex_blur_create_assets();
void render_tex_blur_deallocate_assets();

render_tex_blur_instance* render_tex_blur_create();
void render_tex_blur_set_tex(render_tex_blur_instance* instance, GLuint tex, int tex_w, int tex_h);
void render_tex_blur_set_uv(render_tex_blur_instance* instance, float x, float y, float w, float h);
void render_tex_blur_set_position(render_tex_blur_instance* instance, float x, float y, float w, float h);
void render_tex_blur_destroy(render_tex_blur_instance* instance);

void render_tex_blur_render(render_tex_blur_instance* instance);

#endif // !_RENDER_TEX_BLUR_H_
