#include "ogl-loader.h"
#include "monitor.h"
#include "config-structs.h"
#include "vs-color-corrector.h"
#include "vs-blend.h"
#include "render.h"

#ifndef _VIRTUAL_SCREEN_H
#define _VIRTUAL_SCREEN_H

typedef struct {
    render_output *render_output;

    GLuint texture_id;
    GLuint framebuffer_id;

    vs_color_corrector color_corrector;
    vs_blend blend;
} virtual_screen;

void virtual_screen_initialize();
void virtual_screen_start(render_output *render, config_virtual_screen *config, void **data);

void virtual_screen_render(config_virtual_screen *config, void *data);
void virtual_screen_print(config_virtual_screen *config, void *data);

void virtual_screen_stop(void *data);
void virtual_screen_shutdown();

#endif