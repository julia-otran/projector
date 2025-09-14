#include <unistd.h>
#include <string.h>
#include "debug.h"
#include "ogl-loader.h"
#include "ndi-input.h"
#include "tinycthread.h"
#include "Processing.NDI.Lib.h"
#include "turbojpeg.h"

static NDIlib_recv_instance_t pNDI_recv;
static NDIlib_framesync_instance_t pNDI_framesync;

static NDIlib_video_frame_v2_t video_frame;
static NDIlib_audio_frame_v2_t audio_frame;

static tjhandle turbo_jpeg_decompress;

static mtx_t downstream_mtx;

static int running;
static int render;

void ndi_input_initialize() {
    mtx_init(&downstream_mtx, 0);
    turbo_jpeg_decompress = tjInitDecompress();
}

void ndi_input_send_tally() {
    NDIlib_tally_t tally;

    tally.on_preview = 1;
    tally.on_program = !!render;

    mtx_lock(&downstream_mtx);

    if (running) {
        NDIlib_recv_set_tally(pNDI_recv, &tally);
    }

    mtx_unlock(&downstream_mtx);
}

void ndi_input_set_render(int in_render) {
    render = in_render;
    ndi_input_send_tally();
}

void ndi_input_start_downstream(void *pNDI_recv_void) {
    mtx_lock(&downstream_mtx);
    running = 1;
    pNDI_recv = (NDIlib_recv_instance_t)pNDI_recv_void;
    pNDI_framesync = NDIlib_framesync_create(pNDI_recv);
    mtx_unlock(&downstream_mtx);

    ndi_input_send_tally();
}

void ndi_input_stop_downstream() {
    mtx_lock(&downstream_mtx);
    running = 0;
    
    if (pNDI_framesync) {
        NDIlib_framesync_destroy(pNDI_framesync);
        pNDI_framesync = 0;
    }
    
    if (pNDI_recv) {
        NDIlib_recv_destroy(pNDI_recv);
        pNDI_recv = 0;
    }
    
    mtx_unlock(&downstream_mtx);
}

void ndi_input_lock() {
    mtx_lock(&downstream_mtx);

    if (running) {
        NDIlib_framesync_capture_video(pNDI_framesync, &video_frame, NDIlib_frame_format_type_progressive);
        NDIlib_framesync_capture_audio(pNDI_framesync, &audio_frame, 48000, 4, 1600);
    }
}

void ndi_input_get_frame_size(int *width, int *height, int *bytesPerPixel, GLuint *pixelFormat) {
    if (!running) { 
        (*width) = 0;
        (*height) = 0;
        (*bytesPerPixel) = 0;
        return;
    }

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
    if (!running) { 
        return;
    }

    int u_plane;
    int v_plane;

    unsigned char* planes[3];
    int strides[3];

    switch (video_frame.FourCC) {
        case NDIlib_FourCC_video_type_RGBA:
        case NDIlib_FourCC_video_type_RGBX:
        case NDIlib_FourCC_video_type_BGRA:
        case NDIlib_FourCC_video_type_BGRX:
        memcpy(data, video_frame.p_data, video_frame.xres * video_frame.yres * 4);
        break;

        case NDIlib_FourCC_video_type_UYVY:
        tjDecodeYUV(turbo_jpeg_decompress, video_frame.p_data, 1, TJSAMP_422, data, video_frame.xres, video_frame.xres * 4, video_frame.yres, TJPF_RGBA, 0);
        break;

        case NDIlib_FourCC_video_type_I420:
            u_plane = video_frame.xres * video_frame.yres;
            v_plane = u_plane + (u_plane / 4);

            planes[0] = (unsigned char*) video_frame.p_data;
            planes[1] = (unsigned char*) &video_frame.p_data[u_plane];
            planes[2] = (unsigned char*) &video_frame.p_data[v_plane];

            strides[0] = u_plane;
            strides[1] = u_plane / 4;
            strides[2] = u_plane / 4;

            tjDecodeYUVPlanes(turbo_jpeg_decompress, (const unsigned char**)planes, strides, TJSAMP_420, data, video_frame.xres, video_frame.xres * 4, video_frame.yres, TJPF_RGBA, 0);        
            break;

        case NDIlib_FourCC_video_type_YV12:
            u_plane = video_frame.xres * video_frame.yres;
            v_plane = u_plane + (u_plane / 4);

            planes[0] = (unsigned char*) video_frame.p_data;
            planes[1] = (unsigned char*) &video_frame.p_data[v_plane];
            planes[2] = (unsigned char*) &video_frame.p_data[u_plane];

            strides[0] = u_plane;
            strides[1] = u_plane / 4;
            strides[2] = u_plane / 4;

            tjDecodeYUVPlanes(turbo_jpeg_decompress, (const unsigned char**)planes, strides, TJSAMP_420, data, video_frame.xres, video_frame.xres * 4, video_frame.yres, TJPF_RGBA, 0);
            break;
    }
}

void ndi_input_unlock() {
    NDIlib_framesync_free_video(pNDI_framesync, &video_frame);
    NDIlib_framesync_free_audio(pNDI_framesync, &audio_frame);
    mtx_unlock(&downstream_mtx);
}

void ndi_input_terminate() {
    ndi_input_stop_downstream();
    mtx_destroy(&downstream_mtx);
    tjDestroy(turbo_jpeg_decompress);
}
