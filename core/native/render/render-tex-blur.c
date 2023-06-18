#include <stdlib.h>
#include <stdio.h>

#include "render-tex-blur.h"
#include "ogl-loader.h"

static GLuint program;
static GLuint vertexshader;
static GLuint fragmentshader;
static GLuint imageSizeUniform;
static GLuint textureUniform;

void render_tex_blur_create_assets() {
    vertexshader = loadShader(GL_VERTEX_SHADER, "blur.vertex.shader");
    fragmentshader = loadShader(GL_FRAGMENT_SHADER, "blur.fragment.shader");

    program = glCreateProgram();
    glAttachShader(program, vertexshader);
    glAttachShader(program, fragmentshader);

    glBindAttribLocation(program, 0, "in_Position");
    glBindAttribLocation(program, 1, "in_Uv");

    glLinkProgram(program);
    glValidateProgram(program);

    textureUniform = glGetUniformLocation(program, "image");
    imageSizeUniform = glGetUniformLocation(program, "image_size");

    glUseProgram(0);
}

void render_tex_blur_deallocate_assets() {
    glUseProgram(0);

    glDetachShader(program, vertexshader);
    glDetachShader(program, fragmentshader);
    glDeleteShader(vertexshader);
    glDeleteShader(fragmentshader);
    glDeleteProgram(program);
}

render_tex_blur_instance* render_tex_blur_create() {
    render_tex_blur_instance* instance = (render_tex_blur_instance*) calloc(1, sizeof(render_tex_blur_instance));

    GLuint vertexarray;
    glGenVertexArrays(1, &vertexarray);

    GLuint vertexbuffer;
    glGenBuffers(1, &vertexbuffer);

    GLuint uvbuffer;
    glGenBuffers(1, &uvbuffer);

    instance->vertexarray = vertexarray;
    instance->vertexbuffer = vertexbuffer;
    instance->uvbuffer = uvbuffer;

    return instance;
}
void render_tex_blur_set_tex(render_tex_blur_instance* instance, GLuint tex, int tex_w, int tex_h) {
    instance->tex = tex;
    instance->tex_w = tex_w;
    instance->tex_h = tex_h;
}

void render_tex_blur_set_uv(render_tex_blur_instance* instance, float x, float y, float w, float h) {
    GLfloat* indexed_vertices = calloc(8, sizeof(GLfloat));

    indexed_vertices[0] = x;
    indexed_vertices[1] = y;

    indexed_vertices[2] = x;
    indexed_vertices[3] = y + h;

    indexed_vertices[4] = x + w;
    indexed_vertices[5] = y + h;

    indexed_vertices[6] = x + w;
    indexed_vertices[7] = y;

    glBindVertexArray(instance->vertexarray);

    glBindBuffer(GL_ARRAY_BUFFER, instance->uvbuffer);
    glBufferData(GL_ARRAY_BUFFER, 8 * sizeof(GLfloat), indexed_vertices, GL_DYNAMIC_DRAW);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    glBindVertexArray(0);

    free(indexed_vertices);
}

void render_tex_blur_set_position(render_tex_blur_instance* instance, float x, float y, float w, float h) {
    GLfloat* indexed_vertices = calloc(16, sizeof(GLfloat));

    indexed_vertices[0] = x;
    indexed_vertices[1] = y + h;
    indexed_vertices[2] = 0.0;
    indexed_vertices[3] = 1.0;

    indexed_vertices[4] = x;
    indexed_vertices[5] = y;
    indexed_vertices[6] = 0.0;
    indexed_vertices[7] = 1.0;

    indexed_vertices[8] = x + w;
    indexed_vertices[9] = y;
    indexed_vertices[10] = 0.0;
    indexed_vertices[11] = 1.0;

    indexed_vertices[12] = x + w;
    indexed_vertices[13] = y + h;
    indexed_vertices[14] = 0.0;
    indexed_vertices[15] = 1.0;

    glBindVertexArray(instance->vertexarray);

    glBindBuffer(GL_ARRAY_BUFFER, instance->vertexbuffer);
    glBufferData(GL_ARRAY_BUFFER, 16 * sizeof(GLfloat), indexed_vertices, GL_DYNAMIC_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 0, 0);

    glBindBuffer(GL_ARRAY_BUFFER, 0);

    glBindVertexArray(0);
    free(indexed_vertices);
}

void render_tex_blur_destroy(render_tex_blur_instance* instance) {
    // Select the VAO
    glBindVertexArray(instance->vertexarray);

    // Disable the VBO index from the VAO attributes list
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);

    // Delete the vertex VBO
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDeleteBuffers(1, &instance->vertexbuffer);

    // Delete the color VBO
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDeleteBuffers(1, &instance->uvbuffer);

    // Delete the VAO
    glBindVertexArray(0);
    glDeleteVertexArrays(1, &instance->vertexarray);

    free(instance);
}

void render_tex_blur_render(render_tex_blur_instance* instance) {
    glUseProgram(program);
    glUniform2f(imageSizeUniform, instance->tex_w, instance->tex_h);

    glBindTexture(GL_TEXTURE_2D, instance->tex);
    glActiveTexture(GL_TEXTURE0);
    glUniform1i(textureUniform, 0);

    glBindVertexArray(instance->vertexarray);
    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);

    glDrawArrays(GL_QUADS, 0, 4);

    glBindTexture(GL_TEXTURE_2D, 0);
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindVertexArray(0);

    glUseProgram(0);
}
