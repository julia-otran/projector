#include "config-structs.h"

#ifndef _CONFIG_H_
#define _CONFIG_H_

void prepare_default_config(config_bounds *default_monitor_bounds, int no_display);

projection_config* load_config(const char *file_path);
void generate_config(const char *file_path);
void free_projection_config(projection_config *in);

int config_change_requires_restart(projection_config *config1, projection_config *config2);

#endif
