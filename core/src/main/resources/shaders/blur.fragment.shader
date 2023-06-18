varying vec2 frag_Uv;
uniform sampler2D image;

uniform vec2 image_size;

void main(void) {
    vec4 sum = vec4(0.0);
    vec2 blurSize = 2.0 / image_size;
    vec4 current_pixel = texture2D(image, frag_Uv);

    sum += texture2D(image, vec2(frag_Uv.x - 4.0*blurSize.x, frag_Uv.y)) * 0.05;
    sum += texture2D(image, vec2(frag_Uv.x - 4.0*blurSize.x, frag_Uv.y)) * 0.05;
    sum += texture2D(image, vec2(frag_Uv.x - 3.0*blurSize.x, frag_Uv.y)) * 0.09;
    sum += texture2D(image, vec2(frag_Uv.x - 2.0*blurSize.x, frag_Uv.y)) * 0.12;
    sum += texture2D(image, vec2(frag_Uv.x - blurSize.x, frag_Uv.y)) * 0.15;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y)) * 0.16;
    sum += texture2D(image, vec2(frag_Uv.x + blurSize.x, frag_Uv.y)) * 0.15;
    sum += texture2D(image, vec2(frag_Uv.x + 2.0*blurSize.x, frag_Uv.y)) * 0.12;
    sum += texture2D(image, vec2(frag_Uv.x + 3.0*blurSize.x, frag_Uv.y)) * 0.09;
    sum += texture2D(image, vec2(frag_Uv.x + 4.0*blurSize.x, frag_Uv.y)) * 0.05;

    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y - 4.0*blurSize.y)) * 0.05;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y - 3.0*blurSize.y)) * 0.09;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y - 2.0*blurSize.y)) * 0.12;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y - blurSize.y)) * 0.15;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y)) * 0.16;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y + blurSize.y)) * 0.15;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y + 2.0*blurSize.y)) * 0.12;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y + 3.0*blurSize.y)) * 0.09;
    sum += texture2D(image, vec2(frag_Uv.x, frag_Uv.y + 4.0*blurSize.y)) * 0.05;

    vec4 result = sum/2.0;

    gl_FragColor = vec4(result.rgb, current_pixel.a);
}