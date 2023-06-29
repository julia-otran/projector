#include "vs-black-level-adjust.h"

void vs_black_level_adjust_render(config_virtual_screen *config) {
    glEnable(GL_BLEND);
    glEnable(GL_COLOR_MATERIAL);

    glBlendFunc(GL_ONE_MINUS_DST_COLOR, GL_ONE);
    glBlendEquation(GL_MAX);

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
    glBlendEquation(GL_FUNC_ADD);
    glBlendColor(0.0, 0.0, 0.0, 0.0);

    glDisable(GL_COLOR_MATERIAL);
}
