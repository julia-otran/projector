#include <cjson/cJSON.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include "config-parse.h"
#include "config-serialize.h"
#include "config.h"

static projection_config default_config;

void free_config_virtual_screen(config_virtual_screen *in) {
    free(in->blends);
    free(in->help_lines);
    free(in->black_level_adjusts);
}

void free_config_display(config_display *in) {
    for (int i = 0; i < in->count_virtual_screen; i++) {
        free_config_virtual_screen(&in->virtual_screens[i]);
    }

    free(in->virtual_screens);
}

void free_projection_config(projection_config *in) {
    free(in->renders);

    for (int i = 0; i < in->count_display; i++) {
        free_config_display(&in->display[i]);
    }

    free(in->display);
    free(in);
}

projection_config* load_config(const char *filePath) {
    if (filePath == NULL || access(filePath, F_OK) != 0) {
        return &default_config;
    }

    FILE *config_file = fopen(filePath, "r");
    fseek(config_file, 0L, SEEK_END);

    unsigned long long size = ftell(config_file);

    if (size > 1024 * 1024) {
        printf("Config file too large. Skipped loading this config");
        fclose(config_file);
        return &default_config;
    }

    fseek(config_file, 0L, SEEK_SET);

    char *config_json_string = malloc(size);
    fgets(config_json_string, size, config_file);
    fclose(config_file);

    cJSON *json = cJSON_ParseWithLength(config_json_string, size);
    free(config_json_string);

    projection_config *config = (projection_config*) malloc(sizeof(projection_config));
    parse_projection_config(json, config);

    cJSON_Delete(json);

    return config;
}

void prepare_default_config(config_bounds *default_monitor_bounds, int no_display) {
    default_config.count_renders = 1;

    default_config.renders = (config_render*) calloc(1, sizeof(config_render));

    default_config.renders[0].render_id = 1;
    default_config.renders[0].w = default_monitor_bounds->w;
    default_config.renders[0].h = default_monitor_bounds->h;

    default_config.renders[0].text_area.x = 30.0;
    default_config.renders[0].text_area.y = 30.0;
    default_config.renders[0].text_area.w = default_monitor_bounds->w - 60.0;
    default_config.renders[0].text_area.h = default_monitor_bounds->h - 60.0;

    default_config.renders[0].enable_render_background_assets = 1;
    default_config.renders[0].enable_render_image = 1;
    default_config.renders[0].enable_render_video = 1;
    default_config.renders[0].text_render_mode = CONFIG_RENDER_MODE_MAIN;

    default_config.renders[0].background_clear_color.r = 1.0;
    default_config.renders[0].background_clear_color.g = 0.0;
    default_config.renders[0].background_clear_color.b = 0.0;

    if (no_display) {
        default_config.count_display = 0;
        default_config.display = NULL;
        return;
    }

    default_config.display = (config_display*) calloc(1, sizeof(config_display));
    default_config.display[0].virtual_screens = (config_virtual_screen*) calloc(1, sizeof(config_virtual_screen));
    default_config.count_display = 1;

    default_config.display[0].monitor_bounds.x = default_monitor_bounds->x;
    default_config.display[0].monitor_bounds.y = default_monitor_bounds->y;
    default_config.display[0].monitor_bounds.w = default_monitor_bounds->w;
    default_config.display[0].monitor_bounds.h = default_monitor_bounds->h;
    default_config.display[0].projection_enabled = 1;
    default_config.display[0].count_virtual_screen = 1;

    default_config.display[0].virtual_screens[0].source_render_id = 1;

    default_config.display[0].virtual_screens[0].input_bounds.x = 0;
    default_config.display[0].virtual_screens[0].input_bounds.y = 0;
    default_config.display[0].virtual_screens[0].input_bounds.w = 1;
    default_config.display[0].virtual_screens[0].input_bounds.h = 1;

    default_config.display[0].virtual_screens[0].output_bounds.x = -1;
    default_config.display[0].virtual_screens[0].output_bounds.y = -1;
    default_config.display[0].virtual_screens[0].output_bounds.w = 2;
    default_config.display[0].virtual_screens[0].output_bounds.h = 2;

    default_config.display[0].virtual_screens[0].count_blends = 0;
    default_config.display[0].virtual_screens[0].count_help_lines = 0;
    default_config.display[0].virtual_screens[0].count_black_level_adjusts = 0;

    default_config.display[0].virtual_screens[0].color_balance.shadows.r = 0.0;
    default_config.display[0].virtual_screens[0].color_balance.shadows.g = 0.0;
    default_config.display[0].virtual_screens[0].color_balance.shadows.b = 0.0;

    default_config.display[0].virtual_screens[0].color_balance.midtones.r = 0.0;
    default_config.display[0].virtual_screens[0].color_balance.midtones.g = 0.0;
    default_config.display[0].virtual_screens[0].color_balance.midtones.b = 0.0;

    default_config.display[0].virtual_screens[0].color_balance.highlights.r = 0.0;
    default_config.display[0].virtual_screens[0].color_balance.highlights.g = 0.0;
    default_config.display[0].virtual_screens[0].color_balance.highlights.b = 0.0;

    default_config.display[0].virtual_screens[0].color_balance.preserve_luminosity = 1;

    default_config.display[0].virtual_screens[0].white_balance.bright.r = 0;
    default_config.display[0].virtual_screens[0].white_balance.bright.g = 0;
    default_config.display[0].virtual_screens[0].white_balance.bright.b = 0;

    default_config.display[0].virtual_screens[0].white_balance.exposure.r = 1.0;
    default_config.display[0].virtual_screens[0].white_balance.exposure.g = 1.0;
    default_config.display[0].virtual_screens[0].white_balance.exposure.b = 1.0;
}