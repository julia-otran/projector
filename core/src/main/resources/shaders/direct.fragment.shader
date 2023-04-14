varying vec2 frag_Uv;
uniform sampler2D image;

void main(void) {
    gl_FragColor = texture2D(image, frag_Uv);
}
