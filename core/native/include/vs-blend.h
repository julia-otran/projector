#include "config-structs.h"
#include "ogl-loader.h"

#ifndef _VS_BLEND_H_
#define _VS_BLEND_H_

typedef struct {
    GLuint vertexarray;
    GLuint vertexbuffer;
    GLuint uvbuffer;
} vs_blend_vertex;

typedef struct {
    vs_blend_vertex *vertexes;
    int vertexes_count;

    GLuint vertexshader;
    GLuint fragmentshader;
    GLuint program;
} vs_blend;

void vs_blend_initialize(config_bounds *display_bounds, config_virtual_screen *virtual_screen, vs_blend *instance);
void vs_blend_render(vs_blend *instance);
void vs_blend_shutdown(vs_blend *instance);

#endif