#include <stdint.h>

#ifndef _COLOR_H_
#define _COLOR_H_

void color_from_rgba_to_bgra(uint32_t *out_data, uint32_t *in_data, uint64_t pixelCount);
void color_copy_alpha_to_rgba(uint32_t *output, uint8_t *alpha_input, uint64_t pixelCount);

#endif
