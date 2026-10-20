#version 150

// Wolf-Rayet star: the star body itself is drawn by the normal star mesh; this billboard adds what makes a WR star
// unlike any other -- the wind it is tearing off itself.
//   - a harsh blue-white corona,
//   - radial wind streaks flowing outwards, with a slow two-armed pinwheel spiral through them,
//   - shells of ejected gas that expand, thin and fade as they travel: teal (ionised oxygen) near the star,
//     shading towards red (hydrogen) as they cool.
// Purely additive, no loops beyond three shells, no lensing: this is cheap enough to draw straight to the screen.
// In cube mode the whole thing is evaluated per block and the shells become square.

uniform float Time;
uniform float Extent;   // billboard half-size, in star radii
uniform float Voxel;    // 0 = smooth; otherwise blocks per star radius (cube mode)
uniform float Core;     // radius of the star body in billboard units (1.0, or 0.82 for the cube)
uniform float Detail;   // 2 (low) - 4 (high)
uniform vec3 Tint;      // star colour

in vec2 vLocal;
out vec4 fragColor;

const float PI = 3.14159265;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// value noise, periodic in x so an angle can be fed in with no seam
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
        p = vec2(p.x * 2.0, p.y * 2.0 + 7.3);
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

    // distance from the star in star radii; square in cube mode so shells become nested cubes
    float r = (vox ? max(abs(q.x), abs(q.y)) : length(q)) / Core;
    if (r < 0.98) {
        fragColor = vec4(0.0);   // the star mesh covers this
        return;
    }
    float ang = atan(q.y, q.x);
    float layers = Detail;

    vec3 hot = mix(Tint, vec3(1.0), 0.55);
    vec3 col = vec3(0.0);

    // ---- corona -------------------------------------------------------------------------------------------------
    float d = max(r - 1.0, 0.0);
    col += hot * (1.15 * exp(-d * 1.4) + 0.38 / (1.0 + d * d * 1.8));

    // ---- wind: streaks flowing out, plus a slow two-armed pinwheel ---------------------------------------------
    float streak = pnoise(vec2(ang * 28.0 / (2.0 * PI), r * 0.55 - Time * 0.9), 28.0);
    streak = pow(streak, 2.6) * 2.4;
    float phase = ang - 1.6 * log(r) + Time * 0.22;
    float arms = pow(0.5 + 0.5 * cos(2.0 * phase), 3.0);
    float windFade = smoothstep(1.0, 1.7, r) * exp(-r * 0.28);
    col += mix(Tint, hot, 0.5) * (streak * 0.55 + arms * 0.9) * windFade;

    // ---- expanding shells of ejected gas ----------------------------------------------------------------------
    float maxR = Extent * 0.74;
    float halo = 0.0;
    for (int k = 0; k < 3; k++) {
        if (k == 2 && Detail < 2.5) break;
        float s = fract(Time * 0.035 + float(k) / 3.0);          // 0 = just ejected, 1 = fully dispersed
        float shellR = 2.2 + s * (maxR - 2.2);
        float thick = 0.22 + 0.9 * s;
        float profile = exp(-pow((r - shellR) / thick, 2.0));
        float life = pow(1.0 - s, 1.5) * smoothstep(0.0, 0.08, s);
        float clump = pfbm(ang + float(k) * 1.9, shellR * 0.4, 7.0, layers);
        vec3 shellCol = mix(vec3(0.25, 1.0, 0.85), vec3(1.0, 0.30, 0.42), smoothstep(0.5, 1.0, s) * 0.75);
        col += shellCol * profile * life * (0.12 + 2.6 * pow(clump, 2.2)) * 1.2;
        halo += life * smoothstep(shellR, shellR * 0.35, r);     // glow filling the bubble behind the shell
    }
    col += vec3(0.10, 0.70, 0.60) * halo * 0.035;

    // ---- finish ------------------------------------------------------------------------------------------------
    col = 1.0 - exp(-col * 1.25);

    if (vox) {
        float seam = min(min(cellFrac.x, 1.0 - cellFrac.x), min(cellFrac.y, 1.0 - cellFrac.y));
        col *= (1.0 - 0.26 * (1.0 - smoothstep(0.0, 0.07, seam))) * (0.92 + 0.16 * hash(cellId * 0.37 + 5.0));
        col = floor(col * 10.0 + 0.5) / 10.0;
    }

    // fade to nothing before the billboard edge so no rectangle shows
    float edge = 1.0 - smoothstep(0.7, 1.0, max(abs(vLocal.x), abs(vLocal.y)));
    fragColor = vec4(col * edge, 0.0);   // alpha 0: purely additive
}
