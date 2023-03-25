#include "ogl-loader.h"
#include "config-structs.h"

#ifndef _RENDER_H_
#define _RENDER_H_

typedef struct {
    config_render config;
    GLuint rendered_texture;
    GLuint framebuffer_name;

    GLuint outline_vertex_array;
    GLuint outline_vertex_buffer;
    GLuint outline_uv_buffer;
} render_layer;

typedef struct {
    int render_width, render_height;
} render_output_size;

typedef struct {
    int render_id;
    render_output_size size;
    GLuint rendered_texture;
} render_output;

void initialize_renders();
void activate_renders(GLFWwindow *shared_context, projection_config *config);
void renders_config_hot_reload(projection_config *config);
void shutdown_renders();

void lock_renders();
void unlock_renders();

void renders_init();
void renders_cycle();
void renders_terminate();

void get_render_output(render_output **out, int *render_output_count);

void get_main_output_size(render_output_size *output_size);
void get_main_text_area(config_bounds *text_area);

#endif