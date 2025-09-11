#include "ogl-loader.h"
#include "render.h"

#ifndef _RENDER_NDI_INPUT_H_
#define _RENDER_NDI_INPUT_H_

void render_ndi_input_initialize();

void render_ndi_input_set_enabled(int enabled);
void render_ndi_input_set_render(int render);
void render_ndi_input_set_crop(int crop);
void render_ndi_input_download_preview(int* data, int dataMaxSize, int* width, int* height, int *bytesPerPixel, GLuint *pixelFormat);

void render_ndi_input_create_buffers();
void render_ndi_input_update_buffers();
void render_ndi_input_flush_buffers();
void render_ndi_input_deallocate_buffers();

void render_ndi_input_create_assets();
void render_ndi_input_update_assets();
void render_ndi_input_deallocate_assets();

void render_ndi_input_render(render_layer *layer);

void render_ndi_input_shutdown();

#endif
