#version 150

// The sky of a personal dimension, looked at from inside, reconstructed from the camera's inverse view and projection
// so it turns with the player. At night, and always on a world with no air, it is an opaque nebula and star field.
// On a world with an atmosphere it also has a daytime sky in the atmosphere's own colour, a twilight glow round the
// star at sunrise and sunset, and the stars and nebula fading out as the star climbs (and back in as it sets).

uniform mat4 InvViewMat;
uniform mat4 InvProjMat;
uniform float Time;
uniform float Octaves;
uniform vec3 PrimaryColor;
uniform vec3 SecondaryColor;
uniform vec3 SunDir;        // direction to the star, world space
uniform float Atmo;         // 0 = no atmosphere (space all day), 1 = thick
uniform vec3 ZenithColor;   // daytime sky straight up
uniform vec3 HorizonColor;  // daytime sky at the horizon
uniform vec3 DuskColor;     // twilight glow
uniform vec3 SunColor;      // the star's colour

in vec2 ndc;
out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i), hash(i + vec3(1, 0, 0)), f.x),
            mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), f.x), f.y),
        mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), f.x),
            mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), f.x), f.y),
        f.z);
}

float fbm(vec3 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) {
        if (float(i) >= Octaves) break;
        v += a * noise(p);
        p = p * 2.03 + vec3(7.3, 5.9, 11.1);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec4 viewPos = InvProjMat * vec4(ndc, 1.0, 1.0);
    viewPos /= viewPos.w;
    vec3 dir = normalize((InvViewMat * vec4(viewPos.xyz, 0.0)).xyz);

    vec3 q = dir * 1.6 + Time * 0.0004;
    float warp = fbm(q + 1.4 * vec3(fbm(q + 3.1), fbm(q + 7.7), fbm(q + 1.3)));
    float haze = 0.30 + 0.70 * smoothstep(0.15, 0.85, warp);
    float filament = pow(smoothstep(0.42, 0.78, warp), 2.0);
    vec3 nebula = mix(SecondaryColor, PrimaryColor, smoothstep(0.25, 0.8, warp));

    vec3 col = vec3(0.012, 0.014, 0.034) + nebula * (0.11 * haze + 0.42 * filament);

    vec3 sp = dir * 90.0;
    vec3 cell = floor(sp);
    float h = hash(cell);
    if (h > 0.965) {
        vec3 centre = cell + 0.5 + (vec3(hash(cell + 1.7), hash(cell + 4.1), hash(cell + 9.3)) - 0.5) * 0.6;
        float d = length(sp - centre);
        float twinkle = 0.75 + 0.25 * sin(Time * (1.0 + h * 3.0) + h * 40.0);
        col += vec3(1.0, 0.96, 0.9) * smoothstep(0.42, 0.0, d) * (0.35 + 0.65 * (h - 0.965) / 0.035) * twinkle;
    }

    // ---- the atmosphere ---------------------------------------------------------------------------------------------
    float sunE = SunDir.y;
    float dayF = smoothstep(-0.14, 0.18, sunE) * Atmo;               // how much daylight there is
    float twi = exp(-pow(sunE / 0.20, 2.0)) * Atmo;                  // twilight: strongest with the star on the horizon
    float up = dir.y;
    float hz = pow(1.0 - clamp(up, 0.0, 1.0), 2.5);                  // 0 overhead, 1 at the horizon
    float cosS = clamp(dot(dir, SunDir), -1.0, 1.0);

    vec3 daySky = mix(ZenithColor, HorizonColor, hz);
    float bloomKeep = 1.0 - 0.75 * twi;                              // at twilight the dusk glow takes over from the bloom
    daySky += SunColor * pow(max(cosS, 0.0), 6.0) * 0.28 * bloomKeep;       // a broad bloom round the star
    daySky += mix(SunColor, vec3(1.0), 0.5) * pow(max(cosS, 0.0), 60.0) * 0.55 * bloomKeep;

    float towardSun = pow(max(cosS, 0.0), 2.0);
    vec3 twilight = DuskColor * twi * 1.1 * (0.20 + 0.80 * towardSun) * smoothstep(0.80, 0.0, up);

    vec3 sky = daySky * dayF + twilight;
    sky *= mix(0.35, 1.0, smoothstep(-0.25, 0.05, up));              // below the horizon the sky darkens

    // stars and nebula are washed out by daylight and a little by twilight
    float spaceVis = 1.0 - clamp(dayF * 1.2 + twi * 0.35, 0.0, 1.0);
    col = col * spaceVis + sky;

    fragColor = vec4(col, 1.0);
}
