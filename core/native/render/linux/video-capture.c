#define _GNU_SOURCE

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <linux/videodev2.h>
#include <dirent.h>
#include <stdio.h>
#include <string.h>

#include "debug.h"
#include "video-capture.h"
#include "turbojpeg.h"

#define DEVICE_PATH_PREFIX "/dev/%s"

static int deviceFile, width, height, ready;
static void *buffer_start;

static tjhandle turbo_jpeg;

void video_capture_init() {
    turbo_jpeg = tjInitDecompress();
}

void video_capture_set_device(char* name, int in_width, int in_height) {
    char* device_path;

    int result = asprintf(&device_path, DEVICE_PATH_PREFIX, name);

    if (result == -1 || device_path == NULL) {
        log_debug("Video capture failed to construct device path.\n");
        return;
    }

    deviceFile = open(device_path, O_RDWR);

    if (deviceFile == -1) {
        log_debug("Video capture failed to open device: %s\n", name);
    }

    width = in_width;
    height = in_height;
}

void video_capture_open_device() {
    struct v4l2_capability cap;

    if (deviceFile == -1) { return; }

    if (ioctl(deviceFile, VIDIOC_QUERYCAP, &cap) == -1) {
        log_debug("Video capture failed to query capabilities.\n");
        video_capture_close();
        return;        
    }

    if ((!(cap.capabilities & V4L2_CAP_VIDEO_CAPTURE))) {
        log_debug("Video capture has no video capture capability.\n");
        video_capture_close();
        return;
    }

    if (!(cap.capabilities & V4L2_CAP_STREAMING)) {
        log_debug("Video capture has no stream capability.\n");
        video_capture_close();
        return;
    }

    struct v4l2_format fmt;
    fmt.type                = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_MJPEG;
    fmt.fmt.pix.width  = width;
    fmt.fmt.pix.height = height;
    
    if (ioctl(deviceFile, VIDIOC_S_FMT, &fmt) == -1) {
        log_debug("Video capture set format failed.\n");
        video_capture_close();
        return;
    }
    
    struct v4l2_requestbuffers req;
    memset(&req, 0, sizeof(req));//setting the buffer count as 1
    req.count  = 1;
    req.type   = V4L2_BUF_TYPE_VIDEO_CAPTURE;//use the mmap for mapping the buffer
    req.memory = V4L2_MEMORY_MMAP;
    
    if (ioctl(deviceFile, VIDIOC_REQBUFS, &req) == -1) {
        log_debug("Video capture request buffers failed.\n");
        video_capture_close();
        return;
    }

    struct v4l2_buffer buf;
    memset(&buf, 0, sizeof(buf));
    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    buf.index = 0;
    
    if (ioctl(deviceFile, VIDIOC_QUERYBUF, &buf) == -1) {
        log_debug("Video capture query buffers failed.\n");
        video_capture_close();
        return;
    }

    buffer_start = mmap(NULL /* start anywhere */,
        buf.length,
        PROT_READ | PROT_WRITE /* required */,
        MAP_SHARED /* recommended */,
        deviceFile, 
        buf.m.offset
    );

    log_debug("Video capture memory map length %u; offset %u\n", buf.length, buf.m.offset);
        
    if (buffer_start == MAP_FAILED) {
        log_debug("Video capture mmap failed.\n");
        buffer_start = 0;
        video_capture_close();
        return;
    }

    enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    memset(&buf, 0, sizeof(buf));
    buf.type = type;
    buf.memory = V4L2_MEMORY_MMAP;
    buf.index = 0;

    if (ioctl(deviceFile, VIDIOC_QBUF, &buf) == -1) {
        log_debug("Video capture queue buffer failed.\n");
        video_capture_close();
        return;
    }

    if (ioctl(deviceFile, VIDIOC_STREAMON, &type) == -1) {
        log_debug("Video capture stream on failed.\n");
        video_capture_close();
        return;
    }

    log_debug("Video capture device opened successfully.\n");

    ready = 1;
}

void video_capture_print_frame_int(void* buffer, enum TJPF pixelFormat) {
    if (!ready) {
        return;
    }

    struct v4l2_buffer buf;
    memset(&buf, 0, sizeof(buf));
    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    
    if (ioctl(deviceFile, VIDIOC_DQBUF, &buf) == -1) {
        log_debug("Video capture dequeue buffer failed.\n");
        return;
    }

    int result = tjDecompress2(
        turbo_jpeg, 
        (unsigned char*) buffer_start, 
        buf.bytesused, 
        (unsigned char*)buffer, 
        width, 
        width * 4, 
        height, 
        pixelFormat, 
        0
    );
    
    char *error = tjGetErrorStr2(turbo_jpeg);

    if (result != 0 && error) {
        log_debug("Video capture decode frame failed: %s.\n", error);
    }

    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    
    if (ioctl(deviceFile, VIDIOC_QBUF, &buf) == -1) {
        log_debug("Video capture queue buffer failed.\n");
        return;
    }
}

void video_capture_preview_frame(void* buffer) {
    video_capture_print_frame_int(buffer, TJPF_BGRA);
}

void video_capture_print_frame(void* buffer) {
    video_capture_print_frame_int(buffer, TJPF_RGBA);
}

void video_capture_close() {
    ready = 0;

    enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

    if (deviceFile != -1) {
        ioctl(deviceFile, VIDIOC_STREAMOFF, &type);
        close(deviceFile);
    }

    deviceFile = -1;
}

void video_capture_terminate() {
    tjDestroy(turbo_jpeg);
}
