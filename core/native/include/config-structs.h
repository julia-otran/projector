#ifndef _CONFIG_STRUCTS_H_
#define _CONFIG_STRUCTS_H_

typedef struct {
    double x, y, w, h;
} config_bounds;

typedef struct {
    config_bounds position;
    int direction;
    int use_curve;
} config_blend;

typedef struct {
    int x1, x2, y1, y2;
    double line_width;
} config_help_line;

typedef struct {
    double r, g, b;
} config_color_factor;

typedef struct {
    config_color_factor shadows;
    config_color_factor midtones;
    config_color_factor highlights;

    int preserve_luminosity;
} config_color_balance;

typedef struct {
    config_color_factor bright;
    config_color_factor exposure;
} config_white_balance;

typedef struct {
    int x1, x2, y1, y2;
    int offset;
} config_black_level_adjust;

typedef struct {
    int source_render_id;

    config_bounds input_bounds;
    config_bounds output_bounds;

    int count_blends;
    config_blend *blends;

    int count_help_lines;
    config_help_line *help_lines;

    int count_black_level_adjusts;
    config_black_level_adjust *black_level_adjusts;

    config_color_balance color_balance;
    config_white_balance white_balance;
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
#define TEXT_RENDER_MODE_MAIN 1

// This is a secondary rendering, do not adjust the text according to this layer.
// Enables scaling
#define TEXT_RENDER_MODE_SECONDARY 2

// Text will be positioned like captions, at the bottom of the render
#define TEXT_RENDER_MODE_CAPTION_POSITIONED 4

typedef struct {
    int render_id, w, h;

    config_color_factor background_clear_color;

    int enable_render_background_assets;
    int enable_render_image;
    int enable_render_video;

    int text_render_mode;
    float text_scale;
} config_render;

typedef struct {
    int count_renders;
    config_render *renders;

    int count_display;
    config_display *display;
} projection_config;

#endif