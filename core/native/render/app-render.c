#include <stdlib.h>
#include <string.h>
#include <inttypes.h>

#include "debug.h"
#include "ogl-loader.h"
#include "monitor.h"
#include "config-structs.h"
#include "config.h"
#include "app-render.h"
#include "loop.h"
#include "config-debug.h"
#include "render-video.h"
#include "render-text.h"
#include "render-web-view.h"
#include "render-window-capture.h"
#include "render-image.h"
#include "render-preview.h"
#include "window-capture.h"
#include "vlc-loader.h"
#include "device-capture.h"
#include "video-capture.h"
#include "render-video-capture.h"

#include "blend.fragment.shader.h"
#include "blend.vertex.shader.h"
#include "blur.fragment.shader.h"
#include "blur.vertex.shader.h"
#include "color-corrector.fragment.shader.h"
#include "color-corrector.vertex.shader.h"
#include "direct.fragment.shader.h"
#include "direct.vertex.shader.h"

static int initialized = 0;
static int configured = 0;
static projection_config *config;

#define CHECK_INITIALIZE {\
    if (!initialized) {\
        return;\
    }\
}

#ifdef _WIN32

#include <windows.h>

#endif


void internal_lib_render_shutdown() {
    log_debug("Shutting down main loop...\n");
    main_loop_terminate();

    log_debug("Shutting down renders...\n");
    shutdown_renders();

    log_debug("Shutting down monitors...\n");
    monitors_destroy_windows();
}

void internal_lib_render_load_default_config() {
    config_bounds default_monitor;

    monitors_get_default_projection_bounds(&default_monitor);
    prepare_default_config(&default_monitor);
}

void internal_lib_render_startup() {
    log_debug("Will load config:\n");
    print_projection_config(config);

    log_debug("Initialize renders...\n");
    initialize_renders();

    log_debug("Creating windows...\n");
    monitors_create_windows(config);

    log_debug("Initializing async transfer windows...\n");
    activate_renders(monitors_get_shared_window(), config);

    log_debug("Starting main loop...\n")
    main_loop_schedule_config_reload(config);
    main_loop_start();
}

void internal_lib_render_restart() {
    log_debug("Engine Reload!!");

    internal_lib_render_shutdown();
    
    log_debug("Reload monitors");
    monitors_reload();
    internal_lib_render_load_default_config();
    internal_lib_render_startup();
}

void loadShaders() {
    add_shader_data_len("blend.fragment.shader", blend_fragment_shader, blend_fragment_shader_len);
    add_shader_data_len("blend.vertex.shader", blend_vertex_shader, blend_vertex_shader_len);
    add_shader_data_len("blur.fragment.shader", blur_fragment_shader, blur_fragment_shader_len);
    add_shader_data_len("blur.vertex.shader", blur_vertex_shader, blur_vertex_shader_len);
    add_shader_data_len("color-corrector.fragment.shader", color_corrector_fragment_shader, color_corrector_fragment_shader_len);
    add_shader_data_len("color-corrector.vertex.shader", color_corrector_vertex_shader, color_corrector_vertex_shader_len);
    add_shader_data_len("direct.fragment.shader", direct_fragment_shader, direct_fragment_shader_len);
    add_shader_data_len("direct.vertex.shader", direct_vertex_shader, direct_vertex_shader_len);
}

void glfwIntErrorCallback(GLint _, const GLchar *error_string) {
    log_debug("Catch GLFW error: %s\n", error_string);
}

void glfwIntMonitorCallback(GLFWmonitor* monitor, int event) {
    if (config) {
        monitors_reload();
        monitors_adjust_windows(config);
    }
}

void notify_window_capture_windows(char** window_names, int count) {
}

void initialize() {
    if (!glfwInit()) {
        return;
    }

    config = NULL;

    glfwSetErrorCallback(glfwIntErrorCallback);
    glfwSetMonitorCallback(glfwIntMonitorCallback);

    render_video_create_mtx();

    window_capture_init(&notify_window_capture_windows);
    video_capture_init();

    initialized = 1;
}

void runOnMainThreadLoop() {
    glfwPollEvents();
}

void loadConfig(char *path) {
    CHECK_INITIALIZE
    configured = 0;

    monitors_reload();
    internal_lib_render_load_default_config();

    projection_config *new_config;

    new_config = load_config(path);

    if (config) {
        if (!config_change_requires_restart(new_config, config)) {
            log_debug("New config was loaded! hot reloading...\n");
            projection_config* old_config = config;

            config = new_config;

            renders_config_hot_reload(config);
            main_loop_schedule_config_reload(config);
            free_projection_config(old_config);

            return;
        } else {
            internal_lib_render_shutdown();   
            free_projection_config(config);
        }
    }

    log_debug("Staring engine...\n");

    config = new_config;

    internal_lib_render_startup();

    configured = 1;
}

void reload() {
    internal_lib_render_restart();
}

void shutdown() {
    CHECK_INITIALIZE

    main_loop_terminate();
    shutdown_renders();
    monitors_destroy_windows();
    window_capture_terminate();
    video_capture_terminate();

    glfwTerminate();
}

char* get_config_path_from_args(int argc, char** argv) {
    for (int i = 0; i < argc - 1; i++) {
        if (argv[i][0] == '-' && argv[i][1] == 'c') {
            return argv[i+1];
        }
    }
}

void print_options() {
    log_debug("Press 'h' to hot reload configs;\n");
    log_debug("Press 'r' to restart the engine;\n");
    log_debug("Press 'g' to save default configs to the config file;\n");
    log_debug("Press 'q' to quit;\n");
}

int main(int argc, char** argv) {
    log_debug("app-render invoked\n");
    
    loadShaders();
    initialize();

    capture_device_enum* cap_enum = get_capture_devices();

    if (cap_enum == NULL) {
        goto terminate;
    }

    capture_device_node* cap_dev_node = cap_enum->capture_device_list;
    capture_device_resolution_node* resolution_node;

    log_debug("Printing capture devices...\n");

    int device_id = 1;

    for (int i = 0; i < cap_enum->capture_device_count; i++) {
        resolution_node = cap_dev_node->data->resolutions;

        for (int j = 0; j < cap_dev_node->data->count_resolutions; j++) {
            log_debug("[%i] %s: %ix%i\n", device_id, cap_dev_node->data->name, resolution_node->data->width, resolution_node->data->height);
            resolution_node = resolution_node->next;
            device_id++;
        }

        cap_dev_node = cap_dev_node->next;
    }

    log_debug("Type capture device number: ");

    char device_id_str[3];

    fgets(device_id_str, sizeof(device_id_str), stdin);

    uintmax_t num = strtoumax(device_id_str, NULL, 10);
    
    device_id = 1;
    cap_dev_node = cap_enum->capture_device_list;

    for (int i = 0; i < cap_enum->capture_device_count; i++) {
        resolution_node = cap_dev_node->data->resolutions;

        for (int j = 0; j < cap_dev_node->data->count_resolutions; j++) {
            if (device_id == num) { break; }

            resolution_node = resolution_node->next;
            device_id++;
        }

        if (device_id == num) { break; }
        cap_dev_node = cap_dev_node->next;
    }

    log_debug("Chosen Device: '%s' (%ix%i)\n", cap_dev_node->data->name, resolution_node->data->width, resolution_node->data->height);


    loadConfig(get_config_path_from_args(argc, argv));

    render_video_capture_set_device(cap_dev_node->data->name, resolution_node->data->width, resolution_node->data->height);
    render_video_capture_set_enabled(1);
    render_video_capture_set_crop(1);
    render_video_capture_set_render(0xFF);

    free_capture_device_enum(cap_enum);

    fd_set rfds;
    struct timeval tv;
    int retval, len;
    char buff[100] = {0};

    tv.tv_sec = 2;
    tv.tv_usec = 0;

    while (1) {
        FD_ZERO(&rfds);
        FD_SET(0, &rfds);

        runOnMainThreadLoop();

        retval = select(1, &rfds, NULL, NULL, &tv);

        if (FD_ISSET(0, &rfds)) {
            fgets(buff, sizeof(buff), stdin);

            if (buff[0] == 'r') {
                reload();
            } else if (buff[0] == 'q') {
                goto terminate;
            } else if (buff[0] == 'g') {
                generate_config(get_config_path_from_args(argc, argv));
                log_debug("Config generated\n");
            } else if (buff[0] == 'h') {
                loadConfig(get_config_path_from_args(argc, argv));
            }
        }
    }

terminate:
    shutdown();
}