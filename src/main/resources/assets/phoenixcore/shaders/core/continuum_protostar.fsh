#version 150

// Young star in its protoplanetary disk: the flat ring of dust and gas the star was born from, with dark gaps carved
// where planets are forming, clumps and a faint spiral orbiting at their own speeds, and a pair of jets with glowing
// knots along the spin axis.
//
// The disk lies in the system's orbital plane (DiskNormal is that plane's normal in view space), so it lines up with the
// planets' orbits. Gaps are passed in star radii so they can sit exactly on the planets' orbits.
// Additive, loop-free. In cube mode the rings become squarish and block-built.

uniform float Time;
uniform float Extent;       // billboard half-size, in star radii
uniform float Voxel;        // 0 = smooth; otherwise blocks per star radius (cube mode)
uniform float Core;         // radius of the star body in billboard units (1.0, or 0.82 for the cube)
uniform float Detail;       // 2 (low) - 4 (high)
uniform vec3 Tint;          // star colour
uniform vec3 DiskNormal;    // orbital plane normal, view space
uniform vec4 Gaps;          // up to four gap radii in star radii; 0 = unused
uniform float DiskInner;    // inner edge of the disk in star radii (the cleared cavity); 0 = 2.0
uniform float DiskOuter;    // outer edge in star radii; 0 = 0.46 * Extent

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
        p = vec2(p.x * 2.0, p.y * 2.0 + 3.7);
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

    float rs = (vox ? max(abs(q.x), abs(q.y)) : length(q)) / Core;
    if (rs < 0.98) {
        fragColor = vec4(0.0);
        return;
    }
    float layers = Detail;
    float minW = vox ? 0.55 / Voxel : 0.0;

    vec3 n = normalize(DiskNormal);
    float cosI = clamp(abs(n.z), 0.16, 1.0);
    float sinI = sqrt(max(1.0 - n.z * n.z, 0.0));
    vec2 mn = length(n.xy) > 1e-3 ? normalize(n.xy) : vec2(0.0, 1.0);   // the disk's minor axis on screen
    vec2 mj = vec2(-mn.y, mn.x);

    // disk-plane coordinates in star radii
    vec2 e = vec2(dot(q, mj), dot(q, mn) / cosI);
    float re = vox ? max(abs(e.x), abs(e.y)) : length(e);
    float ang = atan(e.y, e.x);

    float inner = DiskInner > 0.5 ? DiskInner : 2.0;
    float outer = DiskOuter > 0.5 ? DiskOuter : Extent * 0.46;
    vec3 hot = mix(Tint, vec3(1.0), 0.5);
    vec3 col = vec3(0.0);

    // ---- the star's glow ---------------------------------------------------------------------------------------
    float d = max(rs - 1.0, 0.0);
    col += hot * (0.75 * exp(-d * 1.6) + 0.22 / (1.0 + d * d * 1.8));

    // ---- the disk -----------------------------------------------------------------------------------------------
    float body = smoothstep(inner - 0.25, inner + 0.7, re) * (1.0 - smoothstep(outer * 0.72, outer, re));
    float falloff = pow(inner / max(re, inner), 1.1);

    float gapMask = 1.0;
    float rim = 0.0;
    for (int i = 0; i < 4; i++) {
        float g = Gaps[i];
        if (g < 0.5) continue;
        float w = 0.26 + 0.05 * g;
        float x = (re - g) / w;
        gapMask *= 1.0 - 0.9 * exp(-x * x);
        rim += exp(-pow((re - g - w * 1.7) / (w * 0.9), 2.0));   // dust piles up on the outer edge of each gap
    }

    // clumps and vortices drift round at Keplerian speed; the spiral is a faint standing pattern
    float a = ang - Time * 0.3 * pow(max(re, 1.0), -1.5);
    float clump = pfbm(a, re * 0.55, 8.0, layers);
    float spiral = 0.78 + 0.22 * cos(2.0 * (ang - 1.1 * log(max(re, 1.0))));
    float dust = (0.5 + 0.9 * clump) * spiral;

    vec3 diskCol = mix(hot, vec3(0.85, 0.45, 0.22), smoothstep(inner, outer * 0.6, re));
    float diskI = body * falloff * gapMask * dust * (1.0 + 0.5 * min(rim, 1.0));
    col += diskCol * diskI * 1.6;

    // ---- jets along the spin axis -------------------------------------------------------------------------------
    float s = dot(q, mn);
    float dj = dot(q, mj);
    float as = abs(s);
    float len = Extent * 0.85 * sinI;
    float width = max(0.16 + 0.07 * as, minW);
    float along = smoothstep(1.2, 2.6, as) * (1.0 - smoothstep(len * 0.5, len, as));
    float knots = pow(0.5 + 0.5 * cos(as * 1.5 - Time * 1.3), 4.0);
    col += vec3(0.7, 0.9, 1.0) * exp(-(dj * dj) / (width * width)) * along * (0.25 + 1.1 * knots) * 0.9;

    col = 1.0 - exp(-col * 1.2);

    if (vox) {
        float seam = min(min(cellFrac.x, 1.0 - cellFrac.x), min(cellFrac.y, 1.0 - cellFrac.y));
        col *= (1.0 - 0.26 * (1.0 - smoothstep(0.0, 0.07, seam))) * (0.92 + 0.16 * hash(cellId * 0.37 + 5.0));
        col = floor(col * 10.0 + 0.5) / 10.0;
    }

    float edge = 1.0 - smoothstep(0.7, 1.0, max(abs(vLocal.x), abs(vLocal.y)));
    fragColor = vec4(col * edge, 0.0);
}
