#ifndef _OGL_LOADER_H_
#define _OGL_LOADER_H_

#ifdef _WIN32
#define GLFW_EXPOSE_NATIVE_WIN32
#endif

#include <GL/glew.h>
#include <GLFW/glfw3.h>

typedef struct {
    char *shader_name;
    char *shader_code;

    void *next;
} shader_data_node;

void add_shader_data(char *name, char *data);

GLuint loadShader(GLuint type, char *name);

void tex_set_default_params();

#endif