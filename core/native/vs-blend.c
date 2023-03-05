#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "ogl-loader.h"
#include "debug.h"
#include "vs-blend.h"

static const GLfloat UV_VS_BLEND_MODE[4][8] = {
    // Left to Right
    {
        0.0F, 0.0F,
        1.0F, 0.0F,
        1.0F, 0.0F,
        0.0F, 0.0F
    },
    // Right to Left
    {
        1.0F, 0.0F,
        0.0F, 0.0F,
        0.0F, 0.0F,
        1.0F, 0.0F
    }
};

static const GLchar* VERTEX_SHADER_SRC[1] =  {
 "\n"
 "        attribute vec4 in_Position;\n"
 "        attribute vec2 in_Uv;\n"
 "        \n"
 "        varying vec2 frag_Uv;\n"
 "        \n"
 "        void main(void) {\n"
 "            gl_Position = in_Position;\n"
 "            frag_Uv = in_Uv;\n"
 "        }\n"
 };

static GLint VERTEX_SHADER_SRC_LEN = 0;

static const GLchar* FRAGMENT_SHADER_SRC[1] = {
"\n"
"varying vec2 frag_Uv;\n"
"\n"
"void main(void) {\n"
"    float alpha = frag_Uv.x + frag_Uv.y;\n"
"    gl_FragColor = vec4(0.0, 0.0, 0.0, alpha * alpha);\n"
"}"
};

static GLint FRAGMENT_SHADER_SRC_LEN = 0;

void vs_blend_load_coordinates(config_bounds *display_bounds, config_virtual_screen *virtual_screen, config_blend *config, vs_blend_vertex *data) {
    GLuint vertexarray;
    glGenVertexArrays(1, &vertexarray);
    glBindVertexArray(vertexarray);

    data->vertexarray = vertexarray;

    GLfloat *indexed_vertices = calloc(16, sizeof(GLfloat));

    GLfloat x, y, w, h;

    // Virtual screen bounds
    x = (virtual_screen->output_bounds.x * 2.0 / display_bounds->w) - 1;
    y = (virtual_screen->output_bounds.y * 2.0 / display_bounds->h) - 1;
    w = (virtual_screen->output_bounds.w * 2.0 / display_bounds->w);
    h = (virtual_screen->output_bounds.h * 2.0 / display_bounds->h);

    // Blend bounds inside VS
    x = (config->position.x * w / virtual_screen->output_bounds.w) + x;
    y = (config->position.y * h / virtual_screen->output_bounds.h) + y;
    w = (config->position.w * w / virtual_screen->output_bounds.w);
    h = (config->position.h * h / virtual_screen->output_bounds.h);

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

    GLuint uvbuffer;
    glGenBuffers(1, &uvbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, uvbuffer);
    glBufferData(GL_ARRAY_BUFFER, 8 * sizeof(GLfloat), UV_VS_BLEND_MODE[config->direction], GL_STATIC_DRAW);

    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    data->uvbuffer = uvbuffer;

    glBindVertexArray(0);
}

void vs_blend_setup_shaders(vs_blend *data) {
    VERTEX_SHADER_SRC_LEN = strlen(VERTEX_SHADER_SRC[0]);
    FRAGMENT_SHADER_SRC_LEN = strlen(FRAGMENT_SHADER_SRC[0]);

    data->vertexshader = loadShader(VERTEX_SHADER_SRC, &VERTEX_SHADER_SRC_LEN, GL_VERTEX_SHADER, "Blend Vertex Shader");
    data->fragmentshader = loadShader(FRAGMENT_SHADER_SRC, &FRAGMENT_SHADER_SRC_LEN, GL_FRAGMENT_SHADER, "Blend Fragment Shader");

    data->program = glCreateProgram();
    glAttachShader(data->program, data->vertexshader);
    glAttachShader(data->program, data->fragmentshader);

    glBindAttribLocation(data->program, 0, "in_Position");
    glBindAttribLocation(data->program, 1, "in_Uv");

    glLinkProgram(data->program);
    glValidateProgram(data->program);

    glUseProgram(0);
}

void vs_blend_initialize(config_bounds *display_bounds, config_virtual_screen *virtual_screen, vs_blend *instance) {
    instance->vertexes = (vs_blend_vertex*) calloc(virtual_screen->count_blends, sizeof(vs_blend_vertex));
    instance->vertexes_count = virtual_screen->count_blends;

    for (int i = 0; i < virtual_screen->count_blends; i++) {
        vs_blend_load_coordinates(display_bounds, virtual_screen, &virtual_screen->blends[i], &instance->vertexes[i]);
    }

    vs_blend_setup_shaders(instance);
}

void vs_blend_render(vs_blend *instance) {
    glBindTexture(GL_TEXTURE_2D, 0);

    glUseProgram(instance->program);

    for (int i = 0; i < instance->vertexes_count; i++) {
        glBindVertexArray(instance->vertexes[i].vertexarray);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawArrays(GL_QUADS, 0, 4);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    glUseProgram(0);
}

void vs_blend_shutdown(vs_blend *instance) {
    for (int i = 0; i < instance->vertexes_count; i++) {
        vs_blend_vertex *data = &instance->vertexes[i];

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

    free(instance->vertexes);

    glUseProgram(0);

    glDetachShader(instance->program, instance->vertexshader);
    glDetachShader(instance->program, instance->fragmentshader);
    glDeleteShader(instance->vertexshader);
    glDeleteShader(instance->fragmentshader);
    glDeleteProgram(instance->program);
}