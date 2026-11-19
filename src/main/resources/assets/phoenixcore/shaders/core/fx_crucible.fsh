#version 150

// The Aether Crucible, as an alchemist's furnace made of light. A pool of molten metal lies in the bottom of the box with
// glowing cracks of convection running through a dark crust, rings spreading over it and heat shimmering off it. Above it
// hangs a sphere of aether, banded and churning like a gas giant, with a corona. Two tilted rings of runes turn round it,
// motes trail round those, bolts of lightning arc down from the orb to the pool, and shafts of light stand in the air.
// Teal at rest and gold while it works, when everything spins faster and the bolts come more often.
// Cheap on purpose: the pool is a single ray-plane hit, the rings and motes are closed form, and only the column the
// bolts live in, and the orb's sphere, are marched, with few steps. The middle of the box is 0.05 blocks below the chamber
// centre.

uniform float Time;
uniform float Power;
uniform float Seed;
uniform float Scale;
uniform float Detail;   // 0.3 to 1, how many steps the marches take
uniform vec3 Centre;
uniform vec3 ColorA;   // at rest
uniform vec3 ColorB;   // while it works
uniform vec4 P0;       // x how worked up 0..1, y how bright overall

in vec3 wpos;
out vec4 fragColor;

const vec3 HALF = vec3(1.0, 1.45, 1.0);
const float POOL_Y = -0.63;
const float POOL_R = 0.58;
const float TAU = 6.2831853;

float h11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float h31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec2 h22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float n3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(h31(i), h31(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(h31(i + vec3(0.0, 1.0, 0.0)), h31(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(h31(i + vec3(0.0, 0.0, 1.0)), h31(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(h31(i + vec3(0.0, 1.0, 1.0)), h31(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

float fbm2(vec3 p) {
    return 0.62 * n3(p) + 0.38 * n3(p * 2.1 + 3.7);
}

vec2 rot(vec2 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

vec2 boxHit(vec3 ro, vec3 rd, vec3 h) {
    vec3 inv = 1.0 / rd;
    vec3 t0 = (-h - ro) * inv;
    vec3 t1 = (h - ro) * inv;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    return vec2(max(max(tmin.x, tmin.y), tmin.z), min(min(tmax.x, tmax.y), tmax.z));
}

// the light of a point as a ray passes it, in closed form: how near the ray comes
float pointGlow(vec3 ro, vec3 rd, vec3 pos, float size, float t0, float t1) {
    vec3 v = pos - ro;
    float s = dot(v, rd);
    if (s < t0 - 0.3 || s > t1 + 0.3) return 0.0;
    float d2 = dot(v, v) - s * s;
    return exp(-d2 / (size * size));
}

// a ring of runes lying in a plane tilted by tiltA about x and tiltB about z, centred on the orb. Closed form: the
// ray is carried into the ring's own frame and met with its plane
vec3 runeRing(vec3 ro, vec3 rd, vec3 c, float radius, float tiltA, float tiltB, float spin, vec3 tone, float t0, float t1) {
    float ca = cos(tiltA), sa = sin(tiltA), cb = cos(tiltB), sb = sin(tiltB);
    vec3 o = ro - c;
    vec3 d = rd;
    // rotate by -tiltB about z, then -tiltA about x
    o = vec3(cb * o.x + sb * o.y, -sb * o.x + cb * o.y, o.z);
    d = vec3(cb * d.x + sb * d.y, -sb * d.x + cb * d.y, d.z);
    o = vec3(o.x, ca * o.y + sa * o.z, -sa * o.y + ca * o.z);
    d = vec3(d.x, ca * d.y + sa * d.z, -sa * d.y + ca * d.z);
    if (abs(d.y) < 0.0005) return vec3(0.0);
    float tp = -o.y / d.y;
    if (tp < t0 - 0.2 || tp > t1 + 0.2) return vec3(0.0);
    vec3 p = o + d * tp;
    float r = length(p.xz);
    float band = exp(-pow((r - radius) / 0.012, 2.0));
    float band2 = exp(-pow((r - radius * 1.12) / 0.006, 2.0)) * 0.6;
    float ang = atan(p.z, p.x) + spin;
    // runes: dashes and small glyph ticks round it
    float dash = smoothstep(0.35, 0.5, abs(sin(ang * 12.0)));
    float tick = exp(-pow((r - radius * 1.07) / 0.018, 2.0)) * smoothstep(0.88, 0.95, sin(ang * 36.0));
    float glyph = exp(-pow((r - radius * 0.93) / 0.02, 2.0)) *
                  smoothstep(0.55, 0.62, h11(floor(ang * 24.0 / TAU * 6.0) + Seed));
    float head = pow(max(0.0, cos(ang * 1.0 - 0.0)), 18.0);
    return tone * (band * (0.35 + 0.65 * dash) + band2 + tick * 1.2 + glyph * 0.7) * 1.8 + vec3(1.0) * band * head * 0.6;
}

uniform sampler2D DepthSampler;   // the depth of everything drawn so far, so rays stop at the first block in the way
uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

// how far along the ray (in blocks) the nearest thing already drawn is, or a huge number if there is nothing
float sceneDistance(vec3 rayWorld) {
    float d = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    if (d >= 0.99999) return 1.0e9;
    float zn = d * 2.0 - 1.0;
    float zv = -ProjMat[3][2] / (zn + ProjMat[2][2]);       // how far in front of the camera, negative
    vec3 rv = mat3(ModelViewMat) * rayWorld;
    return rv.z < -1.0e-4 ? zv / rv.z : 1.0e9;
}

void main() {
    float k = max(Scale, 0.001);
    vec3 rd = normalize(wpos);
    vec3 ro = -Centre / k;
    float tw = length(wpos) / k;
    vec2 hit = boxHit(ro, rd, HALF);
    if (hit.y <= 0.0 || hit.x >= hit.y) discard;
    float face = hit.x > 0.0 ? hit.x : hit.y;
    if (abs(tw - face) > 0.06) discard;

    float work = P0.x;
    vec3 tone = mix(ColorA, ColorB, work);
    vec3 hotTone = mix(tone, vec3(1.0, 0.97, 0.88), 0.6);
    float t0 = max(hit.x, 0.0);
    float t1 = min(hit.y, sceneDistance(rd) / k);
    if (t1 <= t0) discard;
    vec3 col = vec3(0.0);
    float oy = 0.6 + 0.06 * sin(Time * 1.7);
    float spin = Time * (0.5 + 1.1 * work);
    // the column round the pool and orb, for cheap tests of whether a ray goes anywhere near what lives in it
    float ca = dot(rd.xz, rd.xz);
    float cb = dot(ro.xz, rd.xz);
    float cc = dot(ro.xz, ro.xz);

    // ---- the pool: where this ray meets the plane of its surface -------------------------------------------------
    if (abs(rd.y) > 0.001) {
        float tp = (POOL_Y - ro.y) / rd.y;
        if (tp > t0 && tp < t1) {
            vec3 p = ro + rd * tp;
            float rr = length(p.xz) / POOL_R;
            if (rr < 1.25) {
                vec2 q = rot(p.xz, (1.2 - min(rr, 1.0)) * 1.4 - Time * (0.25 + 0.35 * work));
                // convection cells: dark crust plates with glowing seams between them
                vec2 g = q * 5.5 + vec2(Seed, Time * 0.05);
                vec2 gi = floor(g);
                vec2 gf = fract(g);
                float d1 = 8.0, d2 = 8.0;
                for (int j = -1; j <= 1; j++) {
                    for (int i = 0; i < 3; i++) {
                        vec2 o = vec2(float(i) - 1.0, float(j));
                        vec2 pt = o + 0.5 + 0.4 * sin(Time * 0.6 + TAU * h22(gi + o));
                        float dd = length(pt - gf);
                        if (dd < d1) { d2 = d1; d1 = dd; } else if (dd < d2) { d2 = dd; }
                    }
                }
                float seam = exp(-(d2 - d1) * 9.0);
                float plate = smoothstep(0.0, 0.5, d2 - d1);
                float churn = fbm2(vec3(q * 3.0, Seed + Time * 0.2));
                float heat = clamp(seam * (0.7 + 0.6 * churn) + 0.25 * (1.0 - rr) + 0.25 * work, 0.0, 1.2);
                vec3 crust = tone * 0.07 * (0.5 + churn);
                vec3 lava = mix(tone * 1.0, vec3(1.0, 0.95, 0.8), clamp(heat - 0.75, 0.0, 1.0) * 0.7);
                vec3 surf = crust * plate + lava * heat * (0.7 + 0.6 * work);
                float rings = 0.0;
                for (int i = 0; i < 3; i++) {
                    float f = fract(Time * 0.45 + float(i) / 3.0);
                    rings += exp(-pow((rr - (0.15 + 0.85 * f)) / 0.04, 2.0)) * (1.0 - f);
                }
                float inside = smoothstep(1.0, 0.9, rr);
                col += (surf + vec3(1.0, 1.0, 0.95) * rings * 0.3) * inside * 1.05;
                // the lip of the basin, glowing
                col += tone * exp(-pow((rr - 1.06) / 0.05, 2.0)) * 0.9 * (0.5 + 0.5 * work);
                col += tone * exp(-max(rr - 1.0, 0.0) * 5.0) * 0.12 * (1.0 - inside);
            }
        }
    }

    // ---- the orb and its bolts: the column the bolts live in is marched, a few steps ------------------------------
    vec3 oc = ro - vec3(0.0, oy, 0.0);
    float orb = 0.2 + 0.1 * work + 0.012 * sin(Time * 5.0);
    float bq = dot(oc, rd);
    float disc = bq * bq - (dot(oc, oc) - 0.64);          // a sphere of radius 0.8 round the orb
    float orbHit = 0.0;
    if (disc > 0.0) {
        float sq = sqrt(disc);
        float ta = max(-bq - sq, t0);
        float tb = min(-bq + sq, t1);
        if (tb > ta) {
            int osteps = max(4, int(10.0 * Detail));
            float dt = (tb - ta) / float(osteps);
            float jit = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
            for (int i = 0; i < 10; i++) {
                if (i >= osteps) break;
                vec3 o = oc + rd * (ta + (float(i) + jit) * dt);
                float d = length(o);
                // bands of gas, sheared, like a gas giant, over a fine churn
                float lat = o.y / max(d, 0.001);
                float churn = n3(vec3(rot(o.xz, Time * 0.5), o.y) * 7.0 + vec3(Seed, Time * 0.8, 0.0));
                float bands = 0.5 + 0.5 * sin(lat * 9.0 + 3.0 * churn + Time * 0.6);
                float body = smoothstep(orb, orb * 0.5, d);
                vec3 e = mix(tone, vec3(1.0), smoothstep(orb, 0.0, d) * (0.4 + 0.6 * churn)) * body * (1.8 + 1.6 * bands) * 1.9;
                // a corona that fades out smoothly to nothing before the edge of the sphere
                float edgeFade = smoothstep(0.8, 0.35, d);
                e += tone * exp(-d * d / (orb * orb * 5.0)) * (0.6 + 0.6 * work) * edgeFade;
                e += hotTone * pow(churn, 5.0) * exp(-pow((d - orb * 1.25) / (orb * 0.45), 2.0)) * 2.4;
                col += e * dt;
            }
        }
    }

    // bolts of lightning from the orb down to the pool, a few of them, each wandering as it goes
    if (hit.x < 1e5) {
        // march the column between pool and orb, only where the ray is inside it
        float cr = 0.5;
        float a2 = ca;
        if (a2 > 1e-6) {
            float b2 = cb;
            float c2 = cc - cr * cr;
            float dd2 = b2 * b2 - a2 * c2;
            if (dd2 > 0.0) {
                float s2 = sqrt(dd2);
                float ta = max((-b2 - s2) / a2, t0);
                float tb = min((-b2 + s2) / a2, t1);
                if (tb > ta) {
                    int bsteps = max(4, int(12.0 * Detail));
                    float dt = (tb - ta) / float(bsteps);
                    float jit = fract(sin(dot(gl_FragCoord.xy, vec2(78.233, 12.9898))) * 43758.5453);
                    float bolts = 3.0 + 3.0 * work;
                    for (int i = 0; i < 12; i++) {
                        if (i >= bsteps) break;
                        vec3 p = ro + rd * (ta + (float(i) + jit) * dt);
                        float s = clamp((p.y - POOL_Y) / (oy - orb - POOL_Y), 0.0, 1.0);
                        if (p.y < POOL_Y || p.y > oy - orb * 0.8) continue;
                        float arc = 0.0;
                        for (int b = 0; b < 3; b++) {
                            float fb = float(b);
                            float flick = step(0.35, fract(Time * (1.3 + 0.9 * work) + h11(fb + 7.0) * 5.0));
                            float ang = TAU * h11(fb + 3.0) + Time * 0.3 * (fb - 1.0);
                            float bend = sin(s * 3.1416) * (0.22 + 0.16 * h11(fb + 11.0));
                            vec2 base = vec2(cos(ang), sin(ang)) * bend;
                            vec2 wob = vec2(sin(s * 31.0 + Time * 13.0 + fb * 3.0) + 0.6 * sin(s * 63.0 - Time * 17.0),
                                            sin(s * 27.0 + Time * 11.0 + fb * 5.0) + 0.6 * sin(s * 57.0 + Time * 19.0)) *
                                       0.055 * sin(s * 3.1416);
                            float dist2 = dot(p.xz - base - wob, p.xz - base - wob);
                            arc += exp(-dist2 / 0.0006) * flick * step(fb, bolts - 0.5);
                        }
                        col += mix(hotTone, vec3(1.0), 0.5) * arc * 1.1 * dt * 24.0;
                    }
                }
            }
        }
    }

    // ---- two tilted rings of runes turning round the orb -----------------------------------------------------------
    if (disc > 0.0) {
        col += runeRing(ro, rd, vec3(0.0, oy, 0.0), 0.5, 0.45, 0.2, spin, tone, t0, t1) * (0.6 + 0.5 * work);
        col += runeRing(ro, rd, vec3(0.0, oy, 0.0), 0.62, -0.5, -0.3, -spin * 0.8 + 1.0, hotTone, t0, t1) * (0.5 + 0.5 * work);
    }

    // ---- motes with short trails, and bubbles climbing out of the pool --------------------------------------------
    float count = 6.0 + 8.0 * work;
    if (disc > 0.0) {
    for (int i = 0; i < 14; i++) {
        float fi = float(i);
        if (fi > count - 0.5) break;
        for (int tr = 0; tr < 2; tr++) {
            float lag = float(tr) * 0.09;
            float a = (Time - lag) * (0.8 + 0.7 * work) + fi * TAU / count;
            float rad = 0.4 + 0.12 * sin(fi * 1.7 + Time - lag);
            vec3 pos = vec3(cos(a) * rad, oy + sin(a * 2.0 + fi) * 0.35, sin(a) * rad * 0.9);
            col += tone * pointGlow(ro, rd, pos, 0.032 - 0.008 * float(tr), t0, t1) * (0.9 - 0.3 * float(tr));
        }
    }
    }
    // bubbles: only for rays through the column above the pool
    if (ca > 1e-6 && cb * cb - ca * (cc - 0.16) > 0.0) {
    for (int i = 0; i < 8; i++) {
        float fi = float(i);
        float life = fract(Time * (0.25 + 0.3 * work) + fi * 0.173);
        float a = fi * 2.399;
        vec3 pos = vec3(cos(a) * 0.3 * (1.0 - 0.4 * life), POOL_Y + life * 1.6, sin(a) * 0.3 * (1.0 - 0.4 * life));
        col += vec3(1.0, 1.0, 0.95) * pointGlow(ro, rd, pos, 0.03 + 0.014 * sin(fi), t0, t1) * (1.0 - life) * 0.8;
    }

    }

    // ---- shafts of light standing in the air round the pool, faint ------------------------------------------------
    if (ca > 1e-6 && cb * cb - ca * (cc - 0.7225) > 0.0)
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float a = TAU * fi / 3.0 + Time * 0.1;
        vec3 base = vec3(cos(a) * 0.66, POOL_Y, sin(a) * 0.66);
        // closest approach of the ray to the vertical line through base
        vec2 dxz = base.xz - ro.xz;
        float a2 = dot(rd.xz, rd.xz);
        if (a2 > 1e-6) {
            float s = dot(dxz, rd.xz) / a2;
            if (s > t0 && s < t1) {
                vec3 pp = ro + rd * s;
                float h = (pp.y - POOL_Y);
                if (h > 0.0 && h < 1.5) {
                    float d2 = dot(pp.xz - base.xz, pp.xz - base.xz);
                    col += tone * exp(-d2 / 0.0016) * (1.0 - h / 1.5) * (0.5 + 0.5 * sin(Time * 2.0 + fi * 2.0)) * 0.22;
                }
            }
        }
    }

    fragColor = vec4(col * P0.y * Power, 1.0);
}
