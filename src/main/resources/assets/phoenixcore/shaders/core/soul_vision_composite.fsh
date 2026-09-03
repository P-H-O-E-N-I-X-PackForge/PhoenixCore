#version 150

// Composites the soul vision mask over the already-rendered scene. The mask isn't a separate
// texture - it rides in on the scene's own alpha channel, stamped there per chunk during
// world rendering (see SoulVisionManager) using a color-masked draw that only ever touches
// alpha, never the real RGB. 0 alpha (nothing drawn there - sky, or chunks the lens hasn't
// scanned) means full grayscale; the lens' scanned chunks write a brightness there
// proportional to that chunk's soul density, so soul-rich chunks fade back toward their real
// color while depleted ones stay washed out gray.

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);

void main() {
    vec4 col = texture(InSampler, texCoord);
    float mask = col.a;

    float luma = dot(col.rgb, LUMA);
    vec3 gray = vec3(luma);

    vec3 result = mix(gray, col.rgb, mask);

    fragColor = vec4(result, 1.0);
}
