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
static projection_config *config;

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

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_loadConfig(JNIEnv *env, jobject, jstring j_file_path) {
    CHECK_INITIALIZE

    if (config) {
        log_debug("Config was loaded! restarting engine.\n");
        terminate_main_loop();
        shutdown_renders();
        shutdown_monitors();
        free_projection_config(config);
    }

    if (j_file_path != NULL) {
        char *file_path = (char*) (*env)->GetStringUTFChars(env, j_file_path, 0);
        config = load_config(file_path);
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

JNIEXPORT jint JNICALL Java_dev_juhouse_projector_projection2_Bridge_getTextRenderAreaWidth(JNIEnv *, jobject) {
    config_bounds text_area;
    get_main_text_area(&text_area);
    return (jint) text_area.w;
}

JNIEXPORT jint JNICALL Java_dev_juhouse_projector_projection2_Bridge_getTextRenderAreaHeight(JNIEnv *, jobject) {
    config_bounds text_area;
    get_main_text_area(&text_area);
    return (jint) text_area.h;
}

JNIEXPORT jint JNICALL Java_dev_juhouse_projector_projection2_Bridge_getRenderAreaWidth(JNIEnv *, jobject) {
    render_output_size out_size;
    get_main_output_size(&out_size);
    return out_size.render_width;
}

JNIEXPORT jint JNICALL Java_dev_juhouse_projector_projection2_Bridge_getRenderAreaHeight(JNIEnv *, jobject) {
    render_output_size out_size;
    get_main_output_size(&out_size);
    return out_size.render_height;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setTextImage
  (JNIEnv *, jobject, jintArray, jint) {
  }


JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setVideoBuffer
  (JNIEnv *, jobject, jobject, jint, jint, jboolean) {
  }

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setRenderVideoBuffer
  (JNIEnv *, jobject, jboolean) {
  }

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setImageAsset
  (JNIEnv *, jobject, jintArray, jint, jint, jboolean) {
  }

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setImageBackgroundAsset
  (JNIEnv *, jobject, jintArray, jint, jint, jboolean) {
  }


JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_shutdown(JNIEnv *, jobject) {
    CHECK_INITIALIZE

    terminate_main_loop();
    shutdown_renders();
    shutdown_monitors();
    glfwTerminate();
}
