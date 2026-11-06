#version 150

// Sculk discipline sky backdrop - a near-black abyss reached across by long, curling
// bioluminescent cyan/teal tendrils (evoking the Deep Dark's sculk veins snaking across a
// surface) rather than a nebula, galaxy, or uniform cracked web. Camera-ray reconstruction
// technique shared with void_galaxy.fsh/phoenix_sunflare.fsh; reuses void_galaxy.vsh as its
// vertex stage since every one of these fullscreen sky shaders uses the exact same trivial
// NDC-quad passthrough.

uniform vec2  OutSize;
uniform mat4  InvViewMat;
uniform mat4  InvProjMat;
uniform vec3  VeinColor;
uniform vec3  GlowColor;
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
        p = p * 2.01 + vec3(7.3, 5.9, 11.1);
        a *= 0.5;
    }
    return v;
}

// Folds noise around its midpoint so values near 0.5 become sharp ridges near 1.0 - the standard
// building block for vein/lightning/root shapes (as opposed to the soft rolling hills a plain
// noise threshold gives).
float ridged(vec3 p) {
    return 1.0 - abs(noise(p) * 2.0 - 1.0);
}

// Long curling tendrils rather than straight cracks: space is warped by a coarse fbm field
// first, so the ridges bend and branch organically, then sharpened with a steep power curve so
// only the very peak of each ridge stays lit - a thin glowing line snaking through the warp
// instead of solid ridged terrain. Returns both a knife-thin bright core and a wider, dimmer
// halo derived from the same ridge so the glow bleeds outward without recomputing the warp twice.
void tendrilField(vec3 p, out float core, out float halo) {
    vec3 warp = vec3(fbm(p * 0.5), fbm(p * 0.5 + vec3(3.1, 1.7, 9.4)), fbm(p * 0.5 + vec3(7.2, 4.4, 2.1)));
    vec3 warped = p + (warp - 0.5) * 3.0;

    float r1 = ridged(warped * 0.9);
    float r2 = ridged(warped * 1.8 + 4.2);
    float ridge = mix(r1, r2, 0.4);

    core = pow(ridge, 10.0);
    halo = pow(ridge, 3.0) * 0.4;
}

// Sparse, sharp bright points - only the very top of the noise range becomes a visible star.
float starField(vec3 dir) {
    float n = noise(dir * 400.0);
    return pow(max(n - 0.975, 0.0) * 40.0, 6.0);
}

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 viewDir = InvProjMat * vec4(ndc, 1.0, 1.0);
    viewDir /= viewDir.w;
    vec3 rayDir = normalize((InvViewMat * vec4(viewDir.xyz, 0.0)).xyz);

    float drift = Time * 0.01;
    vec3 p = rayDir * 2.2 + vec3(drift, drift * 0.6, 0.0);

    float core, halo;
    tendrilField(p, core, halo);

    // A slow sine pulse, staggered per-tendril via a hash of the coarse cell it falls in, mimics
    // sculk sensors lighting up unevenly rather than every tendril pulsing in lockstep.
    float pulse = 0.6 + 0.4 * sin(Time * 0.8 + hash(floor(p * 4.0)) * 6.283);
    float vein = core * pulse;

    vec3 col = VeinColor * vein + GlowColor * halo;

    float stars = starField(rayDir);
    col += vec3(0.7, 1.0, 0.9) * stars;

    float alpha = clamp(vein + halo * 0.6 + stars, 0.0, 1.0);
    fragColor = vec4(col, alpha);
}
