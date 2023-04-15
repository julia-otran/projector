#include "vs-black-level-adjust.h"

void vs_black_level_adjust_render(config_virtual_screen *config) {
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR);

    glEnable(GL_MULTISAMPLE);
    glEnable(GL_COLOR_MATERIAL);

    for (int i = 0; i < config->count_black_level_adjusts; i++) {
        config_black_level_adjust *bla = &config->black_level_adjusts[i];

        glColor4f(
            bla->color.r * bla->alpha, 
            bla->color.g * bla->alpha, 
            bla->color.b * bla->alpha,
            bla->alpha);

        glBegin(GL_QUADS);

        glVertex2d(bla->x1, bla->y1);
        glVertex2d(bla->x2, bla->y2);
        glVertex2d(bla->x3, bla->y3);
        glVertex2d(bla->x4, bla->y4);

        glEnd();
    }

    glDisable(GL_MULTISAMPLE);
    glDisable(GL_COLOR_MATERIAL);
}
