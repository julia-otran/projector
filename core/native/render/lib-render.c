#include <stdlib.h>
#include <string.h>

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

static int initialized = 0;
static int configured = 0;
static projection_config *config;

#define CHECK_INITIALIZE {\
    if (!initialized) {\
        return;\
    }\
}

#ifdef _WIN32

#define VLC_DEVICE_PREFIX "dshow://:dshow-vdev="

#pragma comment(lib, "strmiids.lib")

#include <windows.h>
#include <stringapiset.h>
#include <combaseapi.h>
#include <dshow.h>
#include <uuids.h>

#define WideToCharArr(wide, char_out_arr) \
    int internal_len = WideCharToMultiByte(1252, 0, wide, -1, NULL, 0, 0, 0); \
	char_out_arr = malloc(internal_len + 1); \
	WideCharToMultiByte(1252, 0, wide, -1, char_out_arr, internal_len, 0, 0); \
	char_out_arr[internal_len] = 0; \

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
    jstring_out = (*env)->NewString(env, internal_tmp_out, wide_size); \
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

void internal_lib_render_reload() {
    log_debug("Engine Reload!!");

    log_debug("Shutting down main loop...\n");
    main_loop_terminate();

    log_debug("Shutting down renders...\n");
    shutdown_renders();

    log_debug("Shutting down monitors...\n");
    shutdown_monitors();

    log_debug("Reload monitors");
    reload_monitors();

    log_debug("Will load config:\n");
    print_projection_config(config);

    log_debug("Reinitialize renders...\n");
    initialize_renders();

    log_debug("Staring engine...\n");

    activate_monitors(config);
    activate_renders(get_gl_share_context(), config);
    main_loop_schedule_config_reload(config);
    main_loop_start();
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
    internal_lib_render_reload();
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_initialize(JNIEnv *env, jobject _) {
    if (!glfwInit()) {
        return;
    }

    glfwSetErrorCallback(glfwIntErrorCallback);
    glfwSetMonitorCallback(glfwIntMonitorCallback);

    render_video_create_mtx();

    reload_monitors();

    config_bounds default_monitor;
    int no_display;

    get_default_projection_monitor_bounds(&default_monitor, &no_display);
    prepare_default_config(&default_monitor, no_display);

    initialize_renders();

    window_capture_init();

    initialized = 1;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_loadConfig(JNIEnv *env, jobject _, jstring j_file_path) {
    CHECK_INITIALIZE
    configured = 0;

    projection_config *new_config;

    if (j_file_path != NULL) {
        char* file_path;
        jni_jstringToCharArr(env, j_file_path, file_path);

        new_config = load_config(file_path);
        
        jni_releaseCharArr(env, j_file_path, file_path);
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
            log_debug("Reinitialize renders...\n");
            initialize_renders();
        }
    }

    log_debug("Staring engine...\n");
    config = new_config;

    activate_monitors(config);
    activate_renders(get_gl_share_context(), config);
    main_loop_schedule_config_reload(config);
    main_loop_start();
    configured = 1;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_reload(JNIEnv *env, jobject _) {
    internal_lib_render_reload();
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
    jfieldID enable_render_background_assets_field = (*env)->GetFieldID(env, BridgeRenderClass, "enableRenderBackgroundAssets", "Z");
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

// Image render methods
JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setImageAsset
  (JNIEnv *env, jobject _, jintArray arr, jint width, jint height, jboolean crop, jint renderFlag) {
    if (arr) {
        jint *data = (*env)->GetIntArrayElements(env, arr, 0);
        render_image_set_image((void*) data, width, height, crop, renderFlag);
        (*env)->ReleaseIntArrayElements(env, arr, data, 0);
    } else {
        render_image_set_image(NULL, 0, 0, 0, 0);
    }
  }

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_setMultiImageAsset
  (JNIEnv *env, jobject _, jintArray arr, jint width, jint height, jint render_id) {
    if (arr) {
        jint *data = (*env)->GetIntArrayElements(env, arr, 0);
        render_image_set_image_multi(data, width, height, 0, render_id);
        (*env)->ReleaseIntArrayElements(env, arr, data, 0);
    } else {
        render_image_set_image_multi(NULL, 0, 0, 0, render_id);
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

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_downloadPreviewData(JNIEnv *env, jobject _, jobject j_buffer) {
    jbyte *data = (jbyte*) (*env)->GetDirectBufferAddress(env, j_buffer);
    render_preview_download_buffer((void*) data);
}

JNIEXPORT jobjectArray JNICALL Java_dev_juhouse_projector_projection2_Bridge_getWindowList(JNIEnv *env, jobject _) {
    window_node_list *list = window_capture_get_window_list();

    jclass string_class = (*env)->FindClass(env, "java/lang/String");
    jobjectArray result = (*env)->NewObjectArray(env, list->list_size, string_class, NULL);
    jstring window_name;

    for (unsigned int i = 0; i < list->list_size; i++) {
        jni_charArrToJString(env, window_name, list->list[i].window_name);
        (*env)->SetObjectArrayElement(env, result, i, window_name);
    }

    window_capture_free_window_list(list);
    return result;
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
    int max_devices = 100;
    int device_count = 0;
    char** device_names = (char**)calloc(max_devices, sizeof(char*));

#ifdef _WIN32
    struct IEnumMoniker* p_class_enum;
    struct ICreateDevEnum* p_dev_enum;

    const IID rclsid1 = CLSID_SystemDeviceEnum;
    const IID riid1 = IID_ICreateDevEnum;

    HRESULT result = CoCreateInstance(&rclsid1, NULL, CLSCTX_INPROC, &riid1, &p_dev_enum);

    if (!SUCCEEDED(result)) {
        return NULL;
    }

    const IID rclsid2 = CLSID_VideoInputDeviceCategory;
    result = p_dev_enum->lpVtbl->CreateClassEnumerator(p_dev_enum, &rclsid2, &p_class_enum, 0);

    if (!SUCCEEDED(result)) {
        return NULL;
    }

    IGraphBuilder* pGraph = NULL;
    result = CoCreateInstance(&CLSID_FilterGraph, 0, CLSCTX_INPROC_SERVER, &IID_IGraphBuilder, (void**)&pGraph);

    if (!SUCCEEDED(result)) {
        return NULL;
    }

    IMoniker* p_moniker;
    IPropertyBag* p_bag;
    ULONG i_fetched;

    IID riid2 = IID_IPropertyBag;

    IFilterGraph2* filter_graph;

    while (true) {
        result = p_class_enum->lpVtbl->Next(p_class_enum, 1, &p_moniker, &i_fetched);

        if (result != S_OK) {
            break;
        }

        result = p_moniker->lpVtbl->BindToStorage(p_moniker, 0, 0, &riid2, &p_bag);

        if (!SUCCEEDED(result)) {
            continue;
        }

        VARIANT var;
        var.vt = VT_BSTR;

        result = p_bag->lpVtbl->Read(p_bag, L"FriendlyName", &var, NULL);

        if (!SUCCEEDED(result)) {
            continue;
        }

        char* device_name;

        WideToCharArr(var.bstrVal, device_name);

        device_names[device_count] = device_name;

        device_count++;

        IBaseFilter* base_filter;

        pGraph->lpVtbl->QueryInterface(pGraph, &IID_IFilterGraph2, &filter_graph);
        filter_graph->lpVtbl->AddSourceFilterForMoniker(filter_graph, p_moniker, NULL, L"Source", &base_filter);

        IEnumPins* enum_pins;

        base_filter->lpVtbl->EnumPins(base_filter, &enum_pins);

        while (true) {
            IPin* pin;
            long fetched;

            result = enum_pins->lpVtbl->Next(enum_pins, 1, &pin, &fetched);

            if (result != S_OK) {
                break;
            }

            PIN_DIRECTION pin_dir = PINDIR_INPUT;
            
            pin->lpVtbl->QueryDirection(pin, &pin_dir);

            if (pin_dir != PINDIR_OUTPUT) {
                continue;
            }

            IEnumMediaTypes* media_types;

            result = pin->lpVtbl->EnumMediaTypes(pin, &media_types);

            if (!SUCCEEDED(result)) {
                continue;
            }

            while (true) {
                AM_MEDIA_TYPE* media_type;
                long fetched;

                result = media_types->lpVtbl->Next(media_types, 1, &media_type, &fetched);

                if (result != S_OK) {
                    break;
                }

                if (IsEqualGUID(&media_type->formattype, &FORMAT_VideoInfo)) {
                    VIDEOINFOHEADER* info = (VIDEOINFOHEADER*)media_type->pbFormat;
                    if (info->bmiHeader.biWidth > 0) {
                        log_debug("Device: '%s' Resolution: %i %i\n", device_name, info->bmiHeader.biWidth, info->bmiHeader.biHeight);
                    }
                }
            }
        }
        

        if (device_count >= max_devices) {
            break;
        }
    }
#endif

    jclass BridgeCaptureDeviceClass = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeCaptureDevice");
    jclass BridgeCaptureDeviceResolutionClass = (*env)->FindClass(env, "dev/juhouse/projector/projection2/BridgeCaptureDeviceResolution");
    jfieldID BridgeCaptureDevice_DeviceNameField = (*env)->GetFieldID(env, BridgeCaptureDeviceClass, "deviceName", "Ljava/lang/String;");
    jfieldID BridgeCaptureDevice_ResolutionsField = (*env)->GetFieldID(env, BridgeCaptureDeviceClass, "resolutions", "[Ldev/juhouse/projector/projection2/BridgeCaptureDeviceResolution;");

    jobjectArray result_arr = (*env)->NewObjectArray(env, device_count, BridgeCaptureDeviceClass, NULL);
    
    jclass StringClass = (*env)->FindClass(env, "java/lang/String");

    for (unsigned int i = 0; i < device_count; i++) {
        jobject bridge_capture_device_obj = (*env)->AllocObject(env, BridgeCaptureDeviceClass);
        jobjectArray resolutions_arr = (*env)->NewObjectArray(env, 0, BridgeCaptureDeviceResolutionClass, NULL);
        jstring temp_jstring;

        jni_charArrToJString(env, temp_jstring, device_names[i]);
        free(device_names[i]);

        (*env)->SetObjectField(env, bridge_capture_device_obj, BridgeCaptureDevice_DeviceNameField, temp_jstring);
        (*env)->SetObjectField(env, bridge_capture_device_obj, BridgeCaptureDevice_ResolutionsField, resolutions_arr);
        (*env)->SetObjectArrayElement(env, result_arr, i, bridge_capture_device_obj);
    }

    free(device_names);

    return result_arr;
}

JNIEXPORT void JNICALL Java_dev_juhouse_projector_projection2_Bridge_shutdown(JNIEnv *env, jobject _) {
    CHECK_INITIALIZE

    main_loop_terminate();
    shutdown_renders();
    shutdown_monitors();
    window_capture_terminate();

    glfwTerminate();
}
