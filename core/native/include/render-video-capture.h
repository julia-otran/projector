#include "render.h"

#ifndef _RENDER_VIDEO_CAPTURE_H_
#define _RENDER_VIDEO_CAPTURE_H_

void render_video_capture_initialize();

void render_video_capture_set_device(char* device, int width, int height);
void render_video_capture_set_enabled(int enabled);
void render_video_capture_set_render(int render);
void render_video_capture_download_preview(int* data);

void render_video_capture_create_buffers();
void render_video_capture_update_buffers();
void render_video_capture_flush_buffers();
void render_video_capture_deallocate_buffers();

void render_video_capture_create_assets();
void render_video_capture_update_assets();
void render_video_capture_deallocate_assets();

void render_video_capture_render(render_layer *layer);

void render_video_capture_shutdown();

#endif
