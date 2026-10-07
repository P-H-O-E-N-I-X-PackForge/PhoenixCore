#version 150

// Soft additive glow for a quad: a hot core plus a falling halo. Used for stars, unknown signals and the
// corona around bodies on the Continuum map. The tint's alpha scales the whole thing.

in vec2 uv;
in vec4 tint;

out vec4 fragColor;

void main() {
    float r = length(uv - 0.5) * 2.0;
    if (r >= 1.0) discard;

    float core = exp(-r * r * 9.0);
    float halo = pow(1.0 - r, 2.6);
    float a = (core + halo * 0.55) * tint.a;
    fragColor = vec4(tint.rgb * a, a);
}
