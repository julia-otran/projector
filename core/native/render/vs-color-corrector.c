#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "debug.h"
#include "vs-color-corrector.h"

static GLuint vertexshader;
static GLuint fragmentshader;
static GLuint program;

static GLuint textureUniform;

static GLuint redMatrixUniform;
static GLuint greenMatrixUniform;
static GLuint blueMatrixUniform;
static GLuint exposureMatrixUniform;
static GLuint brightMatrixUniform;

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

    redMatrixUniform = glGetUniformLocation(program, "redMatrix");
    greenMatrixUniform = glGetUniformLocation(program, "greenMatrix");
    blueMatrixUniform = glGetUniformLocation(program, "blueMatrix");
    exposureMatrixUniform = glGetUniformLocation(program, "exposureMatrix");
    brightMatrixUniform = glGetUniformLocation(program, "brightMatrix");

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
    glUniform3f(
        redMatrixUniform, 
        config->color_matrix.r_to_r,
        config->color_matrix.r_to_g,
        config->color_matrix.r_to_b
    );

    glUniform3f(
        greenMatrixUniform,
        config->color_matrix.g_to_r,
        config->color_matrix.g_to_g,
        config->color_matrix.g_to_b
    );

    glUniform3f(
        blueMatrixUniform,
        config->color_matrix.b_to_r,
        config->color_matrix.b_to_g,
        config->color_matrix.b_to_b
    );

    glUniform3f(
        exposureMatrixUniform,
        config->color_matrix.r_exposure,
        config->color_matrix.g_exposure,
        config->color_matrix.b_exposure
    );

    glUniform3f(
        brightMatrixUniform,
        config->color_matrix.r_bright,
        config->color_matrix.g_bright,
        config->color_matrix.b_bright
    );
}


void vs_color_corrector_render(config_virtual_screen *config, render_output *render, vs_color_corrector *data) {
    GLuint texture_id = render->rendered_texture;

    if (!texture_id) {
        return;
    }

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glColor4f(1.0, 1.0, 1.0, 1.0);

    glUseProgram(program);

    vs_color_corrector_set_uniforms(config);

    glBindTexture(GL_TEXTURE_2D, texture_id);
    glActiveTexture(GL_TEXTURE0);
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

    glDisable(GL_BLEND);
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
