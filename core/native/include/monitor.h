#include "ogl-loader.h"
#include "config-structs.h"
#include "render.h"

#ifndef _MONITOR_H_
#define _MONITOR_H_

typedef struct {
    GLFWmonitor *gl_monitor;
    GLFWvidmode *mode;
    int xpos;
    int ypos;
    int is_primary;
} monitor;

typedef struct {
    int display_index;
    GLFWwindow *window;
    config_display *config;
    void **virtual_screen_data;
    int active;
    int refresh_rate;
} display_window;

typedef struct {
    int width, height;
} monitor_info;

GLFWwindow* monitors_get_shared_window();
void monitors_set_share_context();

void monitors_reload();
void monitors_adjust_windows(projection_config* config);

void monitors_create_windows(projection_config *config);
void monitors_destroy_windows();

void monitors_config_hot_reload(projection_config *config);

void monitors_get_default_projection_bounds(config_bounds *in);
int window_should_close();

void monitors_load_renders(render_output* data, int render_output_count);
void monitors_start(projection_config* config);
void monitors_stop();

void monitors_cycle();
void monitors_flip();
void monitors_terminate();

#endif
