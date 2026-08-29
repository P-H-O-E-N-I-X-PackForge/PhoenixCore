#version 150

// Void discipline sky centerpiece - a supermassive black hole sitting at the galaxy's core
// (DisciplineSkyEffects anchors it to the same GalaxyDir void_galaxy.fsh already glows toward),
// replacing the orbiting planet meshes VoidSkyRenderer used to draw there. Ported from
// phoenix_black_hole.fsh (gravitational lensing + Doppler-shifted accretion disk against an
// already-rendered scene), recolored gold/orange -> purple/magenta to match this project's
// "purple phoenix" palette, and now lensing the actual void_galaxy nebula (captured to InSampler
// right before this draws) instead of a generic background.
//
// Unlike the other discipline sky shaders, this one works in plain screen-space UV rather than
// reconstructing a camera ray - DisciplineSkyEffects projects the fixed sky direction to a screen
// position on the CPU side (same technique as the dormant BlackHoleSkyLayer/WorldFXManager
// system) and hands it over as BlackHoleScreenPos, so the lensing math itself can stay identical
// to the original in-world/menu versions.

uniform sampler2D InSampler;

uniform vec2  OutSize;
uniform vec2  BlackHoleScreenPos;   // screen UV [0,1]
uniform float EventHorizonRadius;   // in screen-height units
uniform float LensingStrength;      // bending coefficient
uniform float AccretionDiskBrightness;
uniform float AspectRatio;          // width / height
uniform float Time;

in  vec2 texCoord;
out vec4 fragColor;

float hash1(float n) {
    return fract(sin(n) * 43758.5453123);
}

void main() {
    vec2 uv = texCoord;

    vec2 toHole = BlackHoleScreenPos - uv;
    toHole.x *= AspectRatio;
    float dist = length(toHole);

    // Event horizon: pure gravitational void.
    if (dist < EventHorizonRadius) {
        float edge = smoothstep(EventHorizonRadius * 0.95, EventHorizonRadius, dist);
        fragColor = vec4(0.0, 0.0, 0.0, 1.0 - edge * 0.01);
        return;
    }

    // Gravitational lensing: classic 1/r^2 bending toward the hole, warping the already-rendered
    // void_galaxy nebula (InSampler) around the hole instead of a straight sample.
    float deflection = LensingStrength / (dist * dist);
    vec2 deflDir = normalize(toHole);
    deflDir.x /= AspectRatio;
    vec2 lensedUV = clamp(uv + deflDir * deflection, 0.001, 0.999);
    vec4 sky = texture(InSampler, lensedUV);

    // Gravitational "redshift" reworked into a violet shift to match the purple palette - light
    // climbing out of the well leans blue/violet instead of the original's red.
    float gravDepth = EventHorizonRadius / max(dist, 0.0001);
    sky.b = min(sky.b * (1.0 + gravDepth * 0.4), 1.0);
    sky.r = min(sky.r * (1.0 + gravDepth * 0.2), 1.0);
    sky.g = sky.g * max(1.0 - gravDepth * 0.3, 0.0);

    // Photon sphere: thin, very bright ring of captured light at ~1.5x the horizon radius.
    float photonR     = EventHorizonRadius * 1.5;
    float photonWidth = EventHorizonRadius * 0.18;
    float photon = exp(-pow((dist - photonR) / photonWidth, 2.0) * 12.0);
    sky.rgb += photon * vec3(0.85, 0.60, 1.00) * 2.2;

    // Accretion disk: hot lavender-white inner edge fading to deep purple-magenta outward, with
    // Doppler brightening on the approaching limb and hashed clumpy structure around the ring.
    float diskInner = EventHorizonRadius * 1.15;
    float diskOuter = EventHorizonRadius * 4.0;

    if (dist > diskInner && dist < diskOuter) {
        float t = (dist - diskInner) / (diskOuter - diskInner);

        float angle = atan(toHole.y, toHole.x / AspectRatio);
        float animAngle = angle + Time * 0.4;

        float doppler   = 0.5 + 0.5 * cos(animAngle);
        float structure = 0.6 + 0.4 * sin(animAngle * 3.0 + hash1(floor(dist * 120.0)) * 6.28);
        float radialFade = pow(1.0 - t, 1.8) * smoothstep(0.0, 0.05, t);

        vec3 innerCol = vec3(0.95, 0.85, 1.00) * 4.0;
        vec3 outerCol = vec3(0.55, 0.08, 0.85) * 1.2;
        vec3 diskCol  = mix(innerCol, outerCol, t);

        diskCol *= radialFade * (0.4 + 0.6 * doppler) * structure * AccretionDiskBrightness;
        sky.rgb += diskCol;
    }

    fragColor = vec4(sky.rgb, 1.0);
}
