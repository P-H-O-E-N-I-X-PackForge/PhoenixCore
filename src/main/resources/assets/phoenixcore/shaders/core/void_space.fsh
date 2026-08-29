#version 150

// Void deep space shader
// Creates cosmic atmosphere with stars and nebula effects

uniform sampler2D Sampler0;

uniform float GameTime;
uniform vec3 CameraPos;
uniform vec2 ScreenSize;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 FragPos;

out vec4 fragColor;

// Starfield generation
float stars(vec2 uv) {
    float x = sin(uv.x * 13.0) * 43758.5453;
    float y = sin(uv.y * 7.0) * 12345.6789;
    float z = sin((uv.x + uv.y) * 11.0) * 99999.999;

    float n = fract(sin(x + y + z) * 43758.5453);

    // Only show bright stars
    return step(0.98, n);
}

// Nebula cloud
float nebula(vec2 uv, float time) {
    float n1 = sin(uv.x * 2.0 + time * 0.1) * cos(uv.y * 2.0 + time * 0.05);
    float n2 = sin((uv.x + uv.y) * 3.0 + time * 0.08);
    return abs(n1 * n2) * 0.3;
}

void main() {
    vec2 uv = texCoord0;
    vec4 texColor = texture(Sampler0, uv);

    // Deep space blue background
    vec3 spaceColor = vec3(0.05, 0.1, 0.2);

    // Add starfield
    float starIntensity = stars(uv * 50.0 + GameTime * 0.01);
    vec3 starColor = vec3(1.0) * starIntensity * 0.8;

    // Add nebula effect
    float nebulaIntensity = nebula(uv * 3.0, GameTime);
    vec3 nebulaColor = mix(
        vec3(0.5, 0.0, 1.0),  // Purple
        vec3(0.0, 1.0, 1.0),  // Cyan
        sin(GameTime * 0.5) * 0.5 + 0.5
    ) * nebulaIntensity;

    // Combine effects
    vec3 finalColor = spaceColor + starColor + nebulaColor;

    // Blend with original texture (for geometry)
    finalColor = mix(texColor.rgb, finalColor, 0.4);

    // Add depth fog (far objects darker)
    float depth = length(FragPos - CameraPos) / 200.0;
    finalColor = mix(finalColor, spaceColor, clamp(depth, 0.0, 0.5));

    fragColor = vec4(finalColor, texColor.a);
}
