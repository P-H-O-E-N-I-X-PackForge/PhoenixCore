#version 150

// A planetary ring: banded dust with a gap, fading at both edges, in shadow where the planet blocks the sun.

uniform float Time;
uniform vec3 SunDir;
uniform vec3 PlanetCenter;
uniform float PlanetRadius;
uniform float Inner;
uniform float Opacity;
uniform float Seed;
uniform vec3 Color1;
uniform vec3 Color2;

in vec2 vPos;
in vec3 vView;

out vec4 fragColor;

float hash1(float x) {
    return fract(sin(x * 127.1 + 311.7) * 43758.5453);
}

float noise1(float x) {
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash1(i), hash1(i + 1.0), f);
}

void main() {
    float r = length(vPos);
    if (r > 1.0 || r < Inner) discard;
    float t = (r - Inner) / (1.0 - Inner);

    float n = noise1(t * 60.0 + Seed * 13.0) * 0.55 + noise1(t * 17.0 + Seed) * 0.3 + noise1(t * 200.0) * 0.15;
    float gap = smoothstep(0.50, 0.54, t) * (1.0 - smoothstep(0.58, 0.62, t));
    float density = clamp(n * 1.25 - gap * 0.95, 0.0, 1.0);
    density *= smoothstep(0.0, 0.05, t) * (1.0 - smoothstep(0.92, 1.0, t));
    if (density < 0.01) discard;

    vec3 col = mix(Color1, Color2, n);

    // the planet's shadow falls across the ring on the side away from the sun
    vec3 L = normalize(SunDir);
    vec3 oc = vView - PlanetCenter;
    float b = dot(oc, L);
    float c = dot(oc, oc) - PlanetRadius * PlanetRadius;
    float disc = b * b - c;
    float shadow = (disc > 0.0 && -b + sqrt(disc) > 0.0) ? 0.1 : 1.0;

    fragColor = vec4(col * (0.2 + 0.8 * shadow), density * Opacity);
}
