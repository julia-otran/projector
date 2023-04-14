varying vec2 frag_Uv;

uniform sampler2D image;
uniform vec2 adjust_factor;

void main(void) {
    vec2 result = pow(frag_Uv, adjust_factor);
    gl_FragColor = texture2D(image, result);
}
