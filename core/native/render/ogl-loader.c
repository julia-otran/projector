#include <string.h>

#include "ogl-loader.h"
#include "debug.h"

static shader_data_node *shaders;

void add_shader_data(char *name, char *data) {
    shader_data_node *shader_node = (shader_data_node*) malloc(sizeof(shader_data_node));

    size_t len = strlen(name);
    shader_node->shader_name = malloc(len + 1);
    memcpy(shader_node->shader_name, name, len);

    shader_node->shader_name[len] = 0;

    len = strlen(data);
    shader_node->shader_code = malloc(len + 1);
    memcpy(shader_node->shader_code, data, len);

    shader_node->shader_code[len] = 0;

    shader_node->next = shaders;
    shaders = shader_node;

    log_debug("Shader data added: %s\n", name);
}

GLuint loadShader(GLuint type, char *name) {
    GLchar* shader_code[1] = { NULL };
    GLint shader_size[1] = { 0 };

    for (shader_data_node *shader_node = shaders; shader_node; shader_node = shader_node->next) {
        if (strcmp(name, shader_node->shader_name) == 0) {
            shader_code[0] = shader_node->shader_code;
            break;
        }
    }

    if (shader_code[0] == NULL) {
        log_debug("Shader load fail: '%s' not found.\n", name);
        return 0;
    }

    shader_size[0] = strlen(shader_code[0]);

    GLuint shaderID = glCreateShader(type);

    glShaderSource(shaderID, 1, shader_code, shader_size);
    glCompileShader(shaderID);

    GLint compileStatus;

    glGetShaderiv(shaderID, GL_COMPILE_STATUS, &compileStatus);

    if (compileStatus == 0) {
        GLsizei len;
        GLchar log_buffer[255];

        log_debug("Failed compiling shader: %s\n", name);
        log_debug("Shader SRC:\n\n")
        log_debug("--->\n%s<---\n", shader_code[0]);

        glGetShaderInfoLog(shaderID, sizeof(log_buffer) - 1, &len, (GLchar*) &log_buffer);

        log_buffer[len - 1] = 0;

        log_debug("%s\n", log_buffer);
    } else {
        log_debug("Shader compiled: %s\n", name);
    }

    return shaderID;
}
