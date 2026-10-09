#version 150

// Continuum planet surface. Everything is procedural from the object-space direction of the fragment, so
// one mesh serves every body: terrain heights from fbm noise, an ocean level with a specular highlight,
// polar ice, a scrolling cloud shell, emissive cracks (volcanic worlds) and a banded mode for gas giants.

uniform vec3  SunDir;         // direction TO the sun, view space
uniform float Time;
uniform float Seed;
uniform float Scale;          // terrain feature frequency (gas giants: number of bands)
uniform float OceanLevel;     // 0 = no ocean
uniform float PolarIce;       // 0..1 latitude coverage
uniform float CloudCover;     // 0..1
uniform float CloudOffset;    // radians, spins the cloud shell independently of the surface
uniform float Bump;           // relief strength
uniform float Kind;           // 0 = terrestrial, 1 = gas giant, 2 = star (self-lit)
uniform float Octaves;
uniform float Voxel;          // 0 = smooth sphere; otherwise square cells per cube face
uniform vec3  PalLow;
uniform vec3  PalMid;
uniform vec3  PalHigh;
uniform vec3  OceanDeep;
uniform vec3  OceanShallow;
uniform vec3  EmissiveColor;
uniform float EmissiveAmount;
uniform float Style;          // 0 plain, 1 crystal, 2 veins, 3 storm, 4 chrome, 5 shatter, 6 aurora

in vec3 vDir;
in vec3 vPosView;
in vec3 vNormalView;
in mat3 vRot;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i + vec3(0, 0, 0)), hash(i + vec3(1, 0, 0)), f.x),
            mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), f.x), f.y),
        mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), f.x),
            mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), f.x), f.y),
        f.z);
}

float fbm(vec3 p, int octaves) {
    float amplitude = 0.5;
    float sum = 0.0;
    for (int i = 0; i < octaves; i++) {
        sum += amplitude * noise(p);
        p = p * 2.02 + vec3(13.1, 7.7, 3.3);
        amplitude *= 0.5;
    }
    return sum;
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

// cellular noise: x = distance to the nearest feature point, y = to the second nearest; cell = the nearest's cell
vec2 voronoi(vec3 p, out vec3 cell) {
    vec3 ip = floor(p);
    vec3 fp = fract(p);
    float d1 = 8.0;
    float d2 = 8.0;
    cell = ip;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                vec3 g = vec3(float(x), float(y), float(z));
                vec3 r = g + hash33(ip + g) - fp;
                float d = dot(r, r);
                if (d < d1) {
                    d2 = d1;
                    d1 = d;
                    cell = ip + g;
                } else if (d < d2) {
                    d2 = d;
                }
            }
        }
    }
    return vec2(sqrt(d1), sqrt(d2));
}

float height(vec3 d) {
    float raw = fbm(d * Scale + vec3(Seed * 17.0), int(Octaves));
    return clamp((raw - 0.2) / 0.6, 0.0, 1.0);
}

vec3 ramp(float t) {
    return t < 0.5 ? mix(PalLow, PalMid, t * 2.0) : mix(PalMid, PalHigh, (t - 0.5) * 2.0);
}

void main() {
    // Kind 3: a black hole's horizon, plain black
    if (Kind > 2.5) {
        fragColor = vec4(PalLow, 1.0);
        return;
    }

    bool cube = Voxel > 0.5;
    vec3 cubeNormal = vec3(0.0);
    float cellEdge = 1.0;
    float cellJitter = 1.0;
    vec3 dir;
    vec3 geoNormal;
    if (cube) {
        // the mesh is a cube: snap the position to the centre of its square cell and sample the terrain there,
        // and light the face flat, so the planet reads as blocks
        vec3 a = abs(vDir);
        cubeNormal = (a.x >= a.y && a.x >= a.z) ? vec3(sign(vDir.x), 0.0, 0.0) :
                (a.y >= a.z ? vec3(0.0, sign(vDir.y), 0.0) : vec3(0.0, 0.0, sign(vDir.z)));
        float halfCells = Voxel * 0.5;
        vec3 scaled = vDir * 0.9999 * halfCells;
        vec3 cell = (floor(scaled) + 0.5) / halfCells;
        dir = normalize(cell);
        geoNormal = normalize(vRot * cubeNormal);

        vec3 f = fract(scaled);
        vec3 edgeDist = min(f, 1.0 - f) + step(0.5, abs(cubeNormal)) * 10.0;
        cellEdge = 1.0 - 0.22 * (1.0 - smoothstep(0.0, 0.09, min(edgeDist.x, min(edgeDist.y, edgeDist.z))));
        cellJitter = 0.93 + 0.14 * hash(cell * 7.31 + vec3(Seed));
    } else {
        dir = normalize(vDir);
        geoNormal = normalize(vNormalView);
    }
    vec3 viewDir = normalize(-vPosView);
    vec3 sun = normalize(SunDir);

    // star / black hole body: self-lit granulation, darker toward the limb, no surface lighting
    if (Kind > 1.5) {
        float granule = fbm(dir * 6.0 + vec3(Time * 0.05, Seed, 0.0), 5);
        float facing = pow(max(dot(geoNormal, viewDir), 0.0), 0.45);
        vec3 body = PalMid * (0.75 + 0.6 * granule);
        if (cube) body *= cellJitter * cellEdge;
        fragColor = vec4(mix(PalLow, body, facing), 1.0);
        return;
    }

    // ---- crystal: faceted shards that split the light into colours and glitter ----
    if (!cube && Style > 0.5 && Style < 1.5) {
        vec3 cid;
        vec2 v = voronoi(dir * Scale * 1.6 + vec3(Seed * 7.0), cid);
        vec3 fr = hash33(cid * 1.7 + vec3(Seed));
        vec3 facet = normalize(dir + (fr - 0.5) * 1.1);
        vec3 nView = normalize(vRot * facet);
        vec3 base = ramp(height(dir));
        float hue = fract(fr.x + dot(nView, viewDir) * 0.6 + Time * 0.02);
        vec3 prism = 0.5 + 0.5 * cos(6.2831853 * (hue + vec3(0.0, 0.33, 0.67)));
        float term = smoothstep(-0.12, 0.25, dot(geoNormal, sun));
        float diffuse = max(dot(nView, sun), 0.0) * term;
        vec3 col = mix(base, prism, 0.38) * (0.06 + diffuse * 1.1);
        float spec = pow(max(dot(reflect(-sun, nView), viewDir), 0.0), 22.0);
        float twinkle = step(0.82, fract(fr.y * 13.0 + Time * (0.4 + fr.z)));
        col += vec3(1.0) * spec * term * (1.2 + 3.0 * twinkle);
        float edge = 1.0 - smoothstep(0.0, 0.06, v.y - v.x);
        col += mix(vec3(0.8, 0.95, 1.0), prism, 0.5) * edge * 0.55 * (0.4 + 0.6 * term);
        col += EmissiveColor * EmissiveAmount * (0.35 + 0.65 * (0.5 + 0.5 * sin(Time * 1.3 + fr.x * 20.0))) * 0.35;
        fragColor = vec4(col, 1.0);
        return;
    }

    // ---- chrome: a mirror that reflects a starfield, a sunlit horizon and a hard glint ----
    if (!cube && Style > 3.5 && Style < 4.5) {
        float h0 = height(dir);
        float e0 = 0.012;
        vec3 g0 = vec3(height(dir + vec3(e0, 0.0, 0.0)) - h0, height(dir + vec3(0.0, e0, 0.0)) - h0,
                       height(dir + vec3(0.0, 0.0, e0)) - h0) / e0;
        vec3 t0 = g0 - dot(g0, dir) * dir;
        vec3 nView = normalize(vRot * normalize(dir - t0 * Bump * 0.12));
        vec3 r = reflect(-viewDir, nView);
        vec3 env = mix(PalLow, PalHigh, 0.5 + 0.5 * r.y);
        env += vec3(pow(noise(r * 70.0 + vec3(Seed)), 18.0) * 6.0);
        env += PalMid * pow(max(dot(r, sun), 0.0), 8.0) * 1.4;
        float fres = pow(1.0 - max(dot(nView, viewDir), 0.0), 3.0);
        float term = smoothstep(-0.2, 0.25, dot(geoNormal, sun));
        vec3 col = env * (0.35 + 0.65 * fres) * (0.25 + 0.75 * term);
        col += vec3(1.0, 0.95, 0.85) * pow(max(dot(reflect(-sun, nView), viewDir), 0.0), 90.0) * 1.6 * term;
        fragColor = vec4(col, 1.0);
        return;
    }

    // ---- shatter: slabs of the world slip sideways, flip colour and flicker in and out ----
    if (!cube && Style > 4.5 && Style < 5.5) {
        float slabCoord = (dir.y * 0.8 + dir.x * 0.5 + dir.z * 0.3) * 5.0 + Seed;
        float slab = floor(slabCoord);
        float tick = floor(Time * 1.5);
        float jitter = hash(vec3(slab, tick, Seed));
        float moves = step(0.55, hash(vec3(slab, floor(Time * 0.75), 3.0)));
        float angle = (jitter - 0.5) * 1.4 * moves;
        float cs = cos(angle);
        float sn = sin(angle);
        vec3 d2 = vec3(cs * dir.x + sn * dir.z, dir.y, -sn * dir.x + cs * dir.z);
        vec3 a = ramp(height(d2));
        a = mix(a, vec3(1.0) - a, step(0.88, hash(vec3(slab * 3.1, tick, 9.0))));
        a = mix(a, a.gbr, step(0.7, hash(vec3(slab, 5.0, floor(Time * 0.7)))));
        float edgeD = min(fract(slabCoord), 1.0 - fract(slabCoord));
        float seam = 1.0 - smoothstep(0.0, 0.05, edgeD);
        float presence = 0.82 + 0.18 * sin(Time * 0.9);
        if (hash(floor(dir * 30.0) + vec3(floor(Time * 6.0) * 7.0)) > presence) discard;
        float term = smoothstep(-0.08, 0.22, dot(geoNormal, sun));
        vec3 col = a * (0.04 + max(dot(geoNormal, sun), 0.0) * term * 1.1);
        col += EmissiveColor * seam * (0.6 + EmissiveAmount);
        fragColor = vec4(col, 1.0);
        return;
    }

    vec3 albedo;
    vec3 normalObj = dir;
    float water = 0.0;
    float land = 1.0;
    float stormMask = 0.0;
    float eyeCore = 0.0;

    if (Kind > 0.5) {
        // gas giant: latitude bands warped by turbulence
        // a great storm: the bands are wound into a spiral around a fixed point on the planet
        vec3 dg = dir;
        if (Style > 2.5 && Style < 3.5) {
            vec3 eye = normalize(vec3(sin(Seed * 3.1), 0.28 + 0.12 * sin(Seed), cos(Seed * 3.1)));
            float a = acos(clamp(dot(dir, eye), -1.0, 1.0));
            stormMask = smoothstep(0.62, 0.0, a);
            eyeCore = smoothstep(0.12, 0.0, a);
            float rot = stormMask * 5.5 + Time * 0.15 * stormMask;
            float cs = cos(rot);
            float sn = sin(rot);
            dg = dir * cs + cross(eye, dir) * sn + eye * dot(eye, dir) * (1.0 - cs);
        }
        float warp = fbm(dg * vec3(2.0, 5.0, 2.0) + vec3(Seed * 9.0 + Time * 0.01), 5);
        float lat = dg.y + (warp - 0.5) * 0.35;
        float bands = 0.5 + 0.5 * sin(lat * Scale * 6.2831853);
        float fine = fbm(dg * vec3(3.0, 24.0, 3.0) + vec3(Seed * 3.0), 4);
        albedo = ramp(clamp(bands * 0.7 + fine * 0.5 - 0.1, 0.0, 1.0));
        albedo = mix(albedo, ramp(0.95), stormMask * 0.25);
        albedo *= 1.0 - 0.45 * eyeCore;
        land = 0.0;
    } else {
        float h = height(dir);
        if (h < OceanLevel) {
            albedo = mix(OceanDeep, OceanShallow, smoothstep(OceanLevel - 0.35, OceanLevel, h));
            water = 1.0;
            h = OceanLevel;
        } else {
            albedo = ramp((h - OceanLevel) / max(1.0 - OceanLevel, 0.001));

            // relief: perturb the normal against the height gradient (tangent part only)
            float e = 0.012;
            vec3 grad = vec3(height(dir + vec3(e, 0.0, 0.0)) - h,
                             height(dir + vec3(0.0, e, 0.0)) - h,
                             height(dir + vec3(0.0, 0.0, e)) - h) / e;
            vec3 tangent = grad - dot(grad, dir) * dir;
            normalObj = normalize(dir - tangent * Bump * 0.25);
        }

        float iceEdge = 1.0 - PolarIce;
        float ice = PolarIce > 0.001 ? smoothstep(iceEdge, iceEdge + 0.06, abs(dir.y) + (h - 0.5) * 0.12) : 0.0;
        albedo = mix(albedo, vec3(0.93, 0.96, 1.0), ice);
        water *= 1.0 - ice;
    }

    if (cube) {
        normalObj = cubeNormal;
        albedo *= cellJitter * cellEdge;
    }
    vec3 normalView = normalize(vRot * normalObj);
    // a soft terminator on the geometric normal, so bumps never create a hard day/night seam
    float terminator = smoothstep(-0.08, 0.22, dot(geoNormal, sun));
    float light = max(dot(normalView, sun), 0.0) * terminator;

    vec3 color = albedo * (0.025 + light * 1.15);

    vec3 reflected = reflect(-sun, normalView);
    color += vec3(1.0, 0.95, 0.85) * pow(max(dot(reflected, viewDir), 0.0), 60.0) * water * terminator * 0.9;

    // veins: glowing seams that flow and pulse, visible on the night side too
    if (Style > 1.5 && Style < 2.5 && land > 0.5) {
        float flow = Time * 0.04;
        float r1 = fbm(dir * Scale * 2.0 + vec3(Seed * 31.0, flow, 0.0), 5);
        float veins = pow(1.0 - abs(r1 * 2.0 - 1.0), 7.0);
        float pulse = 0.55 + 0.45 * sin(Time * 1.4 + fbm(dir * 3.0 + vec3(Seed), 3) * 12.0);
        color += EmissiveColor * veins * pulse * (0.5 + EmissiveAmount) * (1.0 - water);
    }

    if (EmissiveAmount > 0.0 && land > 0.5 && !(Style > 1.5 && Style < 2.5)) {
        float crack = 1.0 - smoothstep(0.0, 0.03, abs(fbm(dir * Scale * 2.3 + vec3(Seed * 31.0), 5) - 0.5));
        color += EmissiveColor * crack * EmissiveAmount * (1.0 - water);
    }

    if (CloudCover > 0.001 && Kind < 0.5) {
        float cs = cos(CloudOffset);
        float sn = sin(CloudOffset);
        vec3 cd = vec3(cs * dir.x + sn * dir.z, dir.y, -sn * dir.x + cs * dir.z);
        float c = fbm(cd * 3.2 + vec3(Seed * 5.0), 6);
        float threshold = 0.78 - CloudCover * 0.4;
        float clouds = smoothstep(threshold, threshold + 0.14, c) * 0.95;
        float cloudLight = max(dot(geoNormal, sun), 0.0) * terminator;
        color = mix(color, vec3(0.97, 0.98, 1.0) * (0.03 + cloudLight * 1.15), clouds);
    }

    // aurora: curtains of light over both poles
    if (Style > 5.5 && Style < 6.5 && Kind < 0.5) {
        float lat = abs(dir.y);
        float ang = atan(dir.z, dir.x);
        float curtain = fbm(vec3(ang * 3.0 + Time * 0.25, lat * 14.0, Time * 0.1 + Seed), 4);
        float band = smoothstep(0.62, 0.78, lat) * (1.0 - smoothstep(0.86, 0.97, lat));
        float aur = pow(curtain, 1.6) * band * 2.2;
        vec3 c1 = EmissiveAmount > 0.0 ? EmissiveColor : vec3(0.2, 1.0, 0.5);
        vec3 aurColor = mix(c1, c1.bgr, smoothstep(0.7, 0.95, lat));
        color += aurColor * aur * (1.1 - 0.7 * light);
    }

    fragColor = vec4(color, 1.0);
}
