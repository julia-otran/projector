#include "vs-black-level-adjust.h"

void vs_black_level_adjust_render(config_virtual_screen *config) {
    glEnable(GL_COLOR_MATERIAL);

    glBlendEquation(GL_MAX);
    glBlendFunc(GL_ONE, GL_ONE);
    
    for (int i = 0; i < config->count_black_level_adjusts; i++) {
        config_black_level_adjust *bla = &config->black_level_adjusts[i];
            
        glColor4f(
            bla->color.r * bla->color.a,
            bla->color.g * bla->color.a,
            bla->color.b * bla->color.a,
            0.0);

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
