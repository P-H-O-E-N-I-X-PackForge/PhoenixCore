#version 150

// Reality distortion shader for Sealed-B
// Creates wave distortion and glitch effects

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform float GameTime;
uniform vec2 ScreenSize;
uniform float DistortionStrength;
uniform float WaveFrequency;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 FragPos;

out vec4 fragColor;

// Simplex noise function (pseudocode for wave generation)
float noise(vec2 p) {
    return sin(p.x * WaveFrequency + GameTime) * cos(p.y * WaveFrequency + GameTime * 0.7);
}

void main() {
    vec2 uv = texCoord0;

    // Create wave distortion
    float wave = sin((uv.y + GameTime * 0.5) * 10.0) * 0.02;
    float distortion = noise(uv) * DistortionStrength;

    // Apply distortion to UV coordinates
    uv.x += wave * distortion;
    uv.y += cos((uv.x + GameTime * 0.3) * 8.0) * 0.01 * distortion;

    // Clamp to valid texture coordinates
    uv = clamp(uv, 0.0, 1.0);

    // Sample with distorted coordinates
    vec4 texColor = texture(Sampler0, uv);

    // Add glitch effect
    vec3 glitchColor = texColor.rgb;
    float glitchAmount = sin(GameTime * 20.0 + FragPos.x) * 0.5 + 0.5;

    if (glitchAmount > 0.9) {
        // Shift color channels randomly
        glitchColor = vec3(
            texture(Sampler0, uv + vec2(0.02, 0.0)).r,
            glitchColor.g,
            texture(Sampler0, uv - vec2(0.02, 0.0)).b
        );

        // Add digital artifacts
        glitchColor += vec3(glitchAmount * 0.3);
    }

    // Add scan line effect
    float scanLines = sin(FragPos.y * 100.0) * 0.05;
    glitchColor -= scanLines;

    fragColor = vec4(glitchColor, texColor.a);
}
