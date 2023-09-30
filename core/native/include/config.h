#include "config-structs.h"

#ifndef _CONFIG_H_
#define _CONFIG_H_

#ifdef _WIN32

#define open_file(...) fopen_s(__VA_ARGS__)

#endif

#ifndef _WIN32 

#define open_file(ptr, ...) ((*ptr) = fopen(__VA_ARGS__))

#endif


void prepare_default_config(config_bounds *default_monitor_bounds);

projection_config* load_config(const char *file_path);
void generate_config(const char *file_path);
void free_projection_config(projection_config *in);

int config_change_requires_restart(projection_config *config1, projection_config *config2);

#endif
