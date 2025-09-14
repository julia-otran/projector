#include "color.h"

void color_from_rgba_to_bgra(uint32_t *out_data, uint32_t *in_data, uint64_t pixelCount) {
    for (uint64_t i = 0; i < pixelCount; i++) {
        uint8_t a = (in_data[i] >> 24) & 0xFF;
        uint8_t b = (in_data[i] >> 16) & 0xFF;
        uint8_t g = (in_data[i] >> 8) & 0xFF;
        uint8_t r = (in_data[i] >> 0) & 0xFF;

        out_data[i] = (a << 24) | (r << 16) | (g << 8) | b;
    }
}

void color_copy_alpha_to_rgba(uint32_t *output, uint8_t *alpha_input, uint64_t pixelCount) {
    for (uint64_t i = 0; i < pixelCount; i++) {
        output[i] = (output[i] & 0x00FFFFFF) | (alpha_input[i] << 24);
    }
}
