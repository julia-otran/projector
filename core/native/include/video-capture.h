#ifndef _VIDEO_CAPTURE_H_
#define _VIDEO_CAPTURE_H_

void video_capture_init();
void video_capture_set_device(char* name, int width, int height);
void video_capture_open_device();
void video_capture_print_frame(void* buffer);
void video_capture_close();
void video_capture_terminate();

#endif // !_VIDEO_CAPTURE_H_
