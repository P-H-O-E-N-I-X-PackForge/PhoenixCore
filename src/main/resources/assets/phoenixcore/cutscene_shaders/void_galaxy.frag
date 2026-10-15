// Shadertoy-style cutscene port of shaders/core/cinema_void_galaxy.fsh.
// iTime, iResolution and iMouse are provided; the uniforms below come from the background's "uniforms" JSON
// (see cutscene_backgrounds/void_galaxy.json). Instead of a real camera, a slowly turning one is built here.

uniform vec3  PrimaryColor;
uniform vec3  SecondaryColor;
uniform vec3  GalaxyDir;
uniform float Density;
uniform float Scale;
uniform float Seed;

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(mix(hash(i),            hash(i + vec3(1,0,0)), f.x),
            mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
        mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
            mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y),
        f.z);
}

float fbm(vec3 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p  = p * 2.01 + vec3(7.3, 5.9, 11.1);
        a *= 0.5;
    }
    return v;
}

float nebulaDensity(vec3 dir) {
    vec3 p = dir * Scale + Seed;
    vec3 q = vec3(fbm(p),
                  fbm(p + vec3(5.2, 1.3, 2.7)),
                  fbm(p + vec3(1.7, 9.2, 3.6)));
    return fbm(p + 1.5 * q);
}

float starField(vec3 dir) {
    float n = fbm(dir * 90.0 + Seed * 3.7);
    return pow(max(n - 0.965, 0.0) * 28.57, 6.0);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 ndc = (fragCoord * 2.0 - iResolution.xy) / iResolution.y;

    float yaw = iTime * 0.015;
    vec3 forward = normalize(vec3(sin(yaw), 0.35, cos(yaw)));
    vec3 right = normalize(cross(forward, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, forward);
    vec3 rayDir = normalize(forward * 1.4 + ndc.x * right + ndc.y * up);

    float drift = iTime * 0.006;

    float galDot = clamp(dot(normalize(GalaxyDir), rayDir), 0.0, 1.0);
    vec3 glow = PrimaryColor * 1.6 * pow(galDot, 60.0);
    glow += SecondaryColor * 0.5 * pow(galDot, 6.0);

    float density = nebulaDensity(rayDir + drift);
    float clamped = smoothstep(0.35, 0.75, density);
    float bright  = pow(clamped, 2.0);
    vec3 cloud = mix(SecondaryColor * 0.5, PrimaryColor * 1.2, clamped) * bright * Density;

    vec3 starColor = vec3(1.0, 0.95, 0.85) * starField(rayDir);

    fragColor = vec4(glow + cloud + starColor, 1.0);
}
