#include <stdlib.h>

#include "ogl-loader.h"
#include "virtual-screen.h"

void initialize_virtual_screen(config_virtual_screen *config, void **data) {
    virtual_screen *vs = (virtual_screen*) calloc(1, sizeof(virtual_screen));
    (*data) = (void*) vs;

    vs_color_corrector_init(config, &vs->color_corrector);
}

void render_virtual_screen(GLuint texture_id, void *data) {
    virtual_screen *vs = (virtual_screen*) data;
    vs_color_corrector_render_texture(texture_id, &vs->color_corrector);
}

void shutdown_virtual_screen(void *data) {
    free(data);
}
