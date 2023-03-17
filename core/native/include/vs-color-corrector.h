#include "ogl-loader.h"
#include "monitor.h"
#include "config-structs.h"

#ifndef _VS_COLOR_CORRECTOR_H_
#define _VS_COLOR_CORRECTOR_H_

typedef struct {
    GLuint vertexarray;
    GLuint vertexbuffer;
    GLuint uvbuffer;
} vs_color_corrector;

void vs_color_corrector_init();
void vs_color_corrector_start(config_bounds *display_bounds, config_virtual_screen *config, vs_color_corrector *data);
void vs_color_corrector_render_texture(GLuint texture_id, config_virtual_screen *config, vs_color_corrector *data);
void vs_color_corrector_stop(vs_color_corrector *data);
void vs_color_corrector_shutdown();

#endif