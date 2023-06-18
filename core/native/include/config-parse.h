#include "cJSON.h"
#include "config-structs.h"

#ifndef _CONFIG_PARSE_H_
#define _CONFIG_PARSE_H_

void parse_config_bounds(cJSON *config_bounds_json, config_bounds *out);
void parse_config_blend(cJSON *config_blend_json, config_blend *out);
void parse_config_help_line(cJSON *config_help_line_json, config_help_line *out);
void parse_config_color_factor(cJSON *config_color_factor_json, config_color_factor *out);
void parse_config_color_matrix(cJSON *config_color_matrix_json, config_color_matrix *out);
void parse_config_black_level_adjust(cJSON *config_black_level_adjust_json, config_black_level_adjust *out);
void parse_config_virtual_screen(cJSON *config_virtual_screen_json, config_virtual_screen *out);
void parse_config_display(cJSON *config_display_json, config_display *out);
void parse_config_render(cJSON *config_render_json, config_render *out);
void parse_projection_config(cJSON *projection_config_json, projection_config *out);

#endif
