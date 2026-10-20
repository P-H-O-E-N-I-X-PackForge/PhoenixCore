package net.phoenix.core.integration.continuum.client.pdim;

import net.minecraft.client.multiplayer.ClientLevel;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;

import org.joml.Vector3f;

/**
 * Works out what a personal dimension's sky should look like right now, the way it would in a simulation of that world:
 * where its star is in the sky for the time of day, and what colour its air is. A world with an atmosphere gets a
 * daytime sky in the atmosphere's own colour (a violet atmosphere means a violet day), a twilight glow when the star is
 * on the horizon, and a starry night. A world with no air keeps a black sky all day with the star blazing in it. A world
 * orbiting a black hole (or a binary pair) is lit the same way, with the hole and its glowing disk as the "sun": it
 * rises, crosses the sky and sets, and the sky takes a tint from the disk's colour. Stars and black holes themselves
 * stay a space sky.
 *
 * The time of day is the dimension's own day time, so the sky and the world's light level agree.
 */
public final class PdimSkyModel {

    /** How far the star's daily path is tipped from straight overhead, in radians, before the world's axial tilt. */
    private static final float BASE_LATITUDE = 0.30f;

    /**
     * @param sunDir    unit vector towards the star, world space (x east, y up)
     * @param hasSun    whether this world has a star to draw as a sun
     * @param atmo      0 = no air, 1 = thick air
     * @param zenith    daytime sky straight up, as 0..1 RGB
     * @param horizon   daytime sky at the horizon
     * @param dusk      twilight glow colour
     * @param sunColor  the star's colour
     */
    public record Sample(Vector3f sunDir, boolean hasSun, float atmo, float[] zenith, float[] horizon, float[] dusk,
                         float[] sunColor) {

        /** Sky colour at the horizon right now, for fog and the clear colour: pale by day, dark at night. */
        public float[] horizonNow() {
            float e = sunDir.y;
            float day = smooth(-0.14f, 0.18f, e) * atmo;
            float twi = (float) Math.exp(-Math.pow(e / 0.20f, 2.0)) * atmo;
            float[] out = new float[3];
            float[] night = {0.012f, 0.014f, 0.034f};
            for (int i = 0; i < 3; i++) {
                out[i] = night[i] * (1.0f - day) + horizon[i] * 0.85f * day + dusk[i] * 0.35f * twi;
            }
            return out;
        }
    }

    private PdimSkyModel() {}

    public static Sample sample(ClientLevel level, float partialTick, ContinuumBody body, ContinuumSystem system) {
        int starColor = system != null ? system.starColor() : 0xfff1cf;
        boolean holeSun = system != null && system.hasHole();
        // around a black hole the light is the accretion disk's, so use its inner-disk colour when the system sets one
        if (holeSun && system.hole().hotColor() >= 0) starColor = system.hole().hotColor();
        float[] sunColor = rgb(starColor);

        boolean surfaceWorld = body != null && system != null && body.type() != ContinuumBody.Type.STAR &&
                body.type() != ContinuumBody.Type.BLACK_HOLE;

        float dayFrac = ((Math.floorMod(level.getDayTime(), 24000L)) + partialTick) / 24000.0f;
        float lat = BASE_LATITUDE;
        if (body != null) {
            lat = clamp(BASE_LATITUDE + (float) Math.toRadians(Math.abs(body.params().axialTiltDeg())) * 0.8f,
                    0.10f, 1.10f);
        }
        // dayFrac 0 = sunrise, 0.25 = noon, 0.5 = sunset, 0.75 = midnight (the usual Minecraft day)
        double theta = 2.0 * Math.PI * dayFrac;
        Vector3f sunDir = new Vector3f((float) Math.cos(theta), (float) (Math.sin(theta) * Math.cos(lat)),
                (float) (-Math.sin(theta) * Math.sin(lat))).normalize();

        if (!surfaceWorld) {
            return new Sample(sunDir, false, 0.0f, new float[3], new float[3], new float[3], sunColor);
        }

        float atmo = clamp(body.params().atmoDensity() * 2.2f, 0.0f, 1.0f);
        float[] air = rgb(body.params().atmoColor());
        float[] zenith = new float[3];
        float[] horizon = new float[3];
        float[] dusk = new float[3];
        float[] warm = {1.0f, 0.42f, 0.18f};
        for (int i = 0; i < 3; i++) {
            zenith[i] = air[i] * 0.55f;                                   // deep colour overhead
            horizon[i] = clamp(air[i] * 0.45f + 0.55f, 0.0f, 1.0f) * 0.95f;   // paler where you look through more air
            dusk[i] = sunColor[i] * 0.45f + warm[i] * 0.55f;              // sunsets are warm whatever the air is
            if (holeSun) {
                // a disk-lit sky leans towards the disk's colour
                zenith[i] = zenith[i] * 0.8f + sunColor[i] * 0.10f;
                horizon[i] = horizon[i] * 0.8f + sunColor[i] * 0.20f;
            }
        }
        return new Sample(sunDir, true, atmo, zenith, horizon, dusk, sunColor);
    }

    private static float[] rgb(int c) {
        return new float[] {((c >> 16) & 0xFF) / 255.0f, ((c >> 8) & 0xFF) / 255.0f, (c & 0xFF) / 255.0f};
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float smooth(float a, float b, float x) {
        float t = clamp((x - a) / (b - a), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}