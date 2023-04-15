#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "debug.h"
#include "ogl-loader.h"
#include "monitor.h"
#include "virtual-screen.h"

static int monitors_count;
static monitor *monitors;
static GLFWwindow *gl_share_context = NULL;

static render_output *render_output_config;
static int render_output_config_count;

void reload_monitors() {
    int found_monitors_count;

    GLFWmonitor** gl_monitors = glfwGetMonitors(&found_monitors_count);

    monitor *found_monitors = (monitor*) calloc(found_monitors_count, sizeof(monitor));

    for (int i = 0; i < found_monitors_count; i++) {
        found_monitors[i].gl_monitor = gl_monitors[i];
        found_monitors[i].mode = (GLFWvidmode *)glfwGetVideoMode(gl_monitors[i]);
        glfwGetMonitorPos(gl_monitors[i], &(found_monitors[i].xpos), &(found_monitors[i].ypos));
        found_monitors[i].is_primary = i == 0;
        found_monitors[i].window = NULL;
        found_monitors[i].config = NULL;
        found_monitors[i].virtual_screen_data = NULL;
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

void create_window(monitor *m) {
    if (m->window) {
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

    glfwWindowHint(GLFW_SAMPLES, 6);

    m->window = glfwCreateWindow(mode->width, mode->height, "Projector", NULL, gl_share_context);
    glfwSetWindowMonitor(m->window, monitor, m->xpos, m->ypos, mode->width, mode->height, mode->refreshRate);

	glfwSetInputMode(m->window, GLFW_STICKY_KEYS, GL_TRUE);
    glfwSetInputMode(m->window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

    if (gl_share_context == NULL) {
        gl_share_context = m->window;
    }
}

void swap_monitor_buffers() {
    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (m->window) {
            glfwSwapBuffers(m->window);
        }
    }
}

void destroy_window(monitor *m) {
    if (!m->window) {
        return;
    }

    if (gl_share_context == m->window) {
        // Hope this does not happen
        // However in such case we will loose the ctx share, had to recreate all windows.
        // I will just prevent this window destruction for now.
        return;
    }

    glfwDestroyWindow(m->window);
    m->window = NULL;
}

void shutdown_monitors() {
    gl_share_context = NULL;

    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        destroy_window(m);

        if (m->virtual_screen_data) {
            free(m->virtual_screen_data);
        }
    }

    free(monitors);
}

void activate_monitors(projection_config *config) {
    for (int i=0; i<monitors_count; i++) {
        int found = 0;
        monitor *m = &monitors[i];

        for (int j=0; j<config->count_display; j++) {
            config_display *dsp = &config->display[j];

            if (monitor_match_bounds(&dsp->monitor_bounds, m) && dsp->projection_enabled) {
                found = 1;
                m->config = dsp;
                create_window(m);
            }
        }

        if (!found) {
            destroy_window(m);
            m->config = NULL;
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

    in->w = 1280;
    in->h = 720;
    (*no_secondary_mon) = 1;
}

GLFWwindow* get_gl_share_context() {
    return gl_share_context;
}

int window_should_close() {
    for (int i=0; i<monitors_count; i++) {
        if (monitors[i].window && glfwWindowShouldClose(monitors[i].window)) {
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

void monitors_config_hot_reload(projection_config *config) {
    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (m->window) {
            glfwMakeContextCurrent(m->window);

            if (m->virtual_screen_data) {
                for (int j=0; j<m->config->count_virtual_screen; j++) {
                    virtual_screen_stop(m->virtual_screen_data[j]);
                }

                free(m->virtual_screen_data);
                m->virtual_screen_data = NULL;
            }

            for (int j = 0; j < config->count_display; j++) {
                config_display *dsp = &config->display[j];

                if (monitor_match_bounds(&dsp->monitor_bounds, m) && dsp->projection_enabled) {
                    m->config = dsp;
                    m->virtual_screen_data = (void**) calloc(dsp->count_virtual_screen, sizeof(void*));

                    for (int k=0; k<m->config->count_virtual_screen; k++) {
                        config_virtual_screen *config_vs = &m->config->virtual_screens[k];
                        render_output *render = get_render_output_config(config_vs);

                        virtual_screen_start(dsp, render, config_vs, &m->virtual_screen_data[k]);
                    }
                }
            }
        }
    }
}

void monitors_init(render_output *data, int render_output_count) {
    int width, height;

    render_output_config = data;
    render_output_config_count = render_output_count;

    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (m->window) {
            glfwMakeContextCurrent(m->window);
            glfwSwapInterval(1);

            glewInit();

            glfwGetFramebufferSize(m->window, &width, &height);
            glViewport(0, 0, width, height);
        }
    }

    virtual_screen_initialize();
}

void monitors_terminate() {
    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (m->window) {
            glfwMakeContextCurrent(m->window);

            if (m->virtual_screen_data) {
                for (int j=0; j<m->config->count_virtual_screen; j++) {
                    virtual_screen_stop(m->virtual_screen_data[j]);
                    m->virtual_screen_data[j] = NULL;
                }

                free(m->virtual_screen_data);
                m->virtual_screen_data = NULL;
            }
        }
    }

    virtual_screen_shutdown();
}

void monitor_prepare_renders_context() {
    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (m->window) {
            glfwMakeContextCurrent(m->window);
            break;
        }
    }
}

void monitors_cycle() {
    int width, height;

    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (m->window) {
            glfwMakeContextCurrent(m->window);
            glfwGetFramebufferSize(m->window, &width, &height);

            for (int j=0; j < m->config->count_virtual_screen; j++) {
                void *vs_data = m->virtual_screen_data[j];

                virtual_screen_render(&m->config->virtual_screens[j], vs_data);
            }

            glViewport(0, 0, width, height);

            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glPushMatrix();
            glOrtho(0.0, width, height, 0.0, 0.0, 1.0);

            for (int j=0; j < m->config->count_virtual_screen; j++) {
                void *vs_data = m->virtual_screen_data[j];
                virtual_screen_print(&m->config->virtual_screens[j], vs_data);
            }

            glPopMatrix();
        }
    }

    swap_monitor_buffers();
    register_monitor_frame();
}