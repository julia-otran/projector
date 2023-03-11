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

cJSON* serialize_config_blend(config_blend *in) {
    cJSON *config_blend_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_blend_json, "position", serialize_config_bounds(&in->position));
    cJSON_AddItemToObject(config_blend_json, "direction", cJSON_CreateNumber(in->direction));

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

    return config_color_factor_json;
}

cJSON* serialize_config_color_balance(config_color_balance *in) {
    cJSON *config_color_balance_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_color_balance_json, "shadows", serialize_config_color_factor(&in->shadows));
    cJSON_AddItemToObject(config_color_balance_json, "midtones", serialize_config_color_factor(&in->midtones));
    cJSON_AddItemToObject(config_color_balance_json, "highlights", serialize_config_color_factor(&in->highlights));

    // TODO: Fix, it must be serialized as number
    cJSON_AddItemToObject(config_color_balance_json, "preserve_luminosity", cJSON_CreateNumber(in->preserve_luminosity));

    return config_color_balance_json;
}

cJSON* serialize_config_white_balance(config_white_balance *in) {
    cJSON *config_white_balance_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_white_balance_json, "bright", serialize_config_color_factor(&in->bright));
    cJSON_AddItemToObject(config_white_balance_json, "exposure", serialize_config_color_factor(&in->exposure));

    return config_white_balance_json;
}

cJSON* serialize_config_black_level_adjust(config_black_level_adjust *in) {
    cJSON *config_black_level_adjust_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_black_level_adjust_json, "x1", cJSON_CreateNumber(in->x1));
    cJSON_AddItemToObject(config_black_level_adjust_json, "y1", cJSON_CreateNumber(in->y1));
    cJSON_AddItemToObject(config_black_level_adjust_json, "x2", cJSON_CreateNumber(in->x2));
    cJSON_AddItemToObject(config_black_level_adjust_json, "y2", cJSON_CreateNumber(in->y2));
    cJSON_AddItemToObject(config_black_level_adjust_json, "alpha", cJSON_CreateNumber(in->alpha));

    cJSON_AddItemToObject(config_black_level_adjust_json, "color", serialize_config_color_factor(&in->color));

    return config_black_level_adjust_json;
}

cJSON* serialize_config_virtual_screen(config_virtual_screen *in) {
    cJSON *config_virtual_screen_json = cJSON_CreateObject();

    cJSON_AddItemToObject(config_virtual_screen_json, "source_render_id", cJSON_CreateNumber(in->source_render_id));
    cJSON_AddItemToObject(config_virtual_screen_json, "input_bounds", serialize_config_bounds(&in->input_bounds));
    cJSON_AddItemToObject(config_virtual_screen_json, "output_bounds", serialize_config_bounds(&in->output_bounds));
    cJSON_AddItemToObject(config_virtual_screen_json, "color_balance", serialize_config_color_balance(&in->color_balance));
    cJSON_AddItemToObject(config_virtual_screen_json, "white_balance", serialize_config_white_balance(&in->white_balance));

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

    cJSON_AddItemToObject(config_render_json, "enable_render_background_assets", cJSON_CreateNumber(in->enable_render_background_assets));
    cJSON_AddItemToObject(config_render_json, "enable_render_image", cJSON_CreateNumber(in->enable_render_image));
    cJSON_AddItemToObject(config_render_json, "enable_render_video", cJSON_CreateNumber(in->enable_render_video));
    cJSON_AddItemToObject(config_render_json, "render_mode", cJSON_CreateNumber(in->render_mode));

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
