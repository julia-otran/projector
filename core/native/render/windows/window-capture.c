#include <Windows.h>
#include <string.h>

#include "debug.h"
#include "window-capture.h"

typedef struct {
    HWND window_handle;
    HDC hdc_source;
    HDC hdc_memory;
    HBITMAP dst_bitmap;
    BITMAPINFOHEADER bitmap_info;
} window_capture_extra_data;

typedef struct {
    window_node node;
    void* next;
} window_capture_node;

static window_capture_node* window_capture_node_list;
static unsigned int window_capture_node_list_size;

void window_capture_clear_list() {
    window_capture_node* node = window_capture_node_list;
    window_capture_node* aux;

    while (node) {
        aux = node;
        node = node->next;

        free(aux->node.window_name);
        free(aux->node.extra_data);
        free(aux);
    }

    window_capture_node_list = NULL;
    window_capture_node_list_size = 0;
}

void window_capture_init() {
}

BOOL window_capture_enum_windows_callback(HWND handle, LPARAM _) {
    if (!IsWindow(handle)) {
        return TRUE;
    }

    if (!IsWindowVisible(handle)) {
        return TRUE;
    }

    if (GetWindowLongA(handle, GWL_EXSTYLE) & WS_EX_TOOLWINDOW) {
        return TRUE;
    }

    TITLEBARINFO ti;

    ti.cbSize = sizeof(ti);
    GetTitleBarInfo(handle, &ti);

    if (ti.rgstate[0] & STATE_SYSTEM_INVISIBLE) {
        return TRUE;
    }

    DWORD pid;

    GetWindowThreadProcessId(handle, &pid);

    if (pid == GetCurrentProcessId()) {
        return TRUE;
    }

    char name[255];

    GetWindowTextA(handle, name, sizeof(name) - 1);

    window_capture_node* node = (window_capture_node*)calloc(1, sizeof(window_capture_node));

    if (node == NULL) {
        return FALSE;
    }

    node->node.window_name = calloc(1, sizeof(name));

    if (node->node.window_name == NULL) {
        return FALSE;
    }

    memcpy(node->node.window_name, name, sizeof(name) - 1);

    window_capture_extra_data* extra_data = (window_capture_extra_data*)calloc(1, sizeof(window_capture_extra_data));

    if (extra_data == NULL) {
        return FALSE;
    }

    extra_data->window_handle = handle;

    node->node.extra_data = (void*)extra_data;

    node->next = window_capture_node_list;
    window_capture_node_list = node;
    window_capture_node_list_size++;
}

window_node_list* window_capture_get_window_list() {
    window_capture_clear_list();

    EnumWindows(window_capture_enum_windows_callback, NULL);

    window_node_list *result = (window_node_list*) calloc(1, sizeof(window_node_list));
    window_node* result_nodes = (window_node*) calloc(window_capture_node_list_size, sizeof(window_node));
    window_capture_extra_data* result_extra_data = (window_capture_extra_data*)calloc(window_capture_node_list_size, sizeof(window_capture_extra_data));
    char *result_names = (char*)calloc(window_capture_node_list_size, sizeof(char[255]));

    if (result_nodes == NULL || result == NULL || result_extra_data == NULL || result_names == NULL) {
        return NULL;
    }

    result->list_size = window_capture_node_list_size;
    result->list = result_nodes;

    window_capture_node* node = window_capture_node_list;

    for (unsigned int i = 0; i < window_capture_node_list_size; i++) {
        if (node) {
            memcpy(&result_names[i * 255], node->node.window_name, 254);
            memcpy(&result_extra_data[i], node->node.extra_data, sizeof(window_capture_extra_data));

            node = node->next;
        }

        result_nodes[i].window_name = &result_names[i * 255];
        result_nodes[i].extra_data = &result_extra_data[i];
    }

    return result;
}

void window_capture_free_window_list(window_node_list* list) {
    free(list->list[0].extra_data);
    free(list->list[0].window_name);
    free(list->list);
    free(list);
}


void* window_capture_get_handler(char* window_name) {
    window_node_list* node_list = window_capture_get_window_list();
    window_capture_extra_data* src_extra_data = NULL;
    window_capture_extra_data* dst_extra_data = NULL;

    if (node_list == NULL) {
        return NULL;
    }

    for (unsigned int i = 0; i < node_list->list_size; i++) {
        if (node_list->list[i].window_name && strcmp(node_list->list[i].window_name, window_name) == 0) {
            src_extra_data = node_list->list[i].extra_data;
            break;
        }
    }

    if (src_extra_data != NULL) {
        dst_extra_data = (window_capture_extra_data*)calloc(1, sizeof(window_capture_extra_data));
    }

    if (dst_extra_data != NULL) {
        memcpy(dst_extra_data, src_extra_data, sizeof(window_capture_extra_data));

        dst_extra_data->hdc_source = GetWindowDC(dst_extra_data->window_handle);
        dst_extra_data->hdc_memory = CreateCompatibleDC(dst_extra_data->hdc_source);
    }

    window_capture_free_window_list(node_list);

    return dst_extra_data;
}

void window_capture_free_handler(void* handler) {
    window_capture_extra_data* extra_data = (window_capture_extra_data*)handler;

    if (extra_data->hdc_memory) {
        DeleteDC(extra_data->hdc_memory);
    }

    if (extra_data->hdc_source) {
        DeleteDC(extra_data->hdc_source);
    }

    if (extra_data->dst_bitmap) {
        DeleteObject(extra_data->dst_bitmap);
    }

    free(handler);
}

void window_capture_get_window_size(void* handler, int* out_width, int* out_height) {
    window_capture_extra_data* extra_data = (window_capture_extra_data*)handler;

    if (!IsWindow(extra_data->window_handle)) {
        (*out_width) = 0;
        (*out_height) = 0;

        return;
    }

    RECT window_rect;

    int width, height;

    if (GetWindowRect(extra_data->window_handle, &window_rect) == TRUE) {
        width = window_rect.right - window_rect.left;
        height = window_rect.bottom - window_rect.top;
    } else {
        (*out_width) = 0;
        (*out_height) = 0;

        return;
    }

    if (extra_data->dst_bitmap != NULL) {
        BITMAP bitmap_info;

        GetObject(extra_data->dst_bitmap, sizeof(BITMAP), &bitmap_info);

        if (bitmap_info.bmWidth != width || bitmap_info.bmHeight != height) {
            DeleteObject(extra_data->dst_bitmap);
            extra_data->dst_bitmap = NULL;
        }
    }

    if (extra_data->dst_bitmap == NULL) {
        extra_data->dst_bitmap = CreateCompatibleBitmap(extra_data->hdc_source, width, height);

        extra_data->bitmap_info.biSize = sizeof(BITMAPINFOHEADER);
        extra_data->bitmap_info.biWidth = width;
        extra_data->bitmap_info.biHeight = height;
        extra_data->bitmap_info.biPlanes = 1;
        extra_data->bitmap_info.biBitCount = 32;
        extra_data->bitmap_info.biCompression = BI_RGB;
        extra_data->bitmap_info.biSizeImage = width * height * 4;
    }

    (*out_width) = width;
    (*out_height) = height;
}

void window_capture_get_image(void* handler, int width, int height, void* out_data) {
    window_capture_extra_data* extra_data = (window_capture_extra_data*)handler;

    HBITMAP old_bitmap = SelectObject(extra_data->hdc_memory, extra_data->dst_bitmap);

    BOOL result = PrintWindow(extra_data->window_handle, extra_data->hdc_memory, PW_RENDERFULLCONTENT);

    if (result) {
        GetDIBits(extra_data->hdc_memory, extra_data->dst_bitmap, 0, height, out_data, &extra_data->bitmap_info, DIB_RGB_COLORS);
    }

    SelectObject(extra_data->hdc_memory, old_bitmap);
}

void window_capture_terminate() {
}
