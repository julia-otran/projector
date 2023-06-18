varying vec2 frag_Uv;
uniform sampler2D image;

uniform vec3 redMatrix;
uniform vec3 greenMatrix;
uniform vec3 blueMatrix;

uniform vec3 exposureMatrix;
uniform vec3 brightMatrix;

void main(void) {
    vec4 texel = texture2D(image, frag_Uv);

    vec3 maxLevels = 1.0 / (redMatrix + greenMatrix + blueMatrix);

    vec3 matrixMulti = texel.r * redMatrix +
        texel.g * greenMatrix +
        texel.b * blueMatrix;

    vec3 result = (matrixMulti * maxLevels * exposureMatrix) + brightMatrix;

    gl_FragColor = vec4(result.rgb, texel.a);
}
