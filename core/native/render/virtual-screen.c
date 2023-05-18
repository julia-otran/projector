#include <stdlib.h>
#include <string.h>

#define REAL double
#define VOID void
#define __UINT64 __uint64

#include "triangle.h"
#include "ogl-loader.h"
#include "debug.h"
#include "config-structs.h"
#include "virtual-screen.h"
#include "vs-black-level-adjust.h"
#include "vs-blend.h"
#include "vs-color-corrector.h"
#include "vs-help-lines.h"

static GLuint vertexshader;
static GLuint fragmentshader;
static GLuint program;
static GLuint textureUniform;
static GLuint adjustFactorUniform;

void virtual_screen_initialize() {
    vs_color_corrector_init();
    vs_blend_initialize();

    vertexshader = loadShader(GL_VERTEX_SHADER, "direct.vertex.shader");
    fragmentshader = loadShader(GL_FRAGMENT_SHADER, "direct.fragment.shader");

    program = glCreateProgram();
    glAttachShader(program, vertexshader);
    glAttachShader(program, fragmentshader);

    glBindAttribLocation(program, 0, "in_Position");
    glBindAttribLocation(program, 1, "in_Uv");

    glLinkProgram(program);
    glValidateProgram(program);

    textureUniform = glGetUniformLocation(program, "image");
    adjustFactorUniform = glGetUniformLocation(program, "adjust_factor");
}

void virtual_screen_free_triangulateio(struct triangulateio *in) {
    if (in->pointlist) {
        free(in->pointlist);
    }
    if (in->pointattributelist) {
        free(in->pointattributelist);
    }
    if (in->pointmarkerlist) {
        free(in->pointmarkerlist);
    }
    if (in->trianglelist) {
        free(in->trianglelist);
    }
    if (in->triangleattributelist) {
        free(in->triangleattributelist);
    }
    if (in->trianglearealist) {
        free(in->trianglearealist);
    }
    if (in->neighborlist) {
        free(in->neighborlist);
    }
    if (in->segmentlist) {
        free(in->segmentlist);
    }
    if (in->segmentmarkerlist) {
        free(in->segmentmarkerlist);
    }
    if (in->edgelist) {
        free(in->edgelist);
    }
    if (in->edgemarkerlist) {
        free(in->edgemarkerlist);
    }
}

void virtual_screen_load_vertexes(config_display *display, config_virtual_screen *config, virtual_screen *data) {
    struct triangulateio in, out;
    int pindex;
    GLfloat x, y;

    memset(&in, 0, sizeof(struct triangulateio));
    memset(&out, 0, sizeof(struct triangulateio));

    GLuint vertexarray;
    glGenVertexArrays(1, &vertexarray);
    glBindVertexArray(vertexarray);

    data->vertexarray = vertexarray;

    int count_points = config->monitor_position.count_points;

    in.numberofpoints = count_points;
    in.numberofpointattributes = 0;

    in.pointlist = (REAL*) calloc(count_points * 2, sizeof(REAL));
    in.pointmarkerlist = (int *) calloc(count_points, sizeof(int));

    // Output Vertexes

    for (int i = 0; i < count_points; i++) {
        in.pointlist[i * 2] = config->monitor_position.output_points[i].x;
        in.pointlist[(i * 2) + 1] = config->monitor_position.output_points[i].y;
        in.pointmarkerlist[i] = i;
    }

    triangulate("pcz", &in, &out, NULL);

    data->points_count = out.numberoftriangles * 3;

    GLfloat *vertexes = (GLfloat*) calloc(out.numberoftriangles * 3 * 4, sizeof(GLfloat));

    for (int i = 0; i < (out.numberoftriangles * 3); i++) {
        pindex = out.trianglelist[i];

        x = (GLfloat) out.pointlist[pindex * 2];
        y = (GLfloat) out.pointlist[(pindex * 2) + 1];

        x = (x * 2.0 / display->monitor_bounds.w) - 1.0;
        y = 1.0 - (y * 2.0 / display->monitor_bounds.h);

        vertexes[i * 4] = x;
        vertexes[(i * 4) + 1] = y;
        vertexes[(i * 4) + 2] = (float) 0.0;
        vertexes[(i * 4) + 3] = (float) 1.0;
    }

    GLuint vertexbuffer;
    glGenBuffers(1, &vertexbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, vertexbuffer);
    glBufferData(GL_ARRAY_BUFFER, out.numberoftriangles * 3 * 4 * sizeof(GLfloat), vertexes, GL_STATIC_DRAW);

    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 0, 0);

    glBindBuffer(GL_ARRAY_BUFFER, 0);

    free(vertexes);

    data->vertexbuffer = vertexbuffer;

    // UV Buffer

    vertexes = (GLfloat*) calloc(out.numberoftriangles * 3 * 2, sizeof(GLfloat));

    for (int i = 0; i < out.numberoftriangles * 3; i++) {
        pindex = out.trianglelist[i];

        x = (GLfloat) config->monitor_position.input_points[pindex].x;
        y = (GLfloat)config->monitor_position.input_points[pindex].y;

        x = x / (GLfloat) config->w;
        y = y / (GLfloat) config->h;

        vertexes[i * 2] = x;
        vertexes[(i * 2) + 1] = 1.0 - y;
    }

    GLuint uvbuffer;
    glGenBuffers(1, &uvbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, uvbuffer);
    glBufferData(GL_ARRAY_BUFFER, out.numberoftriangles * 3 * 2 * sizeof(GLfloat), vertexes, GL_STATIC_DRAW);

    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    free(vertexes);

    virtual_screen_free_triangulateio(&out);

    free(in.pointlist);
    free(in.pointmarkerlist);

    data->uvbuffer = uvbuffer;

    glBindVertexArray(0);
}

void virtual_screen_start(config_display *display, render_output *render, config_virtual_screen *config, void **data) {
    virtual_screen *vs = (virtual_screen*) calloc(1, sizeof(virtual_screen));
    (*data) = (void*) vs;

    vs->render_output = render;

    int width = config->w;
    int height = config->h;

    GLuint renderedTexture;
    glGenTextures(1, &renderedTexture);

    vs->texture_id = renderedTexture;

    // "Bind" the newly created texture : all future texture functions will modify this texture
    glBindTexture(GL_TEXTURE_2D, renderedTexture);

    // Give an empty image to OpenGL ( the last "0" )
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0 ,GL_RGBA, GL_UNSIGNED_BYTE, 0);
    glTexStorage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, 4, GL_RGBA8, width, height, GL_TRUE);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

    glBindTexture(GL_TEXTURE_2D, 0);

    GLuint framebuffer_id = 0;
    glGenFramebuffers(1, &framebuffer_id);
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer_id);

    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderedTexture, 0);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    vs->framebuffer_id = framebuffer_id;

    vs_color_corrector_start(config, vs->render_output, &vs->color_corrector);
    vs_blend_start(config, &vs->blend);

    virtual_screen_load_vertexes(display, config, vs);
}

void virtual_screen_render(config_virtual_screen *config, void *data) {
    virtual_screen *vs = (virtual_screen*) data;
    config_color_factor *background_clear_color = &config->background_clear_color;

    glPushMatrix();

    glBindFramebuffer(GL_FRAMEBUFFER, vs->framebuffer_id);

    glViewport(0, 0, config->w, config->h);

    glClearColor(background_clear_color->r, background_clear_color->g, background_clear_color->b, background_clear_color->a);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glLoadIdentity();
    glOrtho(0.0, config->w, config->h, 0.0, 0.0, 1.0);

    vs_color_corrector_render(config, vs->render_output, &vs->color_corrector);
    vs_blend_render(&vs->blend);
    vs_help_lines_render(config);
    vs_black_level_adjust_render(config);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    glPopMatrix();
}

void virtual_screen_print(config_virtual_screen *config, void *data) {
    virtual_screen *vs = (virtual_screen*) data;
    glEnable(GL_COLOR_MATERIAL);

    glEnable(GL_TEXTURE_2D);
    glEnable(GL_MULTISAMPLE);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glColor4d(1.0, 1.0, 1.0, 1.0);

    glBindTexture(GL_TEXTURE_2D, vs->texture_id);

    glUseProgram(program);

    glActiveTexture(GL_TEXTURE0);
    glUniform1i(textureUniform, 0);

    glUniform2f(
        adjustFactorUniform,
        config->monitor_position.output_horizontal_adjust_factor,
        config->monitor_position.output_vertical_adjust_factor);

    glBindVertexArray(vs->vertexarray);
    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);

    glDrawArrays(GL_TRIANGLES, 0, vs->points_count);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindVertexArray(0);

    glUseProgram(0);

    glBindTexture(GL_TEXTURE_2D, 0);

    glDisable(GL_MULTISAMPLE);
}

void virtual_screen_stop(void *data) {
    virtual_screen *vs = (virtual_screen*) data;

    vs_color_corrector_stop(&vs->color_corrector);
    vs_blend_stop(&vs->blend);

    glDeleteFramebuffers(1, &vs->framebuffer_id);
    glDeleteTextures(1, &vs->texture_id);

    // Select the VAO
    glBindVertexArray(vs->vertexarray);

    // Disable the VBO index from the VAO attributes list
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);

    // Delete the vertex VBO
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDeleteBuffers(1, &vs->vertexbuffer);

    // Delete the color VBO
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDeleteBuffers(1, &vs->uvbuffer);

    // Delete the VAO
    glBindVertexArray(0);
    glDeleteVertexArrays(1, &vs->vertexarray);

    free(data);
}

void virtual_screen_shutdown() {
    vs_color_corrector_shutdown();
    vs_blend_shutdown();

    glUseProgram(0);

    glDetachShader(program, vertexshader);
    glDetachShader(program, fragmentshader);
    glDeleteShader(vertexshader);
    glDeleteShader(fragmentshader);
    glDeleteProgram(program);
}
