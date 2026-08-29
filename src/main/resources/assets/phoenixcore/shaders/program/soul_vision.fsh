#version 150

// Soul Lens "soul vision" overlay. Tints the world by how much soul each nearby chunk
// holds - gray for a depleted chunk, purple for a vibrant one - using the same 0..2.5
// density scale and color ramp as the lens' in-hand minimap (see SoulMapWidget).

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D DensitySampler;

uniform mat4 InvProjMat;
uniform mat4 InvViewMat;
uniform vec3 CameraPos;
uniform vec2 CenterChunk;
uniform float GridRadius;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;

    // Sky / nothing rendered here - leave it alone rather than guessing a chunk.
    if (depth >= 1.0) {
        fragColor = sceneColor;
        return;
    }

    // Reconstruct the world-space position of this fragment from the depth buffer.
    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPos = InvProjMat * ndc;
    viewPos /= viewPos.w;
    vec3 worldPos = (InvViewMat * viewPos).xyz + CameraPos;

    vec2 chunkCoord = floor(worldPos.xz / 16.0);
    vec2 relChunk = chunkCoord - CenterChunk;

    // Outside the lens' scanned radius - leave the view untouched.
    if (abs(relChunk.x) > GridRadius || abs(relChunk.y) > GridRadius) {
        fragColor = sceneColor;
        return;
    }

    vec2 densityUV = (relChunk + GridRadius + 0.5) / (GridRadius * 2.0 + 1.0);
    float factor = texture(DensitySampler, densityUV).r;

    vec3 lowColor = vec3(0.32, 0.10, 0.34);
    vec3 highColor = vec3(0.85, 0.15, 1.0);
    vec3 tint = mix(lowColor, highColor, factor);

    float luminance = dot(sceneColor.rgb, vec3(0.299, 0.587, 0.114));
    vec3 tinted = tint * (0.4 + luminance * 0.9);

    vec3 finalColor = mix(sceneColor.rgb, tinted, 0.65);
    fragColor = vec4(finalColor, sceneColor.a);
}
