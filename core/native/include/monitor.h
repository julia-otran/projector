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
    GLFWmonitor *monitor;
    GLFWwindow *window;
    config_display *config;
    void **virtual_screen_data;
} display_window;

typedef struct {
    int width, height;
} monitor_info;

void monitors_reload();
void monitors_create_windows(projection_config *config);
void monitors_config_hot_reload(projection_config *config);

void monitors_shutdown();

void get_default_projection_monitor_bounds(config_bounds *in, int *no_secondary_mon);
GLFWwindow* get_gl_share_context();
int window_should_close();

void monitors_load_renders(render_output *data, int render_output_count);
void monitor_set_share_context();
void monitors_cycle();
void monitors_flip();
void monitors_terminate();

#endif
