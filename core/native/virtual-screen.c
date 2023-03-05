#include <stdlib.h>

#include "ogl-loader.h"
#include "virtual-screen.h"

void initialize_virtual_screen(config_bounds *display_bounds, config_virtual_screen *config, void **data) {
    virtual_screen *vs = (virtual_screen*) calloc(1, sizeof(virtual_screen));
    (*data) = (void*) vs;

    vs_color_corrector_init(display_bounds, config, &vs->color_corrector);
    vs_blend_initialize(display_bounds, config, &vs->blend);
}

void render_virtual_screen(GLuint texture_id, void *data) {
    virtual_screen *vs = (virtual_screen*) data;

    vs_color_corrector_render_texture(texture_id, &vs->color_corrector);
    vs_blend_render(&vs->blend);
}

void shutdown_virtual_screen(void *data) {
    virtual_screen *vs = (virtual_screen*) data;

    vs_color_corrector_shutdown(&vs->color_corrector);
    vs_blend_shutdown(&vs->blend);

    free(data);
}
