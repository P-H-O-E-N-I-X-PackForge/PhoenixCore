#version 150

// Sealed-A discipline sky backdrop - an industrial/neon theme: a glowing angular scaffold
// wrapped around the whole sky (like looking up through a superstructure rather than open air)
// over a drifting magenta/cyan smog haze. Camera-ray reconstruction shared with the other
// discipline sky shaders; reuses void_galaxy.vsh as its vertex stage.

uniform vec2  OutSize;
uniform mat4  InvViewMat;
uniform mat4  InvProjMat;
uniform vec3  GridColor;
uniform vec3  HazeColor;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i),            hash(i + vec3(1,0,0)), f.x),
            mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
        mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
            mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y),
        f.z);
}

float fbm(vec3 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p = p * 2.01 + vec3(7.3, 5.9, 11.1);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 viewDir = InvProjMat * vec4(ndc, 1.0, 1.0);
    viewDir /= viewDir.w;
    vec3 rayDir = normalize((InvViewMat * vec4(viewDir.xyz, 0.0)).xyz);

    // Angular scaffold: a grid of glowing lines wrapped around the whole sky sphere via the ray's
    // own spherical angles, so it stays fixed to the world instead of the screen.
    vec2 ang = vec2(atan(rayDir.z, rayDir.x), asin(clamp(rayDir.y, -1.0, 1.0)));
    vec2 gridUv = ang * vec2(6.0, 8.0) + vec2(Time * 0.01, 0.0);
    vec2 gridFrac = abs(fract(gridUv) - 0.5);
    float gridLine = 1.0 - smoothstep(0.0, 0.025, min(gridFrac.x, gridFrac.y));

    // Soft industrial haze/smog drifting behind the scaffold.
    float haze = fbm(rayDir * 2.0 + vec3(Time * 0.01, 0.0, Time * 0.005));
    haze = smoothstep(0.35, 0.85, haze) * 0.4;

    vec3 col = HazeColor * haze + GridColor * gridLine;
    float alpha = clamp(gridLine + haze * 0.6, 0.0, 1.0);

    fragColor = vec4(col, alpha);
}
