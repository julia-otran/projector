#include <GLFW/glfw3.h>
#include <stdlib.h>

#include "monitor.h"
#include "config.h"
#include "lib-render.h"
#include "loop.h"

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_initialize(JNIEnv *, jobject) {
    if (!glfwInit()) {
        return;
    }

    reload_monitors();

    config_bounds default_monitor;
    get_default_projection_monitor_bounds(&default_monitor);
    prepare_default_config(&default_monitor);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_load_1config(JNIEnv *env, jobject, jstring j_file_path) {
    char *file_path;
    projection_config *config;

    if (j_file_path != NULL) {
        file_path = (*env)->GetStringUTFChars(env, j_file_path, 0);
        config = load_config(file_path);
        (*env)->ReleaseStringUTFChars(env, j_file_path, file_path);
    } else {
        config = load_config(NULL);
    }

    activate_monitors(config);
    activate_renders(get_gl_share_context(), config);

    start_main_loop();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_shutdown(JNIEnv *, jobject) {
    terminate_main_loop();
    shutdown_renders();
    shutdown_monitors();
    glfwTerminate();
}
