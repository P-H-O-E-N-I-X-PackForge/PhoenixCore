package net.phoenix.core.client.renderer.cinema;

import net.minecraft.core.BlockPos;
import net.phoenix.continuum.api.client.ContinuumRender;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The live Continuum pictures on cinema screens ({@code @continuum star ffcc66}): each is drawn by its own Continuum
 * preview into an offscreen texture, seen from the angle the player looks at the screen from, so walking round a screen
 * turns the star on it. They are redrawn a few times a second (a couple per frame at most) and the ones nobody has looked
 * at for a while are freed.
 */
final class CinemaContinuumPictures {

    private static final int MAX_PREVIEWS = 8;
    private static final long INTERVAL_MS = 100L;
    private static final long KEEP_MS = 10_000L;
    private static final int PER_FRAME = 1;
    private static final double RANGE_SQ = 20.0 * 20.0;

    private record Key(BlockPos pos, int index) {}

    private static final class Entry {

        final ContinuumRender.Preview preview = ContinuumRender.newCheapPreview(256);
        long rendered;
        long used;
        int texture = -1;
        float yaw;
        float pitch;
    }

    private static final Map<Key, Entry> ENTRIES = new LinkedHashMap<>(16, 0.75f, true);
    private static long frameStamp;
    private static int rendersThisFrame;

    private CinemaContinuumPictures() {}

    static boolean is(String pictureId) {
        return pictureId.startsWith("continuum:");
    }

    /**
     * The colour texture of the picture, or -1 when there is none yet.
     *
     * @param yaw   how far round the body the viewer is, degrees (0 in front of the screen)
     * @param pitch how far above it, degrees
     */
    static int texture(BlockPos centre, int index, String pictureId, float yaw, float pitch, double distanceSq,
                       double lookDot) {
        long now = net.minecraft.Util.getMillis();
        Key key = new Key(centre, index);
        Entry entry = ENTRIES.get(key);
        if (entry == null) {
            if (distanceSq > RANGE_SQ) return -1;
            entry = new Entry();
            ENTRIES.put(key, entry);
            evict(now);
        }
        entry.used = now;
        // nearer is smoother; out of the corner of the eye, or far off, it is only refreshed now and then
        long interval = distanceSq < 36.0 ? INTERVAL_MS : distanceSq < 144.0 ? 2 * INTERVAL_MS : 5 * INTERVAL_MS;
        boolean looked = lookDot > 0.35;
        boolean due = entry.texture < 0 || (looked && (now - entry.rendered >= interval ||
                Math.abs(yaw - entry.yaw) > 10.0f || Math.abs(pitch - entry.pitch) > 10.0f)) ||
                now - entry.rendered >= 4000L;
        if (!due || distanceSq > RANGE_SQ) return entry.texture;
        long stamp = System.nanoTime() / 4_000_000L;
        if (stamp != frameStamp) {
            frameStamp = stamp;
            rendersThisFrame = 0;
        }
        if (rendersThisFrame >= PER_FRAME && entry.texture >= 0) return entry.texture;
        rendersThisFrame++;

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
        switch (name) {
            case "black_hole", "blackhole", "hole" -> {
                body = ContinuumRender.Body.BLACK_HOLE;
                rgb = 0xFFFFFF;
                size = 0.7f;
            }
            case "gas_cloud", "cloud", "nebula" -> {
                body = ContinuumRender.Body.GAS_CLOUD;
                rgb = colour >= 0 ? colour : 0x66AAFF;
                size = 0.8f;
            }
            case "protostar" -> {
                body = ContinuumRender.Body.PROTOSTAR;
                rgb = colour >= 0 ? colour : 0xFF8855;
                size = 0.7f;
            }
            default -> {
                body = ContinuumRender.Body.STAR;
                rgb = colour >= 0 ? colour : 0xFFCC66;
            }
        }
        int hole = body == ContinuumRender.Body.BLACK_HOLE ? colour : -1;
        int id = entry.preview.renderTexture(body, rgb, size, false, hole, yaw, pitch);
        entry.rendered = now;
        entry.yaw = yaw;
        entry.pitch = pitch;
        if (id >= 0) entry.texture = id;
        return entry.texture;
    }

    private static void evict(long now) {
        List<Key> gone = new ArrayList<>();
        for (Map.Entry<Key, Entry> e : ENTRIES.entrySet()) {
            if (now - e.getValue().used > KEEP_MS) gone.add(e.getKey());
        }
        for (Key k : gone) ENTRIES.remove(k).preview.release();
        Iterator<Map.Entry<Key, Entry>> it = ENTRIES.entrySet().iterator();
        while (ENTRIES.size() > MAX_PREVIEWS && it.hasNext()) {
            it.next().getValue().preview.release();
            it.remove();
        }
    }
}
