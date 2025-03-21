#include "debug.h"
#include "video-capture.h"

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

#define size_t ssize_t

int video_fd = 0;
void* video_buffer_mmap[3];
struct v4l2_format fmt;
struct v4l2_buffer buf;

static tjhandle turbo_jpeg;

void video_capture_init() {
    turbo_jpeg = tjInitDecompress();
}

void video_capture_set_device(char* name, int in_width, int in_height) {
    if (video_fd) {
        close(video_fd);
    }

    const char device_name[255];
    
    sprintf((char *)device_name, "/dev/%s", name);
    
    video_fd = open(device_name, O_RDWR);

    fmt.type                = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_MJPEG;
    fmt.fmt.pix.width  = in_width;
    fmt.fmt.pix.height = in_height;

    ioctl(video_fd, VIDIOC_S_FMT, &fmt);
}

void video_capture_open_device() {
    struct v4l2_requestbuffers req_buffers;
    req_buffers.count = 3;
    req_buffers.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    req_buffers.memory = V4L2_MEMORY_MMAP;

    ioctl(video_fd, VIDIOC_REQBUFS, &req_buffers);
    
    memset(&buf, 0, sizeof(buf));
    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    buf.index = 0;
    ioctl(video_fd, VIDIOC_QUERYBUF, &buf);

    video_buffer_mmap[0] = mmap(0 /* start anywhere */,
        buf.length,
        PROT_READ,
        MAP_SHARED,
        video_fd, buf.m.offset);

    ioctl(video_fd, VIDIOC_QBUF, &buf);

    memset(&buf, 0, sizeof(buf));
    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    buf.index = 1;
    ioctl(video_fd, VIDIOC_QUERYBUF, &buf);

    video_buffer_mmap[1] = mmap(0 /* start anywhere */,
        buf.length,
        PROT_READ,
        MAP_SHARED,
        video_fd, buf.m.offset);

    ioctl(video_fd, VIDIOC_QBUF, &buf);

    memset(&buf, 0, sizeof(buf));
    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    buf.index = 2;
    ioctl(video_fd, VIDIOC_QUERYBUF, &buf);

    video_buffer_mmap[2] = mmap(0 /* start anywhere */,
        buf.length,
        PROT_READ,
        MAP_SHARED,
        video_fd, buf.m.offset);

    ioctl(video_fd, VIDIOC_QBUF, &buf);

    ioctl(video_fd, VIDIOC_STREAMON, &buf.type);
}

void video_capture_print_frame_int(void* buffer, enum TJPF color) {
    if (ioctl(video_fd, VIDIOC_DQBUF, &buf) == -1) {
        return;
    }
    
    tjDecompress2(
        turbo_jpeg, 
        video_buffer_mmap[buf.index], 
        buf.bytesused, 
        buffer, fmt.fmt.pix.width, fmt.fmt.pix.width * 4, fmt.fmt.pix.height, color, TJFLAG_FASTDCT | TJ_FASTUPSAMPLE | TJFLAG_NOREALLOC);
    
    ioctl(video_fd, VIDIOC_QBUF, &buf);
}

void video_capture_preview_frame(void* buffer) {
    video_capture_print_frame_int(buffer, TJPF_BGRA);
}

void video_capture_print_frame(void* buffer) {
    video_capture_print_frame_int(buffer, TJPF_RGBA);
}

void video_capture_close() {
    if (video_fd) {
        close(video_fd);
    }
}

void video_capture_terminate() {
    tjDestroy(turbo_jpeg);
}
