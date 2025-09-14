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

typedef struct ndi_inputs_callback_node_list {
    void* data;
    struct ndi_inputs_callback_node_list* next;
} ndi_inputs_callback_node_list;

typedef void (*ndi_inputs_devices_callback_fn)(ndi_inputs_device_list*, ndi_inputs_callback_node_list*);

void ndi_inputs_init();

void ndi_inputs_set_callback(ndi_inputs_devices_callback_fn fn);

void ndi_inputs_find_devices();

void ndi_inputs_connect(char* device_name, void** pNDI_recv, unsigned int* success);

void ndi_inputs_add_callback_node(void *node);
void ndi_inputs_remove_callback_node(void *node);
ndi_inputs_callback_node_list* ndi_inputs_get_callback_node_list();

void ndi_inputs_free_devices(ndi_inputs_device_list* devices);

void ndi_inputs_terminate();
#endif
