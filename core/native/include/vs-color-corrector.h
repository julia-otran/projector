#include "ogl-loader.h"
#include "monitor.h"
#include "config-structs.h"

#ifndef _VS_COLOR_CORRECTOR_H_
#define _VS_COLOR_CORRECTOR_H_

typedef struct {
    GLuint vertexarray;
    GLuint vertexbuffer;
    GLuint uvbuffer;

    GLuint vertexshader;
    GLuint fragmentshader;
    GLuint program;
    
    GLuint textureUniform;
    GLuint brightAdjustUniform;
    GLuint exposureAdjustUniform;
    GLuint lowAdjustUniform;
    GLuint midAdjustUniform;
    GLuint highAdjustUniform;
    GLuint preserveLumUniform;
} vs_color_corrector;

void vs_color_corrector_init(config_virtual_screen *config, vs_color_corrector *data);

void vs_color_corrector_render_texture(GLuint texture_id, vs_color_corrector *data);

#endif