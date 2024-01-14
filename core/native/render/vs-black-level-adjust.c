#include "vs-black-level-adjust.h"

void vs_black_level_adjust_render(config_virtual_screen *config) {
    glEnable(GL_COLOR_MATERIAL);

    double max_r = 0.0d;
    double max_g = 0.0d;
    double max_b = 0.0d;

    for (int i = 0; i < config->count_black_level_adjusts; i++) {
        config_black_level_adjust *bla = &config->black_level_adjusts[i];

        if (bla->color.r * bla->color.a > max_r) {
            max_r = bla->color.r * bla->color.a;
        }
        if (bla->color.g * bla->color.a > max_g) {
            max_g = bla->color.g * bla->color.a;
        }
        if (bla->color.b * bla->color.a > max_b) {
            max_b = bla->color.b * bla->color.a;
        }
    }

    if (max_r + max_g + max_b > 0.0) {
        glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_COLOR);

        glColor4f(
            max_r,
            max_g,
            max_b,
            0.0);

        glBegin(GL_QUADS);

        glVertex2d(0.0d, 0.0d);
        glVertex2d(0.0d, config->h);
        glVertex2d(config->w, config->h);
        glVertex2d(config->w, 0.0d);

        glEnd();
    }

    glBlendEquation(GL_MAX);
    glBlendFunc(GL_ONE, GL_DST_ALPHA);
    
    for (int i = 0; i < config->count_black_level_adjusts; i++) {
        config_black_level_adjust *bla = &config->black_level_adjusts[i];
            
        glColor4f(
            bla->color.r * bla->color.a,
            bla->color.g * bla->color.a,
            bla->color.b * bla->color.a,
            1.0);

        glBegin(GL_QUADS);

        glVertex2d(bla->x1, bla->y1);
        glVertex2d(bla->x2, bla->y2);
        glVertex2d(bla->x3, bla->y3);
        glVertex2d(bla->x4, bla->y4);

        glEnd();
    }

    glBlendEquation(GL_FUNC_ADD);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glBlendColor(0.0, 0.0, 0.0, 0.0);

    glDisable(GL_COLOR_MATERIAL);
}
