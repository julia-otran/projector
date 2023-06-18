attribute vec4 in_Position;
attribute vec2 in_Uv;

varying vec2 frag_Uv;

void main(void) {
    gl_Position = in_Position;
    frag_Uv = in_Uv;
}
