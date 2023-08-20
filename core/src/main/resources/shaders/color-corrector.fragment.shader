varying vec2 frag_Uv;
uniform sampler2D image;

uniform vec3 redMatrix;
uniform vec3 greenMatrix;
uniform vec3 blueMatrix;

uniform vec3 exposureMatrix;
uniform vec3 brightMatrix;

uniform vec3 colorCorrector;

vec3 hsl2rgb(in vec3 c) {
    vec3 rgb = clamp( abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),6.0)-3.0)-1.0, 0.0, 1.0 );

    return c.z + c.y * (rgb-0.5)*(1.0-abs(2.0*c.z-1.0));
}

vec3 rgb2hsl(in vec3 c) {
  float h = 0.0;
	float s = 0.0;
	float l = 0.0;
	float r = c.r;
	float g = c.g;
	float b = c.b;
	float cMin = min( r, min( g, b ) );
	float cMax = max( r, max( g, b ) );

	l = ( cMax + cMin ) / 2.0;
	if ( cMax > cMin ) {
		float cDelta = cMax - cMin;

        //s = l < .05 ? cDelta / ( cMax + cMin ) : cDelta / ( 2.0 - ( cMax + cMin ) ); Original
		s = l < .0 ? cDelta / ( cMax + cMin ) : cDelta / ( 2.0 - ( cMax + cMin ) );

		if ( r == cMax ) {
			h = ( g - b ) / cDelta;
		} else if ( g == cMax ) {
			h = 2.0 + ( b - r ) / cDelta;
		} else {
			h = 4.0 + ( r - g ) / cDelta;
		}

		if ( h < 0.0) {
			h += 6.0;
		}
		h = h / 6.0;
	}
	return vec3( h, s, l );
}

void main(void) {
    vec4 texel = texture2D(image, frag_Uv);

    vec3 hsl = rgb2hsl(texel.rgb);

    hsl.r = hsl.r + colorCorrector.r;

    if (hsl.r > 1.0) {
        hsl.r = hsl.r - 1.0;
    } else if (hsl.r < 0.0) {
        hsl.r = hsl.r + 1.0;
    }

    hsl.g = hsl.g * colorCorrector.g;
    hsl.b = clamp(hsl.b + colorCorrector.b, 0.0, 1.0);

    vec3 correctedRGB = hsl2rgb(hsl);

    vec3 maxLevels = 1.0 / (redMatrix + greenMatrix + blueMatrix);

    vec3 matrixMulti = correctedRGB.r * redMatrix +
        correctedRGB.g * greenMatrix +
        correctedRGB.b * blueMatrix;

    vec3 result = (matrixMulti * maxLevels * exposureMatrix) + brightMatrix;

    gl_FragColor = vec4(result.rgb, texel.a);
}
