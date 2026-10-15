#version 150

// Sealed-B discipline sky backdrop - "reality glitch" theme: the same domain-warped-fBm nebula
// technique as void_galaxy.fsh, but sampled with a per-channel offset for a chromatic-aberration
// fringe and torn by occasional horizontal glitch bands, evoking a broken/corrupted reality
// rather than a clean galaxy. Camera-ray reconstruction shared with the other discipline sky
// shaders; reuses void_galaxy.vsh as its vertex stage.

uniform vec2  OutSize;
uniform mat4  InvViewMat;
uniform mat4  InvProjMat;
uniform vec3  PrimaryColor;
uniform vec3  SecondaryColor;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

float hash1(float n) {
    return fract(sin(n) * 43758.5453123);
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
        p = p * 2.01 + vec3(7.3, 5.9, 11.1);
        a *= 0.5;
    }
    return v;
}

float density(vec3 dir) {
    vec3 q = vec3(fbm(dir), fbm(dir + vec3(5.2, 1.3, 2.7)), fbm(dir + vec3(1.7, 9.2, 3.6)));
    return fbm(dir + 1.5 * q);
}

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;

    // Glitch bands: occasional horizontal strips of the screen tear sideways, like a corrupted
    // broadcast signal - a coarse per-row hash decides whether this row glitches this instant,
    // and by how much, re-rolling every ~1/6th of a second (glitchTime) rather than continuously.
    float row = floor(texCoord.y * 40.0);
    float glitchTime = floor(Time * 6.0);
    float glitchRoll = hash1(row * 13.7 + glitchTime * 91.7);
    float glitchAmount = step(0.93, glitchRoll) * (hash1(row + glitchTime) - 0.5) * 0.06;
    ndc.x += glitchAmount;

    vec4 viewDir = InvProjMat * vec4(ndc, 1.0, 1.0);
    viewDir /= viewDir.w;
    vec3 rayDir = normalize((InvViewMat * vec4(viewDir.xyz, 0.0)).xyz);

    float drift = Time * 0.02;
    vec3 p = rayDir * 1.4 + vec3(drift, -drift * 0.7, drift * 0.3);

    // Chromatic-aberration-style channel split: each colour channel samples the density field at
    // a slightly different offset, so edges fringe red/cyan like a broken CRT instead of
    // resolving to one clean colour.
    float dR = density(p + vec3(0.015, 0.0, 0.0));
    float dG = density(p);
    float dB = density(p - vec3(0.015, 0.0, 0.0));

    vec3 dens = vec3(dR, dG, dB);
    vec3 clamped = smoothstep(vec3(0.35), vec3(0.8), dens);

    vec3 col = mix(SecondaryColor, PrimaryColor, clamped.g) * clamped;
    float alpha = clamp(max(max(clamped.r, clamped.g), clamped.b), 0.0, 1.0);

    fragColor = vec4(col, alpha);
}
