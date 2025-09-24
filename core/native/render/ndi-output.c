#include <string.h>
#include <stdlib.h>
#include "ndi-output.h"
#include "ndi-loader.h"
#include "debug.h"

typedef struct {
	int render_id;
	NDIlib_send_instance_t pNDI_send;
	int connections_number;
	int last_check_count;
} ndi_output_int;

static ndi_output_int* outputs = NULL;
static int count_outputs = 0;
static int frame_rate = 0;

void ndi_output_set_frame_rate(int fps) {
	frame_rate = fps;
}

void ndi_output_free() {
	if (outputs) {
		for (int i = 0; i < count_outputs; i++) {
			if (outputs[i].pNDI_send) {
				NDIlib_send_destroy(outputs[i].pNDI_send);
			}
		}
		free(outputs);
		outputs = NULL;
		count_outputs = 0;
	}
}

void ndi_output_set_renders(render_layer* renders, int count_layers) {
	ndi_output_free();

	outputs = calloc(count_layers, sizeof(ndi_output_int));
	count_outputs = count_layers;

	for (int i = 0; i < count_layers; i++) {
		render_layer* render = &renders[i];
		ndi_output_int* output = &outputs[i];

		output->render_id = render->config.render_id;

		NDIlib_send_create_t NDI_send_create_desc;
		char name_buffer[256] = "Projector Output: ";
		NDI_send_create_desc.p_ndi_name = strncat(name_buffer, render->config.render_name, 150);
		NDI_send_create_desc.p_groups = NULL;
		NDI_send_create_desc.clock_video = false;
		NDI_send_create_desc.clock_audio = false;

		output->pNDI_send = NDIlib_send_create(&NDI_send_create_desc);
	}
}

int ndi_output_has_connection(int render_id) {
	for (int i = 0; i < count_outputs; i++) {
		ndi_output_int* output = &outputs[i];
		if (output->render_id != render_id) {
			continue;
		}

		if (!output->pNDI_send) {
			return 0;
		}

		if (output->last_check_count == 0) {
			output->connections_number = NDIlib_send_get_no_connections(output->pNDI_send, 0);
		}

		output->last_check_count++;

		if (output->last_check_count >= frame_rate) {
			output->last_check_count = 0;
		}
		
		return output->connections_number;
	}

	return 0;
}

void ndi_output_send_frame(int render_id, void* data, int width, int height) {

	for (int i = 0; i < count_outputs; i++) {
		ndi_output_int* output = &outputs[i];

		if (output->render_id != render_id) {
			continue;
		}
		if (!output->pNDI_send) {
			return;
		}

		NDIlib_video_frame_v2_t NDI_video_frame;
		memset(&NDI_video_frame, 0, sizeof(NDI_video_frame));

		NDI_video_frame.xres = width;
		NDI_video_frame.yres = height;
		NDI_video_frame.FourCC = NDIlib_FourCC_type_BGRA;
		NDI_video_frame.picture_aspect_ratio = (float)width / (float)height;
		NDI_video_frame.frame_format_type = NDIlib_frame_format_type_progressive;
		NDI_video_frame.p_data = (uint8_t*)data;
		NDI_video_frame.line_stride_in_bytes = width * 4;
		NDI_video_frame.frame_rate_N = frame_rate;
		NDI_video_frame.frame_rate_D = 1;
		NDIlib_send_send_video_v2(output->pNDI_send, &NDI_video_frame);
		break;
	}
}
