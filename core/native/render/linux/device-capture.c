#define _GNU_SOURCE

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/videodev2.h>
#include <dirent.h>
#include <stdio.h>
#include <string.h>

#include "debug.h"
#include "device-capture.h"

#define DEVICE_PATH_PREFIX "/dev/%s"

capture_device_node* capture_device_get_device_node(char *dev) {
    capture_device_node* result = (capture_device_node*) calloc(1, sizeof(capture_device_node));
    capture_device* device = (capture_device*) calloc(1, sizeof(capture_device));

    device->name = calloc(strlen(dev) + 1, sizeof(char));
    memcpy(device->name, dev, strlen(dev));

    result->data = device;

    return result;
}

int capture_device_resolution_exists(capture_device_resolution_node* list, struct v4l2_frmsize_discrete* frmsize) {
    for (capture_device_resolution_node* node = list; node != NULL; node = node->next) {
        if (((__u32)node->data->width) == frmsize->width && ((__u32)node->data->height) == frmsize->height) {
            return 1;
        }
    }

    return 0;
}

capture_device_resolution_node* capture_device_get_resolution_node(struct v4l2_frmsize_discrete* frmsize) {
    capture_device_resolution_node* resolution_node = (capture_device_resolution_node*) calloc(1, sizeof(capture_device_resolution_node));
    capture_device_resolution* resolution = (capture_device_resolution*) calloc(1, sizeof(capture_device_resolution));

    resolution_node->data = resolution;
    resolution->width = (int) frmsize->width;
    resolution->height = (int) frmsize->height;

    return resolution_node;
}

void capture_device_find_format_resolutions(int device_ptr, capture_device* cap_dev, struct v4l2_fmtdesc* fmtdesc) {
    struct v4l2_frmsizeenum frmsizeenum;

    frmsizeenum.index = 0;
    frmsizeenum.pixel_format = fmtdesc->pixelformat;

    while (ioctl(device_ptr, VIDIOC_ENUM_FRAMESIZES, &frmsizeenum) == 0) {
        log_debug("Found frame size.\n");

        if (frmsizeenum.type == V4L2_FRMSIZE_TYPE_DISCRETE) {
            if (capture_device_resolution_exists(cap_dev->resolutions, &frmsizeenum.discrete) == 0) {
                capture_device_resolution_node* res_node = capture_device_get_resolution_node(&frmsizeenum.discrete);
                res_node->next = cap_dev->resolutions;
                cap_dev->resolutions = res_node;
                cap_dev->count_resolutions++;
            }
        }

        frmsizeenum.index++;
    }
}

void capture_device_find_resolutions(capture_device* cap_dev) {
    char* device_path;

    int result = asprintf(&device_path, DEVICE_PATH_PREFIX, cap_dev->name);

    if (result == -1 || device_path == NULL) {
        return;
    }

    log_debug("Opening device %s\n", device_path);

    int device_ptr = open(device_path, O_RDWR);

    if (device_ptr == -1) {
        return;
    }

    struct v4l2_fmtdesc fmtdesc;

    fmtdesc.index = 0;
    fmtdesc.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

    while (ioctl(device_ptr, VIDIOC_ENUM_FMT, &fmtdesc) == 0) {
        log_debug("Found format %s.\n", fmtdesc.description);
        capture_device_find_format_resolutions(device_ptr, cap_dev, &fmtdesc);
        fmtdesc.index++;
    }

    close(device_ptr);
}

void capture_device_enumerate_devices(capture_device_enum* dev_enum) {
    DIR *d;
    struct dirent *dir;

    d = opendir("/dev");

    if (d) {
        while ((dir = readdir(d)) != NULL) {
            if (strstr(dir->d_name, "video") != NULL) {
                capture_device_node* new_device = capture_device_get_device_node(dir->d_name);

                dev_enum->capture_device_count++;
                new_device->next = dev_enum->capture_device_list;
                dev_enum->capture_device_list = new_device;

                capture_device_find_resolutions(new_device->data);
            }
        }

        closedir(d);
    }
}

capture_device_enum* get_capture_devices() {
    capture_device_enum* dev_enum = (capture_device_enum*) calloc(1, sizeof(capture_device_enum));

    capture_device_enumerate_devices(dev_enum);

    return dev_enum;
}

void free_capture_device_enum(capture_device_enum* cap_enum) {
    capture_device_node* device_node = cap_enum->capture_device_list;
    capture_device_node* device_node_aux;

    while (device_node != NULL) {
        capture_device_resolution_node* res_node = device_node->data->resolutions;
        capture_device_resolution_node* res_node_aux;

        while (res_node != NULL) {
            res_node_aux = res_node->next;

            free(res_node->data);
            free(res_node);

            res_node = res_node_aux;
        }

        device_node_aux = device_node->next;

        free(device_node->data->name);
        free(device_node->data);
        free(device_node);

        device_node = device_node_aux;
    }

    free(cap_enum);
}
