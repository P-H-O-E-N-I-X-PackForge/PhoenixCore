#version 150

// Color inversion shader for Sealed-B reality breaks
// Inverts RGB values in certain zones

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ProjMat;
uniform mat4 ModuleMat;
uniform mat4 ViewMat;

uniform float GameTime;
uniform vec2 ScreenSize;
uniform vec3 InversionZoneCenter;
uniform float InversionZoneRadius;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec2 texCoord2;
in vec3 FragPos;

out vec4 fragColor;

void main() {
    // Sample screen texture
    vec4 texColor = texture(Sampler0, texCoord0);

    // Calculate distance from inversion zone center
    float distToZone = distance(FragPos, InversionZoneCenter);
    float zoneInfluence = smoothstep(InversionZoneRadius + 10.0, InversionZoneRadius - 10.0, distToZone);

    // Invert colors based on zone influence
    vec3 invertedColor = vec3(1.0) - texColor.rgb;
    vec3 finalColor = mix(texColor.rgb, invertedColor, zoneInfluence * 0.8);

    // Add glitch artifacts randomly
    float glitch = sin(GameTime * 10.0 + FragPos.x * 0.1 + FragPos.y * 0.1) * 0.5 + 0.5;
    if (glitch > 0.95 && zoneInfluence > 0.5) {
        // Create digital artifact
        finalColor += vec3(glitch * 0.2) * zoneInfluence;
    }

    fragColor = vec4(finalColor, texColor.a);
}
