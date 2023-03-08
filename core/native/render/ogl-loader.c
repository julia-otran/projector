#include "ogl-loader.h"
#include "debug.h"

GLuint loadShader(const GLchar* const *shaderSource, GLint *sourceLen, GLuint type, char *name) {
    GLuint shaderID = glCreateShader(type);

    glShaderSource(shaderID, 1, shaderSource, sourceLen);
    glCompileShader(shaderID);

    GLint compileStatus;

     glGetShaderiv(shaderID, GL_COMPILE_STATUS, &compileStatus);

    if (compileStatus == 0) {
        GLsizei len;
        GLchar log_buffer[255];

        log_debug("Failed compiling shader: %s\n", name);
        log_debug("Shader SRC:\n\n")
        log_debug("--->\n%s<---\n", shaderSource[0]);

        glGetShaderInfoLog(shaderID, sizeof(log_buffer) - 1, &len, (GLchar*) &log_buffer);

        log_buffer[len - 1] = 0;

        log_debug("%s\n", log_buffer);
    }

    return shaderID;
}
