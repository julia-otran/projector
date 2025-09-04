#define _GNU_SOURCE

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>

#include "debug.h"
#include "ndi-inputs.h"
#include "tinycthread.h"
#include "Processing.NDI.Lib.h"

static ndi_inputs_devices_callback_fn callback_fn;
static ndi_inputs_callback_node_list *callback_list;

static int running_find;
static thrd_t find_thread;
static mtx_t thread_mutex;

void ndi_inputs_init() {
    callback_list = NULL;
    mtx_init(&thread_mutex, 0);
}

void ndi_inputs_set_callback(ndi_inputs_devices_callback_fn fn) {
    callback_fn = fn;
}

void ndi_inputs_add_callback_node(void *data) {
    ndi_inputs_callback_node_list *current = calloc(1, sizeof(ndi_inputs_callback_node_list));
    current->data = data;
    current->next = callback_list;
    callback_list = current;
}

void ndi_inputs_remove_callback_node(void *data) {
    ndi_inputs_callback_node_list **current = &callback_list;

    mtx_lock(&thread_mutex);

    while (current != NULL) {
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
        free(devices->devices->name);
        free(devices->devices->url_address);
    }
    free(devices->devices);
    free(devices);
}

void ndi_inputs_notify(NDIlib_find_instance_t pNDI_find) {
    uint32_t count = 0;
	const NDIlib_source_t* p_sources = NDIlib_find_get_current_sources(pNDI_find, &count);

    ndi_inputs_device_list *list = calloc(1, sizeof(ndi_inputs_device_list));
    list->count = count;
    list->devices = calloc(count, sizeof(ndi_inputs_device));

    for (uint32_t i = 0; i < count; i++) {
        list->devices[i].name = calloc(1, strlen(p_sources[i].p_ndi_name));
        memcpy(list->devices[i].name, p_sources[i].p_ndi_name, strlen(p_sources[i].p_ndi_name));

        list->devices[i].url_address = calloc(1, strlen(p_sources[i].p_url_address));
        memcpy(list->devices[i].url_address, p_sources[i].p_url_address, strlen(p_sources[i].p_url_address));
    }

    mtx_lock(&thread_mutex);

    callback_fn(list, callback_list);

    mtx_unlock(&thread_mutex);

    ndi_inputs_free_devices(list);
}

int ndi_inputs_find_devices_internal(void* _) {
    NDIlib_find_instance_t pNDI_find = NDIlib_find_create_v2(NULL);

    while (NDIlib_find_wait_for_sources(pNDI_find, 5000)) {
        ndi_inputs_notify(pNDI_find);
    }

    running_find = 0;
    ndi_inputs_notify(pNDI_find);

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

    while (callback_list != NULL) {
        ndi_inputs_callback_node_list *current = callback_list;
        callback_list = current->next;
        free(current->data);
        free(current);
    }
}
