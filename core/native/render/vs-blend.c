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
        0.0F, 0.0F,
        1.0F, 0.0F,
        1.0F, 0.0F
    },
    // Right to Left
    {
        1.0F, 0.0F,
        1.0F, 0.0F,
        0.0F, 0.0F,
        0.0F, 0.0F
    },
    // Top to Bottom
    {
        0.0F, 1.0F,
        0.0F, 1.0F,
        0.0F, 0.0F,
        0.0F, 0.0F
    },
    // Bottom to Top
    {
        0.0F, 0.0F,
        0.0F, 0.0F,
        0.0F, 1.0F,
        0.0F, 1.0F
    },
};

static GLuint vertexshader;
static GLuint fragmentshader;
static GLuint program;

void vs_blend_initialize() {
    vertexshader = loadShader(GL_VERTEX_SHADER, "blend.vertex.shader");
    fragmentshader = loadShader(GL_FRAGMENT_SHADER, "blend.fragment.shader");

    program = glCreateProgram();
    glAttachShader(program, vertexshader);
    glAttachShader(program, fragmentshader);

    glBindAttribLocation(program, 0, "in_Position");
    glBindAttribLocation(program, 1, "in_Uv");

    glLinkProgram(program);
    glValidateProgram(program);

    glUseProgram(0);
}

void vs_blend_load_coordinates(config_virtual_screen *virtual_screen, config_blend *config, vs_blend_vertex *data) {
    GLuint vertexarray;
    glGenVertexArrays(1, &vertexarray);
    glBindVertexArray(vertexarray);

    data->vertexarray = vertexarray;

    GLfloat *indexed_vertices = calloc(16, sizeof(GLfloat));

    GLfloat x, y, w, h;

    // Blend bounds inside VS
    w = (config->position.w * 2 / virtual_screen->w);
    h = (config->position.h * 2 / virtual_screen->h);

    x = (config->position.x * 2.0 / virtual_screen->w) - 1.0;
    y = 1.0 - h - (config->position.y * 2.0 / virtual_screen->h);

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

void vs_blend_start(config_virtual_screen *virtual_screen, vs_blend *instance) {
    instance->vertexes = (vs_blend_vertex*) calloc(virtual_screen->count_blends, sizeof(vs_blend_vertex));
    instance->vertexes_count = virtual_screen->count_blends;

    for (int i = 0; i < virtual_screen->count_blends; i++) {
        vs_blend_load_coordinates(virtual_screen, &virtual_screen->blends[i], &instance->vertexes[i]);
    }
}

void vs_blend_render(vs_blend *instance) {
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    glColor4d(1.0, 1.0, 1.0, 1.0);

    glBindTexture(GL_TEXTURE_2D, 0);

    glUseProgram(program);

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

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glDisable(GL_BLEND);
}

void vs_blend_stop(vs_blend *instance) {
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
}

void vs_blend_shutdown() {
    glUseProgram(0);

    glDetachShader(program, vertexshader);
    glDetachShader(program, fragmentshader);
    glDeleteShader(vertexshader);
    glDeleteShader(fragmentshader);
    glDeleteProgram(program);
}
