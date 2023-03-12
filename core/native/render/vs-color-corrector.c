#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "debug.h"
#include "vs-color-corrector.h"

void vs_color_corrector_load_coordinates(config_bounds *display_bounds, config_virtual_screen *config, vs_color_corrector *data) {
    GLuint vertexarray;
    glGenVertexArrays(1, &vertexarray);
    glBindVertexArray(vertexarray);

    data->vertexarray = vertexarray;

    GLfloat *indexed_vertices = calloc(16, sizeof(GLfloat));

    GLfloat x, y, w, h;

    w = (config->output_bounds.w * 2.0 / display_bounds->w);
    h = (config->output_bounds.h * 2.0 / display_bounds->h);

    x = (config->output_bounds.x * 2.0 / display_bounds->w) - 1.0;
    y = 1.0 - h - (config->output_bounds.y * 2.0 / display_bounds->h);

    log_debug("Color corrector bounds: x %f y %f w %f h %f\n", x, y, w, h);

    indexed_vertices[0] = x;
    indexed_vertices[1] = y;
    indexed_vertices[2] = 0.0;
    indexed_vertices[3] = 1.0;

    indexed_vertices[4] = x;
    indexed_vertices[5] = y + h;
    indexed_vertices[6] = 0.0;
    indexed_vertices[7] = 1.0;

    indexed_vertices[8] = x + w;
    indexed_vertices[9] = y + h;
    indexed_vertices[10] = 0.0;
    indexed_vertices[11] = 1.0;

    indexed_vertices[12] = x + w;
    indexed_vertices[13] = y;
    indexed_vertices[14] = 0.0;
    indexed_vertices[15] = 1.0;

    GLuint vertexbuffer;
    glGenBuffers(1, &vertexbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, vertexbuffer);
    glBufferData(GL_ARRAY_BUFFER, 16 * sizeof(GLfloat), indexed_vertices, GL_STATIC_DRAW);

    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 0, 0);

    glBindBuffer(GL_ARRAY_BUFFER, 0);

    data->vertexbuffer = vertexbuffer;
    free(indexed_vertices);

    GLfloat *indexed_uvs = calloc(8, sizeof(GLfloat));

    indexed_uvs[0] = config->input_bounds.x;
    indexed_uvs[1] = config->input_bounds.y;

    indexed_uvs[2] = config->input_bounds.x;
    indexed_uvs[3] = config->input_bounds.y + config->input_bounds.h;

    indexed_uvs[4] = config->input_bounds.x + config->input_bounds.w;
    indexed_uvs[5] = config->input_bounds.y + config->input_bounds.h;

    indexed_uvs[6] = config->input_bounds.x + config->input_bounds.w;
    indexed_uvs[7] = config->input_bounds.y;

    GLuint uvbuffer;
    glGenBuffers(1, &uvbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, uvbuffer);
    glBufferData(GL_ARRAY_BUFFER, 8 * sizeof(GLfloat), indexed_uvs, GL_STATIC_DRAW);

    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    data->uvbuffer = uvbuffer;
    free(indexed_uvs);

    glBindVertexArray(0);
}

void vs_color_corrector_setup_shaders(config_virtual_screen *config, vs_color_corrector *data) {
    data->vertexshader = loadShader(GL_VERTEX_SHADER, "color-corrector.vertex.shader");
    data->fragmentshader = loadShader(GL_FRAGMENT_SHADER, "color-corrector.fragment.shader");

    data->program = glCreateProgram();
    glAttachShader(data->program, data->vertexshader);
    glAttachShader(data->program, data->fragmentshader);

    glBindAttribLocation(data->program, 0, "in_Position");
    glBindAttribLocation(data->program, 1, "in_Uv");

    glLinkProgram(data->program);
    glValidateProgram(data->program);

    data->textureUniform = glGetUniformLocation(data->program, "image");
    data->brightAdjustUniform = glGetUniformLocation(data->program, "brightAdjust");
    data->exposureAdjustUniform = glGetUniformLocation(data->program, "exposureAdjust");
    data->lowAdjustUniform = glGetUniformLocation(data->program, "lowAdjust");
    data->midAdjustUniform = glGetUniformLocation(data->program, "midAdjust");
    data->highAdjustUniform = glGetUniformLocation(data->program, "highAdjust");
    data->preserveLumUniform = glGetUniformLocation(data->program, "preserveLuminosity");

    glUseProgram(0);
}

void vs_color_corrector_init_uniforms(config_virtual_screen *config, vs_color_corrector *data) {
    glUseProgram(data->program);

    glUniform3f(data->brightAdjustUniform, config->white_balance.bright.r, config->white_balance.bright.g, config->white_balance.bright.b);
    glUniform3f(data->exposureAdjustUniform, config->white_balance.exposure.r, config->white_balance.exposure.g, config->white_balance.exposure.b);

    glUniform3f(data->lowAdjustUniform, config->color_balance.shadows.r, config->color_balance.shadows.g, config->color_balance.shadows.b);
    glUniform3f(data->midAdjustUniform, config->color_balance.midtones.r, config->color_balance.midtones.g, config->color_balance.midtones.b);
    glUniform3f(data->highAdjustUniform, config->color_balance.highlights.r, config->color_balance.highlights.g, config->color_balance.highlights.b);
    glUniform1f(data->preserveLumUniform, config->color_balance.preserve_luminosity);

    glUseProgram(0);
}

void vs_color_corrector_init(config_bounds *display_bounds, config_virtual_screen *config, vs_color_corrector *data) {
    vs_color_corrector_load_coordinates(display_bounds, config, data);
    vs_color_corrector_setup_shaders(config, data);
    vs_color_corrector_init_uniforms(config, data);
}

void vs_color_corrector_render_texture(GLuint texture_id, vs_color_corrector *data) {
    glUseProgram(data->program);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture_id);
    glUniform1i(data->textureUniform, 0);

    glBindVertexArray(data->vertexarray);
    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);

    glDrawArrays(GL_QUADS, 0, 4);

    glBindTexture(GL_TEXTURE_2D, 0);
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindVertexArray(0);
    glUseProgram(0);
}

void vs_color_corrector_shutdown(vs_color_corrector *data) {
    glUseProgram(0);

    glDetachShader(data->program, data->vertexshader);
    glDetachShader(data->program, data->fragmentshader);
    glDeleteShader(data->vertexshader);
    glDeleteShader(data->fragmentshader);
    glDeleteProgram(data->program);

    // Select the VAO
    glBindVertexArray(data->vertexarray);

    // Disable the VBO index from the VAO attributes list
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);

    // Delete the vertex VBO
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDeleteBuffers(1, &data->vertexbuffer);

    // Delete the color VBO
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDeleteBuffers(1, &data->uvbuffer);

    // Delete the VAO
    glBindVertexArray(0);
    glDeleteVertexArrays(1, &data->vertexarray);
}