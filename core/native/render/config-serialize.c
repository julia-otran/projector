#include "cJSON.h"
#include "config-structs.h"
#include "config-serialize.h"

cJSON* serialize_config_bounds(config_bounds *in) {
    cJSON *config_bounds_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_bounds_json, "x", cJSON_CreateNumber(in->x));
    cJSON_AddItemToObject(config_bounds_json, "y", cJSON_CreateNumber(in->y));
    cJSON_AddItemToObject(config_bounds_json, "w", cJSON_CreateNumber(in->w));
    cJSON_AddItemToObject(config_bounds_json, "h", cJSON_CreateNumber(in->h));

    return config_bounds_json;
}

cJSON* serialize_config_point(config_point *in) {
    cJSON *config_point_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_point_json, "x", cJSON_CreateNumber(in->x));
    cJSON_AddItemToObject(config_point_json, "y", cJSON_CreateNumber(in->y));

    return config_point_json;
}

cJSON* serialize_config_point_mapping(config_point_mapping *in) {
    cJSON *config_point_mapping_json = cJSON_CreateObject();

    cJSON *input_points_json = cJSON_CreateArray();
    cJSON *output_points_json = cJSON_CreateArray();

    for (int i = 0; i < in->count_points; i++) {
        cJSON_AddItemToArray(input_points_json, serialize_config_point(&in->input_points[i]));
        cJSON_AddItemToArray(output_points_json, serialize_config_point(&in->output_points[i]));
    }

    cJSON_AddItemToObject(config_point_mapping_json, "input_points", input_points_json);
    cJSON_AddItemToObject(config_point_mapping_json, "output_points", output_points_json);

    cJSON_AddItemToObject(
        config_point_mapping_json,
        "output_horizontal_adjust_factor",
        cJSON_CreateNumber(in->output_horizontal_adjust_factor)
    );

    cJSON_AddItemToObject(
        config_point_mapping_json,
        "output_horizontal_adjust_factor",
        cJSON_CreateNumber(in->output_horizontal_adjust_factor)
    );

    cJSON_AddItemToObject(
        config_point_mapping_json,
        "output_vertical_adjust_factor",
        cJSON_CreateNumber(in->output_vertical_adjust_factor)
    );

    return config_point_mapping_json;
}

cJSON* serialize_config_blend(config_blend *in) {
    cJSON *config_blend_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_blend_json, "position", serialize_config_bounds(&in->position));
    cJSON_AddItemToObject(config_blend_json, "direction", cJSON_CreateNumber(in->direction));
    cJSON_AddItemToObject(config_blend_json, "curve_exponent", cJSON_CreateNumber(in->curve_exponent));

    return config_blend_json;
}

cJSON* serialize_config_help_line(config_help_line *in) {
    cJSON *config_help_line_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_help_line_json, "x1", cJSON_CreateNumber(in->x1));
    cJSON_AddItemToObject(config_help_line_json, "y1", cJSON_CreateNumber(in->y1));
    cJSON_AddItemToObject(config_help_line_json, "x2", cJSON_CreateNumber(in->x2));
    cJSON_AddItemToObject(config_help_line_json, "y2", cJSON_CreateNumber(in->y2));
    cJSON_AddItemToObject(config_help_line_json, "line_width", cJSON_CreateNumber(in->line_width));

    return config_help_line_json;
}

cJSON* serialize_config_color_factor(config_color_factor *in) {
    cJSON *config_color_factor_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_color_factor_json, "r", cJSON_CreateNumber(in->r));
    cJSON_AddItemToObject(config_color_factor_json, "g", cJSON_CreateNumber(in->g));
    cJSON_AddItemToObject(config_color_factor_json, "b", cJSON_CreateNumber(in->b));
    cJSON_AddItemToObject(config_color_factor_json, "a", cJSON_CreateNumber(in->a));

    return config_color_factor_json;
}

cJSON* serialize_config_color_matrix(config_color_matrix* in) {
    cJSON* config_color_matrix_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_color_matrix_json, "r_to_r", cJSON_CreateNumber(in->r_to_r));
    cJSON_AddItemToObject(config_color_matrix_json, "r_to_g", cJSON_CreateNumber(in->r_to_g));
    cJSON_AddItemToObject(config_color_matrix_json, "r_to_b", cJSON_CreateNumber(in->r_to_b));
    cJSON_AddItemToObject(config_color_matrix_json, "r_exposure", cJSON_CreateNumber(in->r_exposure));
    cJSON_AddItemToObject(config_color_matrix_json, "r_bright", cJSON_CreateNumber(in->r_bright));

    cJSON_AddItemToObject(config_color_matrix_json, "g_to_r", cJSON_CreateNumber(in->g_to_r));
    cJSON_AddItemToObject(config_color_matrix_json, "g_to_g", cJSON_CreateNumber(in->g_to_g));
    cJSON_AddItemToObject(config_color_matrix_json, "g_to_b", cJSON_CreateNumber(in->g_to_b));
    cJSON_AddItemToObject(config_color_matrix_json, "g_exposure", cJSON_CreateNumber(in->g_exposure));
    cJSON_AddItemToObject(config_color_matrix_json, "g_bright", cJSON_CreateNumber(in->g_bright));

    cJSON_AddItemToObject(config_color_matrix_json, "b_to_r", cJSON_CreateNumber(in->b_to_r));
    cJSON_AddItemToObject(config_color_matrix_json, "b_to_g", cJSON_CreateNumber(in->b_to_g));
    cJSON_AddItemToObject(config_color_matrix_json, "b_to_b", cJSON_CreateNumber(in->b_to_b));
    cJSON_AddItemToObject(config_color_matrix_json, "b_exposure", cJSON_CreateNumber(in->b_exposure));
    cJSON_AddItemToObject(config_color_matrix_json, "b_bright", cJSON_CreateNumber(in->b_bright));

    return config_color_matrix_json;
}

cJSON* serialize_config_color_corrector(config_color_corrector* in) {
    cJSON* config_color_corrector_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_color_corrector_json, "src_hue", cJSON_CreateNumber(in->src_hue));
    cJSON_AddItemToObject(config_color_corrector_json, "src_q", cJSON_CreateNumber(in->src_q));
    cJSON_AddItemToObject(config_color_corrector_json, "dst_hue", cJSON_CreateNumber(in->dst_hue));
    cJSON_AddItemToObject(config_color_corrector_json, "dst_sat", cJSON_CreateNumber(in->dst_sat));
    cJSON_AddItemToObject(config_color_corrector_json, "dst_lum", cJSON_CreateNumber(in->dst_lum));

    return config_color_corrector_json;
}

cJSON* serialize_config_black_level_adjust(config_black_level_adjust *in) {
    cJSON *config_black_level_adjust_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_black_level_adjust_json, "x1", cJSON_CreateNumber(in->x1));
    cJSON_AddItemToObject(config_black_level_adjust_json, "x2", cJSON_CreateNumber(in->x2));
    cJSON_AddItemToObject(config_black_level_adjust_json, "x3", cJSON_CreateNumber(in->x3));
    cJSON_AddItemToObject(config_black_level_adjust_json, "x3", cJSON_CreateNumber(in->x4));

    cJSON_AddItemToObject(config_black_level_adjust_json, "y1", cJSON_CreateNumber(in->y1));
    cJSON_AddItemToObject(config_black_level_adjust_json, "y2", cJSON_CreateNumber(in->y2));
    cJSON_AddItemToObject(config_black_level_adjust_json, "y3", cJSON_CreateNumber(in->y3));
    cJSON_AddItemToObject(config_black_level_adjust_json, "y4", cJSON_CreateNumber(in->y4));

    cJSON_AddItemToObject(config_black_level_adjust_json, "color", serialize_config_color_factor(&in->color));

    return config_black_level_adjust_json;
}

cJSON* serialize_config_virtual_screen(config_virtual_screen *in) {
    cJSON *config_virtual_screen_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_virtual_screen_json, "source_render_id", cJSON_CreateNumber(in->source_render_id));
    cJSON_AddItemToObject(config_virtual_screen_json, "w", cJSON_CreateNumber(in->w));
    cJSON_AddItemToObject(config_virtual_screen_json, "h", cJSON_CreateNumber(in->h));

    cJSON_AddItemToObject(config_virtual_screen_json, "background_clear_color", serialize_config_color_factor(&in->background_clear_color));

    cJSON_AddItemToObject(config_virtual_screen_json, "render_input_bounds", serialize_config_bounds(&in->render_input_bounds));

    cJSON_AddItemToObject(config_virtual_screen_json, "color_matrix", serialize_config_color_matrix(&in->color_matrix));
    cJSON_AddItemToObject(config_virtual_screen_json, "color_corrector", serialize_config_color_corrector(&in->color_corrector));
    
    cJSON_AddItemToObject(config_virtual_screen_json, "monitor_position", serialize_config_point_mapping(&in->monitor_position));

    cJSON *blends_json = cJSON_CreateArray();
    for (int i=0; i < in->count_blends; i++) {
        cJSON_AddItemToArray(blends_json, serialize_config_blend(&in->blends[i]));
    }
    cJSON_AddItemToObject(config_virtual_screen_json, "blends", blends_json);

    cJSON *help_lines_json = cJSON_CreateArray();
    for (int i=0; i < in->count_help_lines; i++) {
        cJSON_AddItemToArray(help_lines_json, serialize_config_help_line(&in->help_lines[i]));
    }
    cJSON_AddItemToObject(config_virtual_screen_json, "help_lines", help_lines_json);

    cJSON *black_level_adjusts_json = cJSON_CreateArray();
    for (int i=0; i < in->count_black_level_adjusts; i++) {
        cJSON_AddItemToArray(black_level_adjusts_json, serialize_config_black_level_adjust(&in->black_level_adjusts[i]));
    }
    cJSON_AddItemToObject(config_virtual_screen_json, "black_level_adjusts", black_level_adjusts_json);

    return config_virtual_screen_json;
}

cJSON* serialize_config_display(config_display *in) {
    cJSON *config_display_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_display_json, "monitor_bounds", serialize_config_bounds(&in->monitor_bounds));
    cJSON_AddItemToObject(config_display_json, "projection_enabled", cJSON_CreateNumber(in->projection_enabled));

    cJSON *virtual_screens_json = cJSON_CreateArray();
    for (int i=0; i < in->count_virtual_screen; i++) {
        cJSON_AddItemToArray(virtual_screens_json, serialize_config_virtual_screen(&in->virtual_screens[i]));
    }
    cJSON_AddItemToObject(config_display_json, "virtual_screens", virtual_screens_json);

    return config_display_json;
}

cJSON* serialize_config_render(config_render *in) {
    cJSON *config_render_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_render_json, "render_id", cJSON_CreateNumber(in->render_id));

    if (in->render_name == NULL) {
        cJSON_AddItemToObject(config_render_json, "render_name", cJSON_CreateNull());
    } else {
        cJSON_AddItemToObject(config_render_json, "render_name", cJSON_CreateString(in->render_name));
    }

    cJSON_AddItemToObject(config_render_json, "w", cJSON_CreateNumber(in->w));
    cJSON_AddItemToObject(config_render_json, "h", cJSON_CreateNumber(in->h));

    cJSON_AddItemToObject(config_render_json, "text_area", serialize_config_bounds(&in->text_area));

    cJSON_AddItemToObject(config_render_json, "enable_render_background_blur", cJSON_CreateNumber(in->enable_render_background_blur));
    cJSON_AddItemToObject(config_render_json, "enable_render_background_assets", cJSON_CreateNumber(in->enable_render_background_assets));
    cJSON_AddItemToObject(config_render_json, "enable_render_text_behind_and_ahead", cJSON_CreateNumber(in->enable_render_text_behind_and_ahead));
    cJSON_AddItemToObject(config_render_json, "enable_render_image", cJSON_CreateNumber(in->enable_render_image));
    cJSON_AddItemToObject(config_render_json, "enable_render_video", cJSON_CreateNumber(in->enable_render_video));
    cJSON_AddItemToObject(config_render_json, "render_mode", cJSON_CreateNumber(in->render_mode));
    cJSON_AddItemToObject(config_render_json, "text_scale", cJSON_CreateNumber(in->text_scale));

    cJSON_AddItemToObject(config_render_json, "background_clear_color", serialize_config_color_factor(&in->background_clear_color));
    cJSON_AddItemToObject(config_render_json, "text_color", serialize_config_color_factor(&in->text_color));

    return config_render_json;
}

cJSON* serialize_projection_config(projection_config *in) {
    cJSON *projection_config_json = cJSON_CreateObject();

    cJSON *renders_json = cJSON_CreateArray();
    for (int i=0; i < in->count_renders; i++) {
        cJSON_AddItemToArray(renders_json, serialize_config_render(&in->renders[i]));
    }
    cJSON_AddItemToObject(projection_config_json, "renders", renders_json);

    cJSON *display_json = cJSON_CreateArray();
    for (int i=0; i < in->count_display; i++) {
        cJSON_AddItemToArray(display_json, serialize_config_display(&in->display[i]));
    }
    cJSON_AddItemToObject(projection_config_json, "display", display_json);

    return projection_config_json;
}
