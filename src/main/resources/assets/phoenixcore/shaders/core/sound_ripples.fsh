#version 150

// Sound ripples shader for Sculk
// Creates visual ripples from sound events

uniform sampler2D Sampler0;

uniform float GameTime;
uniform vec3 SoundSource;
uniform float SoundIntensity;
uniform vec3 CameraPos;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 FragPos;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);

    // Calculate distance from sound source
    float distFromSource = distance(FragPos, SoundSource);

    // Create ripple wave based on distance
    float ripplePhase = distFromSource - GameTime * 15.0; // Wave speed
    float ripple = sin(ripplePhase * 0.5) * cos(ripplePhase * 0.3);

    // Ripple amplitude decreases with distance
    float rippleAmplitude = exp(-distFromSource * 0.02) * SoundIntensity;

    // Create ripple distortion
    float distortion = ripple * rippleAmplitude * 0.1;

    // Apply distortion to texel
    vec2 rippleUV = texCoord0;
    rippleUV += normalize(FragPos.xz - SoundSource.xz) * distortion;

    // Sample with distorted UVs
    vec4 distortedColor = texture(Sampler0, rippleUV);

    // Cyan ripple color
    vec3 rippleColor = vec3(0.0, 1.0, 0.8);

    // Create ripple visibility
    float rippleVisibility = abs(ripple) * rippleAmplitude;
    rippleVisibility = smoothstep(0.0, 0.1, rippleVisibility);

    // Blend original with ripple effect
    vec3 finalColor = mix(distortedColor.rgb, rippleColor, rippleVisibility * 0.5);

    // Add glow from ripple
    finalColor += rippleColor * rippleVisibility * 0.3;

    fragColor = vec4(finalColor, distortedColor.a);
}
