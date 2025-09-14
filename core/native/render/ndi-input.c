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
static int render;

void ndi_input_initialize() {
    mtx_init(&downstream_thread_mtx, 0);
    turbo_jpeg_decompress = tjInitDecompress();
}

void ndi_input_set_render(int in_render) {
    render = in_render;
}

void ndi_input_send_tally(NDIlib_recv_instance_t pNDI_recv) {
    NDIlib_tally_t tally;

    tally.on_preview = 1;
    tally.on_program = !!render;

    NDIlib_recv_set_tally(pNDI_recv, &tally);
}

int ndi_input_downstream_loop(void *pNDI_recv_void) {
    NDIlib_video_frame_v2_t video_frame_tmp;
    NDIlib_audio_frame_v2_t audio_frame;
    NDIlib_recv_instance_t pNDI_recv = (NDIlib_recv_instance_t)pNDI_recv_void;
    int render_local = render;

    NDIlib_framesync_instance_t pNDI_framesync = NDIlib_framesync_create(pNDI_recv);

    ndi_input_send_tally(pNDI_recv);

    log_debug("NDI: Downstream loop init\n");

    while (running) {
        NDIlib_framesync_capture_video(pNDI_framesync, &video_frame_tmp, NDIlib_frame_format_type_progressive);

        mtx_lock(&downstream_thread_mtx);
        NDIlib_framesync_free_video(pNDI_framesync, &video_frame);
        memcpy(&video_frame, &video_frame_tmp, sizeof(NDIlib_video_frame_v2_t));
        mtx_unlock(&downstream_thread_mtx);

        NDIlib_framesync_capture_audio(pNDI_framesync, &audio_frame, 48000, 4, 1600);
        NDIlib_framesync_free_audio(pNDI_framesync, &audio_frame);

        if (render_local != render) {
            ndi_input_send_tally(pNDI_recv);
            render_local = render;
        }
    }

    log_debug("NDI: Downstream loop finished\n");

    NDIlib_framesync_destroy(pNDI_framesync);
    NDIlib_recv_free_video_v2(pNDI_recv, &video_frame);
    NDIlib_recv_destroy(pNDI_recv);

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
    mtx_unlock(&downstream_thread_mtx);
}

void ndi_input_terminate() {
    ndi_input_stop_downstream();
    thrd_join(downstream_thread, NULL);
    mtx_destroy(&downstream_thread_mtx);
    tjDestroy(turbo_jpeg_decompress);
}
