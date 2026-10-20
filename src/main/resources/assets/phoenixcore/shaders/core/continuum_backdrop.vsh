
#version 150

in vec3 Position;
out vec2 vLocal;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vLocal = Position.xy;
}
