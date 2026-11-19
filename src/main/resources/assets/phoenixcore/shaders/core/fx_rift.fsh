#version 150

// A rift in the world. The tear lies in a real plane, standing upright and turned to face the way the furnace faces, so
// walking round it makes it thinner and thinner until it is a bright seam seen edge on. Inside it is a void with three
// layers of stars at different depths that slide against each other as you move, so it has depth. Its edge is a bright
// line one or two pixels wide whatever the distance, with a halo and lightning crawling along it, and cracks run out
// into the sky round it. What is behind it is read back from a copy of the screen and bent toward it. Close to it the
// picture tears into bands that slip sideways and split into colour.

uniform sampler2D InSampler;

uniform vec2 ScreenSize;   // pixels
uniform float Time;        // seconds
uniform vec2 RiftPos;      // where its middle is on the screen, 0 to 1
uniform vec2 RiftSize;     // x unused, y half its height on the screen, in screen heights
uniform vec3 Centre;       // its middle, relative to the camera
uniform vec3 Normal;       // which way its plane faces, horizontal
uniform float HalfH;       // half its height, in blocks
uniform float WidthRatio;  // half its width over half its height
uniform float Growth;      // how open it is, 0 to 1
uniform float Lens;        // how hard it bends what is behind it
uniform float Glitch;      // 0 to 1, how much the picture breaks up
uniform vec3 ColorA;       // the colour of its edge
uniform vec3 ColorB;       // the white-hot part of the edge
uniform float Seed;
uniform float Power;

in vec3 wpos;
out vec4 fragColor;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

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
        p = p * 2.03 + vec2(7.1, 3.7);
        a *= 0.5;
    }
    return s;
}

// the distance to the tear, negative inside it. p is in half-heights, w its half width
float riftDist(vec2 p, float w) {
    float taper = pow(max(1.0 - p.y * p.y, 0.0), 0.7);
    float zig = (vnoise(vec2(p.y * 2.2 + Seed, Time * 0.12)) - 0.5) * 0.6 +
                (vnoise(vec2(p.y * 7.0 + Seed * 3.1, Time * 0.35)) - 0.5) * 0.2;
    float jag = (vnoise(vec2(p.y * 22.0 + Seed * 5.0, 1.7)) - 0.5) * 0.09;
    float d = abs(p.x - zig * taper) - w * taper + jag * taper;
    return max(d, abs(p.y) - 1.0);
}

vec3 starTint(float h) {
    vec3 hot = vec3(0.6, 0.75, 1.0);
    vec3 mid = vec3(1.0, 0.97, 0.9);
    vec3 cool = vec3(1.0, 0.68, 0.4);
    return mix(mix(hot, mid, smoothstep(0.0, 0.5, h)), cool, smoothstep(0.55, 1.0, h));
}

// one layer of stars on a grid: each cell may hold one, of its own colour and brightness, and the bright ones get
// diffraction spikes. They twinkle at their own rates.
vec3 stars(vec2 sp, float seed, float density, float size) {
    vec2 cell = floor(sp);
    float h = hash21(cell + seed);
    if (h < 1.0 - density) return vec3(0.0);
    vec2 off = (vec2(hash21(cell + seed + 3.0), hash21(cell + seed + 7.0)) - 0.5) * 0.6;
    vec2 d = fract(sp) - 0.5 - off;
    float mag = (h - (1.0 - density)) / density;
    float core = exp(-dot(d, d) / (size * size * (0.4 + mag)));
    float spikes = (exp(-abs(d.x) * 38.0 / (0.5 + mag)) * exp(-abs(d.y) * 7.0) +
                    exp(-abs(d.y) * 38.0 / (0.5 + mag)) * exp(-abs(d.x) * 7.0)) * mag * mag * 0.7;
    float tw = 0.6 + 0.4 * sin(Time * (2.0 + hash21(cell + 11.0) * 4.0) + h * 50.0);
    return starTint(hash21(cell + seed + 13.0)) * (core * (0.35 + 1.7 * mag) + spikes) * tw;
}

// what is seen through the tear: a deep sky of coloured gas, dark dust lanes, a band of crowded stars, three layers of
// stars sliding against each other as the view leans, and now and then a shooting star
vec3 space(vec2 p, vec2 lean) {
    vec2 q = p + lean * 0.06;
    vec2 wv = vec2(fbm(q * 2.3 + Seed + Time * 0.02), fbm(q * 2.3 + 7.7 - Time * 0.015));
    float n1 = fbm(q * vec2(2.0, 1.4) + wv * 1.9);
    float n2 = fbm(q * 3.4 + wv * 2.6 + 11.0);
    float n3 = fbm(q * 2.0 - wv * 2.0 + 4.0);
    vec3 gas = mix(vec3(0.02, 0.01, 0.1), ColorA * 0.85, smoothstep(0.35, 0.8, n1));
    gas = mix(gas, vec3(0.95, 0.28, 0.62), smoothstep(0.55, 0.9, n2) * 0.6);
    gas = mix(gas, vec3(0.15, 0.72, 0.98), smoothstep(0.6, 0.95, n3) * 0.4);
    float dust = smoothstep(0.45, 0.78, fbm(q * vec2(4.2, 2.6) + wv * 1.4));
    gas *= 1.0 - 0.6 * dust;

    // a band of crowded stars running across, like a galaxy seen edge on
    float across = dot(q, normalize(vec2(0.45, 1.0)));
    float band = exp(-across * across / 0.07) * (0.5 + n1);

    vec3 col = gas * (0.55 + 0.7 * band);
    col += stars(q * 15.0 + lean * 1.2, 1.0, 0.10 + 0.1 * band, 0.14);
    col += stars(q * 27.0 + lean * 2.6 + 3.7, 5.0, 0.12 + 0.14 * band, 0.12) * 0.8;
    col += stars(q * 49.0 + lean * 5.0 + 8.3, 9.0, 0.15 + 0.2 * band, 0.11) * 0.55;
    col += vec3(0.8, 0.85, 1.0) * band * 0.05 * (0.5 + fbm(q * 40.0));

    // a shooting star every few seconds
    float ph = fract(Time * 0.17 + Seed * 0.1);
    vec2 a0 = vec2(-0.3, 0.9);
    vec2 a1 = vec2(0.3, -0.9);
    vec2 head = mix(a0, a1, clamp(ph * 1.5, 0.0, 1.0));
    vec2 ab = a1 - a0;
    float along = clamp(dot(q - head, -ab) / dot(ab, ab), 0.0, 0.22);
    float dist = length(q - (head - ab * along));
    col += vec3(1.0, 0.95, 0.9) * exp(-dist * dist / 0.0003) * exp(-along * 12.0) * step(ph, 0.66) * (1.0 - ph * 0.5);
    return col;
}

void main() {
    vec2 suv = gl_FragCoord.xy / ScreenSize;
    float aspect = ScreenSize.x / ScreenSize.y;

    // on the screen: how far from the rift, for the bending and the tearing
    vec2 q = (suv - RiftPos) * vec2(aspect, 1.0) / max(RiftSize.y, 0.0001);
    q.y *= 0.55;
    float qlen = length(q);
    float region = smoothstep(3.6, 2.6, qlen);
    if (region <= 0.0) discard;

    // in the world: where this pixel's ray meets the plane of the tear
    vec3 rd = normalize(wpos);
    // the plane faces the way the furnace faces, but turns part of the way toward you, so it is never edge on
    vec3 Nf = normalize(Normal);
    vec3 toCam = normalize(vec3(-Centre.x, 0.0, -Centre.z) + vec3(0.0001, 0.0, 0.0));
    if (dot(Nf, toCam) < 0.0) Nf = -Nf;
    vec3 N = normalize(mix(Nf, toCam, 0.4));
    vec3 T = normalize(cross(vec3(0.0, 1.0, 0.0), N));
    float dn = dot(rd, N);
    float facing = abs(dn);
    float safeDn = (dn < 0.0 ? -1.0 : 1.0) * max(facing, 0.02);
    float tt = dot(Centre, N) / safeDn;
    vec3 hit = rd * tt - Centre;
    vec2 p = vec2(dot(hit, T), hit.y) / max(HalfH, 0.001);
    float w = WidthRatio;
    float d = riftDist(p, w);
    float behind = step(0.0, tt);

    // lensing: what is behind is read from further out, so it seems drawn into the tear
    float r = qlen + 0.15;
    // a gentle bend: strong right at the tear, fading smoothly to nothing, never far enough to smear the sky into streaks
    float pull = min(Lens * Growth * 0.10 / (r * r), 0.28) * smoothstep(3.4, 0.4, qlen);
    // a ripple that leaves the rift every few seconds and runs outward, bending the sky as it passes
    float rip = fract(Time * 0.22);
    float ripple = exp(-pow((qlen - rip * 3.4) / 0.22, 2.0)) * (1.0 - rip) * Growth;
    pull += ripple * 0.06;
    vec2 dir = -vec2(q.x, q.y / 0.55) / max(qlen, 0.0001);
    vec2 offs = dir * pull * RiftSize.y;
    offs.x /= aspect;

    // tearing, only near the rift, and finer than before so it reads as a glitch, not a rainbow
    float zone = smoothstep(1.9, 0.4, qlen);
    float tick = floor(Time * 12.0);
    float band = floor(suv.y * (40.0 + 40.0 * hash11(tick)));
    float slip = Glitch * step(0.82, hash11(band * 1.7 + tick)) * zone;
    float shiftX = (hash11(band + tick * 3.1) - 0.5) * 0.05 * slip;
    vec2 uv = suv + offs + vec2(shiftX, 0.0);
    float split = (0.0004 + 0.0025 * clamp(pull / 0.28, 0.0, 1.0) + 0.004 * slip) * region;
    vec3 bg = vec3(texture(InSampler, clamp(uv + vec2(split, 0.0), 0.001, 0.999)).r,
                   texture(InSampler, clamp(uv, 0.001, 0.999)).g,
                   texture(InSampler, clamp(uv - vec2(split, 0.0), 0.001, 0.999)).b);
    bg *= 1.0 - 0.1 * Glitch * zone * step(0.5, fract(suv.y * ScreenSize.y * 0.5));

    // inside: three layers of stars at three depths, sliding as the view leans, over a slow cloud
    vec2 lean = vec2(dot(rd, T), rd.y);
    float inside = smoothstep(0.0, -0.025, d) * behind;
    vec3 voidCol = vec3(0.0);
    if (inside > 0.001) {
        voidCol = space(p, lean) * 1.15;
        voidCol += ColorB * exp(d * 6.0) * 0.2;     // brighter toward the edge on the inside
    }

    // the edge, one or two pixels wide whatever the distance, with a halo and lightning along it
    float fw = max(fwidth(d), 0.0005);
    float edge = abs(d);
    float rim = exp(-edge / (fw * 1.3));
    float halo = exp(-edge * 7.0) * 0.4 + exp(-max(d, 0.0) * 2.5) * 0.12;
    float arcs = pow(vnoise(vec2(p.y * 30.0 - Time * 7.0, d * 36.0 + Seed)), 6.0) * exp(-edge * 15.0);
    vec3 rimCol = mix(ColorA, ColorB, 0.5 + 0.5 * sin(p.y * 7.0 + Time * 2.0)) * (rim * 1.4 + arcs * 1.6) +
                  ColorB * halo * 0.6;

    // cracks running out into the sky round the tear
    float ridge = abs(fbm(p * vec2(3.2, 2.1) + vec2(Seed, 0.0)) - 0.5);
    float cracks = exp(-ridge * 38.0) * smoothstep(1.4, 0.2, abs(p.x)) * smoothstep(1.0, 0.3, abs(p.y)) *
                   smoothstep(0.0, 0.1, d);
    rimCol += ColorA * cracks * 0.55 * (0.6 + 0.4 * sin(Time * 2.0 + p.y * 9.0));

    vec3 col = mix(bg, voidCol, inside);
    col += rimCol * Power * Growth * behind * smoothstep(1.6, 0.9, abs(p.y));

    // beams out of the two tips of the tear, flickering
    float ay = abs(p.y);
    if (ay > 0.95 && ay < 3.2) {
        float tipX = (vnoise(vec2(sign(p.y) * 4.0 + Seed, Time * 0.6)) - 0.5) * 0.08;
        float spread = 0.012 + 0.035 * (ay - 0.95);
        float beam = exp(-pow((p.x - tipX) / spread, 2.0)) * exp(-(ay - 0.95) * 1.7);
        float flick = 0.6 + 0.4 * vnoise(vec2(ay * 9.0 - Time * 8.0, sign(p.y) + Seed));
        col += mix(ColorA, ColorB, 0.6) * beam * flick * 0.9 * Growth * Power * behind;
    }

    // shards of broken space drifting off the edge, thin and bright, turning to catch the light
    if (d > -0.05 && d < 0.6 && ay < 1.1) {
        vec2 sc = p * vec2(9.0, 7.0) + vec2(Time * 0.04, 0.0);
        vec2 cell = floor(sc);
        float hs = hash21(cell + Seed);
        if (hs > 0.78) {
            vec2 cen = (vec2(hash21(cell + 2.0), hash21(cell + 5.0)) - 0.5) * 0.5;
            vec2 f = fract(sc) - 0.5 - cen;
            float ang = hash21(cell + 9.0) * 3.1416 + Time * (0.4 + hash21(cell + 4.0));
            vec2 rf = vec2(cos(ang) * f.x + sin(ang) * f.y, -sin(ang) * f.x + cos(ang) * f.y);
            float shard = smoothstep(0.09, 0.0, abs(rf.x) * 3.2) * smoothstep(0.32, 0.0, abs(rf.y));
            float lit = exp(-max(d, 0.0) * 4.0) * (0.4 + 0.6 * abs(sin(ang * 2.0 + Time * 2.0)));
            col += mix(ColorA, ColorB, 0.7) * shard * lit * 1.6 * Growth * Power * behind;
        }
    }

    // motes pulled in from far off, falling toward the tear along their own lines
    {
        vec2 q2 = vec2(q.x, q.y / 0.55);
        for (int i = 0; i < 9; i++) {
            float fi = float(i);
            float ph = fract(Time * (0.13 + 0.07 * hash11(fi)) + hash11(fi + 3.0));
            float a = hash11(fi + 7.0) * 6.2831 + ph * 1.2;
            float rad = mix(3.0, 0.25, ph * ph);
            vec2 pos = vec2(cos(a), sin(a)) * rad;
            float dd = length(q2 - pos);
            col += mix(ColorA, ColorB, 0.5) * exp(-dd * dd / 0.0012) * sin(ph * 3.1416) * 1.2 * Growth * Power;
        }
    }

    // a pulsing aura about the whole thing
    col += mix(ColorA, vec3(0.8, 0.4, 0.9), 0.3 * sin(Time * 0.7) + 0.3) * exp(-qlen * qlen / 1.5) * (0.10 + 0.05 * sin(Time * 1.9)) *
           Growth * Power;
    col += ColorB * ripple * 0.25 * Power;

    fragColor = vec4(col, region);
}
