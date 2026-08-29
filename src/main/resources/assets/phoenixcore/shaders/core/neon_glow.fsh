#version 150

// Neon glow shader for Sealed-A megacity
// Creates intense cyberpunk neon colors

uniform sampler2D Sampler0;

uniform float GameTime;
uniform vec2 ScreenSize;
uniform float NeonIntensity;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 FragPos;

out vec4 fragColor;

// Neon color mapping
vec3 getNeonColor(vec3 originalColor) {
    // Map colors to neon intensities
    float r = length(originalColor.r);
    float g = length(originalColor.g);
    float b = length(originalColor.b);

    // Enhance color channels
    vec3 neonColor = originalColor * 1.5;

    // Add neon saturation
    float brightness = (r + g + b) / 3.0;
    neonColor = mix(neonColor, normalize(neonColor) * brightness, 0.3);

    return neonColor;
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);

    // Get neon version of color
    vec3 neonColor = getNeonColor(texColor.rgb);

    // Add pulsating glow
    float pulse = sin(GameTime * 3.0) * 0.3 + 0.7;
    float glow = NeonIntensity * pulse;

    // Apply neon effect
    vec3 finalColor = neonColor + neonColor * glow;

    // Add bloom/halo effect
    float bloom = length(neonColor) * 0.2 * glow;
    finalColor += vec3(bloom);

    // Clamp to avoid oversaturation
    finalColor = clamp(finalColor, 0.0, 2.0);

    fragColor = vec4(finalColor, texColor.a);
}
