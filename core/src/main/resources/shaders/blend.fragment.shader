varying vec2 frag_Uv;

void main(void) {
    float alpha = frag_Uv.x + frag_Uv.y;
    gl_FragColor = vec4(0.0, 0.0, 0.0, alpha * alpha);
}
