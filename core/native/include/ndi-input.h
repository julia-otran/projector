#ifndef _NDI_INPUT_H_
#define _NDI_INPUT_H_

#include "ogl-loader.h"

void ndi_input_initialize();
void ndi_input_set_device(void *pNDI_recv);
void ndi_input_start_downstream();
void ndi_input_stop_downstream();
void ndi_input_lock();
void ndi_input_get_frame_size(int *width, int *height, int *bytesPerPixel, GLuint *pixelFormat);
void ndi_input_download_frame(void *data);
void ndi_input_unlock();
void ndi_input_terminate();

#endif
