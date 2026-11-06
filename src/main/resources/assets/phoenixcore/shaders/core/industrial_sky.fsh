#version 150

// Industrial sky shader for Sealed-A
// Creates smog/haze with neon glow through clouds

uniform sampler2D Sampler0;

uniform float GameTime;
uniform vec2 ScreenSize;
uniform float SmogDensity;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 FragPos;

out vec4 fragColor;

// Procedural cloud/smog generation
float clouds(vec2 uv, float time) {
    float x = sin(uv.x * 2.0 + time * 0.3) * 0.5 + 0.5;
    float y = cos(uv.y * 1.5 + time * 0.2) * 0.5 + 0.5;
    float z = sin((uv.x + uv.y) * 2.5 + time * 0.15);

    return (x + y + abs(z)) / 3.0;
}

void main() {
    vec2 uv = texCoord0;
    vec4 texColor = texture(Sampler0, uv);

    // Industrial smog color (gray-brown)
    vec3 smogColor = vec3(0.3, 0.25, 0.2);

    // Generate cloud/smog pattern
    float cloudPattern = clouds(uv * 3.0, GameTime);

    // Create layered fog effect
    float fog1 = clouds(uv * 2.0 + vec2(GameTime * 0.1, 0.0), GameTime * 0.5);
    float fog2 = clouds(uv * 4.0 + vec2(0.0, GameTime * 0.05), GameTime * 0.3);

    float totalFog = (fog1 + fog2) / 2.0;

    // Blend with smog
    vec3 finalColor = mix(texColor.rgb, smogColor, totalFog * SmogDensity);

    // Add neon glow through smog (distant lights)
    vec3 neonGlows = vec3(0.0);

    // Red glow (left)
    float redGlow = sin(uv.x * 10.0 + GameTime) * 0.5 + 0.5;
    neonGlows.r = redGlow * 0.3;

    // Cyan glow (right)
    float cyanGlow = cos(uv.x * 12.0 + GameTime * 0.8) * 0.5 + 0.5;
    neonGlows.g = cyanGlow * 0.2;
    neonGlows.b = cyanGlow * 0.3;

    // Add neon through smog
    finalColor += neonGlows * (1.0 - totalFog * 0.7);

    // Add horizon gradient
    float horizon = abs(uv.y - 0.5) * 2.0;
    finalColor = mix(finalColor, vec3(0.2, 0.15, 0.1), horizon * 0.3);

    fragColor = vec4(finalColor, texColor.a);
}
