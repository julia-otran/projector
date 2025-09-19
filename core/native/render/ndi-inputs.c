#include <stdio.h>
#include <fcntl.h>
#include <string.h>

#include "debug.h"
#include "ndi-inputs.h"
#include "tinycthread.h"
#include "ndi-loader.h"

static ndi_inputs_devices_callback_fn callback_fn;
static ndi_inputs_callback_node_list *callback_list;

static NDIlib_find_instance_t pNDI_find;
static NDIlib_source_t* p_sources;
static uint32_t p_sources_count;

static int running_find;
static thrd_t find_thread;
static mtx_t thread_mutex;
static mtx_t find_thread_mutex;

void ndi_inputs_init() {
    callback_list = NULL;
    mtx_init(&thread_mutex, 0);
    mtx_init(&find_thread_mutex, 0);

    pNDI_find = NDIlib_find_create_v2(NULL);
}

void ndi_inputs_set_callback(ndi_inputs_devices_callback_fn fn) {
    callback_fn = fn;
}

void ndi_inputs_add_callback_node(void *data) {
    ndi_inputs_callback_node_list *current = calloc(1, sizeof(ndi_inputs_callback_node_list));
    current->data = data;
    
    mtx_lock(&thread_mutex);
    current->next = callback_list;
    callback_list = current;
    mtx_unlock(&thread_mutex);
}

void ndi_inputs_remove_callback_node(void *data) {
    ndi_inputs_callback_node_list **current = &callback_list;

    mtx_lock(&thread_mutex);

    while ((*current) != NULL) {
        ndi_inputs_callback_node_list *aux = (*current);
        
        if (aux->data == data) {
            (*current) = (*current)->next;
            free(aux);
        } else {
            current = &aux->next;
        }
    }

    mtx_unlock(&thread_mutex);
}

ndi_inputs_callback_node_list* ndi_inputs_get_callback_node_list() {
    return callback_list;
}

void ndi_inputs_free_devices(ndi_inputs_device_list* devices) {
    for (uint32_t i = 0; i < devices->count; i++) {
        free(devices->devices[i].name);
        free(devices->devices[i].url_address);
    }
    free(devices->devices);
    free(devices);
}

void ndi_inputs_connect(char* device_name, void** pNDI_recv_in, unsigned int* success) {
    NDIlib_recv_instance_t *recvPP = (NDIlib_recv_instance_t*) pNDI_recv_in;
    NDIlib_recv_create_v3_t create_options;

    mtx_lock(&find_thread_mutex);

    (*success) = 0;

    if (p_sources && p_sources_count) {
        for (uint32_t i = 0; i < p_sources_count; i++) {
            if (strcmp(p_sources[i].p_ndi_name, device_name) == 0) {
                NDIlib_source_t *src = &p_sources[i];

                create_options.source_to_connect_to = (*src);
                // create_options.bandwidth = NDIlib_recv_bandwidth_highest;
                create_options.bandwidth = NDIlib_recv_bandwidth_lowest;
                create_options.color_format = NDIlib_recv_color_format_UYVY_RGBA;
                create_options.allow_video_fields = true;
                create_options.p_ndi_recv_name = "Projector";

                (*recvPP) = NDIlib_recv_create_v3(&create_options);

                NDIlib_recv_instance_t pNDI_recv = (*recvPP);
                NDIlib_recv_connect(pNDI_recv, src);
                (*success) = 1;
            }
        }
    }

    mtx_unlock(&find_thread_mutex);
}

void ndi_inputs_notify(NDIlib_find_instance_t pNDI_find) {
    mtx_lock(&find_thread_mutex);
	p_sources = NDIlib_find_get_current_sources(pNDI_find, &p_sources_count);
    mtx_unlock(&find_thread_mutex);

    ndi_inputs_device_list *list = calloc(1, sizeof(ndi_inputs_device_list));
    list->count = p_sources_count;
    list->devices = calloc(p_sources_count, sizeof(ndi_inputs_device));

    for (uint32_t i = 0; i < p_sources_count; i++) {
        list->devices[i].name = calloc(1, strlen(p_sources[i].p_ndi_name) + 1);
        memcpy(list->devices[i].name, p_sources[i].p_ndi_name, strlen(p_sources[i].p_ndi_name));

        list->devices[i].url_address = calloc(1, strlen(p_sources[i].p_url_address) + 1);
        memcpy(list->devices[i].url_address, p_sources[i].p_url_address, strlen(p_sources[i].p_url_address));
    }

    mtx_lock(&thread_mutex);

    callback_fn(list, callback_list);

    mtx_unlock(&thread_mutex);

    ndi_inputs_free_devices(list);
}

int ndi_inputs_find_devices_internal(void* _) {
    while (NDIlib_find_wait_for_sources(pNDI_find, 5000)) {
        ndi_inputs_notify(pNDI_find);
    }

    running_find = 0;

    return 0;
}

void ndi_inputs_find_devices() {
    if (running_find == 0) {
        running_find = 1;
        thrd_join(find_thread, NULL);
        thrd_create(&find_thread, ndi_inputs_find_devices_internal, NULL);
    }
}

void ndi_inputs_terminate() {
    thrd_join(find_thread, NULL);
    mtx_destroy(&thread_mutex);
    mtx_destroy(&find_thread_mutex);

    while (callback_list != NULL) {
        ndi_inputs_callback_node_list *current = callback_list;
        callback_list = current->next;
        free(current->data);
        free(current);
    }

    NDIlib_find_destroy(pNDI_find);
}
