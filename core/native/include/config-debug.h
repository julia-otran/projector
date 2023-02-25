#include "config-structs.h"

#ifndef _CONFIG_DEBUG_H_
#define _CONFIG_DEBUG_H_

void print_config_bounds(config_bounds *in);
void print_config_blend(config_blend *in);
void print_config_help_line(config_help_line *in);
void print_config_color_factor(config_color_factor *in);
void print_config_color_balance(config_color_balance *in);
void print_config_white_balance(config_white_balance *in);
void print_config_black_level_adjust(config_black_level_adjust *in);
void print_config_virtual_screen(config_virtual_screen *in);
void print_config_display(config_display *in);
void print_config_render(config_render *in);
void print_projection_config(projection_config *in);

#endif
