#version 150

// Sculk bioluminescent shader
// Creates glowing cyan atmosphere

uniform sampler2D Sampler0;

uniform vec3 PlayerPos;
uniform float GameTime;
uniform vec2 ScreenSize;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 FragPos;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);

    // Calculate distance from player
    float distToPlayer = distance(FragPos, PlayerPos);
    float glowRange = 24.0;

    // Glow intensity based on distance
    float glowIntensity = smoothstep(glowRange, 0.0, distToPlayer);

    // Cyan glow color
    vec3 glowColor = vec3(0.0, 1.0, 0.533) * 0.9; // Cyan

    // Add pulsing effect
    float pulse = sin(GameTime * 2.0) * 0.2 + 0.8;
    glowIntensity *= pulse;

    // Blend glow with original texture
    vec3 finalColor = texColor.rgb + glowColor * glowIntensity;

    // Add subtle glow bloom
    float bloom = glowIntensity * 0.3;
    finalColor += vec3(bloom);

    fragColor = vec4(finalColor, texColor.a);
}
