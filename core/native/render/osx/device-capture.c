#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <dirent.h>
#include <stdio.h>
#include <string.h>
#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

#include "debug.h"
#include "device-capture.h"

capture_device_node* capture_device_get_device_node(AVCaptureDevice *dev) {
    capture_device_node* result = (capture_device_node*) calloc(1, sizeof(capture_device_node));
    capture_device* device = (capture_device*) calloc(1, sizeof(capture_device));

    const char *deviceName = [dev.localizedName UTF8String];
    
    device->name = calloc(strlen(deviceName) + 1, sizeof(char));
    memcpy(device->name, deviceName, strlen(deviceName));
    
    result->data = device;

    return result;
}

capture_device_resolution_node* capture_device_get_resolution_node(AVCaptureDeviceFormat *fmt) {
    capture_device_resolution_node* resolution_node = (capture_device_resolution_node*) calloc(1, sizeof(capture_device_resolution_node));
    capture_device_resolution* resolution = (capture_device_resolution*) calloc(1, sizeof(capture_device_resolution));

    resolution_node->data = resolution;
    resolution->width = (int) CMVideoFormatDescriptionGetDimensions(fmt.formatDescription).width;
    resolution->height = (int) CMVideoFormatDescriptionGetDimensions(fmt.formatDescription).height;
    
    return resolution_node;
}

void capture_device_find_resolutions(AVCaptureDevice *dev, capture_device* cap_dev) {
    for (AVCaptureDeviceFormat *fmt in dev.formats) {
        CMVideoDimensions dimensions = CMVideoFormatDescriptionGetDimensions(fmt.formatDescription);
        bool alreadyAdd = false;
        
        for (capture_device_resolution_node* resolution_node = cap_dev->resolutions; resolution_node != NULL; resolution_node = resolution_node->next)  {
            if (resolution_node->data->width == dimensions.width && resolution_node->data->height == dimensions.height) {
                alreadyAdd = true;
                break;
            }
        }
        
        if (alreadyAdd == false) {
            capture_device_resolution_node* res_node = capture_device_get_resolution_node(fmt);
            res_node->next = cap_dev->resolutions;
            cap_dev->resolutions = res_node;
            cap_dev->count_resolutions++;
        }
    }
}

void capture_device_enumerate_devices(capture_device_enum* dev_enum) {
    
    NSArray<AVCaptureDeviceType> *deviceTypes = [
        [NSArray alloc]
        initWithObjects:AVCaptureDeviceTypeExternalUnknown, AVCaptureDeviceTypeBuiltInWideAngleCamera, nil];
    
    AVCaptureDeviceDiscoverySession *ds = [AVCaptureDeviceDiscoverySession discoverySessionWithDeviceTypes:deviceTypes mediaType:AVMediaTypeVideo position: AVCaptureDevicePositionUnspecified];
    
    for (AVCaptureDevice *dev in ds.devices) {
        capture_device_node* new_device = capture_device_get_device_node(dev);
        
        dev_enum->capture_device_count++;
        new_device->next = dev_enum->capture_device_list;
        dev_enum->capture_device_list = new_device;

        capture_device_find_resolutions(dev, new_device->data);
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
