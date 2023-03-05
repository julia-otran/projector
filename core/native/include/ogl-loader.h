#ifndef _OGL_LOADER_H_
#define _OGL_LOADER_H_

#include <GL/glew.h>
#include <GLFW/glfw3.h>

GLuint loadShader(const GLchar* const *shaderSource, GLint *sourceLen, GLuint type, char *name);

#endif