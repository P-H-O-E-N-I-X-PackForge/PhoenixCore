#version 150

// The phoenix of the Phoenix Forge, as a bird of fire seen in three dimensions. Everything is in the bird's own units
// (the old model's, where its wing is 1.5 long), turned to the way it flies. Mode 0 is the ember it is before it wakes: a
// glowing seed with sparks circling it. Mode 1 is the bird:
//   - two wings, each a fan of primaries, secondaries and coverts reaching back from the shoulder, flapping, with the
//     tips lagging behind the beat and bending down, and a fainter ghost of where the wing was a moment ago
//   - a tail of nine long feathers streaming back, swaying
//   - a body, a neck and head with a beak, an eye, and a crest of plumes
//   - a halo of fire round it, and embers falling from the wings
// Feathers run from white-hot at the root through gold and orange to red at the tip, and the tips burn away into flame.
// Wings and tail are thin sheets and are found in closed form where the ray crosses them; the body, head and embers are
// closed form too. Only the flame round the body is marched.

uniform float Time;
uniform float Power;
uniform float Seed;
uniform float Scale;
uniform float Detail;   // 0.3 to 1, how many steps the march takes
uniform vec3 Centre;
uniform vec3 ColorA;    // the gold of the feathers
uniform vec3 ColorB;    // the white-hot root
uniform vec4 P0;        // x how much of it is shown 0..1, y unused, z 0 for the ember and 1 for the bird, w how far it has risen 0..1
uniform vec4 P1;        // xy the way it flies, in the world, as a unit vector (x, z)

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

in vec3 wpos;
out vec4 fragColor;

const vec3 HALF = vec3(2.3, 1.5, 2.3);
const vec3 BOXC = vec3(0.0, 0.2, -0.55);     // the middle of the box, in the bird's own coordinates
float PX = 0.0;   // how big one pixel is, in the bird's own units, where we are looking
const float PI = 3.14159265;
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

float pointGlow(vec3 ro, vec3 rd, vec3 pos, float size, float t0, float t1) {
    vec3 v = pos - ro;
    float s = dot(v, rd);
    if (s < t0 - 0.3 || s > t1 + 0.3) return 0.0;
    float d2 = dot(v, v) - s * s;
    return exp(-d2 / (size * size));
}

// where a ray meets a sphere, nearest first; x > y if it misses
vec2 sphereHit(vec3 ro, vec3 rd, vec3 c, float R) {
    vec3 oc = ro - c;
    float b = dot(oc, rd);
    float h = b * b - (dot(oc, oc) - R * R);
    if (h < 0.0) return vec2(1.0, 0.0);
    float s = sqrt(h);
    return vec2(-b - s, -b + s);
}

// a feather colour: white-hot at the root, gold or orange in the middle, red at the tip, and how bright it is
vec3 featherColour(float u, float k) {
    vec3 hot = ColorB;
    vec3 mid = mix(ColorA, ColorA * vec3(1.0, 0.62, 0.4), mod(k, 2.0));
    vec3 tip = vec3(0.88, 0.2, 0.07);
    vec3 c = u < 0.5 ? mix(hot, mid, u * 2.0) : mix(mid, tip, (u - 0.5) * 2.0);
    float a = u < 0.5 ? mix(1.1, 0.8, u * 2.0) : mix(0.8, 0.0, (u - 0.5) * 2.0);
    return c * a;
}

// One fan of feathers, given where this point is in polar terms about the root: how far (r), and at what angle (a).
// count feathers start at angle a0 and spread across span; each is len0 long, shorter toward the end by fall.
vec3 featherSet(float r, float a, float lateralNoise, float a0, float span, float count, float len0, float fall, float expo,
                float wid) {
    float idx = (a - a0) / span * (count - 1.0);
    float k = floor(idx + 0.5);
    if (k < 0.0 || k > count - 1.0) return vec3(0.0);
    float f = k / max(count - 1.0, 1.0);
    float len = len0 * (1.0 - fall * pow(f, expo));
    float u = r / len;
    if (u > 1.05 || u < 0.02) return vec3(0.0);
    float lateral = (idx - k) * span / max(count - 1.0, 1.0) * r + lateralNoise * (0.2 + u);
    float w = max(wid * sin(PI * (0.18 + 0.82 * pow(clamp(u, 0.0, 1.0), 0.8))), PX * 1.6);
    float edge = smoothstep(w + PX, w * 0.3, abs(lateral));
    // the shaft down the middle of each feather is brighter, and the tip burns away
    float shaft = exp(-lateral * lateral / (w * w * 0.08 + 1e-5));
    float burn = 1.0 - smoothstep(0.82, 1.05, u);
    vec3 fc = featherColour(clamp(u, 0.0, 1.0), k);
    float membrane = 0.32 * (1.0 - smoothstep(0.55, 1.0, u));
    return fc * (edge * (0.6 + 0.9 * shaft) + membrane) * burn;
}

// one wing as a sheet: tilted up by beta about the line of the body, with the feathers bent down and back by the beat
vec3 wingSheet(vec3 ro, vec3 rd, float side, float beta, float vel, float t0, float t1) {
    vec3 sh = vec3(side * 0.08, 0.07, 0.15);
    vec3 e1 = vec3(side * cos(beta), sin(beta), 0.0);
    vec3 n = vec3(sin(beta), -side * cos(beta), 0.0);
    float dn = dot(rd, n);
    if (abs(dn) < 1e-4) return vec3(0.0);
    float shift = 0.0;
    vec3 col = vec3(0.0);
    for (int it = 0; it < 2; it++) {
        vec3 shs = sh + vec3(0.0, shift, 0.0);
        float t = dot(shs - ro, n) / dn;
        if (t < t0 - 0.5 || t > t1 + 0.5) return vec3(0.0);
        vec3 hp = ro + rd * t - sh;
        float u = dot(hp, e1);
        float v = -hp.z;
        float r = length(vec2(u, v));
        float a = atan(v, u);
        // the feathers bend down and lag behind the beat, more toward the tips
        float rr = clamp(r / 1.5, 0.0, 1.0);
        shift = -1.5 * (0.3 * vel + 0.12) * rr * rr * 0.55;
        if (it == 1) {
            if (u < 0.0) return vec3(0.0);
            float lat = (n3(vec3(u * 6.0, v * 6.0, Time * 2.0 + side)) - 0.5) * 0.06;
            // primaries, then the secondaries and coverts laid over them
            col += featherSet(r, a, lat, radians(-6.0), radians(72.0), 10.0, 1.5, 0.4, 1.4, 0.095);
            col += featherSet(r, a, lat, radians(32.0), radians(52.0), 8.0, 0.85, 0.32, 1.0, 0.085) * 0.9;
            col += featherSet(r, a, lat, radians(-4.0), radians(40.0), 6.0, 0.7, 0.35, 1.0, 0.06) * 0.8;
        }
    }
    return col;
}

// the tail: nine feathers fanned back from its root, drooping and swaying
vec3 tailSheet(vec3 ro, vec3 rd, float vel, float t0, float t1) {
    vec3 root = vec3(0.0, -0.03, -0.42);
    vec3 n = normalize(vec3(0.0, 1.0, -0.1));
    float dn = dot(rd, n);
    if (abs(dn) < 1e-4) return vec3(0.0);
    float t = dot(root - ro, n) / dn;
    if (t < t0 - 0.5 || t > t1 + 0.5) return vec3(0.0);
    vec3 hp = ro + rd * t - root;
    float v = -hp.z;
    if (v < 0.02) return vec3(0.0);
    float u = hp.x;
    float ang = atan(u, v);
    float ft = tan(ang) / 0.42;
    float idx = (ft + 1.0) * 4.0;
    float k = floor(idx + 0.5);
    if (k < 0.0 || k > 8.0) return vec3(0.0);
    float f = (k - 4.0) / 4.0;
    float len = 1.05 + 0.7 * (1.0 - abs(f)) + (mod(k, 2.0) < 0.5 ? 0.12 : 0.0);
    float r = length(vec2(u, v));
    float uu = r / len;
    if (uu > 1.05) return vec3(0.0);
    float sway = (sin(Time * 2.0 + k * 0.7) * 0.18 + f * 0.2) * uu * uu;
    float lateral = (idx - k) * 0.42 / 4.0 * r - sway * 0.0 + (n3(vec3(r * 5.0, k, Time * 2.5)) - 0.5) * 0.05 * (0.3 + uu);
    float w = max(0.075 * sin(PI * (0.18 + 0.82 * pow(clamp(uu, 0.0, 1.0), 0.8))), PX * 1.6);
    float edge = smoothstep(w + PX, w * 0.3, abs(lateral));
    float shaft = exp(-lateral * lateral / (w * w * 0.08 + 1e-5));
    vec3 tc = featherColour(clamp(uu, 0.0, 1.0), k);
    return tc * (edge * (0.6 + 0.9 * shaft) + 0.3 * (1.0 - smoothstep(0.55, 1.0, uu))) * (1.0 - smoothstep(0.82, 1.05, uu));
}

void main() {
    float k = max(Scale, 0.001);
    vec3 wrd = normalize(wpos);
    vec3 wro = -Centre / k;
    float tw = length(wpos) / k;
    PX = tw * 2.0 / (ProjMat[1][1] * float(textureSize(DepthSampler, 0).y));
    vec2 hit = boxHit(wro, wrd, HALF);
    if (hit.y <= 0.0 || hit.x >= hit.y) discard;
    float face = hit.x > 0.0 ? hit.x : hit.y;
    if (abs(tw - face) > 0.06) discard;

    // into the bird's own frame: x to its right, z the way it faces, and its middle at the middle of the box
    vec2 dirv = normalize(P1.xy + vec2(1e-5));
    vec2 rightv = vec2(-dirv.y, dirv.x);
    vec3 ro = vec3(dot(wro.xz, rightv), wro.y, dot(wro.xz, dirv)) + BOXC;
    vec3 rd = vec3(dot(wrd.xz, rightv), wrd.y, dot(wrd.xz, dirv));
    float t0 = max(hit.x, 0.0);
    float t1 = min(hit.y, sceneDistance(wrd) / k);
    if (t1 <= t0) discard;
    float alpha = P0.x;
    float jit = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    vec3 col = vec3(0.0);

    // ---- the halo of fire about it, big and soft, in closed form --------------------------------------------------
    col += ColorA * vec3(1.0, 0.6, 0.3) * pointGlow(ro, rd, vec3(0.0, 0.1, -0.1), 1.25, t0, t1) * 0.05 * alpha;
    col += ColorA * pointGlow(ro, rd, vec3(0.0, 0.1, -0.1), 0.62, t0, t1) * 0.07 * alpha;

    if (P0.z < 0.5) {
        // ---- the ember: a glowing seed with sparks circling it ----------------------------------------------------
        float pulse = 0.85 + 0.15 * sin(Time * 3.0);
        col += ColorA * pointGlow(ro, rd, vec3(0.0), 0.5 * pulse, t0, t1) * 0.5;
        col += mix(ColorA, ColorB, 0.6) * pointGlow(ro, rd, vec3(0.0), 0.22, t0, t1) * 0.9;
        col += ColorB * pointGlow(ro, rd, vec3(0.0), 0.09, t0, t1) * 1.4;
        for (int i = 0; i < 9; i++) {
            float fi = float(i);
            float a = Time * (0.6 + 0.1 * fi) + fi * 0.7;
            float r = 0.6 + 0.25 * sin(Time + fi);
            vec3 pos = vec3(cos(a) * r, sin(a * 1.3) * 0.3, sin(a) * r);
            col += ColorA * pointGlow(ro, rd, pos, 0.045, t0, t1) * 1.1;
        }
        fragColor = vec4(col * Power, 1.0);
        return;
    }

    float phase = Time * 2.2;
    float flap = sin(phase);
    float vel = cos(phase);
    float beta = 0.2 + 0.55 * flap;
    float beta2 = 0.2 + 0.55 * sin(phase - 0.55);        // where the wing was a moment ago
    float vel2 = cos(phase - 0.55);

    // ---- the wings, with a fainter ghost of the last beat ---------------------------------------------------------
    for (int s = 0; s < 2; s++) {
        float side = s == 0 ? 1.0 : -1.0;
        col += wingSheet(ro, rd, side, beta, vel, t0, t1) * 2.1 * alpha;
        col += wingSheet(ro, rd, side, beta2, vel2, t0, t1) * 0.55 * alpha;
        // a soft glow along the wing, so it is a body of fire and not a drawing of one
        vec3 e1 = vec3(side * cos(beta), sin(beta), 0.0);
        for (int g = 0; g < 4; g++) {
            float fg = (float(g) + 0.7) / 4.0;
            col += ColorA * pointGlow(ro, rd, vec3(side * 0.08, 0.07, 0.1) + e1 * fg * 1.25 + vec3(0.0, -0.18 * fg * (0.3 * vel + 0.12), -0.25 * fg), 0.2 + 0.18 * fg, t0, t1) * 0.28 * (1.0 - 0.5 * fg) * alpha;
        }
    }

    // ---- the tail --------------------------------------------------------------------------------------------------
    col += tailSheet(ro, rd, vel, t0, t1) * 2.8 * alpha;

    // ---- the body, the neck and head, closed form --------------------------------------------------------------------
    {
        // an ellipsoid for the body, found by squashing space
        vec3 c = vec3(0.0, 0.01, -0.05);
        vec3 sq = vec3(1.0 / 0.15, 1.0 / 0.15, 1.0 / 0.5);
        vec3 o = (ro - c) * sq;
        vec3 d = rd * sq;
        float a = dot(d, d);
        float b = dot(o, d);
        float cc = dot(o, o) - 1.0;
        float h = b * b - a * cc;
        if (h > 0.0) {
            float tt = (-b - sqrt(h)) / a;
            if (tt > t0 - 0.2 && tt < t1 + 0.2) {
                vec3 p = ro + rd * tt;
                float vv = clamp((p.z + 0.5) / 0.9, 0.0, 1.0);
                vec3 nrm = normalize((p - c) * sq * sq);
                float belly = 0.5 + 0.5 * dot(nrm, vec3(0.0, -1.0, 0.0));
                float fres = pow(1.0 - abs(dot(nrm, rd)), 2.0);
                float lick = n3(p * 9.0 + vec3(0.0, -Time * 3.0, 0.0));
                vec3 base = mix(ColorA * vec3(1.0, 0.5, 0.25), ColorB, max(belly * 0.7, vv * 0.9));
                col += (base * (0.7 + 0.5 * lick) + ColorA * fres * 0.8) * 1.15 * alpha;
            }
        }
        // the neck and head: four spheres
        vec4 neck[4] = vec4[4](vec4(0.0, 0.05, 0.38, 0.12), vec4(0.0, 0.12, 0.47, 0.105), vec4(0.0, 0.2, 0.55, 0.09),
                               vec4(0.0, 0.27, 0.61, 0.085));
        for (int i = 0; i < 4; i++) {
            vec2 sh = sphereHit(ro, rd, neck[i].xyz, neck[i].w);
            if (sh.x < sh.y && sh.x > t0 - 0.2 && sh.x < t1 + 0.2) {
                vec3 p = ro + rd * sh.x;
                vec3 nrm = normalize(p - neck[i].xyz);
                float fres = pow(1.0 - abs(dot(nrm, rd)), 2.0);
                col += mix(ColorB, ColorA, 0.4 * float(i) / 3.0) * (0.7 + 0.5 * fres) * 0.9 * alpha;
            }
        }
        // the beak and the eyes
        col += mix(ColorA, ColorB, 0.4) * pointGlow(ro, rd, vec3(0.0, 0.235, 0.78), 0.035, t0, t1) * 1.6 * alpha;
        col += vec3(1.0) * (pointGlow(ro, rd, vec3(0.056, 0.285, 0.64), 0.014, t0, t1) +
                            pointGlow(ro, rd, vec3(-0.056, 0.285, 0.64), 0.014, t0, t1)) * 2.0 * alpha;
        // the crest: five plumes streaming up and back, each a short run of glows
        for (int i = 0; i < 5; i++) {
            float f = (float(i) - 2.0) / 2.0;
            float sway = sin(Time * 3.0 + float(i)) * 0.05;
            vec3 base = vec3(f * 0.015, 0.33, 0.59);
            vec3 dir = normalize(vec3(f * 0.35, 0.55, -0.8));
            for (int j = 0; j < 4; j++) {
                float u = float(j) / 3.0;
                vec3 pos = base + dir * (0.4 * u) + vec3(sway * u * u, -0.1 * u * u, -0.1 * u * u);
                col += mix(ColorB, ColorA, u) * pointGlow(ro, rd, pos, 0.022, t0, t1) * (1.0 - 0.6 * u) * 1.2 * alpha;
            }
        }
    }

    // ---- the flame licking round the body, the only part that is marched -----------------------------------------
    {
        vec2 sg = sphereHit(ro, rd, vec3(0.0, 0.05, -0.1), 0.9);
        if (sg.x < sg.y) {
            float s0 = max(sg.x, t0);
            float s1 = min(sg.y, t1);
            if (s1 > s0) {
                int steps = max(4, int(12.0 * Detail));
                float dt = (s1 - s0) / float(steps);
                for (int i = 0; i < 12; i++) {
                    if (i >= steps) break;
                    vec3 p = ro + rd * (s0 + (float(i) + jit) * dt);
                    // distance to the line of the body, and flame carried back along it
                    float along = clamp(p.z, -0.55, 0.45);
                    float dr = length(vec2(p.x, p.y - 0.02) );
                    float n = n3(vec3(p.x * 4.0, p.y * 4.0 - Time * 2.0, p.z * 4.0 + Time * 3.0));
                    float flame = exp(-pow(dr / (0.22 + 0.1 * (0.5 - along * 0.5)), 2.0)) * smoothstep(0.35, 0.8, n + 0.35 * exp(-dr * dr / 0.04));
                    col += mix(ColorA, ColorB, 0.3) * flame * smoothstep(0.9, 0.45, length(p - vec3(0.0, 0.05, -0.1))) * dt * 2.6 * alpha;
                }
            }
        }
    }

    // ---- embers falling from the wings -----------------------------------------------------------------------------
    for (int i = 0; i < 20; i++) {
        float fi = float(i);
        float life = fract(Time * (0.35 + 0.05 * mod(fi, 5.0)) + fi * 0.137);
        float side = mod(fi, 2.0) < 0.5 ? 1.0 : -1.0;
        float spread = 0.42 + 0.9 * fract(fi * 0.37);
        vec3 pos = vec3(side * spread, 0.0 - life * 1.2 + 0.1 * sin(fi), -0.11 - 0.48 * fract(fi * 0.53));
        col += mix(ColorA, vec3(1.0, 0.7, 0.3), 0.5) * pointGlow(ro, rd, pos, 0.026, t0, t1) * (1.0 - life) * 1.2 * alpha;
    }

    fragColor = vec4(col * Power, 1.0);
}
