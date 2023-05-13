#include "vs-black-level-adjust.h"

void vs_black_level_adjust_render(config_virtual_screen *config) {
    glEnable(GL_BLEND);

    glEnable(GL_MULTISAMPLE);
    glEnable(GL_COLOR_MATERIAL);

    glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_COLOR);

    double r = 0, g = 0, b = 0, a = 0;

    for (int i = 0; i < config->count_black_level_adjusts; i++) {
        config_black_level_adjust* bla = &config->black_level_adjusts[i];

        r = bla->color.r > r ? bla->color.r : r;
        g = bla->color.g > g ? bla->color.g : g;
        b = bla->color.b > b ? bla->color.b : b;
        a = bla->color.a > a ? bla->color.a : a;
    }

    glColor4f(r * a, g * a, b * a, 0.0);

    glBegin(GL_QUADS);
    glVertex2d(0, 0);
    glVertex2d(0, config->h);
    glVertex2d(config->w, config->h);
    glVertex2d(config->w, 0);
    glEnd();
    

    glBlendFunc(GL_SRC_COLOR, GL_ONE);

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

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glDisable(GL_MULTISAMPLE);
    glDisable(GL_COLOR_MATERIAL);
}
