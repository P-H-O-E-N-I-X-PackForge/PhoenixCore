package net.phoenix.core.client.renderer.cinema;

import net.phoenix.continuum.api.client.ContinuumRender;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Continuum pictures on cinema screens ({@code @continuum star ffcc66}). A body costs a lot to draw (a black hole is
 * around 20 ms of GPU on a modest card), so a screen does not redraw it every frame: each view of a body (from one of a
 * handful of angles) is drawn once into its own small texture and kept, and the screen shows the view nearest to where the
 * player stands. Walking round a screen therefore turns the body in steps, and a new angle is drawn the first time someone
 * looks from it, at most one every {@value #BUDGET_MS} ms. A view is drawn again now and then so the star still churns.
 */
final class CinemaContinuumPictures {

    private static final int PIXELS = 256;
    private static final float YAW_STEP = 24.0f;
    private static final float PITCH_STEP = 30.0f;
    private static final int MAX_VIEWS = 40;
    /** The least time between two views being drawn. */
    private static final long BUDGET_MS = 90L;
    private static final long REFRESH_MS = 6_000L;
    private static final long HOLE_REFRESH_MS = 20_000L;
    private static final double RANGE_SQ = 20.0 * 20.0;

    private record Key(String picture, int yaw, int pitch) {}

    private static final class View {

        final ContinuumRender.Preview preview = ContinuumRender.newCheapPreview(PIXELS);
        int texture = -1;
        long drawn;
    }

    private static final Map<Key, View> VIEWS = new LinkedHashMap<>(64, 0.75f, true);
    private static long lastDrawn;

    private CinemaContinuumPictures() {}

    static boolean is(String pictureId) {
        return pictureId.startsWith("continuum:");
    }

    /**
     * The colour texture to show, or -1 when there is none yet.
     *
     * @param yaw   how far round the body the viewer is, degrees (0 in front of the screen)
     * @param pitch how far above it, degrees
     */
    static int texture(String pictureId, float yaw, float pitch, double distanceSq, double lookDot) {
        int yawBucket = Math.round(yaw / YAW_STEP);
        int pitchBucket = Math.round(pitch / PITCH_STEP);
        Key key = new Key(pictureId, yawBucket, pitchBucket);
        View view = VIEWS.get(key);
        long now = net.minecraft.Util.getMillis();
        boolean hole = pictureId.startsWith("continuum:black_hole");
        boolean wanted = distanceSq <= RANGE_SQ && lookDot > 0.35;
        boolean stale = view != null && now - view.drawn >= (hole ? HOLE_REFRESH_MS : REFRESH_MS);
        if ((view == null || stale) && wanted && now - lastDrawn >= BUDGET_MS) {
            if (view == null) {
                view = new View();
                VIEWS.put(key, view);
                trim();
            }
            int id = draw(view, pictureId, yawBucket * YAW_STEP, pitchBucket * PITCH_STEP);
            lastDrawn = now;
            view.drawn = now;
            if (id >= 0) view.texture = id;
        }
        if (view != null && view.texture >= 0) return view.texture;
        // nothing drawn from this angle yet: the nearest view of the same body will do until it is
        View near = null;
        int best = Integer.MAX_VALUE;
        for (Map.Entry<Key, View> e : VIEWS.entrySet()) {
            Key k = e.getKey();
            if (!k.picture().equals(pictureId) || e.getValue().texture < 0) continue;
            int d = Math.abs(k.yaw() - yawBucket) + Math.abs(k.pitch() - pitchBucket);
            if (d < best) {
                best = d;
                near = e.getValue();
            }
        }
        return near != null ? near.texture : -1;
    }

    private static int draw(View view, String pictureId, float yaw, float pitch) {
        String[] words = pictureId.split(":");
        String name = words.length > 1 ? words[1] : "star";
        int colour = -1;
        if (words.length > 2) {
            try {
                colour = Integer.parseInt(words[2], 16);
            } catch (NumberFormatException ignored) {}
        }
        ContinuumRender.Body body;
        int rgb;
        float size = 0.6f;
        // pulled back so the body fills only part of the picture: the shaders cost in proportion to the pixels they cover
        float zoom = 1.5f;
        switch (name) {
            case "black_hole", "blackhole", "hole" -> {
                body = ContinuumRender.Body.BLACK_HOLE;
                rgb = 0xFFFFFF;
                size = 0.7f;
                zoom = 1.9f;
            }
            case "gas_cloud", "cloud", "nebula" -> {
                body = ContinuumRender.Body.GAS_CLOUD;
                rgb = colour >= 0 ? colour : 0x66AAFF;
                size = 0.8f;
                zoom = 1.0f;
            }
            case "protostar" -> {
                body = ContinuumRender.Body.PROTOSTAR;
                rgb = colour >= 0 ? colour : 0xFF8855;
                size = 0.7f;
                zoom = 1.0f;
            }
            default -> {
                body = ContinuumRender.Body.STAR;
                rgb = colour >= 0 ? colour : 0xFFCC66;
            }
        }
        int hole = body == ContinuumRender.Body.BLACK_HOLE ? colour : -1;
        try (var ignored = net.phoenix.continuum.client.ContinuumProfiler.scope("cinema:live")) {
            return view.preview.renderTexture(body, rgb, size, false, hole, yaw, pitch, zoom);
        }
    }

    /** Frees the views used longest ago once there are too many. */
    private static void trim() {
        if (VIEWS.size() <= MAX_VIEWS) return;
        List<Key> gone = new ArrayList<>();
        int over = VIEWS.size() - MAX_VIEWS;
        for (Key k : VIEWS.keySet()) {
            if (gone.size() >= over) break;
            gone.add(k);
        }
        for (Key k : gone) VIEWS.remove(k).preview.release();
    }
}
