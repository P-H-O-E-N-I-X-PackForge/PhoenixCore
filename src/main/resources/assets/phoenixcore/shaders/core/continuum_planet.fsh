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
uniform vec3  PalLow;
uniform vec3  PalMid;
uniform vec3  PalHigh;
uniform vec3  OceanDeep;
uniform vec3  OceanShallow;
uniform vec3  EmissiveColor;
uniform float EmissiveAmount;

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

float height(vec3 d) {
    float raw = fbm(d * Scale + vec3(Seed * 17.0), int(Octaves));
    return clamp((raw - 0.2) / 0.6, 0.0, 1.0);
}

vec3 ramp(float t) {
    return t < 0.5 ? mix(PalLow, PalMid, t * 2.0) : mix(PalMid, PalHigh, (t - 0.5) * 2.0);
}

void main() {
    vec3 dir = normalize(vDir);
    vec3 geoNormal = normalize(vNormalView);
    vec3 viewDir = normalize(-vPosView);
    vec3 sun = normalize(SunDir);

    // star / black hole body: self-lit granulation, darker toward the limb, no surface lighting
    if (Kind > 1.5) {
        float granule = fbm(dir * 6.0 + vec3(Time * 0.05, Seed, 0.0), 5);
        float facing = pow(max(dot(geoNormal, viewDir), 0.0), 0.45);
        vec3 body = PalMid * (0.75 + 0.6 * granule);
        fragColor = vec4(mix(PalLow, body, facing), 1.0);
        return;
    }

    vec3 albedo;
    vec3 normalObj = dir;
    float water = 0.0;
    float land = 1.0;

    if (Kind > 0.5) {
        // gas giant: latitude bands warped by turbulence
        float warp = fbm(dir * vec3(2.0, 5.0, 2.0) + vec3(Seed * 9.0 + Time * 0.01), 5);
        float lat = dir.y + (warp - 0.5) * 0.35;
        float bands = 0.5 + 0.5 * sin(lat * Scale * 6.2831853);
        float fine = fbm(dir * vec3(3.0, 24.0, 3.0) + vec3(Seed * 3.0), 4);
        albedo = ramp(clamp(bands * 0.7 + fine * 0.5 - 0.1, 0.0, 1.0));
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

    vec3 normalView = normalize(vRot * normalObj);
    // a soft terminator on the geometric normal, so bumps never create a hard day/night seam
    float terminator = smoothstep(-0.08, 0.22, dot(geoNormal, sun));
    float light = max(dot(normalView, sun), 0.0) * terminator;

    vec3 color = albedo * (0.025 + light * 1.15);

    vec3 reflected = reflect(-sun, normalView);
    color += vec3(1.0, 0.95, 0.85) * pow(max(dot(reflected, viewDir), 0.0), 60.0) * water * terminator * 0.9;

    if (EmissiveAmount > 0.0 && land > 0.5) {
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

    fragColor = vec4(color, 1.0);
}
