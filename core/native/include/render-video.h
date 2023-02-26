#include "ogl-loader.h"

#ifndef _RENDER_VIDEO_H_
#define _RENDER_VIDEO_H_

void render_video_initialize();

void render_video_set_crop_video(int crop);
void render_video_set_buffer(void *buffer, int width, int height);
void render_video_set_render(int render);
void render_video_update_buffer();

void render_video_generate_assets();
void render_video_upload_texes();
void render_video_render(render_layer *layer);
void render_video_deallocate_assets();

void render_video_shutdown();

#endif
