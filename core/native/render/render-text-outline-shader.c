#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "ogl-loader.h"
#include "debug.h"
#include "render-text-outline-shader.h"

static GLuint program;
static GLuint vertexshader;
static GLuint fragmentshader;
static GLuint textureuniform;
static GLuint texture_size_uniform;

static const GLfloat RENDER_TEXT_OUTLINE_UV[8] = {
    0.0, 1.0,
    0.0, 0.0,
    1.0, 0.0,
    1.0, 1.0
};

void render_text_outline_shader_initialize(int in_width, int in_height) {
    program = glCreateProgram();

    vertexshader = loadShader(GL_VERTEX_SHADER, "bicubic-filter.vertex.shader");
    fragmentshader = loadShader(GL_FRAGMENT_SHADER, "bicubic-filter.fragment.shader");

    glAttachShader(program, vertexshader);
    glAttachShader(program, fragmentshader);

    glBindAttribLocation(program, 0, "in_Position");
    glBindAttribLocation(program, 1, "in_Uv");

    glLinkProgram(program);
    glValidateProgram(program);

    glUseProgram(program);

    textureuniform = glGetUniformLocation(program, "image");
    texture_size_uniform = glGetUniformLocation(program, "inTexSize");

    glProgramUniform2f(program, texture_size_uniform, (GLfloat)in_width, (GLfloat)in_height);

    glUseProgram(0);
}

void render_text_outline_shader_start(render_layer *render) {
    GLuint vertexarray;
    glGenVertexArrays(1, &vertexarray);
    glBindVertexArray(vertexarray);

    render->outline_vertex_array = vertexarray;

    GLfloat *indexed_vertices = calloc(16, sizeof(GLfloat));

    GLfloat x, y, w, h;

    w = (render->config.text_area.w * 2.0 / render->config.w);
    h = (render->config.text_area.h * 2.0 / render->config.h);

    x = (render->config.text_area.x * 2.0 / render->config.w);
    y = (render->config.text_area.y * 2.0 / render->config.h);

    x = x - 1.0;
    y = 1.0 - h - y;

    log_debug("Blend vertices x %f y %f w %f h %f\n", x, y, w, h);

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

    render->outline_vertex_buffer = vertexbuffer;
    free(indexed_vertices);

    GLuint uvbuffer;
    glGenBuffers(1, &uvbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, uvbuffer);
    glBufferData(GL_ARRAY_BUFFER, 8 * sizeof(GLfloat), RENDER_TEXT_OUTLINE_UV, GL_STATIC_DRAW);

    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    render->outline_uv_buffer = uvbuffer;

    glBindVertexArray(0);
}

void render_text_outline_shader_render(render_layer *render) {
    glUseProgram(program);

    glUniform1i(textureuniform, 0);

    glBindVertexArray(render->outline_vertex_array);

    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);

    glDrawArrays(GL_QUADS, 0, 4);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindVertexArray(0);
    glUseProgram(0);
}

void render_text_outline_shader_stop(render_layer *render) {
    glBindVertexArray(render->outline_vertex_array);
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDeleteBuffers(1, &render->outline_vertex_buffer);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDeleteBuffers(1, &render->outline_uv_buffer);

    glBindVertexArray(0);
    glDeleteVertexArrays(1, &render->outline_vertex_array);
}

void render_text_outline_shader_shutdown() {
    glUseProgram(0);

    glDetachShader(program, vertexshader);
    glDetachShader(program, fragmentshader);
    glDeleteShader(vertexshader);
    glDeleteShader(fragmentshader);
    glDeleteProgram(program);
}