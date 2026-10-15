#version 150

// A camera-facing quad for one nebula puff; the fragment stage does the cloud.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 vLocal;

void main() {
    vLocal = Position.xy;
    gl_Position = ProjMat * (ModelViewMat * vec4(Position, 1.0));
}
