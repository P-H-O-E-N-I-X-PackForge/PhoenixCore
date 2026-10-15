#version 150

// Gravity bridge visualization shader
// Shows invisible walkways in void

uniform sampler2D Sampler0;

uniform float GameTime;
uniform vec3 BridgeCenter;
uniform float BridgeRadius;
uniform vec3 CameraPos;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 FragPos;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);

    // Distance to bridge center
    float distToBridge = distance(FragPos, BridgeCenter);

    // Bridge glow - circular falloff
    float bridgeGlow = smoothstep(BridgeRadius + 5.0, BridgeRadius - 5.0, distToBridge);

    // Create pulsing effect
    float pulse = sin(GameTime * 3.0) * 0.3 + 0.7;
    bridgeGlow *= pulse;

    // Bridge color: cyan-white
    vec3 bridgeColor = mix(
        vec3(0.0, 1.0, 1.0),      // Cyan
        vec3(1.0, 1.0, 1.0),      // White
        sin(GameTime * 1.5) * 0.5 + 0.5
    );

    // Add energy grid pattern
    float grid = sin(FragPos.x * 10.0 + GameTime) * sin(FragPos.z * 10.0 + GameTime);
    grid = abs(grid) * 0.3;

    // Combine bridge effect with original texture
    vec3 finalColor = mix(texColor.rgb, bridgeColor, bridgeGlow * 0.6);

    // Add grid overlay
    finalColor += bridgeColor * grid * bridgeGlow;

    // Add outer glow
    float outerGlow = smoothstep(BridgeRadius + 20.0, BridgeRadius + 5.0, distToBridge);
    finalColor += bridgeColor * outerGlow * 0.3;

    fragColor = vec4(finalColor, texColor.a);
}
