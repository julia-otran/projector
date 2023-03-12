#ifndef _OGL_LOADER_H_
#define _OGL_LOADER_H_

#include <GL/glew.h>
#include <GLFW/glfw3.h>

typedef struct {
    char *shader_name;
    char *shader_code;

    void *next;
} shader_data_node;

void add_shader_data(char *name, char *data);

GLuint loadShader(GLuint type, char *name);

#endif