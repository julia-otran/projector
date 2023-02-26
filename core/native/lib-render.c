#include <stdlib.h>

#include "debug.h"
#include "ogl-loader.h"
#include "monitor.h"
#include "config-structs.h"
#include "config.h"
#include "lib-render.h"
#include "loop.h"
#include "config-debug.h"

static int initialized = 0;

#define CHECK_INITIALIZE {\
    if (!initialized) {\
        return;\
    }\
}

void glfwIntErrorCallback(GLint, const GLchar *error_string) {
    log_debug("Catch GLFW error: %s\n", error_string);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_initialize(JNIEnv *, jobject) {
    if (!glfwInit()) {
        return;
    }

    glfwSetErrorCallback(glfwIntErrorCallback);

    reload_monitors();

    config_bounds default_monitor;
    int no_display;

    get_default_projection_monitor_bounds(&default_monitor, &no_display);
    prepare_default_config(&default_monitor, no_display);

    initialized = 1;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_load_1config(JNIEnv *env, jobject, jstring j_file_path) {
    char *file_path;
    projection_config *config;

    CHECK_INITIALIZE

    if (j_file_path != NULL) {
        file_path = (char*) (*env)->GetStringUTFChars(env, j_file_path, 0);
        log_debug("String:\n%s\n", file_path);
        config = load_config(NULL);
        //config = load_config(file_path);
        (*env)->ReleaseStringUTFChars(env, j_file_path, file_path);
    } else {
        config = load_config(NULL);
    }

    log_debug("Will load config:\n");
    print_projection_config(config);

    activate_monitors(config);
    activate_renders(get_gl_share_context(), config);

    start_main_loop();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_shutdown(JNIEnv *, jobject) {
    CHECK_INITIALIZE

    terminate_main_loop();
    shutdown_renders();
    shutdown_monitors();
    glfwTerminate();
}
