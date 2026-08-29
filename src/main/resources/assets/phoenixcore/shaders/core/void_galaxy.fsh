#version 150

// Adapted from the same domain-warped-fBm nebula technique as phoenix_nebula.fsh, with a
// galaxy-core glow and a star field layered on top - inspired by Frank Hugenroth's "Galaxy"
// shader (ShaderToy, 2015), reworked for a fixed sky backdrop viewed via camera-ray
// reconstruction instead of a moving flythrough camera.

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

// ── Noise primitives (same as phoenix_nebula.fsh) ─────────────────────────────

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

// 3 octaves instead of 6 - this runs unconditionally for EVERY pixel on screen every frame
// (no depth test, no early-out), so its cost multiplies directly into whatever nebulaDensity
// and starField spend per call below. Halving it here is the single biggest lever on the whole
// shader's cost.
float fbm(vec3 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p  = p * 2.01 + vec3(7.3, 5.9, 11.1);
        a *= 0.5;
    }
    return v;
}

// Domain-warped fBm - the filament/void structure characteristic of real nebulae. Originally a
// double warp (7 fbm calls per pixel: 3 for the first warp field, 3 for a second warp applied
// on top of that, 1 final sample) - that compounds with the per-fbm octave cost into hundreds of
// noise evaluations per pixel, which is what was actually tanking framerate. A single warp pass
// (4 fbm calls) keeps the same filament/void look, just with slightly less fine substructure.
float nebulaDensity(vec3 dir) {
    vec3 p = dir * Scale + Seed;

    vec3 q = vec3(fbm(p),
                  fbm(p + vec3(5.2, 1.3, 2.7)),
                  fbm(p + vec3(1.7, 9.2, 3.6)));

    return fbm(p + 1.5 * q);
}

// Sparse, sharp bright points - only the very top of the noise range becomes a visible star,
// so the field stays mostly black between them instead of looking like uniform static.
float starField(vec3 dir) {
    float n = fbm(dir * 850.0 + Seed * 3.7);
    return pow(max(n - 0.965, 0.0) * 28.57, 6.0);
}

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 viewDir = InvProjMat * vec4(ndc, 1.0, 1.0);
    viewDir /= viewDir.w;
    vec3 rayDir = normalize((InvViewMat * vec4(viewDir.xyz, 0.0)).xyz);

    float drift = Time * 0.0003;

    // Galaxy core: a tight bright point with a wider, dimmer halo around it, fixed in a sky
    // direction rather than following the camera - this is what a distant galactic core looks
    // like, as opposed to the diffuse filament clouds below.
    float galDot = clamp(dot(normalize(GalaxyDir), rayDir), 0.0, 1.0);
    vec3 glow = PrimaryColor * 1.6 * pow(galDot, 60.0);
    glow += SecondaryColor * 0.5 * pow(galDot, 6.0);

    // Nebula filaments.
    float density = nebulaDensity(rayDir + drift);
    float clamped = smoothstep(0.35, 0.75, density);
    float bright  = pow(clamped, 2.0);
    vec3 cloud = mix(SecondaryColor * 0.5, PrimaryColor * 1.2, clamped) * bright * Density;

    // Stars.
    float stars = starField(rayDir);
    vec3 starColor = vec3(1.0, 0.95, 0.85) * stars;

    vec3 col = glow + cloud + starColor;
    float alpha = clamp(bright * Density + stars + pow(galDot, 40.0), 0.0, 1.0);

    fragColor = vec4(col, alpha);
}
