#ifndef _NDI_INPUTS_H_
#define _NDI_INPUTS_H_

typedef struct {
    char *name;
    char *url_address;
} ndi_inputs_device;

typedef struct {
    ndi_inputs_device *devices;
    unsigned int count;
} ndi_inputs_device_list;

typedef void (*ndi_inputs_devices_callback)(ndi_inputs_device_list*);

void ndi_inputs_init();

void ndi_inputs_find_devices();

void ndi_inputs_add_callback(ndi_inputs_devices_callback fn);
void ndi_inputs_remove_callback(ndi_inputs_devices_callback fn);

void ndi_inputs_free_devices(ndi_inputs_device_list* devices);

void ndi_inputs_terminate();
#endif
