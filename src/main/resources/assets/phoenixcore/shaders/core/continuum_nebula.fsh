#version 150

// One puff of a nebula: a domain-warped cloud with brighter filaments and hot cores, fading softly to nothing at the
// quad's edge. Output is premultiplied so overlapping puffs add up.

uniform float Time;
uniform float Seed;
uniform float Density;
uniform float Octaves;
uniform vec3 Color1;
uniform vec3 Color2;

in vec2 vLocal;
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

float fbm(vec3 p) {
    float amplitude = 0.5;
    float sum = 0.0;
    for (int i = 0; i < 6; i++) {
        if (float(i) >= Octaves) break;
        sum += amplitude * noise(p);
        p = p * 2.03 + vec3(13.1, 7.7, 3.3);
        amplitude *= 0.5;
    }
    return sum;
}

void main() {
    float r = length(vLocal);
    if (r >= 1.0) discard;

    vec3 p = vec3(vLocal * 2.0, Seed * 7.3 + Time * 0.012);
    vec3 q = vec3(fbm(p + vec3(0.0, 1.7, 9.2)), fbm(p + vec3(8.3, 2.8, 4.1)), 0.0);
    float n = fbm(p + q * 1.8);

    float falloff = 1.0 - smoothstep(0.1, 1.0, r);
    float body = pow(clamp(n * 1.7 - 0.38, 0.0, 1.0), 1.3) * falloff;
    float ridge = 1.0 - abs(fbm(p * 2.0 + q * 2.5) * 2.0 - 1.0);
    float filament = pow(ridge, 6.0) * 0.35 * falloff;

    float d = (body + filament) * Density;
    vec3 col = mix(Color1, Color2, clamp(n + q.x * 0.6, 0.0, 1.0));
    col += vec3(1.0, 0.92, 0.85) * pow(max(n - 0.6, 0.0), 2.0) * 3.0 * falloff;

    float a = clamp(d, 0.0, 1.0) * 0.5;
    fragColor = vec4(col * a, a);
}
