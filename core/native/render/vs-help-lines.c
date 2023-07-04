#include "vs-help-lines.h"

void vs_help_lines_render(config_virtual_screen *config) {
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glEnable(GL_LINE_SMOOTH);

    glColor4f(1.0, 1.0, 1.0, 1.0);

    for (int i = 0; i < config->count_help_lines; i++) {
        config_help_line *line = &config->help_lines[i];

        glLineWidth(line->line_width);

        glBegin(GL_LINES);
        glVertex2d(line->x1, line->y1);
        glVertex2d(line->x2, line->y2);
        glEnd();
    }

    glDisable(GL_LINE_SMOOTH);
}
