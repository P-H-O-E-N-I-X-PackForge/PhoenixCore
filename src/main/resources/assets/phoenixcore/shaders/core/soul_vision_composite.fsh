#version 150

// Composites the soul vision over the already-rendered scene. Each pixel's place in the world is rebuilt from the depth
// buffer, so the effect lies on the terrain that was actually drawn (loaded chunks only: where nothing was drawn the sky is
// left alone), at any distance and at any height. The chunk under that point gives its soul from the lens' scanned grid.

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;
uniform mat4 InvViewProj;
uniform vec3 CamPos;
uniform vec2 GridOrigin;
uniform float Density[289];
uniform float GameTime;

in vec2 texCoord;
out vec4 fragColor;

const int GRID = 17;
const vec3 LUMA = vec3(0.299, 0.587, 0.114);
const vec3 SOUL = vec3(0.62, 0.36, 1.0);

// how rich in soul a chunk is, 0..1; outside what the lens has scanned counts as none
float soulAt(ivec2 cell) {
    if (cell.x < 0 || cell.y < 0 || cell.x >= GRID || cell.y >= GRID) return 0.0;
    return Density[cell.y * GRID + cell.x];
}

void main() {
    vec4 col = texture(InSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;

    // nothing drawn here (sky, or a chunk that is not loaded): not part of the effect
    if (depth >= 0.99999) {
        fragColor = vec4(col.rgb, 1.0);
        return;
    }

    vec4 clip = InvViewProj * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 world = clip.xyz / clip.w + CamPos;

    vec2 chunkPos = world.xz / 16.0;
    ivec2 cell = ivec2(floor(chunkPos) - GridOrigin);
    float soul = soulAt(cell);

    float luma = dot(col.rgb, LUMA);

    // GameTime is the day as 0..1; a whole number of cycles per day so it does not jump when the day wraps
    float pulse = 0.5 + 0.5 * sin(GameTime * 6.2831853 * 458.0);

    // depleted ground: drained of colour, darker, a cold blue cast
    vec3 drained = vec3(luma) * vec3(0.52, 0.56, 0.72);

    // soul-rich ground: the real colour, lifted, with a violet glow that breathes
    vec3 rich = col.rgb * 1.12 + SOUL * (0.10 + 0.08 * pulse) * (0.35 + luma);

    vec3 result = mix(drained, rich, smoothstep(0.0, 1.0, soul));

    // a bright line along chunk borders where the soul level changes, a steady width on screen at any distance
    vec2 inChunk = fract(chunkPos) * 16.0;
    vec2 toEdge = min(inChunk, 16.0 - inChunk);
    ivec2 side = ivec2(inChunk.x < 8.0 ? -1 : 1, inChunk.y < 8.0 ? -1 : 1);
    float width = clamp(max(fwidth(world.x), fwidth(world.z)) * 1.5, 0.06, 0.8);
    float lineX = (1.0 - smoothstep(0.0, width, toEdge.x)) * clamp(abs(soul - soulAt(cell + ivec2(side.x, 0))) * 5.0, 0.0, 1.0);
    float lineY = (1.0 - smoothstep(0.0, width, toEdge.y)) * clamp(abs(soul - soulAt(cell + ivec2(0, side.y))) * 5.0, 0.0, 1.0);
    result += SOUL * max(lineX, lineY) * (0.55 + 0.45 * pulse);

    fragColor = vec4(result, 1.0);
}
