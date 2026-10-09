#version 150

// Ring plane vertex stage: a unit quad in the XZ plane, scaled to the ring's outer radius by the model matrix.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 vPos;
out vec3 vView;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    vPos = Position.xz;
    vView = view.xyz;
    gl_Position = ProjMat * view;
}
