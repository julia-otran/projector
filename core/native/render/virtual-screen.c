#include <stdlib.h>

#include "ogl-loader.h"
#include "virtual-screen.h"

void virtual_screen_initialize() {
    vs_color_corrector_init();
    vs_blend_initialize();
}

void virtual_screen_start(config_bounds *display_bounds, config_virtual_screen *config, void **data) {
    virtual_screen *vs = (virtual_screen*) calloc(1, sizeof(virtual_screen));
    (*data) = (void*) vs;

    vs_color_corrector_start(display_bounds, config, &vs->color_corrector);
    vs_blend_start(display_bounds, config, &vs->blend);
}

void virtual_screen_render(GLuint texture_id, config_virtual_screen *config, void *data) {
    virtual_screen *vs = (virtual_screen*) data;

    vs_color_corrector_render_texture(texture_id, config, &vs->color_corrector);
    vs_blend_render(&vs->blend);
}

void virtual_screen_stop(void *data) {
    virtual_screen *vs = (virtual_screen*) data;

    vs_color_corrector_stop(&vs->color_corrector);
    vs_blend_stop(&vs->blend);

    free(data);
}

void virtual_screen_shutdown() {
    vs_color_corrector_shutdown();
    vs_blend_shutdown();
}
