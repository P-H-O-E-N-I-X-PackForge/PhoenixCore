#version 150

// Continuum planet / atmosphere vertex stage. The mesh is a unit icosphere, so a vertex's Position is
// also its direction from the planet's centre: the fragment stage uses it as the surface coordinate
// (seamless procedural terrain, no UVs) and, rotated into view space, as the geometric normal.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 vDir;
out vec3 vPosView;
out vec3 vNormalView;
out mat3 vRot;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);

    // object -> view rotation with the (uniform) scale divided out
    mat3 rot = mat3(ModelViewMat);
    rot /= length(rot[0]);

    vDir = Position;
    vPosView = view.xyz;
    vNormalView = normalize(rot * Position);
    vRot = rot;

    gl_Position = ProjMat * view;
}
