#version 150

// The Antimatter Collider, as a particle accelerator you could stand inside.
// Two crossed tracks run round the chamber as tubes of light, each with a pair of energy strands twisting round it and a
// ring of magnet at even spacing. Bunches of matter go one way and antimatter the other, each a hot core with a comet
// tail and three sizes of bloom. At the four crossings stand pillars of light with arcs of lightning leaping between the
// two tracks. The floor carries rings and spokes, and a ring of detector panels stands round the whole chamber and lights
// up in the direction of every collision. A collision throws a white core, a noisy shell, a second ring, curving particle
// tracks and streaks of debris. In the middle the trap is a turbulent sphere with an accretion ring that grows as it
// fills, flares off its corona, and, while it gives power, jets along its axis and rays across the floor.
//
// Nothing here marches the whole box. The floor, the detector wall and the pillars are found in closed form. The tracks
// are marched only through the thin slab they lie in, the collision only through a sphere round the crossing, and the trap
// only through a sphere round the middle, each with few steps, so a ray only pays for what it actually touches.

uniform float Time;
uniform float Power;
uniform float Seed;
uniform float Scale;
uniform float Detail;   // 0.3 to 1, how many steps the marches take
uniform vec3 Centre;
uniform vec3 ColorA;   // matter
uniform vec3 ColorB;   // antimatter
uniform vec4 P0;       // x how fast the beams go 0..1, y how bright overall, z how full the trap is 0..1, w rays 0/1
uniform vec4 P1;       // x collision age 0..1 (-1 for none), y which crossing 0..3, z how many bunches

in vec3 wpos;
out vec4 fragColor;

const vec3 HALF = vec3(3.0, 1.5, 3.0);
const float TAU = 6.2831853;
const float FLOOR_Y = -0.95;
const float WALL_R = 2.78;

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

float n3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(h31(i), h31(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(h31(i + vec3(0.0, 1.0, 0.0)), h31(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(h31(i + vec3(0.0, 0.0, 1.0)), h31(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(h31(i + vec3(0.0, 1.0, 1.0)), h31(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

vec2 boxHit(vec3 ro, vec3 rd, vec3 h) {
    vec3 inv = 1.0 / rd;
    vec3 t0 = (-h - ro) * inv;
    vec3 t1 = (h - ro) * inv;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    return vec2(max(max(tmin.x, tmin.y), tmin.z), min(min(tmax.x, tmax.y), tmax.z));
}

// the part of the ray inside a sphere, clipped to [t0, t1]; x > y means it misses
vec2 sphereSeg(vec3 ro, vec3 rd, vec3 c, float R, float t0, float t1) {
    vec3 oc = ro - c;
    float b = dot(oc, rd);
    float h = b * b - (dot(oc, oc) - R * R);
    if (h < 0.0) return vec2(1.0, 0.0);
    float s = sqrt(h);
    return vec2(max(-b - s, t0), min(-b + s, t1));
}

vec3 crossingCentre(float idx) {
    return vec3((mod(idx, 2.0) < 0.5) ? 1.25 : -1.25, 0.0, (idx < 1.5) ? 1.25 : -1.25);
}

// one track. ab the two radii, dir the way round it (1 or -1), head the angle of the first bunch, n how many bunches
vec3 track(vec3 p, vec2 ab, float dir, float head, float n, vec3 tone) {
    float g = length(vec2(p.x / ab.x, p.z / ab.y));
    float dr = (g - 1.0) * min(ab.x, ab.y);
    float tube2 = dr * dr + p.y * p.y;
    if (tube2 > 0.3) return vec3(0.0);
    float th = atan(p.z / ab.y, p.x / ab.x);
    float theta = th * dir;
    vec3 hot = mix(tone, vec3(1.0), 0.55);

    vec3 c = tone * (exp(-tube2 / 0.004) * 1.3 + exp(-tube2 / 0.06) * 0.14);

    // two strands twisting round it
    float ph = th * 9.0 * dir + Time * 2.6;
    vec2 cs = 0.1 * vec2(cos(ph), sin(ph));
    vec2 o1 = vec2(dr, p.y) - cs;
    vec2 o2 = vec2(dr, p.y) + cs;
    c += hot * (exp(-dot(o1, o1) / 0.0006) + exp(-dot(o2, o2) / 0.0006)) * 1.2;

    // magnets: a ring round the tube at even spacing, with a bright collar
    float mag = smoothstep(0.88, 1.0, cos(th * 10.0));
    float ring = exp(-pow((sqrt(tube2) - 0.17) / 0.03, 2.0));
    c += tone * mag * ring * 0.8 + vec3(0.9, 0.95, 1.0) * mag * exp(-tube2 / 0.01) * 0.3;

    // bunches: a hot core, a comet tail, and three sizes of bloom
    float gap = TAU / n;
    float behind = mod(head - theta, gap) * 1.8;
    float core = exp(-behind * behind / 0.02) * exp(-tube2 / 0.012);
    float tail = exp(-behind * 2.4) * smoothstep(0.0, 0.05, behind) * exp(-tube2 / 0.005) * step(behind, 1.5);
    float bloom = exp(-behind * behind / 0.12) * exp(-tube2 / 0.05) * 0.4;
    c += hot * core * 6.5 + tone * tail * 3.0 + tone * bloom * 1.4;
    return c;
}

vec3 emitTracks(vec3 p) {
    float speed = 0.25 + 1.9 * P0.x;
    float n = max(P1.z, 1.0);
    vec3 col = track(p, vec2(2.0, 1.6), 1.0, Time * speed, n, ColorA);
    col += track(p, vec2(1.6, 2.0), -1.0, Time * speed - 0.5, n, ColorB);

    // arcs of lightning leaping round the nearest crossing, between the two tracks
    vec2 sg = sign(p.xz + vec2(0.0001));
    vec2 dxz = p.xz - sg * 1.25;
    float r2 = dot(dxz, dxz);
    if (r2 < 0.5) {
        float ang = atan(dxz.y, dxz.x);
        float flick = step(0.4, fract(Time * 2.3 + sg.x * 0.37 + sg.y * 0.19));
        float arcs = pow(n3(vec3(ang * 3.0 + Time * 2.0, sqrt(r2) * 14.0 - Time * 9.0, sg.x + sg.y * 2.0)), 7.0);
        col += mix(ColorA, ColorB, 0.5 + 0.5 * sin(ang * 2.0 + Time)) * arcs * exp(-r2 / 0.05) * exp(-p.y * p.y / 0.03) * flick * 1.6;
    }
    return col;
}

vec3 emitCollision(vec3 p, vec3 c) {
    float f = P1.x;
    vec3 v = p - c;
    float d = length(v);
    vec3 dir = v / max(d, 0.001);
    float grain = n3(dir * 4.0 + Time * 3.0);
    vec3 col = vec3(1.0) * exp(-d * d / (0.05 * (1.0 - f) + 0.005)) * (1.0 - f) * 1.6;
    col += mix(ColorA, ColorB, grain) * exp(-pow((d - 1.2 * f) / (0.09 + 0.12 * f), 2.0)) * (0.5 + grain) * (1.0 - f) * 1.5;
    col += vec3(0.9, 0.95, 1.0) * exp(-pow((d - 0.7 * f) / 0.04, 2.0)) * (1.0 - f) * 0.9;
    float streak = pow(h31(floor(dir * 6.0 + P1.y * 3.0)), 6.0);
    vec3 tint = mix(ColorA, ColorB, step(0.5, h31(floor(dir * 6.0 + 9.0))));
    col += tint * streak * smoothstep(0.1 + 1.6 * f, 0.0, d) * smoothstep(0.0, 0.25, d) * (1.0 - f) * 3.2;
    // particle tracks that curl as they leave, like the traces in a bubble chamber
    float la = atan(v.z, v.x);
    for (int k = 0; k < 5; k++) {
        float fk = float(k);
        float want = TAU * h11(fk + 5.0 + P1.y) + (h11(fk + 9.0) - 0.5) * 4.0 * d;
        float da = atan(sin(la - want), cos(la - want)) * d;
        float reach = smoothstep(0.15 + 1.9 * f, 0.0, d - 0.12);
        col += mix(ColorA, ColorB, h11(fk + 2.0)) * exp(-(da * da + v.y * v.y * 4.0) / 0.0018) * reach * (1.0 - f * 0.7) * 1.8;
    }
    return col;
}

vec3 emitTrap(vec3 p, float fill) {
    float r = 0.1 + 0.3 * sqrt(fill);
    float d = length(p);
    float rad = length(p.xz);
    vec3 col = vec3(0.0);
    if (d < r * 5.0) {
        float turb = n3(p * 7.0 + vec3(Time * 1.4, -Time, Time * 0.7));
        float shell = exp(-pow((d - r) / (r * 0.35), 2.0)) * (0.4 + 1.2 * turb);
        col += mix(ColorB, ColorA, turb) * shell * 1.9;
        col += ColorB * exp(-d * d / (r * r * 2.0)) * 0.55 * (0.4 + fill);
        col += vec3(1.0, 0.95, 1.0) * smoothstep(r * 0.7, r * 0.15, d) * 1.5;
        vec3 nd = p / max(d, 0.001);
        float flare = pow(n3(nd * 4.0 + vec3(Time * 0.7, 0.0, Time * 0.4)), 6.0) * exp(-pow((d - r * 1.5) / (r * 0.6), 2.0));
        col += mix(ColorA, vec3(1.0), 0.5) * flare * 3.2;
        float ringR = r * 2.0 + 0.1 * sin(Time * 3.0);
        float ra = atan(p.z, p.x);
        float beamed = 0.55 + 0.45 * cos(ra - Time * 3.0);
        col += mix(ColorA, ColorB, 0.5 + 0.5 * sin(ra * 3.0 + Time * 4.0)) *
               exp(-(pow(rad - ringR, 2.0) + p.y * p.y * 9.0) / 0.003) * fill * 1.7 * beamed;
    }
    if (P0.w > 0.5) {
        float a = atan(p.z, p.x) + Time * 0.4;
        float rays = pow(abs(cos(a * 5.0)), 10.0) * (0.5 + 0.5 * sin(Time * 6.0 + floor(a * 5.0 / 3.1416) * 1.7));
        col += vec3(1.0, 0.88, 0.55) * rays * exp(-p.y * p.y / 0.015) * smoothstep(1.4, 0.1, rad) * smoothstep(0.1, 0.3, rad) * 1.6;
        float jet = exp(-rad * rad / (0.006 + 0.01 * abs(p.y))) * smoothstep(0.2, 0.5, abs(p.y)) *
                    smoothstep(1.5, 0.6, abs(p.y)) * (0.6 + 0.4 * sin(p.y * 18.0 - Time * 15.0));
        col += vec3(1.0, 0.85, 0.5) * jet * 1.9;
    }
    return col;
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

    float beam = P0.x;
    float fill = P0.z;
    float t0 = max(hit.x, 0.0);
    float t1 = min(hit.y, sceneDistance(rd) / k);
    if (t1 <= t0) discard;
    float jit = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    vec3 col = vec3(0.0);

    // ---- the floor: a plane, shaded once, with rings, spokes and a pulse running out from the middle ---------------
    if (abs(rd.y) > 0.001) {
        float tp = (FLOOR_Y - ro.y) / rd.y;
        if (tp > t0 && tp < t1) {
            vec3 p = ro + rd * tp;
            float rad = length(p.xz);
            if (rad < 2.95) {
                float rings = smoothstep(0.93, 1.0, cos(rad * 11.0)) * 0.6 + exp(-pow(rad - 2.0, 2.0) / 0.01) * 0.7;
                float spokes = pow(abs(cos(atan(p.z, p.x) * 8.0 + Time * 0.15)), 24.0) * smoothstep(2.6, 0.3, rad);
                float wave = 0.5 + 0.5 * sin(rad * 5.0 - Time * 3.0 * (0.3 + beam));
                col += mix(ColorA, ColorB, 0.5) * (0.06 + 0.22 * rings + 0.2 * spokes + 0.07 * wave) * smoothstep(2.95, 2.3, rad);
            }
        }
    }

    // ---- the detector wall: panels round the chamber, lit where a collision has been --------------------------------
    {
        float a2 = dot(rd.xz, rd.xz);
        if (a2 > 1e-6) {
            float b2 = dot(ro.xz, rd.xz);
            float disc = b2 * b2 - a2 * (dot(ro.xz, ro.xz) - WALL_R * WALL_R);
            if (disc > 0.0) {
                float sq = sqrt(disc);
                for (int w = 0; w < 2; w++) {
                    float tt = (-b2 + (w == 0 ? -sq : sq)) / a2;
                    if (tt > t0 && tt < t1) {
                        vec3 p = ro + rd * tt;
                        if (abs(p.y) < 1.2) {
                            float ang = atan(p.z, p.x);
                            float cu = ang / TAU * 28.0;
                            float cell = floor(cu);
                            float fx = fract(cu);
                            float cv = (p.y + 1.2) * 2.5;
                            float row = floor(cv);
                            float fy = fract(cv);
                            float panel = smoothstep(0.04, 0.1, fx) * smoothstep(0.96, 0.9, fx) *
                                          smoothstep(0.04, 0.1, fy) * smoothstep(0.96, 0.9, fy);
                            float base = 0.04 + 0.06 * h11(cell * 7.0 + row * 3.0);
                            float lit = 0.0;
                            if (P1.x >= 0.0) {
                                vec3 cc = crossingCentre(floor(P1.y + 0.5));
                                float ca = atan(cc.z, cc.x);
                                float da = abs(atan(sin(ang - ca), cos(ang - ca)));
                                lit = smoothstep(0.7, 0.0, da) * (1.0 - P1.x) * (0.4 + 0.6 * h11(cell + row * 5.0 + 3.0)) * 2.4;
                            }
                            vec3 tone = mix(ColorA, ColorB, 0.5 + 0.5 * sin(ang * 3.0));
                            float edge = 1.0 - panel;
                            col += tone * (panel * (base + lit) * 0.9 + edge * (0.05 + 0.12 * lit)) *
                                   smoothstep(1.2, 1.0, abs(p.y));
                        }
                    }
                }
            }
        }
    }

    // ---- the pillars and rings at the four crossings, in closed form ---------------------------------------------
    {
        float a2 = dot(rd.xz, rd.xz);
        for (int q = 0; q < 4; q++) {
            vec3 c = crossingCentre(float(q));
            if (a2 > 1e-6) {
                float s = dot(c.xz - ro.xz, rd.xz) / a2;
                if (s > t0 && s < t1) {
                    vec3 pp = ro + rd * s;
                    float h = abs(pp.y);
                    vec2 dd = pp.xz - c.xz;
                    if (h < 1.4) {
                        float pillar = exp(-dot(dd, dd) / 0.005) * smoothstep(1.4, 0.2, h) *
                                       (0.5 + 0.5 * sin(pp.y * 9.0 - Time * 7.0 + float(q)));
                        col += vec3(0.8, 0.9, 1.0) * pillar * 0.5;
                    }
                }
            }
            if (abs(rd.y) > 0.001) {
                float tr = -ro.y / rd.y;
                if (tr > t0 && tr < t1) {
                    vec3 pr = ro + rd * tr;
                    float dd = length(pr.xz - c.xz) - 0.2;
                    col += vec3(0.95, 0.95, 1.0) * exp(-dd * dd / 0.002) * 0.55;
                }
            }
        }
    }

    // ---- the tracks and their bunches: marched through the thin slab they lie in --------------------------------
    if (abs(rd.y) > 0.0005) {
        float tA = (-0.4 - ro.y) / rd.y;
        float tB = (0.4 - ro.y) / rd.y;
        float sa = max(min(tA, tB), t0);
        float sb = min(max(tA, tB), t1);
        if (sb > sa) {
            int steps = max(4, int(22.0 * Detail));
            float dt = (sb - sa) / float(steps);
            for (int i = 0; i < 22; i++) {
                if (i >= steps) break;
                vec3 p = ro + rd * (sa + (float(i) + jit) * dt);
                col += emitTracks(p) * dt;
            }
        }
    } else if (abs(ro.y) < 0.4) {
        int steps = max(4, int(22.0 * Detail));
        float dt = (t1 - t0) / float(steps);
        for (int i = 0; i < 22; i++) {
            if (i >= steps) break;
            vec3 p = ro + rd * (t0 + (float(i) + jit) * dt);
            col += emitTracks(p) * dt;
        }
    }

    // ---- a collision: only rays through the sphere round the crossing pay for it ---------------------------------
    if (P1.x >= 0.0) {
        vec3 c = crossingCentre(floor(P1.y + 0.5));
        vec2 sg = sphereSeg(ro, rd, c, 0.45 + 1.75 * P1.x, t0, t1);
        if (sg.x < sg.y) {
            int steps = max(4, int(14.0 * Detail));
            float dt = (sg.y - sg.x) / float(steps);
            for (int i = 0; i < 14; i++) {
                if (i >= steps) break;
                vec3 p = ro + rd * (sg.x + (float(i) + jit) * dt);
                col += emitCollision(p, c) * dt;
            }
        }
    }

    // ---- the trap: only rays through the sphere round the middle pay for it --------------------------------------
    {
        float trapR = P0.w > 0.5 ? 1.7 : min(1.7, (0.1 + 0.3 * sqrt(fill)) * 5.0);
        vec2 sg = sphereSeg(ro, rd, vec3(0.0), trapR, t0, t1);
        if (sg.x < sg.y) {
            int steps = max(4, int(14.0 * Detail));
            float dt = (sg.y - sg.x) / float(steps);
            for (int i = 0; i < 14; i++) {
                if (i >= steps) break;
                vec3 p = ro + rd * (sg.x + (float(i) + jit) * dt);
                col += emitTrap(p, fill) * dt;
            }
        }
    }

    fragColor = vec4(col * P0.y * Power, 1.0);
}
