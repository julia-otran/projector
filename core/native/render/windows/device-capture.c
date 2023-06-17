#pragma comment(lib, "strmiids.lib")

#include <windows.h>
#include <stringapiset.h>
#include <combaseapi.h>
#include <dshow.h>
#include <uuids.h>

#include "debug.h"
#include "device-capture.h"

#define WideToCharArr(wide, char_out_arr) \
    int internal_len = WideCharToMultiByte(1252, 0, wide, -1, NULL, 0, 0, 0); \
	char_out_arr = malloc(internal_len + 1); \
	WideCharToMultiByte(1252, 0, wide, -1, char_out_arr, internal_len, 0, 0); \
	char_out_arr[internal_len] = 0; \

void FreeMediaType(AM_MEDIA_TYPE* mt)
{
    if (mt->cbFormat != 0) {
        CoTaskMemFree((PVOID)mt->pbFormat);

        // Strictly unnecessary but tidier
        mt->cbFormat = 0;
        mt->pbFormat = NULL;
    }

    if (mt->pUnk != NULL) {
        mt->pUnk->lpVtbl->Release(mt->pUnk);
        mt->pUnk = NULL;
    }
}

int resolution_exists(capture_device_resolution_node* resolution_list, int width, int height) {
    for (capture_device_resolution_node* current = resolution_list; current != NULL; current = current->next) {
        if (current->data->width == width && current->data->height == height) {
            return 1;
        }
    }

    return 0;
}

void get_capture_device_resolutions(struct IMoniker* p_moniker, capture_device *device) {
    IBaseFilter* base_filter;
    IGraphBuilder* pGraph = NULL;
    HRESULT result;
    IFilterGraph2* filter_graph;
    capture_device_resolution_node* resolution_list = NULL;

    device->count_resolutions = 0;
    device->resolutions = NULL;

    result = CoCreateInstance(&CLSID_FilterGraph, 0, CLSCTX_INPROC_SERVER, &IID_IGraphBuilder, (void**)&pGraph);

    if (!SUCCEEDED(result)) {
        return;
    }

    result = pGraph->lpVtbl->QueryInterface(pGraph, &IID_IFilterGraph2, &filter_graph);

    if (!SUCCEEDED(result)) {
        pGraph->lpVtbl->Release(pGraph);
        return;
    }

    result = filter_graph->lpVtbl->AddSourceFilterForMoniker(filter_graph, p_moniker, NULL, L"Source", &base_filter);

    if (!SUCCEEDED(result)) {
        filter_graph->lpVtbl->Release(filter_graph);
        pGraph->lpVtbl->Release(pGraph);
        return;
    }

    IEnumPins* enum_pins;

    result = base_filter->lpVtbl->EnumPins(base_filter, &enum_pins);

    if (!SUCCEEDED(result)) {
        base_filter->lpVtbl->Release(base_filter);
        filter_graph->lpVtbl->Release(filter_graph);
        pGraph->lpVtbl->Release(pGraph);
        return;
    }

    while (1) {
        IPin* pin;
        long fetched;

        result = enum_pins->lpVtbl->Next(enum_pins, 1, &pin, &fetched);

        if (result != S_OK) {
            break;
        }

        PIN_DIRECTION pin_dir = PINDIR_INPUT;

        pin->lpVtbl->QueryDirection(pin, &pin_dir);

        if (pin_dir != PINDIR_OUTPUT) {
            pin->lpVtbl->Release(pin);
            continue;
        }

        IEnumMediaTypes* media_types;

        result = pin->lpVtbl->EnumMediaTypes(pin, &media_types);

        if (!SUCCEEDED(result)) {
            pin->lpVtbl->Release(pin);
            continue;
        }

        while (1) {
            AM_MEDIA_TYPE* media_type;
            long fetched;

            result = media_types->lpVtbl->Next(media_types, 1, &media_type, &fetched);

            if (result != S_OK) {
                break;
            }

            if (IsEqualGUID(&media_type->formattype, &FORMAT_VideoInfo)) {
                VIDEOINFOHEADER* info = (VIDEOINFOHEADER*)media_type->pbFormat;

                if (
                    info && 
                    info->bmiHeader.biWidth > 0 && 
                    resolution_exists(resolution_list, info->bmiHeader.biWidth, info->bmiHeader.biHeight) == 0
                    ) {
                    capture_device_resolution_node* node = calloc(1, sizeof(capture_device_resolution_node));
                    
                    if (node) {
                        node->data = calloc(1, sizeof(capture_device_resolution));
                    }

                    if (node && node->data) {
                        device->count_resolutions++;
                        node->data->width = info->bmiHeader.biWidth;
                        node->data->height = info->bmiHeader.biHeight;
                        node->next = resolution_list;
                        resolution_list = node;
                    }
                    else 
                    {
                        free(node);
                    }

                    log_debug("Resolution: % i % i\n", info->bmiHeader.biWidth, info->bmiHeader.biHeight);
                }
            }

            FreeMediaType(media_type);
        }

        media_types->lpVtbl->Release(media_types);
        pin->lpVtbl->Release(pin);
    }

    enum_pins->lpVtbl->Release(enum_pins);
    base_filter->lpVtbl->Release(base_filter);
    filter_graph->lpVtbl->Release(filter_graph);
    pGraph->lpVtbl->Release(pGraph);
    device->resolutions = resolution_list;
}

capture_device_node* get_capture_device_node(struct IMoniker* p_moniker, char* device_name) {
    capture_device* cap_dev = calloc(1, sizeof(capture_device));

    if (!cap_dev) {
        return NULL;
    }

    capture_device_node* node = calloc(1, sizeof(capture_device_node));

    if (!node) {
        free(cap_dev);
        return NULL;
    }
  
    char* device_name_wide;
    WideToCharArr(device_name, device_name_wide);

    cap_dev->name = device_name_wide;

    log_debug("Found capture device: '%s'\n", device_name_wide);
    get_capture_device_resolutions(p_moniker, cap_dev);
    
    node->data = cap_dev;

    return node;
}

void get_capture_devices_int(capture_device_enum* cap_enum) {
    struct IEnumMoniker* p_class_enum;
    struct ICreateDevEnum* p_dev_enum;

    const IID rclsid1 = CLSID_SystemDeviceEnum;
    const IID riid1 = IID_ICreateDevEnum;

    cap_enum->capture_device_count = 0;

    HRESULT result = CoCreateInstance(&rclsid1, NULL, CLSCTX_INPROC, &riid1, &p_dev_enum);

    if (!SUCCEEDED(result)) {
        return NULL;
    }

    const IID rclsid2 = CLSID_VideoInputDeviceCategory;
    result = p_dev_enum->lpVtbl->CreateClassEnumerator(p_dev_enum, &rclsid2, &p_class_enum, 0);

    if (!SUCCEEDED(result)) {
        p_dev_enum->lpVtbl->Release(p_dev_enum);
        return NULL;
    }

    IMoniker* p_moniker;
    IPropertyBag* p_bag;
    ULONG i_fetched;

    IID riid2 = IID_IPropertyBag;

    while (1) {
        result = p_class_enum->lpVtbl->Next(p_class_enum, 1, &p_moniker, &i_fetched);

        if (result != S_OK) {
            break;
        }

        result = p_moniker->lpVtbl->BindToStorage(p_moniker, 0, 0, &riid2, &p_bag);

        if (!SUCCEEDED(result)) {
            p_moniker->lpVtbl->Release(p_moniker);
            continue;
        }

        VARIANT var;
        var.vt = VT_BSTR;

        result = p_bag->lpVtbl->Read(p_bag, L"FriendlyName", &var, NULL);

        if (!SUCCEEDED(result)) {
            p_bag->lpVtbl->Release(p_bag);
            p_moniker->lpVtbl->Release(p_moniker);
            continue;
        }

        capture_device_node* dev_node = get_capture_device_node(p_moniker, var.bstrVal);

        if (dev_node) {
            dev_node->next = cap_enum->capture_device_list;
            cap_enum->capture_device_list = dev_node;
            cap_enum->capture_device_count++;
        }

        SysFreeString(var.bstrVal);
        p_bag->lpVtbl->Release(p_bag);
        p_moniker->lpVtbl->Release(p_moniker);
    }

    p_dev_enum->lpVtbl->Release(p_dev_enum);
}

capture_device_enum* get_capture_devices() {
    capture_device_enum* cap_enum = calloc(1, sizeof(capture_device_enum));

    if (cap_enum) {
        get_capture_devices_int(cap_enum);
    }

    return cap_enum;
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