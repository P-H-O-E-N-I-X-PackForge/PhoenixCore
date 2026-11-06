#version 150

// Cinema-only fork of void_galaxy.fsh - void_galaxy.fsh itself stays untouched since it's shared
// with the real sky and works fine there. The difference here is entirely about how this gets
// rendered: the real sky feeds this technique the actual camera's inverted projection/view
// matrices, so every ray direction across the screen comes from a genuine (if approximated)
// perspective transform. A cinema screen has no real camera to borrow - CinemaRenderTarget builds
// an artificial one instead, and that ray field, while smooth, is more mathematically "regular"
// than a real camera's. The star field below samples noise at 850x frequency, which is fine
// against a real camera's ray field but aliases into sharp sheared/sectioned artifacts against the
// more regular artificial one - reproducible on a single solo screen, unrelated to grouping. That
// frequency is turned down here to something far less sensitive to exactly how the ray field was
// built. Alpha is also hardcoded fully opaque, since this variant is only ever rendered into a
// standalone offline texture (never composited over the real sky), so there's nothing for
// transparency to composite over.

uniform vec2  OutSize;
uniform mat4  InvViewMat;
uniform mat4  InvProjMat;
uniform vec3  PrimaryColor;
uniform vec3  SecondaryColor;
uniform vec3  GalaxyDir;
uniform float Density;
uniform float Scale;
uniform float Seed;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

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

// Frequency dropped from 850.0 (void_galaxy.fsh) to 90.0 - see file header. Still sparse/sharp
// bright points, just built from a noise domain that doesn't alias against an artificial ray
// field the way the original's much higher frequency did.
float starField(vec3 dir) {
    float n = fbm(dir * 90.0 + Seed * 3.7);
    return pow(max(n - 0.965, 0.0) * 28.57, 6.0);
}

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 viewDir = InvProjMat * vec4(ndc, 1.0, 1.0);
    viewDir /= viewDir.w;
    vec3 rayDir = normalize((InvViewMat * vec4(viewDir.xyz, 0.0)).xyz);

    float drift = Time * 0.0003;

    float galDot = clamp(dot(normalize(GalaxyDir), rayDir), 0.0, 1.0);
    vec3 glow = PrimaryColor * 1.6 * pow(galDot, 60.0);
    glow += SecondaryColor * 0.5 * pow(galDot, 6.0);

    float density = nebulaDensity(rayDir + drift);
    float clamped = smoothstep(0.35, 0.75, density);
    float bright  = pow(clamped, 2.0);
    vec3 cloud = mix(SecondaryColor * 0.5, PrimaryColor * 1.2, clamped) * bright * Density;

    float stars = starField(rayDir);
    vec3 starColor = vec3(1.0, 0.95, 0.85) * stars;

    vec3 col = glow + cloud + starColor;

    fragColor = vec4(col, 1.0);
}
