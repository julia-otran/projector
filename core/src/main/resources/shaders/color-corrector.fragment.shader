varying vec2 frag_Uv;
uniform sampler2D image;

uniform vec3 redMatrix;
uniform vec3 greenMatrix;
uniform vec3 blueMatrix;

uniform vec3 exposureMatrix;
uniform vec3 brightMatrix;

uniform float srcLumMap[16];
uniform float srcLumQMap[16];
uniform float dstHueMap[16];
uniform float dstSatMap[16];
uniform float dstLumMap[16];

vec3 hsl2rgb(in vec3 c) {
    vec3 rgb = clamp( abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),6.0)-3.0)-1.0, 0.0, 1.0);

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

	l = (cMax + cMin) / 2.0;

	if (cMax > cMin) {
		float cDelta = cMax - cMin;

        // s = l < .05 ? cDelta / ( cMax + cMin ) : cDelta / ( 2.0 - ( cMax + cMin ) ); Original
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

// -1.0 * q * (|targetHue - 0.18 - x| - 0.5)^2 + 1.0
float lumaCurveMultiplier(in float luma, in float targetLuma, in float q) {
    float q_inv = -1.0 * q;
    float x = luma - targetLuma;
    return clamp((q_inv * x * x) + 1.0, 0.0, 1.0);
}

void main(void) {
    vec4 texel = texture2D(image, frag_Uv);

    vec3 hsl = rgb2hsl(texel.rgb);
    vec3 hslResult = vec3(hsl);

    float multiply;

    for (int i = 0; i < 16; i++) {
        multiply = lumaCurveMultiplier(hsl.b, srcLumMap[i], srcLumQMap[i]);

        hslResult.r = hslResult.r + (dstHueMap[i] * multiply);

        if (hslResult.r > 1.0) {
            hslResult.r = hslResult.r - 1.0;
        }

        if (hslResult.r < 0.0) {
            hslResult.r = 1.0 - hslResult.r;
        }

        hslResult.g = hslResult.g + (dstSatMap[i] * multiply);
        hslResult.b = hslResult.b + (dstLumMap[i] * multiply);
    }

    vec3 correctedRGB = hsl2rgb(clamp(hslResult, 0.0, 1.0));

    vec3 maxLevels = 1.0 / (redMatrix + greenMatrix + blueMatrix);

    vec3 matrixMulti = correctedRGB.r * redMatrix +
        correctedRGB.g * greenMatrix +
        correctedRGB.b * blueMatrix;

    vec3 result = (matrixMulti * maxLevels * exposureMatrix) + brightMatrix;

    gl_FragColor = vec4(result.rgb, texel.a);
}
