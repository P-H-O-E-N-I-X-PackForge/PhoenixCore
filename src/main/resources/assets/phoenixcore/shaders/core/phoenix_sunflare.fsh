#version 150

// Adapted from nimitz's flare shader (ShaderToy, https://www.shadertoy.com/view/lsSGzy),
// reworked from a screen-space effect (always centered on the viewport) into one anchored to a
// fixed world direction (SunDir), reconstructed via camera ray per pixel the same way as
// void_galaxy.fsh, so it reads as an actual sun sitting in the sky rather than a screen overlay
// that follows the crosshair. The original's texture-sampled noise (iChannel0) is replaced with
// a procedural 2D value noise since there's no equivalent noise-texture input here.
//
// The original ships as a pile of "#define X / // #define X" toggles; only one variant of each
// is ever active in that file, and this ports exactly that resolved combination:
//   MODE = normalize, MODE2 = "r +", MODE3 = "*", DIRECTION = "-", INVERT = "/"

uniform vec2  OutSize;
uniform mat4  InvViewMat;
uniform mat4  InvProjMat;
uniform vec3  SunDir;
uniform vec3  FlareColor;
uniform float CoreRadius;
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

// RAY_BRIGHTNESS/GAMMA were originally ported at the source shader's own values (10.0/5.0), but
// fbmRays here only ever outputs roughly [0, 0.85] (a geometric sum of at most 4 octaves) - miles
// under 10.0. Every value was getting compressed into the bottom ~2% of the smoothstep's range
// before the sqrt() below, so the rays rendered at a small fraction of their intended brightness.
// Retuned to actually span fbmRays' real output range.
const float RAY_BRIGHTNESS = 0.9;
const float GAMMA = 1.0;
const float RAY_DENSITY = 4.5;
const float PROJECTION_SCALE = 8.0;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// Procedural stand-in for the original's texture(iChannel0, x*.01).x noise sample.
float noise(vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i),            hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

mat2 m2 = mat2(0.80, 0.60, -0.60, 0.80);

// The flaring generator - 4 iterations instead of the source shader's 6. This only runs inside
// the facing-angle cone below, but that cone still covers a meaningful chunk of the screen
// whenever you're looking generally toward the sun, so its per-pixel cost still matters.
float fbmRays(vec2 p) {
    float z = 2.0;
    float rz = -0.05;
    p *= 0.25;
    for (int i = 1; i < 4; i++) {
        rz += abs((noise(p) - 0.5) * 2.0) / z;
        z *= 2.0;
        p = p * 2.0 * m2;
    }
    return rz;
}

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 viewDir = InvProjMat * vec4(ndc, 1.0, 1.0);
    viewDir /= viewDir.w;
    vec3 rayDir = normalize((InvViewMat * vec4(viewDir.xyz, 0.0)).xyz);

    vec3 sunDir = normalize(SunDir);
    float facing = dot(rayDir, sunDir);

    // Behind/far off-axis from the sun - nothing to draw, and the perpendicular-projection
    // trick below only stays well-behaved for a reasonably tight cone around sunDir anyway.
    // Widened back out to 0.5 (~60 degrees) - 0.82 (~35 degrees) combined with the tiny edge-fade
    // band below it used to mean you had to look almost exactly at one fixed point in the sky to
    // see anything at all, which combined with the brightness bug above is why this was barely
    // seeable in practice.
    if (facing < 0.5) {
        fragColor = vec4(0.0);
        return;
    }

    // Perpendicular basis around the sun direction, projecting the ray onto that local 2D
    // plane - this is what stands in for the original's literal screen-space fragCoord, so the
    // flare pattern is anchored to a sky direction instead of the viewport center.
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), sunDir));
    vec3 up = cross(sunDir, right);
    vec2 uv = vec2(dot(rayDir, right), dot(rayDir, up)) * PROJECTION_SCALE;

    float t = -Time * 0.33; // DIRECTION = "-"
    float r = length(uv);
    vec2 nuv = normalize(uv + 1e-5);
    float x = dot(nuv, vec2(0.5, 0.0)) + t;
    float y = dot(nuv, vec2(0.0, 0.5)) + t;

    // val = fbm(vec2(MODE2 y * ray_density, MODE2 x MODE3 ray_density)) with MODE2="r +", MODE3="*"
    float val = fbmRays(vec2(r + y * RAY_DENSITY, r + x * RAY_DENSITY));
    val = smoothstep(GAMMA * 0.02 - 0.1, RAY_BRIGHTNESS + (GAMMA * 0.02 - 0.1) + 0.001, val);
    val = sqrt(val);

    // col = val INVERT vec3(red,green,blue) with INVERT="/", then col = 1.-col - channels that
    // divide by a small denominator (blue) blow past 1 and clip out first, which is what gives
    // the red/orange-dominant fire ramp instead of a neutral gray burst.
    vec3 col = val / FlareColor;
    col = 1.0 - col;

    // Bright core blowout near the sun's exact center, same shape as the original's
    // audio-reactive "rad - 266.667*r" mix, just driven by a fixed CoreRadius instead.
    col = mix(col, vec3(1.0), CoreRadius - 266.667 * r);

    // Matches the 0.5 discard threshold above, fading in gradually over that whole wider cone
    // instead of only in a thin sliver right at the edge.
    float edgeFade = smoothstep(0.5, 0.85, facing);
    float alpha = clamp(max(val, CoreRadius - 266.667 * r) * edgeFade, 0.0, 1.0);

    fragColor = vec4(clamp(col, 0.0, 1.0), alpha);
}
