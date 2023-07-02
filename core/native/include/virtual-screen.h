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

    GLuint vertexarray;
    GLuint vertexbuffer;
    GLuint uvbuffer;

    unsigned int points_count;

    vs_color_corrector color_corrector;
    vs_blend blend;
} virtual_screen;

void virtual_screen_shared_initialize();
void virtual_screen_monitor_initialize();

void virtual_screen_shared_start(config_display *display, render_output *render, config_virtual_screen *config, void **data);
void virtual_screen_monitor_start(config_display* display, render_output* render, config_virtual_screen* config, void* data);

void virtual_screen_shared_render(config_virtual_screen *config, void *data);
void virtual_screen_monitor_print(config_virtual_screen *config, void *data);

void virtual_screen_shared_stop(void *data);
void virtual_screen_monitor_stop(void* data);

void virtual_screen_shared_shutdown();
void virtual_screen_monitor_shutdown();

#endif
