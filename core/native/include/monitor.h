#include <GLFW/glfw3.h>
#include "config.h"
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

void reload_monitors();
void activate_monitors(projection_config *config);
void swap_monitor_buffers();
void shutdown_monitors();

void get_default_projection_monitor_bounds(config_bounds *in);
GLFWmonitor* get_gl_share_context();
int window_should_close();

void prepare_monitors();
void render_monitors(render_output *data, int render_output_count);

#endif
