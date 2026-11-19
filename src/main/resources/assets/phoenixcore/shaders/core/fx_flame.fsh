#version 150

// The Phoenix Infuser as a phoenix made of fire. A column of flame rises from a ring of magma runes on the floor, eaten
// into licks by 3D noise, with smaller tongues circling it. Two great wings of fire turn slowly round it, each a fan of
// feathers that reach out and curl up in flame, opening wider as the heat climbs. Above it hangs a halo of golden
// light, with a ring of runes turning in it and rays falling from it. Embers climb through the air, a burst of
// sparks leaves it when something is burned, and a ring of gold rolls up it when something is renewed.
// The floor and the halo are found in closed form; only rays through the cylinder the fire lives in are marched. The
// middle of the box is 1.5 blocks above the floor.

uniform float Time;
uniform float Power;
uniform float Seed;
uniform float Scale;
uniform float Detail;   // 0.3 to 1, how many steps the march takes
uniform vec3 Centre;
uniform vec3 ColorA;   // the body of the flame
uniform vec3 ColorB;   // the white-hot core
uniform vec4 P0;       // x flame 0..1, y how bright overall, z burst age 0..1 (-1 for none), w ring age (-1 for none)
uniform vec4 P1;       // x heat 0..1

in vec3 wpos;
out vec4 fragColor;

const vec3 HALF = vec3(1.9, 1.8, 1.9);
const float TAU = 6.2831853;
const float FLOOR = -1.45;       // local y of the floor
const float HALO_Y = 1.45;       // local y of the halo

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

float arc(float a, float start, float len) {
    float u = mod(a - start, TAU);
    return smoothstep(0.0, 0.15, u) * smoothstep(len, len - 0.15, u);
}

// how high the cupped sheet of the wings stands at a given radius: low near the body, curling up toward the tips
float wingY(float r) {
    float rw = 0.5 + 0.85 * P1.x;
    return 0.15 + 1.25 * pow(clamp(r / (rw * 1.15), 0.0, 1.6), 1.6);
}

// the colour of a wing at a point on its sheet: nine feathers across, each with a bright spine, reaching different
// distances and ending in torn flame
vec3 shadeWing(vec3 p) {
    float heat = P1.x;
    float open = 0.35 + 0.65 * heat;
    float rw = 0.5 + 0.85 * heat;
    float r = length(p.xz);
    float a = atan(p.z, p.x) - Time * 0.3;
    // one wing each side, opposite one another, each spreading about its own direction
    float wl = 1.1 * open + 0.3;
    float wing = arc(a, -0.5 * wl, wl) + arc(a, 3.1416 - 0.5 * wl, wl);
    if (wing < 0.001 || r < 0.2) return vec3(0.0);
    float fan = a * 7.0;
    float feather = pow(0.5 + 0.5 * cos(fan), 1.6);
    float reach = rw * (0.85 + 0.5 * feather) * 1.15;
    float tip = n3(vec3(cos(a) * 2.5, sin(a) * 2.5, r * 4.0 - Time * 2.2));
    float edge = smoothstep(reach * (0.85 + 0.3 * tip), reach * 0.5, r);
    float spine = pow(0.5 + 0.5 * cos(fan), 12.0);
    float rootFade = smoothstep(0.2, 0.42, r);
    vec3 tone = mix(ColorA, ColorB, 0.15 + 0.6 * spine * (1.0 - r / reach * 0.5));
    return tone * (0.5 + 1.5 * spine + 0.6 * feather) * edge * rootFade * wing * (0.75 + 0.25 * sin(Time * 6.0 + fan)) * (0.35 + 0.65 * P0.y);
}

vec3 emitFire(vec3 p) {
    float flame = P0.x;
    float heat = P1.x;
    float yb = p.y + 1.5;
    float r = length(p.xz);
    vec3 col = vec3(0.0);

    // the flame column
    float hb = 0.5 + 1.7 * flame;
    float yy = yb / hb;
    if (yy < 1.3 && yb > -0.3 && r < 0.9) {
        float n = n3(vec3(p.x * 1.8 + Seed, p.y * 1.6 - Time * 2.4, p.z * 1.8));
        float n2 = n3(p * 1.8 + vec3(7.0, -Time * 1.9, 3.0));
        vec2 sway = (vec2(n, n2) - 0.5) * 0.9 * yy;
        float width = (0.42 - 0.34 * min(yy, 1.0)) * (0.6 + 0.7 * flame) * 1.15;
        float d = length(p.xz - sway) / max(width, 0.001);
        float foot = smoothstep(-0.15, 0.1, yb);
        float body = smoothstep(1.0, 0.0, d) * smoothstep(1.3, 0.2, yy) * foot;
        float core = pow(smoothstep(0.65, 0.0, d), 2.0) * smoothstep(0.9, 0.0, yy) * foot;
        col += mix(ColorA, ColorB, clamp(core * 1.4, 0.0, 1.0)) * (body * 1.4 + core * 1.3) * 3.4;
    }

    // three smaller tongues circling the column
    if (r < 0.95 && yb > -0.3 && yb < 2.7) {
        for (int s = 0; s < 3; s++) {
            float a2 = Time * 0.9 + float(s) * 2.0944;
            vec2 c2 = vec2(cos(a2), sin(a2)) * (0.2 + 0.25 * flame);
            float h2 = hb * (0.5 + 0.2 * sin(Time * 2.7 + float(s) * 2.0));
            float y2 = yb / h2;
            if (y2 < 1.2) {
                float nn = n3(vec3(p.x * 2.6 + Seed + float(s) * 5.0, p.y * 2.2 - Time * 3.1, p.z * 2.6));
                vec2 sw = c2 * (1.0 - 0.5 * min(y2, 1.0)) + (nn - 0.5) * 0.5 * y2;
                float wd = (0.2 - 0.16 * min(y2, 1.0)) * (0.5 + 0.6 * flame);
                float dd = length(p.xz - sw) / max(wd, 0.001);
                col += mix(ColorA, ColorB, 0.25) * smoothstep(1.0, 0.0, dd) * smoothstep(1.2, 0.3, y2) * smoothstep(-0.1, 0.1, yb) * 1.6;
            }
        }
    }

    // a faint glow round where the wings are, so they sit in the fire. The wings themselves are surfaces, shaded
    // where the ray crosses them, in the march loop below
    float wglow = exp(-pow(r - (0.5 + 0.85 * heat) * 0.8, 2.0) / 0.25) * exp(-pow(yb - 0.7, 2.0) / 0.5);
    col += mix(ColorA, ColorB, 0.2) * wglow * 0.16 * (0.4 + 0.6 * P0.y);

    // embers drifting up, two grids at two scales
    vec3 g = p * 2.6 + vec3(Seed, -Time * (0.5 + flame), 0.0);
    vec3 cell = floor(g);
    if (h31(cell) > 0.9 - 0.06 * heat) {
        vec3 c = cell + 0.2 + 0.6 * vec3(h31(cell + 1.0), h31(cell + 2.0), h31(cell + 3.0));
        float d = length(g - c);
        col += vec3(1.0, 0.7, 0.3) * exp(-d * d * 28.0) * smoothstep(2.6, 0.2, yb) * smoothstep(-0.2, 0.3, yb) * smoothstep(1.7, 0.6, r) * 2.2;
    }

    // sparks thrown out in all directions when something is burned
    if (P0.z >= 0.0) {
        float f = P0.z;
        vec3 v = p - vec3(0.0, 0.7, 0.0);
        float dist = length(v);
        vec3 dir = v / max(dist, 0.001);
        col += vec3(1.0, 0.8, 0.35) * exp(-pow((dist - (0.2 + 1.3 * f)) / 0.14, 2.0)) * (0.3 + 14.0 * pow(n3(dir * 7.0 + Seed), 6.0)) * (1.0 - f);
    }

    // a golden ring rolling up the column when something is renewed
    if (P0.w >= 0.0) {
        float f = P0.w;
        float dd = sqrt(pow(r - (0.3 + 1.1 * f), 2.0) + pow(yb - (0.5 + 0.4 * f), 2.0));
        col += vec3(1.0, 0.83, 0.3) * exp(-dd * dd / (0.004 + 0.003 * (1.0 - f))) * (1.0 - f) * 2.8;
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

    float flame = P0.x;
    float heat = P1.x;
    float t0 = max(hit.x, 0.0);
    float t1 = min(hit.y, sceneDistance(rd) / k);
    if (t1 <= t0) discard;
    float jit = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    vec3 col = vec3(0.0);

    // ---- the floor: a ring of magma runes round the base, cracks running out of it, all in a plane ---------------
    if (abs(rd.y) > 0.001) {
        float tp = (FLOOR - ro.y) / rd.y;
        if (tp > t0 && tp < t1) {
            vec3 p = ro + rd * tp;
            float r = length(p.xz);
            if (r < 1.8) {
                float a = atan(p.z, p.x);
                float ring = exp(-pow((r - 1.15) / 0.045, 2.0)) + exp(-pow((r - 1.32) / 0.02, 2.0)) * 0.7;
                float runes = smoothstep(0.55, 0.62, h11(floor((a + Time * 0.15 * sign(sin(floor(r * 0.0 + 1.0)))) / TAU * 20.0) + Seed));
                float glyph = exp(-pow((r - 1.23) / 0.05, 2.0)) * runes;
                float cracks = pow(n3(vec3(cos(a) * 3.0, sin(a) * 3.0, r * 5.0 - Time * 0.3)), 6.0) * smoothstep(1.1, 0.2, r) * 1.0;
                float glow = exp(-r * r / 0.3) * (0.4 + 0.5 * flame);
                col += mix(ColorA, ColorB, 0.3) * (ring * 1.2 + glyph * 1.8 + cracks * 1.4 + glow * 1.0) * (0.4 + 0.6 * P0.y) * smoothstep(1.8, 1.4, r);
            }
        }
    }

    // ---- the halo: a golden ring above the fire, with a ring of runes turning in it and rays falling from it -----
    if (abs(rd.y) > 0.001) {
        float tp = (HALO_Y + 0.0 - ro.y) / rd.y;
        if (tp > t0 && tp < t1) {
            vec3 p = ro + rd * tp;
            float r = length(p.xz);
            float a = atan(p.z, p.x) + Time * 0.5;
            float pulse = 0.75 + 0.25 * sin(Time * 2.0);
            float band = exp(-pow((r - 0.8) / 0.02, 2.0)) * (0.5 + 0.5 * smoothstep(0.35, 0.5, abs(sin(a * 8.0))));
            float outer = exp(-pow((r - 0.93) / 0.01, 2.0)) * 0.7;
            float ticks = exp(-pow((r - 0.87) / 0.02, 2.0)) * smoothstep(0.85, 0.95, sin(a * 40.0));
            float fill = exp(-r * r / 0.6) * 0.08;
            col += vec3(1.0, 0.82, 0.35) * (band * 1.4 + outer + ticks * 1.2 + fill) * pulse * (0.4 + 0.6 * heat);
        }
    }

    // ---- the fire itself: only rays through the cylinder it lives in are marched ---------------------------------
    {
        float a2 = dot(rd.xz, rd.xz);
        float b2 = dot(ro.xz, rd.xz);
        float disc = b2 * b2 - a2 * (dot(ro.xz, ro.xz) - 3.0);   // a cylinder of radius 1.75
        if (a2 > 1e-6 && disc > 0.0) {
            float sq = sqrt(disc);
            float ta = max((-b2 - sq) / a2, t0);
            float tb = min((-b2 + sq) / a2, t1);
            if (tb > ta) {
                int steps = max(6, int(30.0 * Detail));
                float dt = (tb - ta) / float(steps);
                vec3 prevP = ro + rd * ta;
                float prevSd = (prevP.y + 1.5) - wingY(length(prevP.xz));
                for (int i = 0; i < 30; i++) {
                    if (i >= steps) break;
                    vec3 p = ro + rd * (ta + (float(i) + jit) * dt);
                    col += emitFire(p) * dt;
                    float sd = (p.y + 1.5) - wingY(length(p.xz));
                    if (i > 0 && sd * prevSd < 0.0) {
                        float ft = prevSd / (prevSd - sd);
                        col += shadeWing(mix(prevP, p, ft)) * 2.2;
                    }
                    prevP = p;
                    prevSd = sd;
                }
            }
        }
    }

    fragColor = vec4(col * Power, 1.0);
}
