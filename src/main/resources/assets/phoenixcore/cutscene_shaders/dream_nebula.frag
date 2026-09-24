// Shadertoy-style cutscene shader: iTime, iResolution and iMouse are provided.

// --- tunables -----------------------------------------------------------
const int OCTAVE = 10;          // dropped from the original 12 for headroom —
                                 // this runs full-screen every frame on top of
                                 // the game itself. Push back to 12 if it's cheap
                                 // on your GPU.
const float TIMESCALE = 5.0;
const vec3  SPACE      = vec3(0.045, 0.028, 0.14);   // darker base sky
const vec4  CLOUD1_COL = vec4(0.41, 0.64, 0.78, 0.4);
const vec4  CLOUD2_COL = vec4(0.99, 0.79, 0.46, 0.2);
const vec4  CLOUD3_COL = vec4(0.81, 0.31, 0.59, 1.0);
const vec4  CLOUD4_COL = vec4(0.27, 0.15, 0.33, 1.0);
const float ZOOM_SCALE  = 6.0;
const float STAR_SIZE   = 10.0;
const float STAR_SCALE  = 20.0;
const float STAR_PROB   = 0.985;  // slightly fewer stars than the original 0.98
const float DARKNESS    = 0.55;   // <- turn down further for even darker, up for brighter
// --------------------------------------------------------------------------

float rand(vec2 p) {
    return fract(sin(dot(p, vec2(23.53, 44.0))) * 42350.45);
}

float perlin(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = smoothstep(0.0, 1.0, f);
    float a = rand(i);
    float b = rand(i + vec2(1.0, 0.0));
    float c = rand(i + vec2(0.0, 1.0));
    float d = rand(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbmCloud(vec2 p, float minimum) {
    float value = 0.0;
    float scale = 0.5;
    for (int i = 0; i < OCTAVE; i++) {
        value += perlin(p) * scale;
        p *= 2.0;
        scale *= 0.5;
    }
    return smoothstep(0.0, 1.0, (smoothstep(minimum, 1.0, value) - minimum) / (1.0 - minimum));
}

float fbmCloud2(vec2 p, float minimum) {
    float value = 0.0;
    float scale = 0.5;
    for (int i = 0; i < OCTAVE; i++) {
        value += perlin(p) * scale;
        p *= 2.0;
        scale *= 0.5;
    }
    return (smoothstep(minimum, 1.0, value) - minimum) / (1.0 - minimum);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    float timescaled = iTime * TIMESCALE;

    vec2 zoomUV2 = vec2(ZOOM_SCALE*uv.x + 0.03*timescaled*sin(0.07*timescaled), ZOOM_SCALE*uv.y + 0.03*timescaled*cos(0.06*timescaled));
    vec2 zoomUV3 = vec2(ZOOM_SCALE*uv.x + 0.027*timescaled*sin(0.07*timescaled), ZOOM_SCALE*uv.y + 0.025*timescaled*cos(0.06*timescaled));
    vec2 zoomUV4 = vec2(ZOOM_SCALE*uv.x + 0.021*timescaled*sin(0.07*timescaled), ZOOM_SCALE*uv.y + 0.021*timescaled*cos(0.07*timescaled));

    float tide  = 0.05 * sin(iTime);
    float tide2 = 0.06 * cos(0.3 * iTime);

    vec4 nebula = vec4(SPACE, 0.5 + 0.2*sin(0.23*iTime + uv.x - uv.y));
    nebula += fbmCloud2(zoomUV3, 0.26 + tide) * CLOUD1_COL;
    nebula += fbmCloud(zoomUV2 * 0.9, 0.36 - tide) * CLOUD2_COL;
    nebula = mix(nebula, CLOUD3_COL, fbmCloud(vec2(0.9*zoomUV4.x, 0.9*zoomUV4.y), 0.28 + tide2));
    nebula = mix(nebula, CLOUD4_COL, fbmCloud(zoomUV3 * 0.7 + 2.0, 0.42 + tide2));

    vec2 zoomstar = STAR_SCALE * zoomUV2;
    vec2 pos = floor(zoomstar / STAR_SIZE);
    float starValue = rand(pos);

    if (starValue > STAR_PROB) {
        vec2 center = STAR_SIZE*pos + vec2(STAR_SIZE, STAR_SIZE)*0.5;
        float t = 0.9 + 0.2*sin(iTime*8.0 + (starValue-STAR_PROB)/(1.0-STAR_PROB)*45.0);
        float c = 1.0 - distance(zoomstar, center) / (0.5*STAR_SIZE);
        float glow = smoothstep(0.0, 1.0, c*t/max(abs(zoomstar.y-center.y),0.0008) * t/max(abs(zoomstar.x-center.x),0.0008));
        nebula = mix(nebula, vec4(1.0), glow * 0.85);
    } else {
        zoomstar *= 5.0;
        pos = floor(zoomstar / STAR_SIZE);
        float starValue2 = rand(pos + vec2(13.0, 13.0));
        if (starValue2 >= 0.95) {
            vec2 center = STAR_SIZE*pos + vec2(STAR_SIZE, STAR_SIZE)*0.5;
            float t = 0.9 + 0.2*sin(iTime*8.0 + (starValue-STAR_PROB)/(1.0-STAR_PROB)*45.0);
            float c = 1.0 - distance(zoomstar, center) / (0.5*STAR_SIZE);
            float glow = fbmCloud(pos, 0.0) * smoothstep(0.0, 1.0, c*t/max(abs(zoomstar.y-center.y),0.0008) * t/max(abs(zoomstar.x-center.x),0.0008));
            nebula = mix(nebula, vec4(1.0), glow * 0.85);
        }
    }

    fragColor = vec4(nebula.rgb * DARKNESS, 1.0);
}
