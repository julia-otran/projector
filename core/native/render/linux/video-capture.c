#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <linux/videodev2.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <unistd.h>

#include "turbojpeg.h"
#include "debug.h"
#include "video-capture.h"
#include "video-capture-extend.h"
#include "tinycthread.h"

#define size_t ssize_t

typedef struct {
    int video_fd;
    void* video_buffer_mmap[3];
    struct v4l2_format fmt;
    struct v4l2_buffer buf;
} video_capture_multi_device_extra_data;

static tjhandle turbo_jpeg;
static video_capture_multi_device* current_device;
static mtx_t thread_mutex;

void video_capture_init() {
    mtx_init(&thread_mutex, 0);
    turbo_jpeg = tjInitDecompress();
}

void video_capture_multi_get_device(char* name, int width, int height, video_capture_multi_device** device_out) {
    video_capture_multi_device* device = calloc(1, sizeof(video_capture_multi_device));
    video_capture_multi_device_extra_data* extra_data = calloc(1, sizeof(video_capture_multi_device_extra_data));
    device->extra_data = (void*) extra_data;

    device->name = calloc(strlen(name), sizeof(name));
    memcpy(device->name, name, strlen(name));

    device->width = width;
    device->height = height;

    const char device_name[255];
    
    sprintf((char *)device_name, "/dev/%s", name);
    
    extra_data->video_fd = open(device_name, O_RDWR);

    extra_data->fmt.type                = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    extra_data->fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_MJPEG;
    extra_data->fmt.fmt.pix.width  = width;
    extra_data->fmt.fmt.pix.height = height;

    ioctl(extra_data->video_fd, VIDIOC_S_FMT, &extra_data->fmt);

    (*device_out) = device;
}

void video_capture_multi_open_device(video_capture_multi_device* device) {
    video_capture_multi_device_extra_data* extra_data = (video_capture_multi_device_extra_data*) device->extra_data;

    struct v4l2_requestbuffers req_buffers;
    req_buffers.count = 3;
    req_buffers.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    req_buffers.memory = V4L2_MEMORY_MMAP;

    ioctl(extra_data->video_fd, VIDIOC_REQBUFS, &req_buffers);
    
    for (int i = 0; i < 3; i++) { 
        memset(&extra_data->buf, 0, sizeof(extra_data->buf));
        extra_data->buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        extra_data->buf.memory = V4L2_MEMORY_MMAP;
        extra_data->buf.index = i;
        ioctl(extra_data->video_fd, VIDIOC_QUERYBUF, &extra_data->buf);

        extra_data->video_buffer_mmap[i] = mmap(0 /* start anywhere */,
            extra_data->buf.length,
            PROT_READ,
            MAP_SHARED,
            extra_data->video_fd, extra_data->buf.m.offset);

        ioctl(extra_data->video_fd, VIDIOC_QBUF, &extra_data->buf);
    }

    ioctl(extra_data->video_fd, VIDIOC_STREAMON, &extra_data->buf.type);
}

void video_capture_multi_set_device(video_capture_multi_device* device) {
    mtx_lock(&thread_mutex);
    current_device = device;
    mtx_unlock(&thread_mutex);
}

void video_capture_multi_close(video_capture_multi_device* device) {
    if (current_device == device) {
        mtx_lock(&thread_mutex);
    }

    video_capture_multi_device_extra_data* extra_data = (video_capture_multi_device_extra_data*) device->extra_data;

    int local_video_fd = extra_data->video_fd;
    extra_data->video_fd = 0;
    
    munmap(extra_data->video_buffer_mmap[0], extra_data->buf.length);
    munmap(extra_data->video_buffer_mmap[1], extra_data->buf.length);
    munmap(extra_data->video_buffer_mmap[2], extra_data->buf.length);

    ioctl(local_video_fd, VIDIOC_STREAMOFF, &extra_data->buf.type);
    close(local_video_fd);

    free(extra_data);
    free(device);

    if (current_device == device) {
        device = NULL;
        mtx_unlock(&thread_mutex);
    }
}

void video_capture_set_device(char* name, int width, int height) {
    if (current_device) {
        video_capture_multi_close(current_device);
    }

    video_capture_multi_device *device;

    video_capture_multi_get_device(name, width, height, &device);
    video_capture_multi_set_device(device);
}

void video_capture_open_device() {
    video_capture_multi_open_device(current_device);
}

void video_capture_print_frame_int(void* buffer, enum TJPF color) {
    mtx_lock(&thread_mutex);

    if (current_device == NULL) {
        mtx_unlock(&thread_mutex);
        return;
    }

    video_capture_multi_device_extra_data* extra_data = (video_capture_multi_device_extra_data*) current_device->extra_data;    

    if (extra_data->video_fd == 0) {
        mtx_unlock(&thread_mutex);
        return;
    }

    if (ioctl(extra_data->video_fd, VIDIOC_DQBUF, &extra_data->buf) == -1) {
        mtx_unlock(&thread_mutex);
        return;
    }
    
    tjDecompress2(
        turbo_jpeg, 
        extra_data->video_buffer_mmap[extra_data->buf.index], 
        extra_data->buf.bytesused, 
        buffer, 
        extra_data->fmt.fmt.pix.width, 
        extra_data->fmt.fmt.pix.width * 4, 
        extra_data->fmt.fmt.pix.height, 
        color, 
        TJFLAG_FASTDCT | TJ_FASTUPSAMPLE | TJFLAG_NOREALLOC
    );
    
    ioctl(extra_data->video_fd, VIDIOC_QBUF, &extra_data->buf);

    mtx_unlock(&thread_mutex);
}

void video_capture_preview_frame(void* buffer) {
    video_capture_print_frame_int(buffer, TJPF_BGRA);
}

void video_capture_print_frame(void* buffer) {
    video_capture_print_frame_int(buffer, TJPF_RGBA);
}

void video_capture_close() {
    if (current_device == NULL) {
        return;
    }

    video_capture_multi_close(current_device);
}

void video_capture_terminate() {
    mtx_destroy(&thread_mutex);
    tjDestroy(turbo_jpeg);
}
