#include <stdlib.h>
#include <string.h>
#include <Processing.NDI.Lib.h>

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
#include "render-window-capture.h"
#include "render-image.h"
#include "render-preview.h"
#include "window-capture.h"
#include "vlc-loader.h"
#include "device-capture.h"
#include "video-capture.h"
#include "render-video-capture.h"
#include "ndi-inputs.h"
#include "ndi-input.h"


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

#define jni_jstringToCharArr(env, jstr, char_out_arr) \
	const jchar *internal_jchar = (*env)->GetStringChars(env, jstr, 0); \
	int internal_jlen = (*env)->GetStringLength(env, jstr); \
	int internal_len = WideCharToMultiByte(1252, 0, internal_jchar, internal_jlen, NULL, 0, 0, 0); \
	char_out_arr = malloc(internal_len + 1); \
	WideCharToMultiByte(1252, 0, internal_jchar, internal_jlen, char_out_arr, internal_len, 0, 0); \
	char_out_arr[internal_len] = 0; \
	(*env)->ReleaseStringChars(env, jstr, internal_jchar);

#define jni_releaseCharArr(env, jstr, char_out_arr) \
	free(char_out_arr);

#define jni_charArrToJString(env, jstring_out, char_arr_in) \
    int wide_size = MultiByteToWideChar(1252, 0, char_arr_in, -1, ((void*)0), 0); \
    LPWSTR internal_tmp_out = calloc(wide_size, sizeof(WCHAR)); \
    MultiByteToWideChar(1252, 0, char_arr_in, -1, internal_tmp_out, wide_size); \
    jstring_out = (*env)->NewString(env, internal_tmp_out, wide_size - 1); \
    free(internal_tmp_out);

#endif

#ifdef __gnu_linux__

#define jni_jstringToCharArr(env, jstr, char_out_arr) \
	char_out_arr = (char*) (*env)->GetStringUTFChars(env, jstr, 0);

#define jni_releaseCharArr(env, jstr, char_out_arr) \
	(*env)->ReleaseStringUTFChars(env, jstr, char_out_arr); \

#define jni_charArrToJString(env, jstring_out, char_arr_in) \
    jstring_out = (*env)->NewStringUTF(env, char_arr_in); 

#endif

#ifdef __APPLE_CC__

#define jni_jstringToCharArr(env, jstr, char_out_arr) \
    char_out_arr = (char*) (*env)->GetStringUTFChars(env, jstr, 0);

#define jni_releaseCharArr(env, jstr, char_out_arr) \
    (*env)->ReleaseStringUTFChars(env, jstr, char_out_arr); \

#define jni_charArrToJString(env, jstring_out, char_arr_in) \
    jstring_out = (*env)->NewStringUTF(env, char_arr_in);

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

void glfwIntMonitorCallback(GLFWmonitor* monitor, int event) {
    if (config) {
        monitors_reload();
        monitors_adjust_windows(config);
    }
}

static JavaVM *jvm;
static jobject window_capture_jobject;
typedef struct {
    jobject obj;
} jobject_container;

void notify_window_capture_windows(char** window_names, int count) {
    if (window_capture_jobject == NULL) {
        return;
    }
    
    JNIEnv *env;
    
    (*jvm)->AttachCurrentThread(jvm, (void**)&env, NULL);
    
    jclass class = (*env)->GetObjectClass(env, window_capture_jobject);
    jmethodID method_id = (*env)->GetMethodID(env, class, "onWindowListDone", "([Ljava/lang/String;)V");
    
    jclass string_class = (*env)->FindClass(env, "java/lang/String");
    jobjectArray result = (*env)->NewObjectArray(env, count, string_class, NULL);
    jstring window_name;

    for (unsigned int i = 0; i < count; i++) {
        jni_charArrToJString(env, window_name, window_names[i]);
        (*env)->SetObjectArrayElement(env, result, i, window_name);
    }
    
    (*env)->CallObjectMethod(env, window_capture_jobject, method_id, result);
    
    (*env)->DeleteGlobalRef(env, window_capture_jobject);
    window_capture_jobject = NULL;
    
    (*jvm)->DetachCurrentThread(jvm);
}

void notify_ndi_device_change(ndi_inputs_device_list *input_list, ndi_inputs_callback_node_list *node_list) {
    JNIEnv *env;
    
    (*jvm)->AttachCurrentThread(jvm, (void**)&env, NULL);

    jclass device_class = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeNDIDevice");
    jfieldID device_name_field = (*env)->GetFieldID(env, device_class, "name", "Ljava/lang/String;");
    jfieldID device_url_field = (*env)->GetFieldID(env, device_class, "url", "Ljava/lang/String;");

    jobjectArray devices_array = (*env)->NewObjectArray(env, input_list->count, device_class, NULL);

    for (uint32_t i = 0; i < input_list->count; i++) {
        jobject device_object = (*env)->AllocObject(env, device_class);

        jstring device_name;
        jni_charArrToJString(env, device_name, input_list->devices[i].name);
        (*env)->SetObjectField(env, device_object, device_name_field, device_name);

        jstring device_url;
        jni_charArrToJString(env, device_url, input_list->devices[i].url_address);
        (*env)->SetObjectField(env, device_object, device_url_field, device_url);

        (*env)->SetObjectArrayElement(env, devices_array, i, device_object);
    }

    jclass callback_class = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeNDIDeviceFindCallback");
    jmethodID callback_method_id = (*env)->GetMethodID(env, callback_class, "onDevicesChanged", "([Ldev/juhouse/projector/projection2/BridgeNDIDevice;)V");

    for (
        ndi_inputs_callback_node_list *callback_node = node_list;
        callback_node != NULL;
        callback_node = callback_node->next
    ) {
        jobject_container *objc = (jobject_container*) callback_node->data;
        (*env)->CallObjectMethod(env, objc->obj, callback_method_id, devices_array);
    }

    (*jvm)->DetachCurrentThread(jvm);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_initialize(JNIEnv *env, jobject _) {
    (*env)->GetJavaVM(env, &jvm);
    
    if (!glfwInit()) {
        return;
    }

    config = NULL;

    glfwSetErrorCallback(glfwIntErrorCallback);
    glfwSetMonitorCallback(glfwIntMonitorCallback);

    render_video_create_mtx();

    window_capture_init(&notify_window_capture_windows);
    video_capture_init();

    NDIlib_initialize();
    ndi_inputs_init();
    ndi_inputs_set_callback(&notify_ndi_device_change);
    ndi_input_initialize();

    initialized = 1;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_runOnMainThreadLoop(JNIEnv* env, jobject _) {
    glfwPollEvents();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_loadConfig(JNIEnv *env, jobject _, jstring j_file_path) {
    CHECK_INITIALIZE
    configured = 0;

    monitors_reload();
    internal_lib_render_load_default_config();

    projection_config *new_config;

    if (j_file_path != NULL) {
        char* file_path;
        jni_jstringToCharArr(env, j_file_path, file_path);

        new_config = load_config(file_path);
        
        jni_releaseCharArr(env, j_file_path, file_path);
    } else {
        new_config = load_config(NULL);
    }

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

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_reload(JNIEnv *env, jobject _) {
    internal_lib_render_restart();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_generateConfig(JNIEnv *env, jobject _, jstring j_path) {
    char* file_path;
    jni_jstringToCharArr(env, j_path, file_path);

    generate_config(file_path);
    
    jni_releaseCharArr(env, j_path, file_path);
}

JNIEXPORT jobjectArray JNICALL Java_dev_juhouse_projector_projection2_Bridge_getRenderSettings(JNIEnv *env, jobject _) {
    jclass BridgeRenderClass = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeRender");

    jfieldID render_id_field = (*env)->GetFieldID(env, BridgeRenderClass, "renderId", "I");
    jfieldID render_name_field = (*env)->GetFieldID(env, BridgeRenderClass, "renderName", "Ljava/lang/String;");

    jfieldID text_scale_field = (*env)->GetFieldID(env, BridgeRenderClass, "textScale", "D");
    jfieldID enable_render_background_image_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderBackgroundImage", "Z");
    jfieldID enable_render_background_assets_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderBackgroundAssets", "Z");
    jfieldID enable_render_text_behind_and_ahead_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderTextBehindAndAhead", "Z");
    jfieldID enable_render_image_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderImage", "Z");
    jfieldID enable_render_video_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderVideo", "Z");
    jfieldID render_mode_field = (*env)->GetFieldID(env, BridgeRenderClass, "renderMode", "I");

    jfieldID render_width_field = (*env)->GetFieldID(env, BridgeRenderClass, "width", "I");
    jfieldID render_height_field = (*env)->GetFieldID(env, BridgeRenderClass, "height", "I");

    jfieldID render_text_area_x_field = (*env)->GetFieldID(env, BridgeRenderClass, "textAreaX", "I");
    jfieldID render_text_area_y_field = (*env)->GetFieldID(env, BridgeRenderClass, "textAreaY", "I");
    jfieldID render_text_area_width_field = (*env)->GetFieldID(env, BridgeRenderClass, "textAreaWidth", "I");
    jfieldID render_text_area_height_field = (*env)->GetFieldID(env, BridgeRenderClass, "textAreaHeight", "I");

    jobjectArray result = (*env)->NewObjectArray(env, config->count_renders, BridgeRenderClass, NULL);

    for (int i = 0; i < config->count_renders; i++) {
        config_render *render = &config->renders[i];
        jobject render_object = (*env)->AllocObject(env, BridgeRenderClass);

        (*env)->SetIntField(env, render_object, render_id_field, render->render_id);

        if (render->render_name != NULL) {
            jstring render_name;
            jni_charArrToJString(env, render_name, render->render_name);
            (*env)->SetObjectField(env, render_object, render_name_field, render_name);
        }

        (*env)->SetDoubleField(env, render_object, text_scale_field, render->text_scale);
        (*env)->SetBooleanField(env, render_object, enable_render_text_behind_and_ahead_field, render->enable_render_text_behind_and_ahead);
        (*env)->SetBooleanField(env, render_object, enable_render_background_image_field, render->enable_render_background_image);
        (*env)->SetBooleanField(env, render_object, enable_render_background_assets_field, render->enable_render_background_assets);
        (*env)->SetBooleanField(env, render_object, enable_render_image_field, render->enable_render_image);
        (*env)->SetBooleanField(env, render_object, enable_render_video_field, render->enable_render_video);
        (*env)->SetIntField(env, render_object, render_mode_field, render->render_mode);

        (*env)->SetIntField(env, render_object, render_width_field, render->w);
        (*env)->SetIntField(env, render_object, render_height_field, render->h);

        (*env)->SetIntField(env, render_object, render_text_area_x_field, render->text_area.x);
        (*env)->SetIntField(env, render_object, render_text_area_y_field, render->text_area.y);
        (*env)->SetIntField(env, render_object, render_text_area_width_field, render->text_area.w);
        (*env)->SetIntField(env, render_object, render_text_area_height_field, render->text_area.h);

        (*env)->SetObjectArrayElement(env, result, i, render_object);
    }

    return result;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setTextData(JNIEnv *env, jobject _, jobjectArray j_text_data_arr) {
    if (j_text_data_arr == NULL) {
        render_text_set_data(NULL, 0);
        return;
    }

    jclass BridgeTextDataClass = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeTextData");

    jfieldID render_id_field = (*env)->GetFieldID(env, BridgeTextDataClass, "renderId", "I");

    jfieldID image_data_field = (*env)->GetFieldID(env, BridgeTextDataClass, "imageData", "[I");
    jfieldID position_x_field = (*env)->GetFieldID(env, BridgeTextDataClass, "positionX", "I");
    jfieldID position_y_field = (*env)->GetFieldID(env, BridgeTextDataClass, "positionY", "I");
    jfieldID image_w_field = (*env)->GetFieldID(env, BridgeTextDataClass, "imageWidth", "I");
    jfieldID image_h_field = (*env)->GetFieldID(env, BridgeTextDataClass, "imageHeight", "I");
    jfieldID darken_background_field = (*env)->GetFieldID(env, BridgeTextDataClass, "darkenBackground", "Z");

    jfieldID x_field = (*env)->GetFieldID(env, BridgeTextDataClass, "x", "D");
    jfieldID y_field = (*env)->GetFieldID(env, BridgeTextDataClass, "y", "D");
    jfieldID w_field = (*env)->GetFieldID(env, BridgeTextDataClass, "w", "D");
    jfieldID h_field = (*env)->GetFieldID(env, BridgeTextDataClass, "h", "D");

    jsize data_length = (*env)->GetArrayLength(env, j_text_data_arr);

    render_text_data *datum = calloc(data_length, sizeof(render_text_data));

    for (int i = 0; i < data_length; i++) {
        render_text_data *data = &datum[i];
        jobject obj = (*env)->GetObjectArrayElement(env, j_text_data_arr, i);

        data->render_id = (*env)->GetIntField(env, obj, render_id_field);
        data->position_x = (*env)->GetIntField(env, obj, position_x_field);
        data->position_y = (*env)->GetIntField(env, obj, position_y_field);
        data->image_w = (*env)->GetIntField(env, obj, image_w_field);
        data->image_h = (*env)->GetIntField(env, obj, image_h_field);
        data->dark_background = (*env)->GetBooleanField(env, obj, darken_background_field);

        jintArray j_img_data = (*env)->GetObjectField(env, obj, image_data_field);
        jint *img_data = (*env)->GetIntArrayElements(env, j_img_data, NULL);

        data->image_data = (void*) malloc(data->image_w * data->image_h * 4);
        memcpy(data->image_data, img_data, data->image_w * data->image_h * 4);

        (*env)->ReleaseIntArrayElements(env, j_img_data, img_data, 0);

        data->x = (*env)->GetIntField(env, obj, x_field);
        data->y = (*env)->GetIntField(env, obj, y_field);
        data->w = (*env)->GetIntField(env, obj, w_field);
        data->h = (*env)->GetIntField(env, obj, h_field);
    }

    render_text_set_data(datum, data_length);
}

// Video render methods
JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_attachPlayerPtr
(JNIEnv* env, jobject _, jlong player_addr) {
    render_video_attach_player((void*)player_addr);
}

JNIEXPORT jobject JNICALL Java_dev_juhouse_projector_projection2_Bridge_downloadPlayerPreviewPtr
  (JNIEnv *env, jobject _, jlong player_addr, jobject j_buffer) {

    jint preview_w = 0;
    jint preview_h = 0;

    jclass VideoPreviewSizeClass = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeVideoPreviewSize");
    jfieldID width_field = (*env)->GetFieldID(env, VideoPreviewSizeClass, "width", "I");
    jfieldID height_field = (*env)->GetFieldID(env, VideoPreviewSizeClass, "height", "I");

    jobject video_preview_size_object = (*env)->AllocObject(env, VideoPreviewSizeClass);

    jbyte *data = (jbyte*) (*env)->GetDirectBufferAddress(env, j_buffer);
    jlong capacity = (*env)->GetDirectBufferCapacity(env, j_buffer);

    render_video_download_preview((void *) player_addr, data, capacity, &preview_w, &preview_h);

    (*env)->SetIntField(env, video_preview_size_object, width_field, preview_w);
    (*env)->SetIntField(env, video_preview_size_object, height_field, preview_h);

    return video_preview_size_object;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setVideoRenderFlagPtr
(JNIEnv* env, jobject _, jlong player_addr, jboolean crop, jint render_flag) {
    render_video_src_set_crop_video(crop);
    render_video_src_set_render((void*)player_addr, render_flag);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setMultiImageAsset
  (JNIEnv *env, jobject _, jintArray arr, jint width, jint height, jint render_id, jboolean crop) {
    if (arr) {
        jint *data = (*env)->GetIntArrayElements(env, arr, 0);
        render_image_set_image_multi(data, width, height, render_id, crop);
        (*env)->ReleaseIntArrayElements(env, arr, data, 0);
    } else {
        render_image_set_image_multi(NULL, 0, 0, render_id, crop);
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

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setRenderWebViewBuffer(JNIEnv *env, jobject _, jint render) {
    render_web_view_src_set_render(render);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_downloadPreviewData
(JNIEnv* env, jobject _, jint render_id, jobject j_buffer) {
    jbyte *data = (jbyte*) (*env)->GetDirectBufferAddress(env, j_buffer);
    render_preview_download_buffer(render_id, (void*) data);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_getWindowList(JNIEnv *env, jobject _, jobject callback) {
    window_capture_jobject = (*env)->NewGlobalRef(env, callback);
    window_capture_get_window_list();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setWindowCaptureWindowName(JNIEnv *env, jobject _, jstring j_window_name) {
    char* window_name;
    
    jni_jstringToCharArr(env, j_window_name, window_name);

    render_window_capture_src_set_window_name(window_name);

    jni_releaseCharArr(env, j_window_name, window_name);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setWindowCaptureRender(JNIEnv *env, jobject _, jint render) {
    render_window_capture_src_set_render(render);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setWindowCaptureCrop(JNIEnv *env, jobject _, jboolean crop) {
    render_window_capture_src_set_crop(crop);
}

JNIEXPORT jobjectArray JNICALL Java_dev_juhouse_projector_projection2_Bridge_getVideoCaptureDevices(JNIEnv* env, jobject _) {
    jclass BridgeCaptureDeviceClass = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeCaptureDevice");
    jfieldID BridgeCaptureDevice_DeviceNameField = (*env)->GetFieldID(env, BridgeCaptureDeviceClass, "deviceName", "Ljava/lang/String;");
    jfieldID BridgeCaptureDevice_ResolutionsField = (*env)->GetFieldID(env, BridgeCaptureDeviceClass, "resolutions", "[Ldev/juhouse/projector/projection2/BridgeCaptureDeviceResolution;");

    capture_device_enum* cap_enum = get_capture_devices();

    if (cap_enum == NULL) {
        return (*env)->NewObjectArray(env, 0, BridgeCaptureDeviceClass, NULL);
    }

    jclass BridgeCaptureDeviceResolutionClass = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeCaptureDeviceResolution");
    jfieldID BridgeCaptureDeviceResolution_Width = (*env)->GetFieldID(env, BridgeCaptureDeviceResolutionClass, "width", "I");
    jfieldID BridgeCaptureDeviceResolution_Height = (*env)->GetFieldID(env, BridgeCaptureDeviceResolutionClass, "height", "I");

    jobjectArray result_arr = (*env)->NewObjectArray(env, cap_enum->capture_device_count, BridgeCaptureDeviceClass, NULL);

    capture_device_node* cap_dev_node = cap_enum->capture_device_list;

    for (int i = 0; i < cap_enum->capture_device_count; i++) {
        jobjectArray resolutions_arr = (*env)->NewObjectArray(env, cap_dev_node->data->count_resolutions, BridgeCaptureDeviceResolutionClass, NULL);
        capture_device_resolution_node* resolution_node = cap_dev_node->data->resolutions;

        for (int j = 0; j < cap_dev_node->data->count_resolutions; j++) {
            jobject bridge_capture_device_resolution_obj = (*env)->AllocObject(env, BridgeCaptureDeviceResolutionClass);
            (*env)->SetIntField(env, bridge_capture_device_resolution_obj, BridgeCaptureDeviceResolution_Width, resolution_node->data->width);
            (*env)->SetIntField(env, bridge_capture_device_resolution_obj, BridgeCaptureDeviceResolution_Height, resolution_node->data->height);
            (*env)->SetObjectArrayElement(env, resolutions_arr, j, bridge_capture_device_resolution_obj);
            resolution_node = resolution_node->next;
        }

        jobject bridge_capture_device_obj = (*env)->AllocObject(env, BridgeCaptureDeviceClass);
        
        jstring temp_jstring;

        jni_charArrToJString(env, temp_jstring, cap_dev_node->data->name);

        (*env)->SetObjectField(env, bridge_capture_device_obj, BridgeCaptureDevice_DeviceNameField, temp_jstring);
        (*env)->SetObjectField(env, bridge_capture_device_obj, BridgeCaptureDevice_ResolutionsField, resolutions_arr);
        (*env)->SetObjectArrayElement(env, result_arr, i, bridge_capture_device_obj);

        cap_dev_node = cap_dev_node->next;
    }

    free_capture_device_enum(cap_enum);

    return result_arr;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setVideoCaptureDevice
(JNIEnv* env, jobject _, jstring jdevice_name, jint width, jint height)
{
    char* device_name;

    jni_jstringToCharArr(env, jdevice_name, device_name);

    render_video_capture_set_device(device_name, width, height);

    jni_releaseCharArr(env, jdevice_name, device_name);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_downloadVideoCapturePreview
(JNIEnv* env, jobject _, jobject j_buffer)
{
    jbyte* data = (jbyte*)(*env)->GetDirectBufferAddress(env, j_buffer);
    render_video_capture_download_preview((int*)data);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setVideoCaptureEnabled
(JNIEnv* env, jobject _, jboolean enabled)
{
    render_video_capture_set_enabled(enabled);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setVideoCaptureRender
(JNIEnv* env, jobject _, jint render_flag)
{
    render_video_capture_set_render(render_flag);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setVideoCaptureCrop
(JNIEnv* env, jobject _, jboolean crop)
{
    render_video_capture_set_crop(crop);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_addNDIDeviceFindCallback
(JNIEnv *env, jobject this, jobject callback)
{
    jobject callback_ref = (*env)->NewGlobalRef(env, callback);
    jobject_container *cont = calloc(1, sizeof(jobject_container));
    cont->obj = callback_ref;
    ndi_inputs_add_callback_node((void*) cont);
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_removeNDIDeviceFindCallback
  (JNIEnv *env, jobject this, jobject callback) 
{
    ndi_inputs_callback_node_list *list = ndi_inputs_get_callback_node_list();

    for (ndi_inputs_callback_node_list *node = list; node != NULL; node = node->next) {
        jobject_container *cont = (jobject_container*)node->data;

        if ((*env)->IsSameObject(env, callback, cont->obj)) {
            ndi_inputs_remove_callback_node(cont);
            (*env)->DeleteGlobalRef(env, cont->obj);
        }
    }
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_searchNDIDevices
  (JNIEnv *env, jobject this) 
{
    ndi_inputs_find_devices();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_shutdown(JNIEnv *env, jobject _) {
    CHECK_INITIALIZE

    main_loop_terminate();
    shutdown_renders();
    monitors_destroy_windows();
    window_capture_terminate();
    video_capture_terminate();
    ndi_inputs_terminate();
    ndi_input_terminate();

    NDIlib_destroy();

    glfwTerminate();
}
