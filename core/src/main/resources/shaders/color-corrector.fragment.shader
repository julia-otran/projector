varying vec2 frag_Uv;
uniform sampler2D image;

uniform vec4 lowAdjust;
uniform vec4 midAdjust;
uniform vec4 highAdjust;

uniform vec4 redMatrix;
uniform vec4 greenMatrix;
uniform vec4 blueMatrix;

vec3 rgbToHsl(vec3 rgbColor) {
    float minVal = min(rgbColor.r, min(rgbColor.g, rgbColor.b));
    float maxVal = max(rgbColor.r, max(rgbColor.g, rgbColor.b));

    float lum = (minVal + maxVal) / 2.0;
    float sat = 0.0;
    float hue = 0.0;

    float delta = maxVal - minVal; 

    if (lum > 0.5) {
        sat = delta / (2.0 - delta);
    } else {
        sat = delta / (maxVal + minVal);
    }

    if (delta == 0.0) {
        delta = 1.0;
    }

    if (rgbColor.r == maxVal) {
        hue = (rgbColor.g - rgbColor.b) / delta; 
    }

    if (rgbColor.g == maxVal) {
        hue = 2.0 + ((rgbColor.b - rgbColor.r) / delta);
    }

    if (rgbColor.b == maxVal) {
        hue = 4.0 + ((rgbColor.r - rgbColor.g) / delta);
    }

    hue = hue / 6.0;

    if (hue < 0.0) {
        hue = 1.0 + hue;
    }

    return(vec3(hue, sat, lum));
}

float convertColorPart(float n1, float n2, float hue) {
    if (hue > 6.0) {
        hue = hue - 6.0;
    }
    if (hue < 0.0) {
        hue = hue + 6.0;
    }
    if (hue < 1.0) {
        return(n1 + (n2 - n1) * hue);
    }
    if (hue < 3.0) {
        return(n2);
    }
    if (hue < 4.0) {
        return(n1 + (n2 - n1) * (4.0 - hue));
    }

    return(n1);
}

vec3 hslToRgb(vec3 hsl) {
    float r = 0.0;
    float g = 0.0;
    float b = 0.0;

    float x = 0.0;
    float y = 0.0;

    if (hsl.g > 0.0) {
        if (hsl.b <= 0.5) {
            y = hsl.b * (1.0 + hsl.g);
        } else {
            y = hsl.g + hsl.b - (hsl.g * hsl.b);
        }

        x = (2.0 * hsl.b) - y;

        r = convertColorPart(x, y, hsl.r * 6.0 + 2.0);
        g = convertColorPart(x, y, hsl.r * 6.0);
        b = convertColorPart(x, y, hsl.r * 6.0 - 2.0);
    } else {
        r = hsl.b;
        g = hsl.b;
        b = hsl.b;
    }

    return(vec3(r, g, b));
}

void main(void) {
    vec4 texel = texture2D(image, frag_Uv);                
    vec3 hsl = rgbToHsl(texel.rgb);

    float lum = hsl.b;

    float a = 0.25;
    float scale = 0.7;
    float b = 1.0 - scale;

    float shadowsLum = lum - b;
    float highlightsLum = lum + b -1.0;

    float shadowsMultiply = clamp((shadowsLum / (-1.0 * a)) + 0.5, 0.0, 1.0) * scale;
    float highlightsMultiply = clamp((highlightsLum / a) + 0.5, 0.0, 1.0) * scale;

    float midtonesMultiply0 = clamp((shadowsLum / a) + 0.5, 0.0, 1.0);
    float midtonesMultiply1 = clamp((highlightsLum / (-1.0 * a)) + 0.5, 0.0, 1.0);
    float midtonesMultiply = midtonesMultiply0 * midtonesMultiply1 * scale;

    vec4 shadows = shadowsMultiply * lowAdjust;
    vec4 mids = midtonesMultiply * midAdjust;
    vec4 highs = highlightsMultiply * highAdjust;

    vec3 colorCorrected = texel.rgb + shadows.rgb + mids.rgb + highs.rgb;
    colorCorrected = clamp(colorCorrected, 0.0, 1.0);

    vec3 colorCorrectedHsl;
    colorCorrectedHsl = rgbToHsl(colorCorrected);

    colorCorrectedHsl.b = clamp(hsl.b + shadows.a + mids.a + highs.a, 0.0, 1.0);

    colorCorrected = hslToRgb(colorCorrectedHsl);

    vec3 result = colorCorrected.r * redMatrix.rgb +
        colorCorrected.g * greenMatrix.rgb +
        colorCorrected.b * blueMatrix.rgb +
        vec3(redMatrix.a, greenMatrix.a, blueMatrix.a);

    gl_FragColor = vec4(result.rgb, texel.a);
}
