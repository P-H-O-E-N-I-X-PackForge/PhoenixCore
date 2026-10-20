#version 150

// Planetary nebula around a white dwarf: the last outer layers of the dead star, still drifting away.
// A tilted glowing ring (the shell seen edge-on-ish), cometary knots streaming back towards the star, a faint outer
// halo, and two dim bipolar lobes out of the ring's poles. Teal near the star (ionised oxygen), red at the rim.
// Purely additive, no loops beyond the noise, no per-pixel integration. In cube mode the ring becomes a square one.

uniform float Time;
uniform float Extent;   // billboard half-size, in star radii
uniform float Voxel;    // 0 = smooth; otherwise blocks per star radius (cube mode)
uniform float Core;     // radius of the star body in billboard units (1.0, or 0.82 for the cube)
uniform float Detail;   // 2 (low) - 4 (high)
uniform vec3 Tint;      // star colour

in vec2 vLocal;
out vec4 fragColor;

const float PI = 3.14159265;
const float TILT = 0.62;   // cos of the ring's inclination
const float ROT = 0.5;     // radians the ring is turned on screen

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
        p = vec2(p.x * 2.0, p.y * 2.0 + 5.1);
        period *= 2.0;
        a *= 0.5;
    }
    return v / total;
}

void main() {
    vec2 qRaw = vLocal * Extent;
    bool vox = Voxel > 0.5;
    vec2 cellId = vec2(0.0);
    vec2 cellFrac = vec2(0.5);
    vec2 q = qRaw;
    if (vox) {
        vec2 g = qRaw * Voxel;
        cellId = floor(g);
        cellFrac = g - cellId;
        q = (cellId + 0.5) / Voxel;
    }

    float rs = (vox ? max(abs(q.x), abs(q.y)) : length(q)) / Core;   // distance from the star, in star radii
    if (rs < 0.98) {
        fragColor = vec4(0.0);
        return;
    }
    float layers = Detail;

    // the ring plane: turn, then stretch the minor axis so a circle becomes the tilted ellipse
    // cube mode keeps the ring square to the block grid
    float cr = vox ? 1.0 : cos(ROT), sr = vox ? 0.0 : sin(ROT);
    vec2 p = vec2(cr * q.x + sr * q.y, -sr * q.x + cr * q.y);
    vec2 e = vec2(p.x, p.y / TILT);
    float re = vox ? max(abs(e.x), abs(e.y)) : length(e);
    float ang = atan(e.y, e.x);

    float R0 = Extent * 0.40 * (1.0 + 0.015 * sin(Time * 0.15));   // the shell slowly breathes
    float rn = re / R0;                                           // 1 at the ring

    // ---- the ring: clumpy, with a cool inner edge and a warm outer edge -------------------------------------------
    float clump = pfbm(ang, rn * 3.0, 6.0, layers);
    float ring = exp(-pow((rn - 1.0) / (0.10 + 0.06 * clump), 2.0)) * (0.5 + 1.3 * clump);
    vec3 ringCol = mix(vec3(0.25, 1.0, 0.85), vec3(1.0, 0.42, 0.22), smoothstep(0.98, 1.22, rn));

    // ---- glow filling the inside of the shell, and cometary knots streaming back to the star ----------------------
    float fill = smoothstep(1.0, 0.2, rn) * (0.16 + 0.22 * pfbm(ang + 2.1, rn * 2.0, 5.0, layers));
    float knots = pow(pnoise(vec2(ang * 40.0 / (2.0 * PI), rn * 1.7), 40.0), 3.0) * smoothstep(0.3, 0.85, rn) * (1.0 - smoothstep(0.98, 1.35, rn));

    // ---- faint outer shell ----------------------------------------------------------------------------------------
    float halo = exp(-pow((rn - 1.6) / 0.34, 2.0)) * (0.15 + 0.8 * pfbm(ang + 1.3, rn * 2.0, 5.0, layers));

    // ---- two dim lobes out of the ring's poles ----------------------------------------------------------------------
    vec2 lp = vec2(p.x / (0.55 * R0), (abs(p.y) - 1.2 * R0) / (0.75 * R0));
    float lobe = exp(-dot(lp, lp)) * (0.3 + 0.7 * pfbm(ang, rn * 1.5, 5.0, layers));

    vec3 col = ringCol * ring * 1.35;
    col += vec3(0.20, 0.85, 0.75) * fill;
    col += vec3(0.95, 0.40, 0.30) * knots * 0.55;
    col += vec3(1.0, 0.32, 0.30) * halo * 0.45;
    col += vec3(0.25, 0.9, 0.7) * lobe * 0.30;

    // the white dwarf's own hot glow
    col += mix(Tint, vec3(1.0), 0.5) * 0.7 * exp(-max(rs - 1.0, 0.0) * 1.8);

    col = 1.0 - exp(-col * 1.2);

    if (vox) {
        float seam = min(min(cellFrac.x, 1.0 - cellFrac.x), min(cellFrac.y, 1.0 - cellFrac.y));
        col *= (1.0 - 0.26 * (1.0 - smoothstep(0.0, 0.07, seam))) * (0.92 + 0.16 * hash(cellId * 0.37 + 5.0));
        col = floor(col * 10.0 + 0.5) / 10.0;
    }

    float edge = 1.0 - smoothstep(0.7, 1.0, max(abs(vLocal.x), abs(vLocal.y)));
    fragColor = vec4(col * edge, 0.0);   // alpha 0: purely additive
}
