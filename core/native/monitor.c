#include <stdio.h>
#include <stdlib.h>

#include "debug.h"
#include "ogl-loader.h"
#include "monitor.h"
#include "virtual-screen.h"

static int monitors_count;
static monitor *monitors;
static GLFWwindow *gl_share_context = NULL;

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

    m->window = glfwCreateWindow(mode->width, mode->height, "Projector", NULL, gl_share_context);
    glfwSetWindowMonitor(m->window, monitor, m->xpos, m->ypos, mode->width, mode->height, mode->refreshRate);

	glfwSetInputMode(m->window, GLFW_STICKY_KEYS, GL_TRUE);
    glfwSetInputMode(m->window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

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
}

void shutdown_monitors() {
    gl_share_context = NULL;

    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];
        destroy_window(m);
    }
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
                m->virtual_screen_data = (void**) calloc(dsp->count_virtual_screen, sizeof(void*));

                create_window(m);
            }
        }

        if (!found) {
            m->config = NULL;
            destroy_window(m);
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

    log("No secondary monitor found! Will use simulation mode\n");

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

void prepare_monitors() {
    int width, height;

    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (m->window) {
            glfwMakeContextCurrent(m->window);
            glewInit();

            glfwGetFramebufferSize(m->window, &width, &height);

            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            glPushMatrix();
            glOrtho(0.f, width, height, 0.f, 0.f, 1.f );

            for (int j=0; j<m->config->count_virtual_screen; j++) {
                initialize_virtual_screen(&m->config->virtual_screens[i], &m->virtual_screen_data[i]);
            }

            glPopMatrix();
        }
    }
}

GLuint find_texture_id(render_output *data, int render_output_count, config_virtual_screen *vs_config) {
    for (int i=0; i<render_output_count; i++) {
        if (data[i].render_id == vs_config->source_render_id) {
            return data[i].rendered_texture;
        }
    }

    return 0;
}

void render_monitors(render_output *data, int render_output_count) {
    GLuint texture_id;
    int width, height;

    for (int i=0; i<monitors_count; i++) {
        monitor *m = &monitors[i];

        if (m->window) {
            glfwMakeContextCurrent(m->window);
            glfwGetFramebufferSize(m->window, &width, &height);

            glClear(GL_COLOR_BUFFER_BIT);

            glPushMatrix();
            glOrtho(0.f, width, height, 0.f, 0.f, 1.f );

            for (int j=0; j < m->config->count_virtual_screen; j++) {
                void *vs_data = &m->virtual_screen_data[i];
                texture_id = find_texture_id(data, render_output_count, &m->config->virtual_screens[i]);

                if (texture_id) {
                    render_virtual_screen(texture_id, vs_data);
                }
            }

            glPopMatrix();
        }
    }

    swap_monitor_buffers();
}