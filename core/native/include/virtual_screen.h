#include <GLFW/glfw3.h>

#include "monitor.h"
#include "config.h"
#include "vs_color_corrector.h"

#ifndef _VIRTUAL_SCREEN_H
#define _VIRTUAL_SCREEN_H

typedef struct {
    vs_color_corrector color_corrector;
} virtual_screen;

void initialize_virtual_screen(config_virtual_screen *config, void **data);
void render_virtual_screen(GLuint texture_id, void *data);

#endif