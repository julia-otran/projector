#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "cJSON.h"
#include "debug.h"
#include "config-structs.h"
#include "config-parse.h"

void parse_config_bounds(cJSON *config_bounds_json, config_bounds *out) {
    out->x = cJSON_GetObjectItemCaseSensitive(config_bounds_json, "x")->valuedouble;
    out->y = cJSON_GetObjectItemCaseSensitive(config_bounds_json, "y")->valuedouble;
    out->w = cJSON_GetObjectItemCaseSensitive(config_bounds_json, "w")->valuedouble;
    out->h = cJSON_GetObjectItemCaseSensitive(config_bounds_json, "h")->valuedouble;
}

void parse_config_point(cJSON *config_point_json, config_point *out) {
    out->x = cJSON_GetObjectItemCaseSensitive(config_point_json, "x")->valuedouble;
    out->y = cJSON_GetObjectItemCaseSensitive(config_point_json, "y")->valuedouble;
}

void parse_config_point_mapping(cJSON *config_point_mapping_json, config_point_mapping *out) {
    cJSON *input_points_json = cJSON_GetObjectItemCaseSensitive(config_point_mapping_json, "input_points");
    cJSON *output_points_json = cJSON_GetObjectItemCaseSensitive(config_point_mapping_json, "output_points");

    int count_input_points = cJSON_GetArraySize(input_points_json);
    int count_output_points = cJSON_GetArraySize(output_points_json);

    if (count_input_points != count_output_points) {
        log_debug("Invalid point mapping. Input points: %i; Output points %i\n", count_input_points, count_output_points);
    }

    int count_points = count_input_points < count_output_points ? count_input_points : count_output_points;

    config_point *input_points = (config_point*) calloc(count_points, sizeof(config_point));
    config_point *output_points = (config_point*) calloc(count_points, sizeof(config_point));

    for (int i = 0; i < count_points; i++) {
        parse_config_point(cJSON_GetArrayItem(input_points_json, i), &input_points[i]);
        parse_config_point(cJSON_GetArrayItem(output_points_json, i), &output_points[i]);
    }

    out->input_points = input_points;
    out->output_points = output_points;
    out->count_points = count_points;

    cJSON *output_horizontal_adjust_factor_json = cJSON_GetObjectItemCaseSensitive(config_point_mapping_json, "output_horizontal_adjust_factor");

    if (cJSON_IsNumber(output_horizontal_adjust_factor_json)) {
        out->output_horizontal_adjust_factor = output_horizontal_adjust_factor_json->valuedouble;
    } else {
        out->output_horizontal_adjust_factor = 1.0;
    }

    cJSON *output_vertical_adjust_factor_json = cJSON_GetObjectItemCaseSensitive(config_point_mapping_json, "output_vertical_adjust_factor");

    if (cJSON_IsNumber(output_vertical_adjust_factor_json)) {
        out->output_vertical_adjust_factor = output_vertical_adjust_factor_json->valuedouble;
    } else {
        out->output_vertical_adjust_factor = 1.0;
    }
}

void parse_config_blend(cJSON *config_blend_json, config_blend *out) {
    parse_config_bounds(cJSON_GetObjectItemCaseSensitive(config_blend_json, "position"), &out->position);
    out->direction = cJSON_GetObjectItemCaseSensitive(config_blend_json, "direction")->valueint;

    cJSON* curve_exponent_json = cJSON_GetObjectItemCaseSensitive(config_blend_json, "curve_exponent");

    if (cJSON_IsNumber(curve_exponent_json)) {
        out->curve_exponent = curve_exponent_json->valuedouble;
    }
    else 
    {
        out->curve_exponent = 2.0;
    }
}

void parse_config_help_line(cJSON *config_help_line_json, config_help_line *out) {
    out->x1 = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "x1")->valueint;
    out->x2 = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "x2")->valueint;
    out->y1 = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "y1")->valueint;
    out->y2 = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "y2")->valueint;
    out->line_width = cJSON_GetObjectItemCaseSensitive(config_help_line_json, "line_width")->valuedouble;
}

void parse_config_color_factor(cJSON *config_color_factor_json, config_color_factor *out) {
    if (cJSON_IsObject(config_color_factor_json)) {
        out->r = cJSON_GetObjectItemCaseSensitive(config_color_factor_json, "r")->valuedouble;
        out->g = cJSON_GetObjectItemCaseSensitive(config_color_factor_json, "g")->valuedouble;
        out->b = cJSON_GetObjectItemCaseSensitive(config_color_factor_json, "b")->valuedouble;

        cJSON *alpha_json = cJSON_GetObjectItemCaseSensitive(config_color_factor_json, "a");

        if (cJSON_IsNumber(alpha_json)) {
            out->a = alpha_json->valuedouble;
        } else {
            out->a = 1.0;
        }
    } else {
        out->a = 1.0;
    }
}

void parse_config_black_level_adjust(cJSON *config_black_level_adjust_json, config_black_level_adjust *out) {
    out->x1 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "x1")->valueint;
    out->x2 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "x2")->valueint;
    out->x3 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "x3")->valueint;
    out->x4 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "x4")->valueint;

    out->y1 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "y1")->valueint;
    out->y2 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "y2")->valueint;
    out->y3 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "y3")->valueint;
    out->y4 = cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "y4")->valueint;

    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_black_level_adjust_json, "color"), &out->color);
}

void parse_config_color_matrix(cJSON* config_color_matrix_json, config_color_matrix* out) {
    out->r_to_r = 1.0;
    out->r_to_g = 0.0;
    out->r_to_b = 0.0;
    out->r_exposure = 1.0;
    out->r_bright = 0.0;

    out->g_to_r = 0.0;
    out->g_to_g = 1.0;
    out->g_to_b = 0.0;
    out->g_exposure = 1.0;
    out->g_bright = 0.0;

    out->b_to_r = 0.0;
    out->b_to_g = 0.0;
    out->b_to_b = 1.0;
    out->b_exposure = 1.0;
    out->b_bright = 0.0;

    if (!cJSON_IsObject(config_color_matrix_json)) {
        return;
    }

    cJSON* r_to_r_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "r_to_r");
    cJSON* r_to_g_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "r_to_g");
    cJSON* r_to_b_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "r_to_b");
    cJSON* r_exposure_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "r_exposure");
    cJSON* r_bright_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "r_bright");

    cJSON* g_to_r_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "g_to_r");
    cJSON* g_to_g_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "g_to_g");
    cJSON* g_to_b_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "g_to_b");
    cJSON* g_exposure_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "g_exposure");
    cJSON* g_bright_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "g_bright");
    
    cJSON* b_to_r_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "b_to_r");
    cJSON* b_to_g_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "b_to_g");
    cJSON* b_to_b_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "b_to_b");
    cJSON* b_exposure_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "b_exposure");
    cJSON* b_bright_json = cJSON_GetObjectItemCaseSensitive(config_color_matrix_json, "b_bright");

    if (cJSON_IsNumber(r_to_r_json)) {
        out->r_to_r = r_to_r_json->valuedouble;
    }

    if (cJSON_IsNumber(r_to_g_json)) {
        out->r_to_g = r_to_g_json->valuedouble;
    }

    if (cJSON_IsNumber(r_to_b_json)) {
        out->r_to_b = r_to_b_json->valuedouble;
    }

    if (cJSON_IsNumber(r_exposure_json)) {
        out->r_exposure = r_exposure_json->valuedouble;
    }

    if (cJSON_IsNumber(r_bright_json)) {
        out->r_bright = r_bright_json->valuedouble;
    }


    if (cJSON_IsNumber(g_to_r_json)) {
        out->g_to_r = g_to_r_json->valuedouble;
    }

    if (cJSON_IsNumber(g_to_g_json)) {
        out->g_to_g = g_to_g_json->valuedouble;
    }   
    
    if (cJSON_IsNumber(g_to_b_json)) {
        out->g_to_b = g_to_b_json->valuedouble;
    }

    if (cJSON_IsNumber(g_exposure_json)) {
        out->g_exposure = g_exposure_json->valuedouble;
    }

    if (cJSON_IsNumber(g_bright_json)) {
        out->g_bright = g_bright_json->valuedouble;
    }


    if (cJSON_IsNumber(b_to_r_json)) {
        out->b_to_r = b_to_r_json->valuedouble;
    }

    if (cJSON_IsNumber(b_to_g_json)) {
        out->b_to_g = b_to_g_json->valuedouble;
    }

    if (cJSON_IsNumber(b_to_b_json)) {
        out->b_to_b = b_to_b_json->valuedouble;
    }

    if (cJSON_IsNumber(b_exposure_json)) {
        out->b_exposure = b_exposure_json->valuedouble;
    }

    if (cJSON_IsNumber(b_bright_json)) {
        out->b_bright = b_bright_json->valuedouble;
    }
}

void parse_config_color_corrector_single(cJSON* config_color_corrector_json, config_color_corrector* out) {
    out->src_hue = 0.0;
    out->src_q = 1.0;
    out->dst_hue = 0.0;
    out->dst_sat = 1.0;
    out->dst_lum = 1.0;

    if (config_color_corrector_json == NULL) {
        return;
    }

    if (!cJSON_IsObject(config_color_corrector_json)) {
        return;
    }

    cJSON* src_hue_json = cJSON_GetObjectItemCaseSensitive(config_color_corrector_json, "src_hue");
    cJSON* src_q_json = cJSON_GetObjectItemCaseSensitive(config_color_corrector_json, "src_q");
    cJSON* dst_hue_json = cJSON_GetObjectItemCaseSensitive(config_color_corrector_json, "dst_hue");
    cJSON* dst_sat_json = cJSON_GetObjectItemCaseSensitive(config_color_corrector_json, "dst_sat");
    cJSON* dst_lum_json = cJSON_GetObjectItemCaseSensitive(config_color_corrector_json, "dst_lum");

    if (cJSON_IsNumber(src_hue_json)) {
        out->src_hue = src_hue_json->valuedouble;
    }
    if (cJSON_IsNumber(src_q_json)) {
        out->src_q = src_q_json->valuedouble;
    }
    if (cJSON_IsNumber(dst_hue_json)) {
        out->dst_hue = dst_hue_json->valuedouble;
    }
    if (cJSON_IsNumber(dst_sat_json)) {
        out->dst_sat = dst_sat_json->valuedouble;
    }
    if (cJSON_IsNumber(dst_lum_json)) {
        out->dst_lum = dst_lum_json->valuedouble;
    }
}

void parse_config_color_corrector(cJSON* config_color_corrector_multi_json, config_color_corrector* out) {
    for (int i = 0; i < CONFIG_COLOR_CORRECTOR_LENGTH; i++) {
        parse_config_color_corrector_single(NULL, &out[i]);
    }

    if (cJSON_IsObject(config_color_corrector_multi_json)) {
        parse_config_color_corrector_single(config_color_corrector_multi_json, out);

        return;
    }

    if (cJSON_IsArray(config_color_corrector_multi_json)) {
        int count = cJSON_GetArraySize(config_color_corrector_multi_json);

        for (int i = 0; i < CONFIG_COLOR_CORRECTOR_LENGTH; i++) {
            cJSON* config_color_corrector_single_json = NULL;

            if (i < count) {
                config_color_corrector_single_json = cJSON_GetArrayItem(config_color_corrector_multi_json, i);
                parse_config_color_corrector_single(config_color_corrector_single_json, &out[i]);
            }
        }

        return;
    }

    for (int i = 0; i < CONFIG_COLOR_CORRECTOR_LENGTH; i++) {
        parse_config_color_corrector_single(NULL, &out[i]);
    }
}

void parse_config_virtual_screen(cJSON *config_virtual_screen_json, config_virtual_screen *out) {
    out->source_render_id = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "source_render_id")->valueint;

    out->w = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "w")->valueint;
    out->h = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "h")->valueint;

    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "background_clear_color"), &out->background_clear_color);

    parse_config_bounds(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "render_input_bounds"), &out->render_input_bounds);

    parse_config_color_matrix(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "color_matrix"), &out->color_matrix);
    parse_config_color_corrector(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "color_corrector"), &out->color_corrector[0]);

    parse_config_point_mapping(cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "monitor_position"), &out->monitor_position);

    // Blends
    cJSON *blends = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "blends");

    out->count_blends = cJSON_GetArraySize(blends);

    if (out->count_blends) {
        out->blends = (config_blend*) calloc(out->count_blends, sizeof(config_blend));
    }

    for (int i = 0; i < out->count_blends; i++) {
        parse_config_blend(cJSON_GetArrayItem(blends, i), &out->blends[i]);
    }

    // Help Lines
    cJSON *help_lines = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "help_lines");

    out->count_help_lines = cJSON_GetArraySize(help_lines);

    if (out->count_help_lines) {
        out->help_lines = (config_help_line*) calloc(out->count_help_lines, sizeof(config_help_line));
    }

    for (int i = 0; i < out->count_help_lines; i++) {
        parse_config_help_line(cJSON_GetArrayItem(help_lines, i), &out->help_lines[i]);
    }

    // Black Level adjust
    cJSON *black_level_adjusts = cJSON_GetObjectItemCaseSensitive(config_virtual_screen_json, "black_level_adjusts");

    out->count_black_level_adjusts = cJSON_GetArraySize(black_level_adjusts);

    if (out->count_black_level_adjusts) {
        out->black_level_adjusts = (config_black_level_adjust*) calloc(out->count_black_level_adjusts, sizeof(config_black_level_adjust));
    }

    for (int i = 0; i < out->count_black_level_adjusts; i++) {
        parse_config_black_level_adjust(cJSON_GetArrayItem(black_level_adjusts, i), &out->black_level_adjusts[i]);
    }
}

void parse_config_display(cJSON *config_display_json, config_display *out) {
    parse_config_bounds(cJSON_GetObjectItemCaseSensitive(config_display_json, "monitor_bounds"), &out->monitor_bounds);
    out->projection_enabled = cJSON_GetObjectItemCaseSensitive(config_display_json, "projection_enabled")->valueint;

    // Virtual Screens
    cJSON *virtual_screens = cJSON_GetObjectItemCaseSensitive(config_display_json, "virtual_screens");

    out->count_virtual_screen = cJSON_GetArraySize(virtual_screens);

    if (out->count_virtual_screen) {
        out->virtual_screens = (config_virtual_screen*) calloc(out->count_virtual_screen, sizeof(config_virtual_screen));
    }

    for (int i = 0; i < out->count_virtual_screen; i++) {
        parse_config_virtual_screen(cJSON_GetArrayItem(virtual_screens, i), &out->virtual_screens[i]);
    }
}

void parse_config_render(cJSON *config_render_json, config_render *out) {
    out->render_id = cJSON_GetObjectItemCaseSensitive(config_render_json, "render_id")->valueint;
    out->w = cJSON_GetObjectItemCaseSensitive(config_render_json, "w")->valueint;
    out->h = cJSON_GetObjectItemCaseSensitive(config_render_json, "h")->valueint;

    cJSON *render_name_json = cJSON_GetObjectItemCaseSensitive(config_render_json, "render_name");

    if (cJSON_IsString(render_name_json) && render_name_json->valuestring != NULL) {
        out->render_name = calloc(1, strlen(render_name_json->valuestring) + 1);
        memcpy(out->render_name, render_name_json->valuestring, strlen(render_name_json->valuestring));
    } else {
        out->render_name = NULL;
    }

    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_render_json, "background_clear_color"), &out->background_clear_color);
    parse_config_color_factor(cJSON_GetObjectItemCaseSensitive(config_render_json, "text_color"), &out->text_color);

    parse_config_bounds(cJSON_GetObjectItemCaseSensitive(config_render_json, "text_area"), &out->text_area);

    cJSON* enable_render_background_blur_json = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_background_blur");

    if (cJSON_IsNumber(enable_render_background_blur_json)) {
        out->enable_render_background_blur = enable_render_background_blur_json->valueint;
    }
    else 
    {
        out->enable_render_background_blur = 0;
    }

    cJSON* enable_render_text_behind_and_ahead_json = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_text_behind_and_ahead");

    if (cJSON_IsNumber(enable_render_text_behind_and_ahead_json)) 
    {
        out->enable_render_text_behind_and_ahead = enable_render_text_behind_and_ahead_json->valueint;
    }
    else
    {
        out->enable_render_text_behind_and_ahead = 0;
    }

    out->enable_render_background_image = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_background_image")->valueint;
    out->enable_render_background_assets = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_background_assets")->valueint;
    out->enable_render_image = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_image")->valueint;
    out->enable_render_video = cJSON_GetObjectItemCaseSensitive(config_render_json, "enable_render_video")->valueint;
    out->render_mode = cJSON_GetObjectItemCaseSensitive(config_render_json, "render_mode")->valueint;

    cJSON *text_scale_json = cJSON_GetObjectItemCaseSensitive(config_render_json, "text_scale");

    if (cJSON_IsNumber(text_scale_json)) {
        out->text_scale = text_scale_json->valuedouble;
    } else {
        out->text_scale = 1.0;
    }
}

void parse_projection_config(cJSON *projection_config_json, projection_config *out) {
    // Renders
    cJSON *renders = cJSON_GetObjectItemCaseSensitive(projection_config_json, "renders");

    out->count_renders = cJSON_GetArraySize(renders);
    log_debug("Count renders: %i\n", out->count_renders);

    if (out->count_renders) {
        out->renders = (config_render*) calloc(out->count_renders, sizeof(config_render));
    }

    for (int i = 0; i < out->count_renders; i++) {
        parse_config_render(cJSON_GetArrayItem(renders, i), &out->renders[i]);
    }

    // Displays
    cJSON *display = cJSON_GetObjectItemCaseSensitive(projection_config_json, "display");

    out->count_display = cJSON_GetArraySize(display);
    log_debug("Count display: %i\n", out->count_display);

    if (out->count_display) {
        out->display = (config_display*) calloc(out->count_display, sizeof(config_display));
    }

    for (int i = 0; i < out->count_display; i++) {
        parse_config_display(cJSON_GetArrayItem(display, i), &out->display[i]);
    }
}
