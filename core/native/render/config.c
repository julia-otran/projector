#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "cJSON.h"
#include "debug.h"
#include "config-parse.h"
#include "config-serialize.h"
#include "config.h"

static projection_config default_config;

void free_config_virtual_screen(config_virtual_screen *in) {
    if (in->blends) {
        free(in->blends);
    }

    if (in->help_lines) {
        free(in->help_lines);
    }

    if (in->black_level_adjusts) {
        free(in->black_level_adjusts);
    }
}

void free_config_display(config_display *in) {
    for (int i = 0; i < in->count_virtual_screen; i++) {
        free_config_virtual_screen(&in->virtual_screens[i]);
    }

    if (in->virtual_screens) {
        free(in->virtual_screens);
    }
}

void free_projection_config(projection_config *in) {
    if (in == &default_config) {
        return;
    }

    if (in->renders) {
        free(in->renders);
    }

    for (int i = 0; i < in->count_display; i++) {
        free_config_display(&in->display[i]);
    }

    if (in->display) {
        free(in->display);
    }

    free(in);
}

projection_config* load_config(const char *filePath) {
    if (filePath == NULL) {
        log_debug("Config file null or cannot access.\nReturning default\n.");
        return &default_config;
    }

    FILE* config_file;
    
    open_file(&config_file, filePath, "r");

    fseek(config_file, 0L, SEEK_END);

    unsigned long long size = ftell(config_file);

    log_debug("File string size %llu\n", size);

    if (size > 1024 * 1024) {
        log_debug("Config file too large. Skipped loading this config\n");
        fclose(config_file);
        return &default_config;
    }

    fseek(config_file, 0L, SEEK_SET);

    char *config_json_string = malloc(size);
    size_t read_size = fread(config_json_string, 1, size, config_file);
    fclose(config_file);

    if (read_size != size) {
        log_debug("Failed reading json data\n");
        free(config_json_string);
        return &default_config;
    }

    cJSON *json = cJSON_ParseWithLength(config_json_string, size);
    free(config_json_string);

    projection_config *config = (projection_config*) malloc(sizeof(projection_config));
    parse_projection_config(json, config);

    cJSON_Delete(json);

    log_debug("Config file parse success: '%s'\n", filePath);

    return config;
}

void generate_config(const char *file_path) {
    if (file_path == NULL) {
        return;
    }

    FILE* config_file;
    
    open_file(&config_file, file_path, "w");

    cJSON *json = serialize_projection_config(&default_config);

    char *config_json_string = cJSON_Print(json);
    fwrite(config_json_string, strlen(config_json_string), 1, config_file);
    fclose(config_file);

    free(config_json_string);
    cJSON_Delete(json);
}

int config_change_requires_restart(projection_config *config1, projection_config *config2) {
    if (config1->count_renders != config2->count_renders) {
        return 1;
    }

    for (int i = 0; i < config1->count_renders; i++) {
        config_render *render1 = &config1->renders[i];
        config_render *render2 = &config2->renders[i];

        if (render1->render_id != render2->render_id) {
            return 1;
        }

        if (render1->w != render2->w) {
            return 1;
        }

        if (render1->h != render2->h) {
            return 1;
        }
    }

    if (config1->count_display != config2->count_display) {
        return 1;
    }

    for (int i = 0; i < config1->count_display; i++) {
        config_display *display1 = &config1->display[i];
        config_display *display2 = &config2->display[i];

        if (display1->monitor_bounds.x != display2->monitor_bounds.x) {
            return 1;
        }

        if (display1->monitor_bounds.y != display2->monitor_bounds.y) {
            return 1;
        }

        if (display1->monitor_bounds.w != display2->monitor_bounds.w) {
            return 1;
        }

        if (display1->monitor_bounds.h != display2->monitor_bounds.h) {
            return 1;
        }

        if (display1->projection_enabled != display2->projection_enabled) {
            return 1;
        }
    }

    return 0;
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
    default_config.renders[0].render_mode = CONFIG_RENDER_MODE_MAIN;

    default_config.renders[0].background_clear_color.r = 0.0;
    default_config.renders[0].background_clear_color.g = 0.0;
    default_config.renders[0].background_clear_color.b = 0.0;

    default_config.renders[0].text_color.r = 1.0;
    default_config.renders[0].text_color.g = 1.0;
    default_config.renders[0].text_color.b = 0.0;

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

    default_config.display[0].virtual_screens[0].output_bounds.x = 0.0;
    default_config.display[0].virtual_screens[0].output_bounds.y = 0.0;
    default_config.display[0].virtual_screens[0].output_bounds.w = default_monitor_bounds->w;
    default_config.display[0].virtual_screens[0].output_bounds.h = default_monitor_bounds->h;

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