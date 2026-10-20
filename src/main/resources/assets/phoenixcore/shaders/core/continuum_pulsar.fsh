#version 150

// Pulsar: a tiny, brutally dense star whose magnetic axis is tilted from its spin axis, so two beams sweep round like
// a lighthouse. Each time a beam swings towards the camera the star flares. Faint dipole field-line arcs turn with the
// beams. Additive, loop-free, cheap. In cube mode the beams are streaks of blocks.
//
// Rotation is slow (one turn every 3.2 s, so a flash about every 1.6 s): nothing here strobes fast.

uniform float Time;
uniform float Extent;   // billboard half-size, in star radii
uniform float Voxel;    // 0 = smooth; otherwise blocks per star radius (cube mode)
uniform float Core;     // radius of the star body in billboard units (1.0, or 0.82 for the cube)
uniform vec3 Tint;      // star colour

in vec2 vLocal;
out vec4 fragColor;

const float PI = 3.14159265;
const float PERIOD = 3.2;   // seconds per full turn
const float INCL = 0.95;    // radians between the magnetic axis and the spin axis

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
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
    float minW = vox ? 0.55 / Voxel : 0.0;

    vec3 hot = mix(Tint, vec3(1.0), 0.5);
    vec3 beamCol = mix(Tint, vec3(1.0), 0.22);   // beams keep more of the star's colour than its glare

    // spin axis, leaning towards the camera; the magnetic axis circles it
    vec3 S = normalize(vec3(0.30, 0.85, 0.43));
    vec3 U = normalize(cross(S, vec3(0.0, 0.0, 1.0)));
    vec3 V = cross(S, U);
    float w = Time * 2.0 * PI / PERIOD;
    vec3 b = cos(INCL) * S + sin(INCL) * (cos(w) * U + sin(w) * V);

    vec3 col = vec3(0.0);

    // ---- the hot glow round the star ---------------------------------------------------------------------------
    float d = max(rs - 1.0, 0.0);
    col += hot * (1.0 * exp(-d * 1.7) + 0.25 / (1.0 + d * d * 2.0));

    // ---- the two beams ------------------------------------------------------------------------------------------
    float flare = 0.0;
    for (int k = 0; k < 2; k++) {
        vec3 bb = (k == 0) ? b : -b;
        vec2 pd = bb.xy;
        float plen = length(pd);
        vec2 dirp = pd / max(plen, 1e-4);
        float s = dot(q, dirp);                          // distance along the beam on screen
        float dd = dot(q, vec2(-dirp.y, dirp.x));        // distance across it
        float toward = max(bb.z, 0.0);                   // how directly it points at the camera
        float len = Extent * 0.85 * plen;                // a beam pointing at us is foreshortened to nothing
        float width = max(0.22 + 0.11 * max(s, 0.0), minW);
        float along = smoothstep(0.9, 2.2, s) * (1.0 - smoothstep(len * 0.55, len, s));
        float beam = exp(-(dd * dd) / (width * width)) * along * (0.3 + 1.3 * toward * toward);
        col += beamCol * beam * 1.35;
        flare += pow(toward, 9.0);
    }
    // the flash as a beam sweeps over us (capped so it never gets harsh)
    col += mix(hot, vec3(1.0), 0.4) * min(flare, 1.0) * 1.5 * (exp(-d * 0.35) + 0.5 * exp(-d * 0.08));

    // ---- dipole field-line arcs, turning with the magnetic axis -------------------------------------------------
    vec2 a2 = normalize(b.xy + vec2(1e-4));
    vec2 qh = q / max(length(q), 1e-4);
    float cosT = dot(qh, a2);
    float L = rs / max(1.0 - cosT * cosT, 0.04);
    float v = abs(fract(L / 2.4) - 0.5) * 2.0;
    float arcs = smoothstep(0.86, 1.0, v) * (1.0 - smoothstep(4.0, 10.0, L)) * smoothstep(1.3, 2.0, rs);
    col += mix(Tint, vec3(0.5, 0.7, 1.0), 0.5) * arcs * 0.30;

    col = 1.0 - exp(-col * 1.2);

    if (vox) {
        float seam = min(min(cellFrac.x, 1.0 - cellFrac.x), min(cellFrac.y, 1.0 - cellFrac.y));
        col *= (1.0 - 0.26 * (1.0 - smoothstep(0.0, 0.07, seam))) * (0.92 + 0.16 * hash(cellId * 0.37 + 5.0));
        col = floor(col * 10.0 + 0.5) / 10.0;
    }

    float edge = 1.0 - smoothstep(0.7, 1.0, max(abs(vLocal.x), abs(vLocal.y)));
    fragColor = vec4(col * edge, 0.0);
}
