#version 150

// Phoenix volcanic glow shader
// Creates orange/red glow with heat shimmer effect

uniform sampler2D Sampler0;

uniform float GameTime;
uniform vec3 CameraPos;
uniform float HeatIntensity;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 FragPos;

out vec4 fragColor;

// Heat distortion function
float heatShimmer(vec3 pos, float time) {
    return sin(pos.x * 0.1 + time) * cos(pos.y * 0.15 + time * 0.7) * 0.02;
}

void main() {
    vec2 uv = texCoord0;

    // Add heat shimmer distortion
    float shimmer = heatShimmer(FragPos, GameTime);
    uv.x += shimmer;
    uv.y += abs(shimmer) * 0.5;

    vec4 texColor = texture(Sampler0, uv);

    // Volcanic orange/red color
    vec3 volcanicColor = vec3(1.0, 0.4, 0.0); // Orange
    vec3 lavaColor = vec3(1.0, 0.1, 0.0);    // Deep red

    // Blend based on brightness
    vec3 blended = mix(lavaColor, volcanicColor, length(texColor.rgb) * 0.5);

    // Add glow intensity
    float glowAmount = HeatIntensity * (0.5 + 0.5 * sin(GameTime * 1.5));
    vec3 finalColor = mix(texColor.rgb, blended, glowAmount * 0.6);

    // Add bloom
    finalColor += blended * glowAmount * 0.4;

    // Heat effect (brighten)
    finalColor = mix(finalColor, vec3(1.0), glowAmount * 0.2);

    fragColor = vec4(finalColor, texColor.a);
}
