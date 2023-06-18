#include "config-structs.h"
#include "debug.h"
#include "config-serialize.h"
#include "config-debug.h"

void print_json(cJSON *json) {
    char *json_str = cJSON_Print(json);
    log_debug("Printing Config...\n");
    log_debug("%s\n", json_str);
    log_debug("Print config done!\n");
    cJSON_Delete(json);
    free(json_str);
}

void print_config_bounds(config_bounds *in) {
    print_json(serialize_config_bounds(in));
}

void print_config_blend(config_blend *in) {
    print_json(serialize_config_blend(in));
}

void print_config_help_line(config_help_line *in) {
    print_json(serialize_config_help_line(in));
}

void print_config_color_factor(config_color_factor *in) {
    print_json(serialize_config_color_factor(in));
}

void print_config_color_matrix(config_color_matrix *in) {
    print_json(serialize_config_color_matrix(in));
}

void print_config_black_level_adjust(config_black_level_adjust *in) {
    print_json(serialize_config_black_level_adjust(in));
}

void print_config_virtual_screen(config_virtual_screen *in) {
    print_json(serialize_config_virtual_screen(in));
}

void print_config_display(config_display *in) {
    print_json(serialize_config_display(in));
}

void print_config_render(config_render *in) {
    print_json(serialize_config_render(in));
}

void print_projection_config(projection_config *in) {
    print_json(serialize_projection_config(in));
}
