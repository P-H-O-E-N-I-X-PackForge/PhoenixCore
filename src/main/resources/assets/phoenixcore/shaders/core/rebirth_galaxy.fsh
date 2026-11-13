#version 150

// Mode 1 is not a galaxy at all: it draws the map's whole space backdrop (the nebula haze and the star field of
// continuum_backdrop), so the backdrop can be drawn by this shader when continuum_backdrop is missing or stale.
//
// Mode 0: a distant spiral galaxy for the skybox: a warm bulge, two log-spiral arms with dust lanes along their inner edges,
// and bright star-forming knots in the arms. Tilt and turn give each one its own orientation; Seed varies the arms.
// Additive and cheap; one small billboard each. In cube mode it is built from blocks.

uniform float Seed;
uniform float Cells;        // 0 = smooth; otherwise blocks across the half-width (cube mode)
uniform float Detail;       // 2 (low) - 4 (high)
uniform float Tilt;         // cos of the inclination: 1 = face-on, ~0.2 = nearly edge-on
uniform float Rot;          // radians the galaxy is turned on screen
uniform float Intensity;
uniform vec3 CoreColor;
uniform vec3 ArmColor;

// Mode 1 only
uniform float Mode;         // 0 = a galaxy, 1 = the map backdrop
uniform float Aspect;
uniform float Yaw;
uniform float Time;
uniform float Octaves;
uniform vec3 PrimaryColor;
uniform vec3 SecondaryColor;

// the nebula comes from the baked picture (see continuum_nebula_bake) when it is there, else it is worked out here
uniform sampler2D NebulaTex;
uniform float UseBake;

vec3 bakedNebula(vec3 dir) {
    float u = atan(dir.z, dir.x) / 6.2831853 + 0.5 + Time * 0.00002;
    float v = asin(clamp(dir.y, -1.0, 1.0)) / 3.14159265 + 0.5;
    return textureLod(NebulaTex, vec2(u, v), 0.0).rgb * 0.5;
}

in vec2 vLocal;
out vec4 fragColor;

const float PI = 3.14159265;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float pnoise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float x0 = mod(i.x, period);
    float x1 = mod(i.x + 1.0, period);
    return mix(mix(hash(vec2(x0, i.y)), hash(vec2(x1, i.y)), f.x),
               mix(hash(vec2(x0, i.y + 1.0)), hash(vec2(x1, i.y + 1.0)), f.x), f.y);
}

float pfbm(float ang, float r, float period, float layers) {
    vec2 p = vec2(ang * period / (2.0 * PI), r);
    float v = 0.0, a = 0.5, total = 0.0;
    for (int i = 0; i < 4; i++) {
        if (float(i) >= layers) break;
        v += a * pnoise(p, period);
        total += a;
        p = vec2(p.x * 2.0, p.y * 2.0 + 4.9);
        period *= 2.0;
        a *= 0.5;
    }
    return v / total;
}

// ---- the map backdrop (same picture as continuum_backdrop) ---------------------------------------------------------

float bhash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

float bnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(bhash(i), bhash(i + vec3(1, 0, 0)), f.x),
            mix(bhash(i + vec3(0, 1, 0)), bhash(i + vec3(1, 1, 0)), f.x), f.y),
        mix(mix(bhash(i + vec3(0, 0, 1)), bhash(i + vec3(1, 0, 1)), f.x),
            mix(bhash(i + vec3(0, 1, 1)), bhash(i + vec3(1, 1, 1)), f.x), f.y),
        f.z);
}

float bfbm(vec3 p, float limit) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) {
        if (float(i) >= min(Octaves, limit)) break;
        v += a * bnoise(p);
        p = p * 2.03 + vec3(7.3, 5.9, 11.1);
        a *= 0.5;
    }
    return v;
}

float bfbm(vec3 p) { return bfbm(p, 6.0); }

vec3 backdrop(vec2 uv) {
    vec2 p = (uv * 2.0 - 1.0) * vec2(Aspect, 1.0);
    vec3 dir = normalize(vec3(p, 1.15));
    float c = cos(Yaw), s = sin(Yaw);
    dir = vec3(c * dir.x + s * dir.z, dir.y, -s * dir.x + c * dir.z);

    vec3 nebulaLight;
    if (UseBake > 0.5) {
        nebulaLight = bakedNebula(dir);
    } else {
        float drift = Time * 0.0004;
        vec3 q = dir * 1.6 + drift;
        float warp = bfbm(q + 1.4 * vec3(bfbm(q + 3.1, 3.0), bfbm(q + 7.7, 3.0), bfbm(q + 1.3, 3.0)));
        float haze = 0.30 + 0.70 * smoothstep(0.15, 0.85, warp);
        float filament = pow(smoothstep(0.42, 0.78, warp), 2.0);
        vec3 nebula = mix(SecondaryColor, PrimaryColor, smoothstep(0.25, 0.8, warp));
        nebulaLight = nebula * (0.11 * haze + 0.42 * filament);
    }

    float vignette = 1.0 - 0.45 * pow(abs(uv.y - 0.5) * 2.0, 2.0) - 0.20 * pow(abs(uv.x - 0.5) * 2.0, 3.0);
    vec3 base = vec3(0.012, 0.014, 0.034);
    vec3 col = base + nebulaLight;
    col *= vignette;

    vec3 sp = dir * 90.0;
    vec3 cell = floor(sp);
    float h = bhash(cell);
    if (h > 0.965) {
        vec3 centre = cell + 0.5 + (vec3(bhash(cell + 1.7), bhash(cell + 4.1), bhash(cell + 9.3)) - 0.5) * 0.6;
        float d = length(sp - centre);
        float twinkle = 0.75 + 0.25 * sin(Time * (1.0 + h * 3.0) + h * 40.0);
        // 1 - smoothstep(0, 0.42, d): the same fall-off, written with the edges the right way round
        col += vec3(1.0, 0.96, 0.9) * (1.0 - smoothstep(0.0, 0.42, d)) * (0.35 + 0.65 * (h - 0.965) / 0.035) * twinkle;
    }
    return col;
}

void main() {
    if (Mode > 0.5) {
        fragColor = vec4(backdrop(vLocal * 0.5 + 0.5), 1.0);
        return;
    }

    vec2 q = vLocal;
    vec2 cellId = vec2(0.0);
    vec2 cellFrac = vec2(0.5);
    bool vox = Cells > 0.5;
    if (vox) {
        vec2 g = q * Cells;
        cellId = floor(g);
        cellFrac = g - cellId;
        q = (cellId + 0.5) / Cells;
    }

    float cr = cos(Rot), sr = sin(Rot);
    vec2 p = vec2(cr * q.x + sr * q.y, -sr * q.x + cr * q.y);
    vec2 e = vec2(p.x, p.y / Tilt);
    float r = length(e);
    if (r > 1.0) {
        fragColor = vec4(0.0);
        return;
    }
    float ang = atan(e.y, e.x);

    float spiral = ang - 2.6 * log(r + 0.04) + Seed * 6.2831;
    float arms = pow(0.5 + 0.5 * cos(2.0 * spiral), 1.7);
    float lane = pow(0.5 + 0.5 * cos(2.0 * spiral + 1.0), 3.0) * smoothstep(0.08, 0.4, r);
    float clump = pfbm(ang, r * 5.0 + Seed * 7.0, 7.0, Detail);

    float disk = exp(-r * 3.4) * (0.25 + 0.9 * arms) * (0.55 + 0.8 * clump) * (1.0 - 0.55 * lane);
    float bulge = exp(-r * r * 55.0) * 1.2 + exp(-r * r * 10.0) * 0.18;
    // star-forming knots: soft round dots at jittered positions, only along the arms
    vec2 kg = e * 38.0 + Seed * 50.0;
    vec2 kid = floor(kg);
    vec2 kf = fract(kg) - 0.5 - (vec2(hash(kid + 7.1), hash(kid + 3.3)) - 0.5) * 0.6;
    float knots = step(0.93, hash(kid)) * exp(-dot(kf, kf) * 55.0) * arms * smoothstep(0.1, 0.35, r)
            * (1.0 - smoothstep(0.6, 0.9, r));

    vec3 col = ArmColor * disk * 1.5 + CoreColor * bulge + vec3(0.9, 0.95, 1.0) * knots * 0.9;
    col *= Intensity;
    col = 1.0 - exp(-col * 1.3);

    if (vox) {
        float seam = min(min(cellFrac.x, 1.0 - cellFrac.x), min(cellFrac.y, 1.0 - cellFrac.y));
        col *= (1.0 - 0.22 * (1.0 - smoothstep(0.0, 0.08, seam))) * (0.93 + 0.14 * hash(cellId * 0.37 + 5.0));
        col = floor(col * 10.0 + 0.5) / 10.0;
    }

    float fade = (1.0 - smoothstep(0.78, 1.0, r)) * (1.0 - smoothstep(0.85, 1.0, max(abs(vLocal.x), abs(vLocal.y))));
    fragColor = vec4(col * fade, 0.0);
}
