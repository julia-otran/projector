#include <unistd.h>
#include <string.h>
#include "debug.h"
#include "ogl-loader.h"
#include "ndi-input.h"
#include "tinycthread.h"
#include "Processing.NDI.Lib.h"
#include "turbojpeg.h"

static NDIlib_video_frame_v2_t video_frame;

static tjhandle turbo_jpeg_decompress;

static thrd_t downstream_thread;
static mtx_t downstream_thread_mtx;

static int running;

void ndi_input_initialize() {
    mtx_init(&downstream_thread_mtx, 0);
    turbo_jpeg_decompress = tjInitDecompress();
}

int ndi_input_downstream_loop(void *pNDI_recv_void) {
    NDIlib_video_frame_v2_t video_frame_tmp;
    NDIlib_recv_instance_t pNDI_recv_local = (NDIlib_recv_instance_t)pNDI_recv_void;

    log_debug("NDI: Downstream loop init\n");

    while (running) {
        switch (NDIlib_recv_capture_v2(pNDI_recv_local, &video_frame_tmp, NULL, NULL, 1000)) {
            // Video data
            case NDIlib_frame_type_video:
                log_debug("NDI: Frame received\n");
                mtx_lock(&downstream_thread_mtx);
                NDIlib_recv_free_video_v2(pNDI_recv_local, &video_frame);
                memcpy(&video_frame, &video_frame_tmp, sizeof(NDIlib_video_frame_v2_t));
                mtx_unlock(&downstream_thread_mtx);
                break;

            case NDIlib_frame_type_error:
                log_debug("NDI: Error getting frame\n");
                running = 0;
                break;

            default:
                log_debug("NDI: Received other frame type\n");
                break;
        }
    }

    log_debug("NDI: Downstream loop finished\n");

    NDIlib_recv_free_video_v2(pNDI_recv_local, &video_frame);
    NDIlib_recv_destroy(pNDI_recv_local);

    return 0;
}

void ndi_input_start_downstream(void *pNDI_recv) {
    running = 1;
    log_debug("NDI: Starting downstream\n");
    thrd_create(&downstream_thread, ndi_input_downstream_loop, (void*)pNDI_recv);
}

void ndi_input_stop_downstream() {
    running = 0;
}

void ndi_input_lock() {
    mtx_lock(&downstream_thread_mtx);
}

void ndi_input_get_frame_size(int *width, int *height, int *bytesPerPixel, GLuint *pixelFormat) {
    GLuint aux;

    if (!pixelFormat) {
        pixelFormat = &aux;
    }

    (*width) = video_frame.xres;
    (*height) = video_frame.yres;

    switch (video_frame.FourCC) {
        case NDIlib_FourCC_video_type_RGBA:
        case NDIlib_FourCC_video_type_RGBX:
        case NDIlib_FourCC_video_type_UYVA:
        (*bytesPerPixel) = 4;
        (*pixelFormat) = GL_RGBA;
        break;

        case NDIlib_FourCC_video_type_BGRA:
        case NDIlib_FourCC_video_type_BGRX:
        (*bytesPerPixel) = 4;
        (*pixelFormat) = GL_BGRA;
        break;

        default: 
        (*bytesPerPixel) = 4;
        (*pixelFormat) = GL_RGBA;
    }
}

void ndi_input_download_frame(void *data) {
    switch (video_frame.FourCC) {
        case NDIlib_FourCC_video_type_RGBA:
        case NDIlib_FourCC_video_type_RGBX:
        case NDIlib_FourCC_video_type_BGRA:
        case NDIlib_FourCC_video_type_BGRX:
        memcpy(data, video_frame.p_data, video_frame.xres * video_frame.yres * 4);
        break;

        case NDIlib_FourCC_video_type_UYVY:
        tjDecodeYUV(turbo_jpeg_decompress, video_frame.p_data, 1, TJSAMP_422, data, video_frame.xres, video_frame.xres * 3, video_frame.yres, TJPF_RGBA, 0);
        break;

        case NDIlib_FourCC_video_type_I420:
        tjDecodeYUV(turbo_jpeg_decompress, video_frame.p_data, 1, TJSAMP_420, data, video_frame.xres, video_frame.xres * 3, video_frame.yres, TJPF_RGBA, 0);
        break;
    }
}

void ndi_input_unlock() {
    mtx_unlock(&downstream_thread_mtx);
}

void ndi_input_terminate() {
    ndi_input_stop_downstream();
    thrd_join(downstream_thread, NULL);
    mtx_destroy(&downstream_thread_mtx);
    tjDestroy(turbo_jpeg_decompress);
}
