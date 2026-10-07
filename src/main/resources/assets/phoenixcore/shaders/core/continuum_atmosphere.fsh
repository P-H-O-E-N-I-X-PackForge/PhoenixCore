#version 150

// Continuum atmosphere: a slightly larger shell drawn additively over the planet. Brightness is a fresnel
// rim (thin at the centre, strong at the limb), fades at the shell's own silhouette so there is no hard
// edge, is dimmer on the night side, and warms toward orange near the terminator.

uniform vec3  SunDir;       // direction TO the sun, view space
uniform vec3  AtmoColor;
uniform float AtmoDensity;

in vec3 vPosView;
in vec3 vNormalView;

out vec4 fragColor;

void main() {
    vec3 normal = normalize(vNormalView);
    float ndv = clamp(dot(normal, normalize(-vPosView)), 0.0, 1.0);
    float sun = dot(normal, normalize(SunDir));

    float rim = pow(1.0 - ndv, 2.2);
    float edgeFade = smoothstep(0.0, 0.12, ndv);
    float lit = smoothstep(-0.35, 0.45, sun);

    float sunset = 1.0 - smoothstep(0.0, 0.35, abs(sun));
    vec3 color = mix(AtmoColor, vec3(1.0, 0.5, 0.2), sunset * 0.6);

    float alpha = rim * edgeFade * AtmoDensity * mix(0.1, 1.0, lit);
    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
