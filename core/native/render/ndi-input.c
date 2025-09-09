#include "ogl-loader.h"
#include "ndi-input.h"
#include "tinycthread.h"
#include "Processing.NDI.Lib.h"
#include "turbojpeg.h"

static NDIlib_recv_instance_t pNDI_recv;
static NDIlib_video_frame_v2_t video_frame;

static tjhandle turbo_jpeg_decompress;

static thrd_t downstream_thread;
static mtx_t downstream_thread_mtx;

static int running;

void ndi_input_initialize() {
    mtx_init(&downstream_thread_mtx, 0);
    turbo_jpeg_decompress = tjInitDecompress();
}

void ndi_input_set_device(void *pNDI_recv_in) {
    pNDI_recv = (NDIlib_recv_instance_t)pNDI_recv_in;
}

int ndi_input_downstream_loop(void *_) {
    NDIlib_video_frame_v2_t video_frame_tmp;

    while (running) {
        switch (NDIlib_recv_capture_v2(pNDI_recv, &video_frame_tmp, NULL, NULL, 1000)) {
            // No data
            case NDIlib_frame_type_none:
                break;

            // Video data
            case NDIlib_frame_type_video:
                mtx_lock(&downstream_thread_mtx);
                NDIlib_recv_free_video_v2(pNDI_recv, &video_frame);
                memcpy(&video_frame, &video_frame_tmp, sizeof(NDIlib_video_frame_v2_t));
                mtx_unlock(&downstream_thread_mtx);
                break;

            // Audio data
            case NDIlib_frame_type_audio:
                break;

            case NDIlib_frame_type_error:
                running = 0;
                break;
        }
    }
}

void ndi_input_start_downstream() {
    running = 1;
    thrd_create(&downstream_thread, ndi_input_downstream_loop, NULL);
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

        case NDIlib_FourCC_type_BGRA:
        case NDIlib_FourCC_type_BGRX:
        (*bytesPerPixel) = 4;
        (*pixelFormat) = GL_BGRA;
        break;

        default: 
        (*bytesPerPixel) = 3;
        (*pixelFormat) = GL_RGB;
    }
}

void ndi_input_download_frame(void *data) {
    switch (video_frame.FourCC) {
        case NDIlib_FourCC_video_type_RGBA:
        case NDIlib_FourCC_video_type_RGBX:
        case NDIlib_FourCC_type_BGRA:
        case NDIlib_FourCC_type_BGRX:
        memcpy(data, video_frame.p_data, video_frame.xres * video_frame.yres * 4);
        break;

        case NDIlib_FourCC_video_type_UYVY:
        tjDecodeYUV(turbo_jpeg_decompress, video_frame.p_data, 1, TJSAMP_422, data, video_frame.xres, video_frame.xres * 3, video_frame.yres, TJPF_RGB, 0);
        break;

        case NDIlib_FourCC_video_type_I420:
        tjDecodeYUV(turbo_jpeg_decompress, video_frame.p_data, 1, TJSAMP_420, data, video_frame.xres, video_frame.xres * 3, video_frame.yres, TJPF_RGB, 0);
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
