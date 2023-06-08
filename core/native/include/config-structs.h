#ifndef _CONFIG_STRUCTS_H_
#define _CONFIG_STRUCTS_H_

typedef struct {
    double x, y;
} config_point;

typedef struct {
    config_point *input_points;
    config_point *output_points;

    double output_horizontal_adjust_factor;
    double output_vertical_adjust_factor;

    int count_points;
} config_point_mapping;

typedef struct {
    double x, y, w, h;
} config_bounds;

typedef struct {
    config_bounds position;
    int direction;
} config_blend;

typedef struct {
    int x1, x2, y1, y2;
    double line_width;
} config_help_line;

typedef struct {
    double r, g, b, a;
} config_color_factor;

typedef struct {
    config_color_factor shadows;
    config_color_factor midtones;
    config_color_factor highlights;

    double shadows_luminance;
    double midtones_luminance;
    double highlights_luminance;
} config_color_balance;

typedef struct {
    double
        r_to_r, r_to_g, r_to_b, r_trim,
        g_to_r, g_to_g, g_to_b, g_trim,
        b_to_r, b_to_g, b_to_b, b_trim;
} config_color_matrix;

typedef struct {
    int x1, x2, x3, x4, y1, y2, y3, y4;
    config_color_factor color;
} config_black_level_adjust;

typedef struct {
    int source_render_id, w, h;

    config_color_factor background_clear_color;

    config_bounds render_input_bounds;

    config_color_balance color_balance;
    config_color_matrix color_matrix;

    config_point_mapping monitor_position;

    int count_blends;
    config_blend *blends;

    int count_help_lines;
    config_help_line *help_lines;

    int count_black_level_adjusts;
    config_black_level_adjust *black_level_adjusts;
} config_virtual_screen;

typedef struct {
    config_bounds monitor_bounds;
    int projection_enabled;

    int count_virtual_screen;
    config_virtual_screen *virtual_screens;
} config_display;

// The texts will be adjusted to fill this render
// Impossible to scale the text, as its adjusted to this render.
// Required to have one (and only one) render as main mode.
#define CONFIG_RENDER_MODE_MAIN 1

// This is a secondary rendering,
// do not set the text according to this layer.
// Instead, text will be scaled to fill text_area
#define CONFIG_RENDER_MODE_SECONDARY 2

typedef struct {
    int render_id, w, h;

    char *render_name;

    config_bounds text_area;
    config_color_factor background_clear_color;
    config_color_factor text_color;

    double text_scale;

    int enable_render_background_assets;
    int enable_render_image;
    int enable_render_video;

    int render_mode;
} config_render;

typedef struct {
    int count_renders;
    config_render *renders;

    int count_display;
    config_display *display;
} projection_config;

#endif
