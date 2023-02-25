#include <cjson/cJSON.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include "config-structs.h"
#include "config-parse.h"

void parse_config_bounds(cJSON *config_bounds_json, config_bounds *out) {
    out->x = cJSON_GetObjectItemCaseSensitive(config_bounds_json, "x")->valueint;
    out->y = cJSON_GetObjectItemCaseSensitive(config_bounds_json, "y")->valueint;
    out->w = cJSON_GetObjectItemCaseSensitive(config_bounds_json, "w")->valueint;
    out->h = cJSON_GetObjectItemCaseSensitive(config_bounds_json, "h")->valueint;
}

void parse_config_blend(cJSON *config_blend_json, config_blend *out) {
    parse_config_bounds(cJSON_GetObjectItemCaseSensitive(config_blend_json, "position"), &out->position);
    out->direction = cJSON_GetObjectItemCaseSensitive(config_blend_json, "direction")->valueint;
    out->use_curve = cJSON_GetObjectItemCaseSensitive(config_blend_json, "use_curve")->valueint;
}

void parse_config_help_line(cJSON *config_help_line_json, config_help_line *out) {
    out->x1 = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "x1")->valueint;
    out->x2 = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "x2")->valueint;
    out->y1 = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "y1")->valueint;
    out->y2 = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "y2")->valueint;
    out->line_width = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "line_width")->valuedouble;
}

void parse_config_color_factor(cJSON *config_color_factor_json, config_color_factor *out) {
    out->r = cJSON_GetObjectItemCaseSensitive(config_color_factor_json, "r")->valuedouble;
    out->g = cJSON_GetObjectItemCaseSensitive(config_color_factor_json, "g")->valuedouble;
    out->b = cJSON_GetObjectItemCaseSensitive(config_color_factor_json, "b")->valuedouble;
}

void parse_config_color_balance(cJSON *config_color_balance_json, config_color_balance *out) {
    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_color_balance_json, "shadows"), &out->shadows);
    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_color_balance_json, "midtones"), &out->midtones);
    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_color_balance_json, "highlights"), &out->highlights);

    out->preserve_luminosity = cJSON_IsTrue(cJSON_GetObjectItemCaseSensitive(config_color_balance_json, "preserve_luminosity"));
}

void parse_config_white_balance(cJSON *config_white_balance_json, config_white_balance *out) {
    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_white_balance_json, "bright"), &out->bright);
    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_white_balance_json, "exposure"), &out->exposure);
}

void parse_config_black_level_adjust(cJSON *config_black_level_adjust_json, config_black_level_adjust *out) {
    out->x1 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "x1")->valueint;
    out->x2 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "x2")->valueint;
    out->y1 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "y1")->valueint;
    out->y2 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "y2")->valueint;
    out->offset = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "offset")->valueint;
}

void parse_config_virtual_screen(cJSON *config_virtual_screen_json, config_virtual_screen *out) {
    out->source_render_id = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "source_render_id")->valueint;

    parse_config_bounds(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "input_bounds"), &out->input_bounds);
    parse_config_bounds(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "output_bounds"), &out->output_bounds);

    parse_config_color_balance(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "color_balance"), &out->color_balance);
    parse_config_white_balance(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "white_balance"), &out->white_balance);

    // Blends
    cJSON *blends = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "blends");

    out->count_blends = cJSON_GetArraySize(blends);
    out->blends = (config_blend*) calloc(out->count_blends, sizeof(config_blend));

    for (int i = 0; i < out->count_blends; i++) {
        parse_config_blend(cJSON_GetArrayItem(blends, i), &out->blends[i]);
    }

    // Help Lines
    cJSON *help_lines = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "help_lines");

    out->count_help_lines = cJSON_GetArraySize(help_lines);
    out->help_lines = (config_help_line*) calloc(out->count_help_lines, sizeof(config_help_line));

    for (int i = 0; i < out->count_help_lines; i++) {
        parse_config_help_line(cJSON_GetArrayItem(help_lines, i), &out->help_lines[i]);
    }

    // Black Level adjust
    cJSON *black_level_adjusts = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "black_level_adjusts");

    out->count_black_level_adjusts = cJSON_GetArraySize(black_level_adjusts);
    out->black_level_adjusts = (config_black_level_adjust*) calloc(out->count_black_level_adjusts, sizeof(config_black_level_adjust));

    for (int i = 0; i < out->count_black_level_adjusts; i++) {
        parse_config_black_level_adjust(cJSON_GetArrayItem(black_level_adjusts, i), &out->black_level_adjusts[i]);
    }
}

void parse_config_display(cJSON *config_display_json, config_display *out) {
    parse_config_bounds(cJSON_GetObjectItemCaseSensitive(config_display_json, "monitor_bounds"), &out->monitor_bounds);
    out->projection_enabled = cJSON_IsTrue(cJSON_GetObjectItemCaseSensitive(config_display_json, "projection_enabled"));

    // Virtual Screens
    cJSON *virtual_screens = cJSON_GetObjectItemCaseSensitive(config_display_json, "virtual_screens");

    out->count_virtual_screen = cJSON_GetArraySize(virtual_screens);
    out->virtual_screens = (config_virtual_screen*) calloc(out->count_virtual_screen, sizeof(config_virtual_screen));

    for (int i = 0; i < out->count_virtual_screen; i++) {
        parse_config_virtual_screen(cJSON_GetArrayItem(virtual_screens, i), &out->virtual_screens[i]);
    }
}

void parse_config_render(cJSON *config_render_json, config_render *out) {
    out->render_id = cJSON_GetObjectItemCaseSensitive(config_render_json, "render_id")->valueint;
    out->w = cJSON_GetObjectItemCaseSensitive(config_render_json, "w")->valueint;
    out->h = cJSON_GetObjectItemCaseSensitive(config_render_json, "h")->valueint;

    out->enable_render_background_assets = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_background_assets")->valueint;
    out->enable_render_image = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_image")->valueint;
    out->enable_render_video = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_video")->valueint;
    out->text_render_mode = cJSON_GetObjectItemCaseSensitive(config_render_json, "text_render_mode")->valueint;

    out->text_scale = cJSON_GetObjectItemCaseSensitive(config_render_json, "text_scale")->valuedouble;
}

void parse_projection_config(cJSON *projection_config_json, projection_config *out) {
    // Renders
    cJSON *renders = cJSON_GetObjectItemCaseSensitive(projection_config_json, "renders");

    out->count_renders = cJSON_GetArraySize(renders);
    out->renders = (config_render*) calloc(out->count_renders, sizeof(config_render));

    for (int i = 0; i < out->count_renders; i++) {
        parse_config_render(cJSON_GetArrayItem(renders, i), &out->renders[i]);
    }

    // Displays
    cJSON *display = cJSON_GetObjectItemCaseSensitive(projection_config_json, "display");

    out->count_display = cJSON_GetArraySize(display);
    out->display = (config_display*) calloc(out->count_display, sizeof(config_display));

    for (int i = 0; i < out->count_display; i++) {
        parse_config_display(cJSON_GetArrayItem(display, i), &out->display[i]);
    }
}