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

    GLFWwindow *window;

    config_display *config;

    void **virtual_screen_data;
} monitor;

typedef struct {
    int width, height;
} monitor_info;

void reload_monitors();
int activate_monitors(projection_config *config);
void monitors_config_hot_reload(projection_config *config);

void shutdown_monitors();

void get_default_projection_monitor_bounds(config_bounds *in, int *no_secondary_mon);
GLFWwindow* get_gl_share_context();
int window_should_close();

void monitors_init(render_output *data, int render_output_count);
void monitor_set_share_context();
void monitors_cycle();
void monitors_flip();
void monitors_terminate();

int monitors_get_minor_refresh_rate();

#endif
