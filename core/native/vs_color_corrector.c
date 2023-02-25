#include <stdlib.h>
#include "vs_color_corrector.h"

#define VERTEX_SHADER_SRC (""
        "attribute vec4 in_Position;\n"
        "attribute vec2 in_Uv;\n"

        "varying vec2 frag_Uv;\n"

        "void main(void) {\n"
        "    gl_Position = in_Position;\n"
        "    frag_Uv = in_Uv;\n"
        "}"
)

#define FRAGMENT_SHADER_SRC (""
"    varying vec2 frag_Uv;\n"
"    uniform sampler2D image;\n"

"    uniform vec4 brightAdjust;\n"
"    uniform vec4 exposureAdjust;\n"

"    uniform vec4 lowAdjust;\n"
"    uniform vec4 midAdjust;\n"
"    uniform vec4 highAdjust;\n"

"    uniform float preserveLuminosity;\n"

"    vec4 rgbToHsl(vec4 rgbColor) {\n"
"        float minVal = min(rgbColor.r, min(rgbColor.g, rgbColor.b));\n"
"        float maxVal = max(rgbColor.r, max(rgbColor.g, rgbColor.b));\n"

"        float lum = (minVal + maxVal) / 2.0;\n"
"        float sat = 0.0;\n"
"        float hue = 0.0;\n"

"        float delta = maxVal - minVal; \n"

"        if (lum > 0.5) {\n"
"            sat = delta / (2.0 - delta);\n"
"        } else {\n"
"            sat = delta / (maxVal + minVal);\n"
"        }\n"

"        if (delta == 0.0) {\n"
"            delta = 1.0;\n"
"        }\n"
"            \n"
"        if (rgbColor.r == maxVal) {\n"
"            hue = (rgbColor.g - rgbColor.b) / delta; \n"
"        }\n"

"        if (rgbColor.g == maxVal) {\n"
"            hue = 2.0 + ((rgbColor.b - rgbColor.r) / delta);\n"
"        }\n"

"        if (rgbColor.b == maxVal) {\n"
"            hue = 4.0 + ((rgbColor.r - rgbColor.g) / delta);\n"
"        }          \n"

"        hue = hue / 6.0;\n"

"        if (hue < 0.0) {\n"
"            hue = 1.0 + hue;\n"
"        }\n"

"        return(vec4(hue, sat, lum, rgbColor.a));\n"
"    }\n"

"    float convertColorPart(float n1, float n2, float hue) {\n"
"        if (hue > 6.0) {\n"
"            hue = hue - 6.0;\n"
"        }\n"
"        if (hue < 0.0) {\n"
"            hue = hue + 6.0;\n"
"        }\n"
"        if (hue < 1.0) {\n"
"            return(n1 + (n2 - n1) * hue);\n"
"        }\n"
"        if (hue < 3.0) {\n"
"            return(n2);\n"
"        }\n"
"        if (hue < 4.0) {\n"
"            return(n1 + (n2 - n1) * (4.0 - hue));\n"
"        }\n"

"        return(n1);\n"
"    }\n"

"    vec4 hslToRgb(vec4 hsl) {\n"
"        float r = 0.0;\n"
"        float g = 0.0;\n"
"        float b = 0.0;\n"

"        float x = 0.0;\n"
"        float y = 0.0;\n"

"        if (hsl.g > 0.0) {\n"
"            if (hsl.b <= 0.5) {\n"
"                y = hsl.b * (1.0 + hsl.g);\n"
"            } else {\n"
"                y = hsl.g + hsl.b - (hsl.g * hsl.b);\n"
"            }\n"
"            \n"
"            x = (2.0 * hsl.b) - y;\n"
"\n"
"            r = convertColorPart(x, y, hsl.r * 6.0 + 2.0);\n"
"            g = convertColorPart(x, y, hsl.r * 6.0);\n"
"            b = convertColorPart(x, y, hsl.r * 6.0 - 2.0);\n"
"        } else {\n"
"            r = hsl.b;\n"
"            g = hsl.b;\n"
"            b = hsl.b;\n"
"        }\n"

"        return(vec4(r, g, b, hsl.a));\n"
"    }\n"

"    void main(void) {\n"
"        vec4 texel = texture2D(image, frag_Uv);                \n"
"        vec4 hsl = rgbToHsl(texel);\n"

"        float lum = hsl.b;\n"

"        float a = 0.25;\n"
"        float scale = 0.7;\n"
"        float b = 1.0 - scale;\n"

"        float shadowsLum = lum - b;\n"
"        float highlightsLum = lum + b -1.0;\n"

"        float shadowsMultiply = clamp((shadowsLum / (-1.0 * a)) + 0.5, 0.0, 1.0) * scale;\n"
"        float highlightsMultiply = clamp((highlightsLum / a) + 0.5, 0.0, 1.0) * scale;\n"

"        float midtonesMultiply0 = clamp((shadowsLum / a) + 0.5, 0.0, 1.0);\n"
"        float midtonesMultiply1 = clamp((highlightsLum / (-1.0 * a)) + 0.5, 0.0, 1.0);\n"
"        float midtonesMultiply = midtonesMultiply0 * midtonesMultiply1 * scale;\n"

"        vec4 shadows = shadowsMultiply * lowAdjust;\n"
"        vec4 mids = midtonesMultiply * midAdjust;\n"
"        vec4 highs = highlightsMultiply * highAdjust;\n"

"        vec4 colorCorrected = texel + shadows + mids + highs;\n"
"        colorCorrected = clamp(colorCorrected, 0.0, 1.0);\n"
"        vec4 colorCorrectedHsl;\n"

"        if (preserveLuminosity > 0.0) {\n"
"            colorCorrectedHsl = rgbToHsl(colorCorrected);\n"
"            colorCorrectedHsl.b = hsl.b;\n"
"            colorCorrected = hslToRgb(colorCorrectedHsl);\n"
"        }\n"

"        vec4 result = colorCorrected * exposureAdjust + brightAdjust;\n"

"        gl_FragColor = vec4(result.rgb, 1.0);\n"
"    }"
)

GLuint loadShader(char *shaderSource, GLuint type, char *name) {
    GLuint shaderID = glCreateShader(type);

    glShaderSource(shaderID, shaderSource);
    glCompileShader(shaderID);

    GLuint compileStatus = glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS)

    if (compileStatus == 0) {
        printf("Failed compiling shader %s\n", name);
        printf("%s\n", glGetShaderInfoLog(shaderID));
    }

    return shaderID;
}

void load_coordinates(config_virtual_screen *config, vs_color_corrector *data) {
    GLuint vertexarray;
    glGenVertexArrays(1, &vertexarray);
    glBindVertexArray(vertexarray);
    data->vertexarray = vertexarray;

    GLfloat *indexed_vertices = calloc(16, sizeof(GLfloat));

    indexed_vertices[0] = config->output_bounds.x;
    indexed_vertices[1] = config->output_bounds.y;
    indexed_vertices[2] = 0.0;
    indexed_vertices[3] = 1.0;

    indexed_vertices[4] = config->output_bounds.x;
    indexed_vertices[5] = config->output_bounds.y + config->output_bounds.h;
    indexed_vertices[6] = 0.0;
    indexed_vertices[7] = 1.0;

    indexed_vertices[8] = config->output_bounds.x + config->output_bounds.w;
    indexed_vertices[9] = config->output_bounds.y + config->output_bounds.h;
    indexed_vertices[10] = 0.0;
    indexed_vertices[11] = 1.0;

    indexed_vertices[12] = config->output_bounds.x + config->output_bounds.w;
    indexed_vertices[13] = config->output_bounds.y;
    indexed_vertices[14] = 0.0;
    indexed_vertices[15] = 1.0;

    GLuint vertexbuffer;
    glGenBuffers(1, &vertexbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, vertexbuffer);
    glBufferData(GL_ARRAY_BUFFER, 16, &indexed_vertices[0], GL_STATIC_DRAW);
    glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 0, 0);

    data->vertexbuffer = vertexbuffer;
    free(indexed_vertices);

    GLfloat *indexed_uvs = calloc(8, sizeof(GLfloat));

    indexed_uvs[0] = config->input_bounds.x;
    indexed_uvs[1] = config->input_bounds.y;

    indexed_uvs[2] = config->input_bounds.x;
    indexed_uvs[3] = config->input_bounds.y + config->input_bounds.h;

    indexed_uvs[4] = config->input_bounds.x + config->input_bounds.w;
    indexed_uvs[5] = config->input_bounds.y + config->input_bounds.h;

    indexed_uvs[6] = config->input_bounds.x + config->input_bounds.w;
    indexed_uvs[7] = config->input_bounds.y;

    GLuint uvbuffer;
    glGenBuffers(1, &uvbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, uvbuffer);
    glBufferData(GL_ARRAY_BUFFER, 8, &indexed_uvs[0], GL_STATIC_DRAW);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 0, 0);

    data->uvbuffer = uvbuffer;
    free(indexed_uvs);

    glBindVertexArray(0);
}

void setup_shaders(config_virtual_screen *, vs_color_corrector *data) {
    data->vertexshader = loadShader(VERTEX_SHADER_SRC, GL_VERTEX_SHADER, "Vertex Shader");
    data->fragmentshader = loadShader(FRAGMENT_SHADER_SRC, GL_FRAGMENT_SHADER, "Fragment Shader");

    data->program = glCreateProgram();
    glAttachShader(data->program, data->vertexshader);
    glAttachShader(data->program, data->fragmentshader);

    glBindAttribLocation(data->program, 0, "in_Position");
    glBindAttribLocation(data->program, 1, "in_Uv");

    glLinkProgram(data->program);
    glValidateProgram(data->program);

    data->textureUniform = glGetUniformLocation(data->program, "image");
    data->brightAdjustUniform = glGetUniformLocation(data->program, "brightAdjust");
    data->exposureAdjustUniform = glGetUniformLocation(data->program, "exposureAdjust");
    data->lowAdjustUniform = glGetUniformLocation(data->program, "lowAdjust");
    data->midAdjustUniform = glGetUniformLocation(data->program, "midAdjust");
    data->highAdjustUniform = glGetUniformLocation(data->program, "highAdjust");
    data->preserveLumUniform = glGetUniformLocation(data->program, "preserveLuminosity");

    glUseProgram(0);
}

void initUniforms(config_virtual_screen *config, vs_color_corrector *data) {
    glUseProgram(data->program);
    glUniform4f(data->brightAdjustUniform, config->white_balance.bright.r, config->white_balance.bright.g, config->white_balance.bright.b, 0.0);
    glUniform4f(data->exposureAdjustUniform, config->white_balance.bright.r, config->white_balance.bright.g, config->white_balance.bright.b, 1.0);

    glUniform4f(data->lowAdjustUniform, config->color_balance.shadows.r, config->color_balance.shadows.g, config->color_balance.shadows.b, 0.0);
    glUniform4f(data->midAdjustUniform, config->color_balance.midtones.r, config->color_balance.midtones.g, config->color_balance.midtones.b, 0.0);
    glUniform4f(data->highAdjustUniform, config->color_balance.highlights.r, config->color_balance.highlights.g, config->color_balance.highlights.b, 0.0);
    glUniform1f(data->preserveLumUniform, config->color_balance.preserve_luminosity);

    glUniform1i(data->textureUniform, GL_TEXTURE0);

    glUseProgram(0);
}

void vs_color_corrector_init(config_virtual_screen *config, vs_color_corrector *data) {
    load_coordinates(config, data);
    setup_shaders(config, data);
    initUniforms(config, data);
}

void vs_color_corrector_render_texture(GLuint texture_id, vs_color_corrector *data) {
        glUseProgram(data->program);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture_id);

        // Bind to the VAO that has all the information about the vertices
        glBindVertexArray(data->vertexarray);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawArrays(GL_QUADS, 0, 4);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
        glUseProgram(0);
}