#version 150

// A black hole drawn on a camera-facing billboard: the event horizon, a photon ring, an accretion disk lying in a
// tilted plane (intersected analytically per pixel, so it is a true ellipse with a hidden back half), a lensed copy
// of the far side of the disk arching over and under the hole, and - for quasars - relativistic jets along the spin
// axis. Output is premultiplied: colour adds, alpha occludes whatever was behind.
//
// Coordinates are in units of the horizon radius, so the horizon is exactly radius 1.

uniform float Time;
uniform float Extent;       // billboard half-size, in horizon radii
uniform vec3 DiskNormal;    // disk plane normal, in view space
uniform float Jets;         // 1.0 for a quasar
uniform float DiskOuter;    // outer edge of the disk, in horizon radii
uniform float Detail;      // noise layers in the disk (2 - 4)
uniform vec3 HotColor;
uniform vec3 CoolColor;

in vec2 vLocal;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5, total = 0.0;
    for (int i = 0; i < 4; i++) {
        if (float(i) >= Detail) break;
        v += a * noise(p);
        total += a;
        p = p * 2.03 + 11.7;
        a *= 0.5;
    }
    // fewer layers must not make the disk dimmer: scale to the brightness of the full four
    return v / total * 0.9375;
}

const float INNER = 1.7;

// colour of the disk at orbital radius r and angle ang: hot and bright inside, streaked by orbiting turbulence
vec3 diskColor(float r, float ang) {
    float t = clamp((r - INNER) / (DiskOuter - INNER), 0.0, 1.0);
    float spin = Time * 1.7 * pow(r, -1.5);
    float streaks = fbm(vec2((ang - spin) * 2.4, r * 2.2));
    float fine = Detail > 3.5 ? noise(vec2((ang - spin * 1.3) * 9.0, r * 7.0)) : 0.5;
    float b = pow(1.0 - t, 1.5) * (0.45 + 0.9 * streaks) * (0.8 + 0.4 * fine);
    vec3 c = mix(HotColor, CoolColor, pow(t, 0.55));
    return c * b * 2.2;
}

void main() {
    vec2 q = vLocal * Extent;
    float rho = length(q);

    vec3 n = normalize(DiskNormal);
    float nz = abs(n.z) < 0.05 ? (n.z < 0.0 ? -0.05 : 0.05) : n.z;

    // where this pixel's view ray crosses the disk plane (orthographic approximation)
    float zhit = -(n.x * q.x + n.y * q.y) / nz;
    vec3 p = vec3(q, zhit);
    float r = length(p);

    vec3 e1 = cross(n, vec3(0.0, 0.0, 1.0));
    e1 = length(e1) < 0.05 ? vec3(1.0, 0.0, 0.0) : normalize(e1);
    vec3 e2 = cross(n, e1);
    float ang = atan(dot(p, e2), dot(p, e1));

    vec3 col = vec3(0.0);
    float alpha = 0.0;

    // soft halo outside the horizon, the light bent into a faint ring
    col += HotColor * 0.22 * exp(-(rho - 1.0) * 1.4) * step(1.0, rho);

    // the disk's near half, or its far half where it is not hidden behind the hole
    bool front = zhit > 0.0;
    bool hidden = !front && rho < 1.0;
    if (!hidden && r > INNER - 0.25 && r < DiskOuter) {
        float edge = smoothstep(INNER - 0.25, INNER + 0.2, r) * (1.0 - smoothstep(DiskOuter - 1.4, DiskOuter, r));
        // the side moving toward us is brighter
        float doppler = 1.0 + 0.55 * clamp(dot(normalize(p.xy + 1e-4), vec2(0.0, 1.0)) * sign(nz), -1.0, 1.0);
        vec3 d = diskColor(r, ang) * edge * doppler;
        col += d;
        alpha = max(alpha, clamp(dot(d, vec3(0.33)) * 0.8, 0.0, 0.85));
    }

    // lensed image of the far side of the disk, arching over and under the horizon
    float band = smoothstep(1.0, 1.3, rho) * (1.0 - smoothstep(1.7, 3.6, rho));
    if (band > 0.0) {
        vec2 dir = q / max(rho, 1e-4);
        vec2 axis = length(n.xy) > 1e-3 ? normalize(n.xy) : vec2(0.0, 1.0);
        float along = abs(dot(dir, axis));
        float edgeOn = 1.0 - abs(n.z);
        float arch = mix(0.75, pow(along, 1.6), smoothstep(0.0, 0.5, edgeOn));
        float rl = mix(INNER + 0.2, DiskOuter * 0.62, clamp((rho - 1.0) / 2.6, 0.0, 1.0));
        float screenAng = atan(dir.y, dir.x);
        col += diskColor(rl, screenAng) * band * arch * 1.15;
        alpha = max(alpha, band * arch * 0.35);
    }

    // photon ring
    float ring = exp(-pow((rho - 1.1) / 0.05, 2.0));
    col += vec3(1.0, 0.88, 0.65) * ring * 1.6;
    alpha = max(alpha, ring * 0.6);

    // the horizon itself is opaque black, with the near half of the disk drawn over it
    if (rho < 1.0) {
        alpha = 1.0;
        if (!(front && r > INNER - 0.25 && r < DiskOuter)) col = vec3(0.0);
    }

    // jets along the spin axis
    if (Jets > 0.5 && length(n.xy) > 0.04) {
        vec2 ax = normalize(n.xy);
        float t = dot(q, ax);
        float s = dot(q, vec2(-ax.y, ax.x));
        float at = abs(t);
        float w = 0.16 + 0.05 * at;
        float core = exp(-(s * s) / (w * w));
        float pulse = 0.65 + 0.35 * sin(at * 1.6 - Time * 5.5 + (t > 0.0 ? 0.0 : 1.4));
        float jet = core * smoothstep(1.4, 2.8, at) * exp(-at * 0.07) * pulse;
        float cone = exp(-(s * s) / (w * w * 14.0)) * smoothstep(1.4, 3.0, at) * exp(-at * 0.1) * 0.2;
        col += vec3(0.55, 0.75, 1.0) * (jet * 2.0 + cone);
    }

    // fade to nothing at the billboard edge so there is no visible rectangle
    float edgeMask = 1.0 - smoothstep(0.82, 1.0, max(abs(vLocal.x), abs(vLocal.y)));
    col *= edgeMask;
    alpha = rho < 1.0 ? alpha : alpha * edgeMask;

    fragColor = vec4(col, clamp(alpha, 0.0, 1.0));
}
