#ifndef _NDI_OUTPUT_H_
#define _NDI_OUTPUT_H_

#include "render.h"

void ndi_output_set_frame_rate(int fps);
void ndi_output_set_renders(render_layer *renders, int count_layers);
void ndi_output_send_frame(int render_id, void *data, int width, int height);
void ndi_output_free();

#endif // _NDI_OUTPUT_H_
