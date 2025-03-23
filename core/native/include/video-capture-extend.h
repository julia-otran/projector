#ifndef _VIDEO_CAPTURE_EXTEND_H_
#define _VIDEO_CAPTURE_EXTEND_H_

typedef struct {
    char* name;
    unsigned int width, height;
    void* extra_data;
} video_capture_multi_device;

void video_capture_multi_get_device(char* name, int width, int height, video_capture_multi_device** device);
void video_capture_multi_open_device(video_capture_multi_device* device);
void video_capture_multi_set_device(video_capture_multi_device* device);
void video_capture_multi_close(video_capture_multi_device* device);

#endif // !_VIDEO_CAPTURE_EXTEND_H_
