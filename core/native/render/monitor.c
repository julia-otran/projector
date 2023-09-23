#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#pragma comment (lib, "dwmapi.lib")
#include <Windows.h>
#include <WinUser.h>
#include <dwmapi.h>
#endif

#include "debug.h"
#include "ogl-loader.h"
#include "monitor.h"
#include "virtual-screen.h"

static int monitors_count;
static monitor *monitors;

#define MAX_DISPLAYS 10

static int display_window_count;
static display_window display_windows[MAX_DISPLAYS];

static GLFWwindow *gl_share_context = NULL;

static render_output *render_output_config;
static int render_output_config_count;

void monitors_reload() {
    int found_monitors_count;

    GLFWmonitor** gl_monitors = glfwGetMonitors(&found_monitors_count);

    monitor *found_monitors = (monitor*) calloc(found_monitors_count, sizeof(monitor));

    for (int i = 0; i < found_monitors_count; i++) {
        found_monitors[i].gl_monitor = gl_monitors[i];
        found_monitors[i].mode = (GLFWvidmode *)glfwGetVideoMode(gl_monitors[i]);
        glfwGetMonitorPos(gl_monitors[i], &(found_monitors[i].xpos), &(found_monitors[i].ypos));
        found_monitors[i].is_primary = i == 0;
    }

    monitors_count = found_monitors_count;
    monitors = found_monitors;
}

int monitor_match_bounds(config_bounds *bounds, monitor *m) {
    return m->xpos == bounds->x &&
                       m->ypos == bounds->y &&
                       m->mode->width == bounds->w &&
                       m->mode->height == bounds->h;
}

void create_window(monitor *m, display_window *dw) {
    if (dw->window) {
        return;
    }

    GLFWmonitor *monitor = m->gl_monitor;
    GLFWvidmode *mode = m->mode;

    glfwWindowHint(GLFW_RED_BITS, mode->redBits);
    glfwWindowHint(GLFW_GREEN_BITS, mode->greenBits);
    glfwWindowHint(GLFW_BLUE_BITS, mode->blueBits);
    glfwWindowHint(GLFW_REFRESH_RATE, mode->refreshRate);

    glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
    glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
    glfwWindowHint(GLFW_CENTER_CURSOR, GLFW_FALSE);
    glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);

    glfwWindowHint(GLFW_SAMPLES, 4);

    dw->window = glfwCreateWindow(mode->width, mode->height, "Projector", monitor, gl_share_context);

    glfwSetInputMode(dw->window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

#ifdef _WIN32
    BOOL exclude = 1;
    HWND window_hwnd = glfwGetWin32Window(dw->window);
    DwmSetWindowAttribute(window_hwnd, DWMWA_EXCLUDED_FROM_PEEK, &exclude, sizeof(exclude));
#endif

    if (gl_share_context == NULL) {
        gl_share_context = dw->window;
    }
}

void create_non_fs_window(monitor* m, display_window *dw) {
    if (dw->window) {
        return;
    }

    GLFWvidmode* mode = m->mode;

    glfwWindowHint(GLFW_RED_BITS, mode->redBits);
    glfwWindowHint(GLFW_GREEN_BITS, mode->greenBits);
    glfwWindowHint(GLFW_BLUE_BITS, mode->blueBits);
    glfwWindowHint(GLFW_REFRESH_RATE, mode->refreshRate);

    glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);
    glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
    glfwWindowHint(GLFW_CENTER_CURSOR, GLFW_FALSE);
    glfwWindowHint(GLFW_FOCUSED, GLFW_FALSE);

    glfwWindowHint(GLFW_SAMPLES, 4);

    dw->window = glfwCreateWindow(mode->width / 2, mode->height / 2, "Projector", NULL, gl_share_context);

    glfwSetInputMode(dw->window, GLFW_STICKY_KEYS, GL_TRUE);
    glfwSetInputMode(dw->window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

    if (gl_share_context == NULL) {
        gl_share_context = dw->window;
    }
}

void destroy_display_window(display_window *dw) {
    if (!dw->window) {
        return;
    }

    if (gl_share_context == dw->window) {
        gl_share_context = NULL;
    }

    glfwDestroyWindow(dw->window);
    dw->window = NULL;
}

void monitors_shutdown() {
    gl_share_context = NULL;

    for (int i=0; i<display_window_count; i++) {
        display_window *dw = &display_windows[i];

        destroy_display_window(dw);

        if (dw->virtual_screen_data) {
            free(dw->virtual_screen_data);
        }
    }

    free(monitors);

    display_window_count = 0;
    monitors_count = 0;
}

void monitors_create_windows(projection_config *config) {
    display_window_count = config->count_display;

    if (display_window_count > MAX_DISPLAYS) {
        display_window_count = 0;
        log_debug("Maximum number of displays exceeded. This is unsupported. System will crash soon.");
        return;
    }

    for (int i=0; i<config->count_display; i++) {
        int found = 0;

        config_display *dsp = &config->display[i];
        display_window *dw = &display_windows[i];

        for (int j=0; j<monitors_count; j++) {
            monitor *m = &monitors[j];

            if (monitor_match_bounds(&dsp->monitor_bounds, m) && dsp->projection_enabled) {
                found = 1;

                create_window(m, dw);
                dw->config = dsp;
                dw->display_index = i;
            }
        }

        if (!found) {
            if (dsp->projection_enabled) {
                create_non_fs_window(&monitors[0], dw);
                dw->config = dsp;
                dw->display_index = i;
            } else {
                dw->config = NULL;
                dw->display_index = -1;
            }
        }
    }
}

void get_default_projection_monitor_bounds(config_bounds *in, int *no_secondary_mon) {
    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (!m->is_primary) {
            in->x = m->xpos;
            in->y = m->ypos;
            in->w = m->mode->width;
            in->h = m->mode->height;

            (*no_secondary_mon) = 0;
            return;
        }
    }

    log_debug("No secondary monitor found! Will use simulation mode\n");

    in->w = monitors[0].mode->width / 2;
    in->h = monitors[0].mode->height / 2;

    (*no_secondary_mon) = 1;
}

GLFWwindow* get_gl_share_context() {
    return gl_share_context;
}

int window_should_close() {
    for (int i=0; i<display_window_count; i++) {
        if (display_windows[i].window && glfwWindowShouldClose(display_windows[i].window)) {
            return 1;
        }
    }

    return 0;
}

render_output* get_render_output_config(config_virtual_screen *vs_config) {
    for (int i=0; i<render_output_config_count; i++) {
        if (render_output_config[i].render_id == vs_config->source_render_id) {
            return &render_output_config[i];
        }
    }

    return 0;
}

void monitor_set_context_if_need(GLFWwindow* context)
{
    if (glfwGetCurrentContext() != context)
    {
        glfwMakeContextCurrent(context);
    }
}

void monitor_set_share_context() {
    if (gl_share_context) {
        monitor_set_context_if_need(gl_share_context);
    }
}

void monitors_config_hot_reload(projection_config *config) {
    for (int i=0; i<display_window_count; i++) {
        display_window *dw = &display_windows[i];

        if (dw->window) {
            if (dw->virtual_screen_data) {
                for (int j=0; j<dw->config->count_virtual_screen; j++) {
                    monitor_set_share_context();
                    virtual_screen_shared_stop(dw->virtual_screen_data[j]);

                    monitor_set_context_if_need(dw->window);
                    virtual_screen_monitor_stop(dw->virtual_screen_data[j]);
                }

                free(dw->virtual_screen_data);
                dw->virtual_screen_data = NULL;
            }

            config_display *dsp = &config->display[dw->display_index];

            dw->config = dsp;
            dw->virtual_screen_data = (void**) calloc(dsp->count_virtual_screen, sizeof(void*));

            for (int k=0; k<dw->config->count_virtual_screen; k++) {
                config_virtual_screen *config_vs = &dw->config->virtual_screens[k];
                render_output *render = get_render_output_config(config_vs);

                monitor_set_share_context();
                virtual_screen_shared_start(dsp, render, config_vs, &dw->virtual_screen_data[k]);

                monitor_set_context_if_need(dw->window);
                virtual_screen_monitor_start(dsp, render, config_vs, dw->virtual_screen_data[k]);
            }
        }
    }
}

void monitors_load_renders(render_output *data, int render_output_count) {
    render_output_config = data;
    render_output_config_count = render_output_count;

    for (int i=0; i<display_window_count; i++) {
        display_window *dw = &display_windows[i];

        if (dw->window) {
            monitor_set_context_if_need(dw->window);

            if (dw->window == gl_share_context)
            {
                glfwSwapInterval(1);
            }
            else 
            {
                glfwSwapInterval(0);
            }

            glewInit();
            glEnable(GL_BLEND);
        }
    }

    monitor_set_share_context();
    virtual_screen_shared_initialize();
    virtual_screen_monitor_initialize();
}

void monitors_terminate() {
    for (int i=0; i<display_window_count; i++) {
        display_window *dw = &display_windows[i];

        if (dw->window) {
            if (dw->virtual_screen_data) {
                for (int j=0; j<dw->config->count_virtual_screen; j++) {
                    monitor_set_share_context();
                    virtual_screen_shared_stop(dw->virtual_screen_data[j]);
                    
                    monitor_set_context_if_need(dw->window);
                    virtual_screen_monitor_stop(dw->virtual_screen_data[j]);

                    dw->virtual_screen_data[j] = NULL;
                }

                free(dw->virtual_screen_data);
                dw->virtual_screen_data = NULL;
            }
        }
    }

    monitor_set_share_context();
    virtual_screen_shared_shutdown();
    virtual_screen_monitor_shutdown();
}

void monitors_cycle() {
    for (int i = 0; i < display_window_count; i++) {
        display_window *dw = &display_windows[i];

        if (dw->window && dw->config) {
            for (int j = 0; j < dw->config->count_virtual_screen; j++) {
                void* vs_data = dw->virtual_screen_data[j];
                virtual_screen_shared_render(&dw->config->virtual_screens[j], vs_data);
            }
        }
    }

    for (int i=0; i<display_window_count; i++) {
        display_window *dw = &display_windows[i];

        if (dw->window && dw->config) {
            monitor_set_context_if_need(dw->window);
            glEnable(GL_TEXTURE_2D);

            int width, height;
            glfwGetFramebufferSize(dw->window, &width, &height);
            glViewport(0, 0, width, height);

            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            glPushMatrix();
            glLoadIdentity();

            glOrtho(0.0, dw->config->monitor_bounds.w, dw->config->monitor_bounds.h, 0.0, 0.0, 1.0);

            for (int j=0; j < dw->config->count_virtual_screen; j++) {
                void *vs_data = dw->virtual_screen_data[j];
                virtual_screen_monitor_print(&dw->config->virtual_screens[j], vs_data);
            }

            glPopMatrix();
   
            glDisable(GL_TEXTURE_2D);
        }
    }
}

void monitors_flip() {
    for (int i = 0; i < display_window_count; i++) {
        display_window *dw = &display_windows[i];

        if (dw->window) {
            monitor_set_context_if_need(dw->window);
            glfwSwapBuffers(dw->window);
        }
    }
}
