#version 150

// Basic vertex shader used by all fragment shaders
// Passes through position, UV, and color data

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 UV0;
in vec2 UV1;
in vec2 UV2;
in vec4 Color;

out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;
out vec4 vertexColor;
out vec3 FragPos;

void main() {
    // Transform position to clip space
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // Pass through UV coordinates
    texCoord0 = UV0;
    texCoord1 = UV1;
    texCoord2 = UV2;

    // Pass through vertex color
    vertexColor = Color;

    // Pass world position for distance calculations
    FragPos = Position;
}
