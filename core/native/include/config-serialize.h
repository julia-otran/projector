#include "cJSON.h"
#include "config-structs.h"

#ifndef _CONFIG_SERIALIZE_H_
#define _CONFIG_SERIALIZE_H_

cJSON* serialize_config_bounds(config_bounds *in);
cJSON* serialize_config_blend(config_blend *in);
cJSON* serialize_config_help_line(config_help_line *in);
cJSON* serialize_config_color_factor(config_color_factor *in);
cJSON* serialize_config_color_matrix(config_color_matrix *in);
cJSON* serialize_config_black_level_adjust(config_black_level_adjust *in);
cJSON* serialize_config_virtual_screen(config_virtual_screen *in);
cJSON* serialize_config_display(config_display *in);
cJSON* serialize_config_render(config_render *in);
cJSON* serialize_projection_config(projection_config *in);

#endif
