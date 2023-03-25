#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "debug.h"
#include "vs-color-corrector.h"

static GLuint vertexshader;
static GLuint fragmentshader;
static GLuint program;

static GLuint textureUniform;
static GLuint brightAdjustUniform;
static GLuint exposureAdjustUniform;
static GLuint lowAdjustUniform;
static GLuint midAdjustUniform;
static GLuint highAdjustUniform;

void vs_color_corrector_init() {
    vertexshader = loadShader(GL_VERTEX_SHADER, "color-corrector.vertex.shader");
    fragmentshader = loadShader(GL_FRAGMENT_SHADER, "color-corrector.fragment.shader");

    program = glCreateProgram();
    glAttachShader(program, vertexshader);
    glAttachShader(program, fragmentshader);

    glBindAttribLocation(program, 0, "in_Position");
    glBindAttribLocation(program, 1, "in_Uv");

    glLinkProgram(program);
    glValidateProgram(program);

    textureUniform = glGetUniformLocation(program, "image");
    brightAdjustUniform = glGetUniformLocation(program, "brightAdjust");
    exposureAdjustUniform = glGetUniformLocation(program, "exposureAdjust");
    lowAdjustUniform = glGetUniformLocation(program, "lowAdjust");
    midAdjustUniform = glGetUniformLocation(program, "midAdjust");
    highAdjustUniform = glGetUniformLocation(program, "highAdjust");

    glUseProgram(0);
}

void vs_color_corrector_start(config_virtual_screen *config, render_output *render, vs_color_corrector *data) {
    GLuint vertexarray;
    glGenVertexArrays(1, &vertexarray);
    glBindVertexArray(vertexarray);

    data->vertexarray = vertexarray;

    GLfloat *indexed_vertices = calloc(16, sizeof(GLfloat));

    indexed_vertices[0] = -1.0;
    indexed_vertices[1] = -1.0;
    indexed_vertices[2] = 0.0;
    indexed_vertices[3] = 1.0;

    indexed_vertices[4] = -1.0;
    indexed_vertices[5] = 1.0;
    indexed_vertices[6] = 0.0;
    indexed_vertices[7] = 1.0;

    indexed_vertices[8] = 1.0;
    indexed_vertices[9] = 1.0;
    indexed_vertices[10] = 0.0;
    indexed_vertices[11] = 1.0;

    indexed_vertices[12] = 1.0;
    indexed_vertices[13] = -1.0;
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

    float x, y, w, h;

    x = config->render_input_bounds.x / (float) render->size.render_width;
    y = config->render_input_bounds.y / (float) render->size.render_height;
    w = config->render_input_bounds.w / (float) render->size.render_width;
    h = config->render_input_bounds.h / (float) render->size.render_height;

    GLfloat *indexed_uvs = calloc(8, sizeof(GLfloat));

    indexed_uvs[0] = x;
    indexed_uvs[1] = y;

    indexed_uvs[2] = x;
    indexed_uvs[3] = y + h;

    indexed_uvs[4] = x + w;
    indexed_uvs[5] = y + h;

    indexed_uvs[6] = x + w;
    indexed_uvs[7] = y;

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

void vs_color_corrector_set_uniforms(config_virtual_screen *config) {
    glUniform3f(brightAdjustUniform, config->white_balance.bright.r, config->white_balance.bright.g, config->white_balance.bright.b);
    glUniform3f(exposureAdjustUniform, config->white_balance.exposure.r, config->white_balance.exposure.g, config->white_balance.exposure.b);

    glUniform4f(
        lowAdjustUniform,
        config->color_balance.shadows.r,
        config->color_balance.shadows.g,
        config->color_balance.shadows.b,
        config->color_balance.shadows_luminance
    );

    glUniform4f(
        midAdjustUniform,
        config->color_balance.midtones.r,
        config->color_balance.midtones.g,
        config->color_balance.midtones.b,
        config->color_balance.midtones_luminance
    );

    glUniform4f(
        highAdjustUniform,
        config->color_balance.highlights.r,
        config->color_balance.highlights.g,
        config->color_balance.highlights.b,
        config->color_balance.highlights_luminance
    );
}


void vs_color_corrector_render(config_virtual_screen *config, render_output *render, vs_color_corrector *data) {
    GLuint texture_id = render->rendered_texture;

    if (!texture_id) {
        return;
    }

    glUseProgram(program);

    vs_color_corrector_set_uniforms(config);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture_id);
    glUniform1i(textureUniform, 0);

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

void vs_color_corrector_stop(vs_color_corrector *data) {
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

void vs_color_corrector_shutdown() {
    glUseProgram(0);

    glDetachShader(program, vertexshader);
    glDetachShader(program, fragmentshader);
    glDeleteShader(vertexshader);
    glDeleteShader(fragmentshader);
    glDeleteProgram(program);
}
