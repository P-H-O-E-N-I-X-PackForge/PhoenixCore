#version 150

// The Sculk Cortex as a living brain-sphere. A membrane 1.5 blocks out from the middle breathes slowly, carrying a net of
// veins that are the borders of cells wrapped round the ball in three dimensions, lit by waves that travel through them,
// with a node shining in each cell it remembers. In the middle a nucleus throbs, and filaments of light run from it out
// to the membrane like nerves. Tendrils reach out of the membrane into the air, spores drift round it, and a rim of light
// stands round its edge. After it has eaten, a ring of light goes round the ball. It turns red and stutters when it
// starves.
// Only rays that touch the ball pay for it: the membrane is marched only through its thin shell (the front and the back of
// it), the filaments only through the inside, and the nucleus and spores are closed form.

uniform float Time;
uniform float Power;
uniform float Seed;
uniform float Scale;
uniform float Detail;   // 0.3 to 1, how many steps the marches take
uniform vec3 Centre;
uniform vec3 ColorA;   // the veins
uniform vec3 ColorB;   // the pulse and the nodes
uniform vec4 P0;       // x how bright, y starving 0/1, z how many veins 0..1
uniform vec4 P1;       // x pulse age 0..1 (-1 for none), y how many nodes 0..1

in vec3 wpos;
out vec4 fragColor;

const vec3 HALF = vec3(1.8);
const float SHELL = 1.5;
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

vec3 h33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
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

// the part of a ray inside a sphere about the origin: x > y means it misses
vec2 sphereSeg(vec3 ro, vec3 rd, float R) {
    float b = dot(ro, rd);
    float h = b * b - (dot(ro, ro) - R * R);
    if (h < 0.0) return vec2(1.0, 0.0);
    float s = sqrt(h);
    return vec2(-b - s, -b + s);
}

float pointGlow(vec3 ro, vec3 rd, vec3 pos, float size, float t0, float t1) {
    vec3 v = pos - ro;
    float s = dot(v, rd);
    if (s < t0 - 0.3 || s > t1 + 0.3) return 0.0;
    float d2 = dot(v, v) - s * s;
    return exp(-d2 / (size * size));
}

// the membrane, shaded at a point in the shell: veins, waves, nodes, tendrils, and the ring after eating
vec3 emitShell(vec3 p) {
    float len = length(p);
    vec3 dir = p / max(len, 0.001);
    float breathe = SHELL + 0.05 * sin(Time * 1.3 + 3.0 * n3(dir * 1.6 + Seed));
    float s = len - breathe;
    float starving = P0.y;
    float density = P0.z;
    vec3 col = vec3(0.0);

    if (abs(s) < 0.2) {
        vec3 q = dir * (2.6 + 3.0 * density) + vec3(Seed, Time * 0.03, 0.0);
        vec3 g = floor(q);
        vec3 f = fract(q);
        float d1 = 8.0;
        float d2 = 8.0;
        vec3 id = vec3(0.0);
        for (int k = -1; k <= 1; k++) {
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    vec3 o = vec3(float(i), float(j), float(k));
                    vec3 h = h33(g + o);
                    vec3 pt = o + 0.5 + 0.4 * sin(Time * 0.5 + TAU * h);
                    float d = length(pt - f);
                    if (d < d1) { d2 = d1; d1 = d; id = g + o; }
                    else if (d < d2) { d2 = d; }
                }
            }
        }
        float vein = exp(-(d2 - d1) * 12.0);
        float layer = exp(-s * s / 0.006);
        float cellRand = h31(id);
        float wave = pow(0.5 + 0.5 * sin(dir.y * 6.0 + dir.x * 3.0 - Time * 1.8 + cellRand * TAU), 5.0);
        float flicker = mix(1.0, 0.55 + 0.45 * sin(Time * 9.0 + cellRand * 20.0), starving);
        float node = step(h31(id + 4.0), P1.y) * exp(-d1 * d1 * 40.0) * (0.7 + 0.3 * sin(Time * 2.2 + cellRand * 9.0));
        // a faint membrane between the veins, so the ball has a skin
        float skin = layer * (0.05 + 0.08 * (1.0 - vein)) * (0.6 + 0.4 * n3(dir * 5.0 + Time * 0.1));
        col += ColorA * (vein * (0.3 + 1.3 * wave) * flicker * layer * 3.4 + skin * 2.0);
        col += ColorB * (node * layer * 4.0 + node * exp(-s * s / 0.05) * 1.0);
    }

    // tendrils reaching out of the membrane into the air
    if (s > -0.05 && s < 0.45) {
        float reach = 0.45 * pow(n3(dir * 6.0 + Seed), 2.0);
        float tendril = pow(n3(dir * 9.0 + vec3(0.0, Time * 0.1, Seed)), 9.0) * smoothstep(reach, 0.0, s);
        col += ColorA * tendril * 8.0 * (0.6 + 0.4 * sin(Time * 3.0 + dir.y * 9.0));
    }

    // a ring that goes round the ball from one side to the other after it has eaten
    if (P1.x >= 0.0) {
        float u = P1.x;
        vec3 axis = normalize(vec3(sin(Seed), 0.6, cos(Seed)));
        float ang = acos(clamp(dot(dir, axis), -1.0, 1.0));
        col += ColorB * exp(-pow((ang - u * 3.1416) / 0.12, 2.0)) * (1.0 - u) * exp(-s * s / 0.02) * 4.5;
    }
    return col;
}

// the filaments inside: nerves running from the nucleus out to the membrane, with pulses travelling along them
vec3 emitInside(vec3 p) {
    float len = length(p);
    if (len > SHELL - 0.08 || len < 0.1) return vec3(0.0);
    vec3 dir = p / len;
    float n = n3(dir * 5.0 + vec3(Seed, 0.0, 0.0));
    float ridge = pow(n3(dir * 8.0 + 2.0 * n), 10.0);
    float travel = 0.5 + 0.5 * sin(len * 7.0 - Time * 2.6 + h31(floor(dir * 8.0)) * TAU);
    float fade = smoothstep(0.1, 0.5, len) * smoothstep(SHELL - 0.08, SHELL - 0.4, len);
    return mix(ColorA, ColorB, 0.4) * ridge * (0.3 + 1.4 * travel) * fade * 3.6;
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

    float t0 = max(hit.x, 0.0);
    float t1 = min(hit.y, sceneDistance(rd) / k);
    if (t1 <= t0) discard;
    float starving = P0.y;
    float jit = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    vec3 col = vec3(0.0);

    // ---- the nucleus and the rim of light: closed form --------------------------------------------------------------
    {
        float throb = 0.5 + 0.25 * sin(Time * 1.6) + 0.15 * sin(Time * 4.1);
        col += mix(ColorA, ColorB, 0.4) * pointGlow(ro, rd, vec3(0.0), 0.28, t0, t1) * throb * 1.6;
        col += ColorB * pointGlow(ro, rd, vec3(0.0), 0.09, t0, t1) * throb * 1.4;
        vec2 sg = sphereSeg(ro, rd, SHELL);
        if (sg.x < sg.y) {
            vec3 pn = ro + rd * sg.x;
            float rim = pow(1.0 - abs(dot(normalize(pn), rd)), 3.0);
            col += ColorA * rim * 0.45 * (1.0 + 0.4 * sin(Time * 1.6));
        }
        // spores drifting round the outside
        for (int i = 0; i < 10; i++) {
            float fi = float(i);
            float a = Time * (0.12 + 0.08 * h11(fi)) + fi * 2.399;
            float el = (h11(fi + 4.0) - 0.5) * 2.2;
            float rr = 1.75 + 0.35 * h11(fi + 8.0);
            vec3 pos = vec3(cos(a) * rr * sqrt(1.0 - el * el * 0.2), el * 1.2, sin(a) * rr * sqrt(1.0 - el * el * 0.2));
            col += ColorB * pointGlow(ro, rd, pos, 0.035, t0, t1) * (0.5 + 0.5 * sin(Time * 2.0 + fi * 3.0)) * 0.9;
        }
    }

    // ---- the membrane: only its thin shell is marched, the near side and the far side ------------------------------
    {
        vec2 outer = sphereSeg(ro, rd, SHELL + 0.45);
        vec2 inner = sphereSeg(ro, rd, SHELL - 0.2);
        if (outer.x < outer.y) {
            float aS = max(outer.x, t0);
            float aE = min(outer.y, t1);
            int steps = max(5, int(12.0 * Detail));
            for (int seg = 0; seg < 2; seg++) {
                float s0 = aS;
                float s1 = aE;
                if (inner.x < inner.y) {
                    if (seg == 0) s1 = min(aE, inner.x);
                    else s0 = max(aS, inner.y);
                } else if (seg == 1) {
                    break;
                }
                if (s1 > s0) {
                    float dt = (s1 - s0) / float(steps);
                    for (int i = 0; i < 12; i++) {
                        if (i >= steps) break;
                        vec3 p = ro + rd * (s0 + (float(i) + jit) * dt);
                        col += emitShell(p) * dt;
                    }
                }
            }
        }
    }

    // ---- the inside: filaments, marched only through the sphere --------------------------------------------------
    {
        vec2 sg = sphereSeg(ro, rd, SHELL - 0.08);
        if (sg.x < sg.y) {
            float s0 = max(sg.x, t0);
            float s1 = min(sg.y, t1);
            if (s1 > s0) {
                int steps = max(5, int(12.0 * Detail));
                float dt = (s1 - s0) / float(steps);
                for (int i = 0; i < 12; i++) {
                    if (i >= steps) break;
                    vec3 p = ro + rd * (s0 + (float(i) + jit) * dt);
                    col += emitInside(p) * dt;
                }
            }
        }
    }

    // a starving cortex stutters as a whole
    col *= mix(1.0, 0.7 + 0.3 * sin(Time * 17.0), starving);
    fragColor = vec4(col * P0.x * Power, 1.0);
}
