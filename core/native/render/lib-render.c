#include <stdlib.h>

#include "debug.h"
#include "ogl-loader.h"
#include "monitor.h"
#include "config-structs.h"
#include "config.h"
#include "lib-render.h"
#include "loop.h"
#include "config-debug.h"
#include "render-video.h"
#include "render-text.h"
#include "render-web-view.h"
#include "render-image.h"
#include "render-preview.h"

static int initialized = 0;
static projection_config *config;

#define CHECK_INITIALIZE {\
    if (!initialized) {\
        return;\
    }\
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_loadShader(JNIEnv *env, jobject _, jstring shader_name, jstring shader_data) {
    char *name = (char*) (*env)->GetStringUTFChars(env, shader_name, 0);
    char *data = (char*) (*env)->GetStringUTFChars(env, shader_data, 0);

    add_shader_data(name, data);

    (*env)->ReleaseStringUTFChars(env, shader_name, name);
    (*env)->ReleaseStringUTFChars(env, shader_data, data);
}

void glfwIntErrorCallback(GLint _, const GLchar *error_string) {
    log_debug("Catch GLFW error: %s\n", error_string);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_initialize(JNIEnv *env, jobject _) {
    if (!glfwInit()) {
        return;
    }

    glfwSetErrorCallback(glfwIntErrorCallback);

    reload_monitors();

    config_bounds default_monitor;
    int no_display;

    get_default_projection_monitor_bounds(&default_monitor, &no_display);
    prepare_default_config(&default_monitor, no_display);

    initialize_renders();

    initialized = 1;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_loadConfig(JNIEnv *env, jobject _, jstring j_file_path) {
    CHECK_INITIALIZE

    projection_config *new_config;

    if (j_file_path != NULL) {
        char *file_path = (char*) (*env)->GetStringUTFChars(env, j_file_path, 0);
        new_config = load_config(file_path);
        (*env)->ReleaseStringUTFChars(env, j_file_path, file_path);
    } else {
        new_config = load_config(NULL);
    }

    log_debug("Will load config:\n");
    print_projection_config(new_config);

    if (config) {
        if (!config_change_requires_restart(new_config, config)) {
            log_debug("New config was loaded! hot reloading...\n");

            projection_config *old_config = config;
            config = new_config;

            renders_config_hot_reload(config);
            main_loop_schedule_config_reload(config);

            free_projection_config(old_config);
            return;
        } else {
            log_debug("New config was loaded! restart engine required.\n");

            log_debug("Shutting down main loop...\n");
            main_loop_terminate();
            log_debug("Shutting down renders...\n");
            shutdown_renders();
            log_debug("Freeing configs...\n");
            free_projection_config(config);
        }
    }

    log_debug("Staring engine...\n");
    config = new_config;

    activate_monitors(config);
    activate_renders(get_gl_share_context(), config);
    main_loop_schedule_config_reload(config);
    main_loop_start();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_generateConfig(JNIEnv *env, jobject _, jstring j_path) {
    char *file_path = (char*) (*env)->GetStringUTFChars(env, j_path, 0);
    generate_config(file_path);
    (*env)->ReleaseStringUTFChars(env, j_path, file_path);
}

JNIEXPORT jint JNICALL Java_dev_juhouse_projector_projection2_Bridge_getTextRenderAreaWidth(JNIEnv *env, jobject _) {
    config_bounds text_area;
    get_main_text_area(&text_area);
    return (jint) text_area.w;
}

JNIEXPORT jint JNICALL Java_dev_juhouse_projector_projection2_Bridge_getTextRenderAreaHeight(JNIEnv *env, jobject _) {
    config_bounds text_area;
    get_main_text_area(&text_area);
    return (jint) text_area.h;
}

JNIEXPORT jint JNICALL Java_dev_juhouse_projector_projection2_Bridge_getRenderAreaWidth(JNIEnv *env, jobject _) {
    render_output_size out_size;
    get_main_output_size(&out_size);
    return out_size.render_width;
}

JNIEXPORT jint JNICALL Java_dev_juhouse_projector_projection2_Bridge_getRenderAreaHeight(JNIEnv *env, jobject _) {
    render_output_size out_size;
    get_main_output_size(&out_size);
    return out_size.render_height;
}

JNIEXPORT jobjectArray JNICALL Java_dev_juhouse_projector_projection2_Bridge_getRenderSettings(JNIEnv *env, jobject _) {
    jclass BridgeRenderClass = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeRender");

    jfieldID render_id_field = (*env)->GetFieldID(env, BridgeRenderClass, "renderId", "I");
    jfieldID render_name_field = (*env)->GetFieldID(env, BridgeRenderClass, "renderName", "Ljava/lang/String;");
    jfieldID enable_render_background_assets_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderBackgroundAssets", "Z");
    jfieldID enable_render_image_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderImage", "Z");
    jfieldID enable_render_video_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderVideo", "Z");

    jobjectArray result = (*env)->NewObjectArray(env, config->count_renders, BridgeRenderClass, NULL);

    for (int i = 0; i < config->count_renders; i++) {
        config_render *render = &config->renders[i];
        jobject render_object = (*env)->AllocObject(env, BridgeRenderClass);

        (*env)->SetIntField(env, render_object, render_id_field, render->render_id);

        (*env)->SetObjectField(env, render_object, render_name_field, (*env)->NewStringUTF(env, render->render_name));

        (*env)->SetBooleanField(env, render_object, enable_render_background_assets_field, render->enable_render_background_assets);
        (*env)->SetBooleanField(env, render_object, enable_render_image_field, render->enable_render_image);
        (*env)->SetBooleanField(env, render_object, enable_render_video_field, render->enable_render_video);

        (*env)->SetObjectArrayElement(env, result, i, render_object);
    }

    return result;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setTextImage
  (JNIEnv *env, jobject _, jintArray arr, jint text_height) {

    if (arr) {
        jint *data = (*env)->GetIntArrayElements(env, arr, 0);
        render_text_set_image((void*) data);
        (*env)->ReleaseIntArrayElements(env, arr, data, 0);
    } else {
        render_text_set_image(NULL);
    }
  }

// Video render methods
JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setVideoBuffer
  (JNIEnv *env, jobject _, jobject j_buffer, jint width, jint height, jboolean crop) {

    jbyte *data = (jbyte*) (*env)->GetDirectBufferAddress(env, j_buffer);
    render_video_src_set_crop_video(crop);
    render_video_src_set_buffer((void*)data, width, height);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setVideoBufferRenderFlag(JNIEnv *env, jobject _, jint render) {
    render_video_src_set_render(render);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_updateVideoBuffer
  (JNIEnv *env, jobject _) {

  render_video_src_buffer_update();

  }

// Image render methods
JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setImageAsset
  (JNIEnv *env, jobject _, jintArray arr, jint width, jint height, jboolean crop) {
    if (arr) {
        jint *data = (*env)->GetIntArrayElements(env, arr, 0);
        render_image_set_image((void*) data, width, height, crop);
        (*env)->ReleaseIntArrayElements(env, arr, data, 0);
    } else {
        render_image_set_image(NULL, 0, 0, 0);
    }
  }

// WebView render methods
JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setWebViewBuffer
  (JNIEnv *env, jobject _, jobject j_buffer, jint width, jint height) {

    // Null buffer isn't expected, set render false instead
    jbyte *data = (jbyte*) (*env)->GetDirectBufferAddress(env, j_buffer);
    render_web_view_src_set_buffer((void*) data, width, height);
    render_web_view_src_buffer_update();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setRenderWebViewBuffer(JNIEnv *env, jobject _, jboolean render) {
    render_web_view_src_set_render(render);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_downloadPreviewData(JNIEnv *env, jobject _, jobject j_buffer) {
    jbyte *data = (jbyte*) (*env)->GetDirectBufferAddress(env, j_buffer);
    render_preview_download_buffer((void*) data);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_shutdown(JNIEnv *env, jobject _) {
    CHECK_INITIALIZE

    main_loop_terminate();
    shutdown_renders();
    shutdown_monitors();
    glfwTerminate();
}
