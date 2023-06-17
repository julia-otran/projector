#ifndef _DEVICE_CAPTURE_H_
#define _DEVICE_CAPTURE_H_

typedef struct {
	int width, height;
} capture_device_resolution;

typedef struct {
	capture_device_resolution* data;
	void* next;
} capture_device_resolution_node;

typedef struct {
	char* name;
	int count_resolutions;
	capture_device_resolution_node* resolutions;
} capture_device;

typedef struct {
	capture_device* data;
	void* next;
} capture_device_node;

typedef struct {
	int capture_device_count;
	capture_device_node* capture_device_list;
} capture_device_enum;

capture_device_enum* get_capture_devices();
void free_capture_device_enum(capture_device_enum* cap_enum);

#endif // !_DEVICE_CAPTURE_H_
