#include <stdlib.h>

#include "ogl-loader.h"
#include "config-structs.h"
#include "virtual-screen.h"
#include "vs-black-level-adjust.h"
#include "vs-blend.h"
#include "vs-color-corrector.h"
#include "vs-help-lines.h"

void virtual_screen_initialize() {
    vs_color_corrector_init();
    vs_blend_initialize();
}

void virtual_screen_start(config_bounds *display_bounds, config_virtual_screen *config, void **data) {
    virtual_screen *vs = (virtual_screen*) calloc(1, sizeof(virtual_screen));
    (*data) = (void*) vs;

    int width = config->w;
    int height = config->h;

    GLuint renderedTexture;
    glGenTextures(1, &renderedTexture);

    vs->texture_id = renderedTexture;

    // "Bind" the newly created texture : all future texture functions will modify this texture
    glBindTexture(GL_TEXTURE_2D, renderedTexture);

    // Give an empty image to OpenGL ( the last "0" )
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,GL_RGBA, GL_UNSIGNED_BYTE, 0);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

    glBindTexture(GL_TEXTURE_2D, 0);

    GLuint framebuffer_id = 0;
    glGenFramebuffers(1, &framebuffer_id);
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer_id);

    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderedTexture, 0);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    vs->framebuffer_id = framebuffer_id;

    vs_color_corrector_start(display_bounds, config, &vs->color_corrector);
    vs_blend_start(display_bounds, config, &vs->blend);
}

void virtual_screen_render(GLuint texture_id, config_virtual_screen *config, void *data) {
    virtual_screen *vs = (virtual_screen*) data;
    config_color_factor *background_clear_color = &config->background_clear_color;

    glPushMatrix();

    glBindFramebuffer(GL_FRAMEBUFFER, vs->framebuffer_id);

    glViewport(0, 0, config->w, config->h);

    glClearColor(background_clear_color->r, background_clear_color->g, background_clear_color->b, background_clear_color->a);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glLoadIdentity();
    glOrtho(0.0, config->w, config->h, 0.0, 0.0, 1.0);

    vs_color_corrector_render_texture(texture_id, config, &vs->color_corrector);
    vs_blend_render(&vs->blend);
    vs_help_lines_render(config);
    vs_black_level_adjust_render(config);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    glPopMatrix();
}

void virtual_screen_print(config_virtual_screen *config, void *data) {
    virtual_screen *vs = (virtual_screen*) data;

    glEnableClientState(GL_VERTEX_ARRAY);
    glEnable(GL_TEXTURE_2D);
    glEnable(GL_MULTISAMPLE);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glBindTexture(GL_TEXTURE_2D, vs->texture_id);

    glColor4d(1.0, 1.0, 1.0, 1.0);

    double tex_x, tex_y, dst_x, dst_y;

    glBegin(GL_QUADS);

    for (int i = 0; i < config->monitor_position.count_points; i++) {
        tex_x = config->monitor_position.input_points[i].x;
        tex_y = 1.0 - config->monitor_position.input_points[i].y;

        dst_x = config->monitor_position.output_points[i].x;
        dst_y = config->monitor_position.output_points[i].y;

        glTexCoord2d(tex_x, tex_y);
        glVertex2d(dst_x, dst_y);
    }

    glEnd();

    glBindTexture(GL_TEXTURE_2D, 0);

    glDisable(GL_MULTISAMPLE);
    glDisableClientState(GL_VERTEX_ARRAY);
}

void virtual_screen_stop(void *data) {
    virtual_screen *vs = (virtual_screen*) data;

    vs_color_corrector_stop(&vs->color_corrector);
    vs_blend_stop(&vs->blend);

    glDeleteFramebuffers(1, &vs->framebuffer_id);
    glDeleteTextures(1, &vs->texture_id);

    free(data);
}

void virtual_screen_shutdown() {
    vs_color_corrector_shutdown();
    vs_blend_shutdown();
}
