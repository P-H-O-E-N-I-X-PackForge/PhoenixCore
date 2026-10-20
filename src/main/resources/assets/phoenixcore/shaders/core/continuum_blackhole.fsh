#version 150

// A black hole on a camera-facing billboard, with real gravitational lensing.
//
// Light around a non-rotating black hole travels in a plane that contains the hole and the view ray, so each pixel
// only needs a 1-D orbit integral u(phi) (u = 1/r), not a full 3-D raymarch. Per pixel we step that orbit a few dozen
// times and, every time the ray crosses the disk plane, shade the disk there. That one loop produces:
//   - the shadow (its radius is exactly 1 in the units used here),
//   - the direct image of the disk, including the near half passing in front of the hole,
//   - the lensed image of the far half arching over and under the hole,
//   - the thin second and third order images that pile up into the photon ring,
//   - Doppler beaming and gravitational redshift for free (the photon direction is known at every hit).
//
// Cost control: pixels outside the disk skip the loop completely, the step size follows the Detail uniform
// (LOW .. HIGH), and the loop breaks as soon as the ray escapes, falls in, or the disk is opaque.
//
// Output is premultiplied: colour adds, alpha occludes whatever was behind.

uniform float Time;
uniform float Extent;       // billboard half-size, in shadow radii
uniform vec3 DiskNormal;    // disk plane normal (spin axis), in view space
uniform float Jets;         // 1.0 for a quasar
uniform float DiskOuter;    // outer edge of the disk, in shadow radii
uniform float Detail;       // 2 (low) - 4 (high): orbit step size and noise layers
uniform vec3 HotColor;
uniform vec3 CoolColor;
uniform float Lensing;      // 0 - 1: lensed starfield and background dimming (0 on LOW)
uniform float Beaming;      // 0 - 1: strength of the Doppler brightness asymmetry
uniform float Voxel;        // 0 = smooth; otherwise blocks per shadow radius (cube mode)
uniform float DiskIntensity; // brightness (and opacity) of the accretion disk; 1 = normal, 2+ = a furnace
uniform float Feeding;      // 0 - 1: how much the hole is eating. Adds infalling streams of debris and bright clumps,
                            // and lets them reach past the edge of the disk

in vec2 vLocal;
out vec4 fragColor;

const float PI = 3.14159265;
// Schwarzschild radius in shadow units: shadow radius = 3*sqrt(3)/2 * Rs = 1
const float RS = 0.3849;
const float INNER = 1.22;   // just outside the innermost stable orbit (3 Rs = 1.155)

// ---------------------------------------------------------------------------------------------------------------
// noise. Periodic in x so a disk angle can be fed in directly with no seam where atan() wraps.

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

// ang is in radians; period is the lattice cells that fit round the full circle
float pfbm(float ang, float r, float period, float layers) {
    vec2 p = vec2(ang * period / (2.0 * PI), r);
    float v = 0.0, a = 0.5, total = 0.0;
    for (int i = 0; i < 4; i++) {
        if (float(i) >= layers) break;
        v += a * pnoise(p, period);
        total += a;
        p = vec2(p.x * 2.0, p.y * 2.0 + 11.7);
        period *= 2.0;
        a *= 0.5;
    }
    // fewer layers must not make the disk dimmer: scale to the brightness of the full four
    return v / total * 0.9375;
}

// orbiting turbulence: coarse clumps plus fine filaments stretched along the orbit, sheared by Keplerian rotation
float diskTexture(float r, float ang, float layers) {
    float spin = Time * 0.9 * pow(r, -1.5);
    float a = ang - spin;
    float coarse = pfbm(a, r * 2.6, 5.0, layers);
    float filaments = pnoise(vec2((a) * 13.0 / (2.0 * PI), r * 24.0), 13.0);
    float fine = layers > 3.5 ? pnoise(vec2((a - spin * 0.3) * 37.0 / (2.0 * PI), r * 58.0), 37.0) : 0.5;
    return (0.5 + 0.95 * coarse) * (0.72 + 0.56 * filaments) * (0.86 + 0.28 * fine);
}

// ---------------------------------------------------------------------------------------------------------------
// lensed background stars (thin-lens mapping, no orbit needed)

vec3 stars(vec2 p, bool blocky) {
    vec2 g = p * 6.0;
    vec2 id = floor(g);
    vec2 f = fract(g) - 0.5;
    float h = hash(id + 3.1);
    if (h < 0.86) return vec3(0.0);
    vec2 off = vec2(hash(id + 7.7), hash(id + 13.3)) - 0.5;
    vec2 df = f - off * 0.6;
    float d = blocky ? max(abs(df.x), abs(df.y)) : length(df);
    float size = 0.05 + 0.07 * hash(id + 21.9);
    float s = (blocky ? step(d, size * 0.9) : exp(-d * d / (size * size))) * (0.35 + 1.1 * (h - 0.86) * 7.0);
    vec3 tint = mix(vec3(0.7, 0.8, 1.0), vec3(1.0, 0.85, 0.7), hash(id + 5.5));
    return tint * s;
}

// ---------------------------------------------------------------------------------------------------------------

void main() {
    vec2 qRaw = vLocal * Extent;
    bool vox = Voxel > 0.5;
    vec2 cellId = vec2(0.0);
    vec2 cellFrac = vec2(0.5);
    vec2 q = qRaw;
    if (vox) {
        // cube mode: the whole hole is evaluated per block, so lensing, disk and ring read as voxels
        vec2 g = qRaw * Voxel;
        cellId = floor(g);
        cellFrac = g - cellId;
        q = (cellId + 0.5) / Voxel;
    }
    float rho = length(q);
    float fw = max(fwidth(length(qRaw)), 1e-4);

    vec3 n = normalize(DiskNormal);
    float diskOuter = DiskOuter;
    float outer = DiskOuter * (1.0 + 0.35 * Feeding);   // debris streams reach past the disk itself

    vec3 e1 = cross(n, vec3(0.0, 0.0, 1.0));
    e1 = length(e1) < 0.05 ? vec3(1.0, 0.0, 0.0) : normalize(e1);
    vec3 e2 = cross(n, e1);

    vec3 col = vec3(0.0);
    float T = 1.0;           // how much of the light behind the disk survives to the camera
    float layers = Detail;

    // ---- trace the ray through the disk ---------------------------------------------------------------------
    if (rho < outer * 1.06) {
        vec2 dir2 = q / max(rho, 1e-4);
        vec3 d2 = vec3(dir2, 0.0);

        // angle in the ray's orbital plane at which it crosses the disk plane; further crossings repeat every pi
        float phiC = mod(-atan(n.z, dot(n.xy, dir2)), PI);
        if (phiC < 0.02) phiC += PI;

        float h = mix(0.17, 0.085, clamp((Detail - 2.0) * 0.5, 0.0, 1.0));
        float u = 0.0;
        float v = 1.0 / max(rho, 1e-3);   // du/dphi at the camera
        float phi = 0.0;
        int crossings = 0;

        for (int i = 0; i < 170; i++) {
            // photon orbit: u'' = -u + 1.5 Rs u^2   (velocity Verlet)
            float a0 = -u + 1.5 * RS * u * u;
            float un = u + v * h + 0.5 * a0 * h * h;
            float an = -un + 1.5 * RS * un * un;
            float vn = v + 0.5 * (a0 + an) * h;
            float phin = phi + h;

            if (phin >= phiC) {
                float f = (phiC - phi) / h;
                float uc = mix(u, un, f);
                float vc = mix(v, vn, f);
                float r = 1.0 / max(uc, 1e-3);

                if (uc > 1e-3 && r > INNER - 0.2 && r < outer) {
                    vec3 rhat = sin(phiC) * d2 + cos(phiC) * vec3(0.0, 0.0, 1.0);
                    vec3 phat = cos(phiC) * d2 - sin(phiC) * vec3(0.0, 0.0, 1.0);

                    // where the photon is heading on its way to the camera
                    vec3 toCam = normalize(vc * rhat - uc * phat);

                    // matter orbiting the hole, and what that does to the light
                    float ang = atan(dot(rhat, e2), dot(rhat, e1));
                    vec3 orbit = normalize(cross(n, rhat));
                    float beta = clamp(sqrt(RS / (2.0 * max(r - RS, 0.05))), 0.0, 0.7);
                    float gamma = inversesqrt(1.0 - beta * beta);
                    float g = sqrt(max(1.0 - RS / r, 0.05)) / (gamma * (1.0 - beta * dot(orbit, toCam)));
                    float beam = mix(1.0, pow(clamp(g, 0.3, 1.7), 3.0), Beaming);

                    // disk body
                    float t = clamp((r - INNER) / (outer - INNER), 0.0, 1.0);
                    float bright = pow(INNER / r, 2.4);
                    float edge = smoothstep(INNER - 0.2, INNER + 0.12, r) * (1.0 - smoothstep(diskOuter * 0.62, diskOuter, r));
                    float tex = diskTexture(r, ang, crossings > 0 ? max(layers - 1.0, 2.0) : layers);

                    // redshift cools the colour, blueshift heats it
                    float heat = clamp(t * 1.15 - (g - 1.0) * 0.8, 0.0, 1.0);
                    vec3 c = mix(HotColor, CoolColor, pow(heat, 0.6));
                    vec3 L = c * bright * tex * beam * 3.0 * edge * DiskIntensity;

                    // infalling debris: spiral streams with bright clumps racing inwards. Feeding = 0 skips all of it.
                    float stream = 0.0;
                    if (Feeding > 0.01) {
                        float lr = log(max(r, 1.0));
                        float arms = pow(0.5 + 0.5 * cos(3.0 * (ang + 1.8 * lr) - Time * 0.9), 5.0);
                        float clumps = pow(pnoise(vec2((ang + 1.8 * lr) * 9.0 / (2.0 * PI), lr * 3.2 + Time * 0.9), 9.0), 3.0);
                        float reach = smoothstep(INNER - 0.1, INNER + 0.5, r) * (1.0 - smoothstep(outer * 0.8, outer, r));
                        stream = arms * (0.2 + 3.2 * clumps) * pow(INNER / max(r, INNER), 1.1) * reach * Feeding;
                        L += mix(HotColor, vec3(1.0), 0.22) * stream * 3.2 * DiskIntensity * beam;
                    }

                    // a slab seen at a grazing angle is more opaque than one seen face-on; a brighter disk is denser
                    float tau = ((0.35 + 1.5 * bright) * edge + 0.9 * stream) * mix(1.0, DiskIntensity, 0.5);
                    float a = 1.0 - exp(-tau / clamp(abs(dot(toCam, n)), 0.12, 1.0));

                    col += T * a * L;
                    T *= 1.0 - a;
                }
                phiC += PI;
                crossings++;
            }

            if (un > 1.0 / RS) { T = 0.0; break; }   // fell through the horizon
            if (un < 0.0) break;                     // escaped to the background
            if (T < 0.03 || crossings >= 3) break;

            u = un;
            v = vn;
            phi = phin;
        }
    }

    // ---- everything that is not the disk ---------------------------------------------------------------------
    // rays inside the shadow never reach the background; the edge is analytic so it stays smooth at low resolution
    float esc = vox ? step(1.0, rho) : smoothstep(1.0 - fw, 1.0 + fw, rho);

    vec3 bg = vec3(0.0);
    float bgAlpha = 0.0;

    // the light bent into a faint glow, and the bright thin photon ring
    bg += HotColor * 0.14 * DiskIntensity * exp(-(rho - 1.0) / (1.0 + 0.25 * Feeding) * 1.3);
    // in cube mode the ring is one block thick, hugging the edge of the blocky shadow
    float ringC = vox ? 1.0 + 0.45 / Voxel : 1.025;
    float ringW = vox ? 0.5 / Voxel : 0.03;
    float ring = exp(-pow((rho - ringC) / ringW, 2.0));
    bg += vec3(1.0, 0.9, 0.7) * ring * 1.3 * sqrt(DiskIntensity);
    bgAlpha = max(bgAlpha, ring * 0.5);

    if (Lensing > 0.01 && rho < 5.0) {
        float w = (1.0 - smoothstep(2.4, 5.0, rho)) * Lensing;
        // point-mass thin lens: source = image * (1 - E^2/rho^2)
        float E2 = 2.2;
        vec2 src = q * (1.0 - E2 / max(rho * rho, 0.25));
        float mag = clamp(1.0 / max(abs(1.0 - E2 * E2 / max(pow(rho, 4.0), 0.0625)), 0.15), 1.0, 5.0);
        bg += stars(src, vox) * mag * w;
        // the original sky behind is not what we see here
        bgAlpha = max(bgAlpha, w * 0.5);
    }

    col += T * esc * bg;
    float alpha = 1.0 - T * esc * (1.0 - bgAlpha);

    // ---- jets along the spin axis (quasar) ---------------------------------------------------------------------
    if (Jets > 0.5 && length(n.xy) > 0.04) {
        vec2 ax = normalize(n.xy);
        float t = dot(q, ax);
        float s = dot(q, vec2(-ax.y, ax.x));
        float at = abs(t);
        float w = max(0.16 + 0.05 * at, vox ? 0.55 / Voxel : 0.0);
        float core = exp(-(s * s) / (w * w));
        float pulse = 0.65 + 0.35 * sin(at * 1.6 - Time * 5.5 + (t > 0.0 ? 0.0 : 1.4));
        float jet = core * smoothstep(1.4, 2.8, at) * exp(-at * 0.07) * pulse;
        float cone = exp(-(s * s) / (w * w * 14.0)) * smoothstep(1.4, 3.0, at) * exp(-at * 0.1) * 0.2;
        col += vec3(0.55, 0.75, 1.0) * (jet * 2.0 + cone);
    }

    // soft tone map so the hot inner disk burns towards white instead of clipping flat
    col = 1.0 - exp(-col * 1.15);

    if (vox) {
        // block look: a faint seam between blocks, per-block brightness jitter, and a limited colour palette
        float seam = min(min(cellFrac.x, 1.0 - cellFrac.x), min(cellFrac.y, 1.0 - cellFrac.y));
        col *= (1.0 - 0.26 * (1.0 - smoothstep(0.0, 0.07, seam))) * (0.92 + 0.16 * hash(cellId * 0.37 + 5.0));
        col = floor(col * 10.0 + 0.5) / 10.0;
    }

    // fade to nothing at the billboard edge so there is no visible rectangle
    float edgeMask = 1.0 - smoothstep(0.82, 1.0, max(abs(vLocal.x), abs(vLocal.y)));
    col *= edgeMask;
    alpha = rho < 1.0 ? alpha : alpha * edgeMask;

    fragColor = vec4(col, clamp(alpha, 0.0, 1.0));
}
