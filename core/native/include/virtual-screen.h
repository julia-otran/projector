#include "monitor.h"
#include "config-structs.h"
#include "vs-color-corrector.h"
#include "vs-blend.h"

#ifndef _VIRTUAL_SCREEN_H
#define _VIRTUAL_SCREEN_H

typedef struct {
    vs_color_corrector color_corrector;
    vs_blend blend;
} virtual_screen;

void initialize_virtual_screen(config_bounds *display_bounds, config_virtual_screen *config, void **data);
void render_virtual_screen(GLuint texture_id, void *data);
void shutdown_virtual_screen(void *data);

#endif