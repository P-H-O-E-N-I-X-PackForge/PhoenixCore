#version 150

// The Null Well seen straight on, drawn flat on a square, so it reads as a picture of a black hole rather than a sphere.
//   - a black horizon with a double photon ring round it, thin and bright
//   - an accretion disc of spiral arms and torn filaments, brighter on the side that turns toward you (doppler beaming),
//     shading from white-hot at the inside to deep red at the outside, with a hot spot that orbits
//   - a field of background stars bent round the hole, with the arcs of an Einstein ring
//   - streams of matter falling in along spirals, with bright knots in them, quicker the harder it is fed
//   - jets from the poles when it is drawn on: a cone with knots running up it and shock rings
//   - diffraction spikes off the glare, with a hint of colour fringing
// Output is premultiplied: colour already multiplied by alpha, the black horizon opaque.

uniform float Time;
uniform float Horizon;   // the black disc, as a fraction of half the square
uniform float Energy;    // how bright the disc is
uniform float Feed;      // 0 to 1, how fast it is fed: quickens the arms and the streams
uniform float Draw;      // 0 to 1, how hard it is drawn on: the jets
uniform float Power;
uniform float Seed;
uniform vec3 ColorA;     // the outer disc
uniform vec3 ColorB;     // the hot inner disc

uniform sampler2D DepthSampler;   // the depth of everything solid, taken before water and glass were drawn

in vec2 texCoord;
out vec4 fragColor;

const float TAU = 6.2831853;

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float a = 0.5;
    float s = 0.0;
    for (int i = 0; i < 4; i++) {
        s += a * vnoise(p);
        p = p * 2.03 + vec2(5.3, 1.9);
        a *= 0.5;
    }
    return s;
}

// a field of stars at a point, each a small bright dot of its own colour, on a grid
vec3 stars(vec2 sp, float density) {
    vec2 cell = floor(sp);
    float h = hash21(cell + 17.0);
    if (h < 1.0 - density) return vec3(0.0);
    vec2 off = (vec2(hash21(cell + 3.0), hash21(cell + 7.0)) - 0.5) * 0.6;
    vec2 d = fract(sp) - 0.5 - off;
    float mag = (h - (1.0 - density)) / density;
    vec3 tint = mix(vec3(0.7, 0.8, 1.0), vec3(1.0, 0.85, 0.65), hash21(cell + 11.0));
    return tint * exp(-dot(d, d) / (0.012 + 0.02 * mag)) * (0.4 + 1.4 * mag);
}

void main() {
    // hidden by solid things in front of it, but not by water or glass
    if (gl_FragCoord.z > texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r + 1.0e-5) discard;
    vec2 p = (texCoord - 0.5) * 2.0;
    float r = length(p);
    if (r > 1.0) discard;
    float ang = atan(p.y, p.x);
    vec2 dirv = r > 0.0001 ? p / r : vec2(1.0, 0.0);

    float rh = Horizon;
    float spin = Time * (0.5 + 2.2 * Feed);
    vec3 col = vec3(0.0);
    float alpha = 0.0;

    // ---- lensed stars: the background pulled toward the hole, so near the ring it smears into arcs -----------------
    {
        float bend = rh * rh * 2.6 / max(r * r, rh * rh * 0.8);    // how far the picture is pulled in, falling with distance
        vec2 lp = p - dirv * bend * 1.0;                           // where this pixel is looking
        float stretch = 1.0 + 3.0 * exp(-pow((r - rh * 1.6) / (rh * 0.7), 2.0));
        vec2 sp = lp * 20.0;
        vec3 st = stars(sp, 0.12) + stars(sp * 1.9 + 4.0, 0.1) * 0.7;
        st *= smoothstep(rh * 1.0, rh * 1.5, r) * smoothstep(1.0, 0.8, r);
        st *= 0.55 + 0.7 * exp(-pow((r - rh * 1.7) / (rh * 0.5), 2.0)) * stretch;
        col += st;
        alpha = max(alpha, clamp(max(st.r, max(st.g, st.b)), 0.0, 1.0) * 0.9);
    }

    // ---- the accretion disc ---------------------------------------------------------------------------------------
    float inner = rh * 1.2;
    float outer = rh * 3.8;
    float rr = (r - inner) / (outer - inner);
    float mask = smoothstep(0.0, 0.06, rr) * smoothstep(1.0, 0.35, rr);
    float arms = 0.5 + 0.5 * sin(ang * 3.0 - log(max(r, 0.001)) * 5.5 + spin + Seed);
    float arms2 = 0.5 + 0.5 * sin(ang * 5.0 + log(max(r, 0.001)) * 7.0 - spin * 1.4);
    vec2 circ = vec2(cos(ang), sin(ang));
    // turbulence read off a circle so there is no seam, and dragged round faster near the middle
    float swirl = spin * (1.5 - rr);
    float turb = fbm(circ * 2.2 + vec2(r * 9.0 - Time * (0.4 + Feed), r * 4.0 + Time * 0.2));
    float fil = pow(fbm(vec2(cos(ang + swirl * 0.3), sin(ang + swirl * 0.3)) * 3.0 + vec2(r * 22.0, Time * 0.3)), 3.0);
    // the side turning toward you is brighter, the other dimmer
    float doppler = 0.55 + 0.45 * cos(ang - 0.6 - Time * 0.1);
    float heat = pow(1.0 - clamp(rr, 0.0, 1.0), 1.5);
    float disc = mask * (0.2 + 0.7 * arms + 0.3 * arms2) * (0.4 + turb + 1.4 * fil) * Energy * (0.45 + 1.1 * doppler);
    vec3 discCol = mix(mix(ColorA * vec3(1.0, 0.55, 0.6), ColorA, smoothstep(0.7, 0.2, rr)), ColorB, heat * 0.9);
    col += discCol * disc * 0.95;
    alpha = max(alpha, clamp(disc, 0.0, 1.0));

    // a hot spot orbiting in the disc
    float hs = Time * 1.4;
    vec2 hsPos = vec2(cos(hs), sin(hs)) * rh * 1.9;
    float hsd = length(p - hsPos);
    col += ColorB * exp(-hsd * hsd / (rh * rh * 0.06)) * 1.6 * Energy;
    alpha = max(alpha, exp(-hsd * hsd / (rh * rh * 0.06)) * 0.7);

    // ---- the double photon ring ------------------------------------------------------------------------------------
    float ring1 = exp(-pow((r - rh * 1.06) / (rh * 0.035), 2.0));
    float ring2 = exp(-pow((r - rh * 1.17) / (rh * 0.018), 2.0)) * 0.55;
    vec3 ringCol = vec3(1.0, 0.95, 0.9);
    col += ringCol * (ring1 * 1.7 + ring2 * 1.2) * (0.75 + 0.25 * doppler);
    alpha = max(alpha, clamp(ring1 + ring2, 0.0, 1.0));

    // ---- streams of matter falling in along spirals, with bright knots in them -----------------------------------
    float streams = 0.0;
    for (int k = 0; k < 4; k++) {
        float a0 = float(k) * 1.5708 + spin * 0.6;
        float spiral = a0 + log(max(r, 0.001) / rh) * 1.7;
        float da = abs(atan(sin(ang - spiral), cos(ang - spiral)));
        float along = fract(log(max(r, 0.001) / rh) * 0.9 - Time * (0.25 + 0.6 * Feed) + float(k) * 0.27);
        float knots = pow(max(0.0, sin(along * TAU)), 4.0);
        float body = smoothstep(0.14, 0.0, da) * smoothstep(rh * 1.3, rh * 4.4, r) * smoothstep(1.0, 0.7, r);
        streams += body * (0.35 + 1.3 * knots) * Feed;
    }
    col += ColorA * streams * 0.9 + ColorB * streams * streams * 0.3;
    alpha = max(alpha, clamp(streams * 0.7, 0.0, 1.0));

    // ---- jets from the poles while it is drawn on ---------------------------------------------------------------------
    {
        float ay = abs(p.y);
        float cone = 0.025 + 0.05 * ay;
        float beam = exp(-pow(p.x / cone, 2.0)) * smoothstep(rh * 0.9, rh * 1.5, ay) * (1.0 - smoothstep(0.7, 1.0, ay));
        float knotsJ = 0.5 + 0.5 * sin(ay * 26.0 - Time * 15.0);
        float shock = exp(-pow((fract(ay * 3.0 - Time * 1.2) - 0.5) / 0.08, 2.0)) * exp(-pow(p.x / (cone * 2.5), 2.0));
        float jet = Draw * (beam * (0.4 + 0.9 * knotsJ) + shock * 0.5);
        col += mix(ColorA, vec3(1.0), 0.5) * jet * 1.7;
        alpha = max(alpha, clamp(jet, 0.0, 1.0));
    }

    // ---- a soft glow round the whole thing, and spikes off the glare ---------------------------------------------------
    float glow = exp(-max(r - rh, 0.0) * 2.0 / max(rh, 0.01)) * 0.2 * Energy;
    col += ColorB * glow;
    alpha = max(alpha, glow);
    {
        float spike = (exp(-abs(p.x) * 26.0 / (0.5 + rh)) * exp(-abs(p.y) * 3.2) +
                       exp(-abs(p.y) * 26.0 / (0.5 + rh)) * exp(-abs(p.x) * 3.2)) * 0.22 * Energy;
        float fringe = 0.5 + 0.5 * sin(r * 40.0);
        col += vec3(1.0, 0.95 - 0.2 * fringe, 0.9 + 0.1 * fringe) * spike * smoothstep(rh, rh * 1.4, r);
        alpha = max(alpha, spike * 0.6);
    }

    // ---- the black horizon hides what is behind, the light is drawn over it -----------------------------------------
    float hole = smoothstep(rh, rh * 0.95, r);
    float edge = smoothstep(1.0, 0.82, r);
    col *= (1.0 - hole) * edge * Power;
    alpha = max(alpha * (1.0 - hole) * edge * min(Power, 1.0), hole * edge * min(Power * 1.5, 1.0));
    fragColor = vec4(col, alpha);
}
