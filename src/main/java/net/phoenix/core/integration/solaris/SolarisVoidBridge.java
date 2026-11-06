package net.phoenix.core.integration.solaris;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenixvine.solaris.client.color.ChunkColorCache;
import net.phoenixvine.solaris.client.color.ChunkKey;
import net.phoenixvine.solaris.client.render.MapLayer;
import net.phoenixvine.solaris.client.render.MapLayers;
import net.phoenixvine.solaris.client.render.MapProjection;

import com.mojang.blaze3d.systems.RenderSystem;

@OnlyIn(Dist.CLIENT)
final class SolarisVoidBridge implements MapLayer {

    private static final ResourceLocation VOID = ResourceLocation.fromNamespaceAndPath("phoenixcore", "conflux/void");
    private static final int MAX_CHUNKS = 20000;
    private static final int ABYSS = 0xD0090420;

    private static final int HALO = 3;
    private static final int[] HALO_ALPHA = { 0, 0x78, 0x4A, 0x26 };
    private static final int RECOMPUTES_PER_FRAME = 40;

    private record Cached(int[] pixels, boolean[] open, byte[] halo, int generation) {}

    private static int generation = 0;
    private static int recomputesLeft = 0;

    private static final java.util.Map<Long, Cached> CHUNKS = new java.util.concurrent.ConcurrentHashMap<>();

    private static final SolarisVoidBridge INSTANCE = new SolarisVoidBridge();
    private static boolean registered = false;

    private SolarisVoidBridge() {}

    static void register() {
        if (registered) return;
        registered = true;
        MapLayers.register(INSTANCE);
        MapLayers.registerMinimap(INSTANCE);
    }

    @Override
    public void render(GuiGraphics g, ResourceLocation dimension, MapProjection projection, float partialTick) {
        if (!VOID.equals(dimension) || !PhoenixConfigs.INSTANCE.confluxSolaris.voidChasms) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float zoom = projection.zoom();
        if (zoom <= 0.0f) return;
        double originX = projection.screenX(0.0);
        double originY = projection.screenY(0.0);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int minCx = Math.floorDiv((int) Math.floor((0 - originX) / zoom), 16);
        int maxCx = Math.floorDiv((int) Math.ceil((screenW - originX) / zoom), 16);
        int minCz = Math.floorDiv((int) Math.floor((0 - originY) / zoom), 16);
        int maxCz = Math.floorDiv((int) Math.ceil((screenH - originY) / zoom), 16);
        if ((long) (maxCx - minCx + 1) * (maxCz - minCz + 1) > MAX_CHUNKS) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        double time = (System.currentTimeMillis() % 600000L) / 1800.0;
        if (CHUNKS.size() > 16384) CHUNKS.clear();
        recomputesLeft = RECOMPUTES_PER_FRAME;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                int[] pixels = ChunkColorCache.get(new ChunkKey(dimension, cx, cz));
                if (pixels == null) continue;
                boolean[] open = openColumns(mc, pixels, cx, cz);
                byte[] halo = haloOf(pixels, cx, cz);
                if (halo != null) drawHalo(g, projection, halo, cx, cz);
                if (open != null) {
                    drawChunk(g, projection, zoom, open, cx, cz);
                    stars(g, projection, zoom, open, cx, cz, time);
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private static boolean[] openColumns(Minecraft mc, int[] pixels, int cx, int cz) {
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        Cached cached = CHUNKS.get(key);
        if (cached != null && cached.pixels() == pixels) return cached.open();

        LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL,
                false) instanceof LevelChunk lc ?
                        lc : null;
        if (chunk == null) return null;
        int floor = mc.level.getMinBuildHeight() + 1;
        boolean[] open = new boolean[256];
        boolean any = false;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                if (pixels[lz * 16 + lx] == 0 && chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz) <= floor) {
                    open[lz * 16 + lx] = true;
                    any = true;
                }
            }
        }
        boolean[] result = any ? open : null;
        CHUNKS.put(key, new Cached(pixels, result, null, ++generation));
        return result;
    }

    private static long keyOf(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    private static byte[] haloOf(int[] pixels, int cx, int cz) {
        long key = keyOf(cx, cz);
        Cached me = CHUNKS.get(key);
        if (me == null) return null;
        if (me.halo() != null && me.generation() == generation) return me.halo();
        if (me.generation() != generation && recomputesLeft <= 0) return me.halo();

        boolean near = false;
        boolean[][] around = new boolean[9][];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Cached c = CHUNKS.get(keyOf(cx + dx, cz + dz));
                boolean[] o = c == null ? null : c.open();
                around[(dx + 1) * 3 + dz + 1] = o;
                if (o != null) near = true;
            }
        }
        recomputesLeft--;
        byte[] halo = null;
        if (near) {
            int size = 16 + 2 * HALO;
            boolean[] grid = new boolean[size * size];
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean[] o = around[(dx + 1) * 3 + dz + 1];
                    if (o == null) continue;
                    for (int lz = 0; lz < 16; lz++) {
                        for (int lx = 0; lx < 16; lx++) {
                            if (!o[lz * 16 + lx]) continue;
                            int gx = dx * 16 + lx + HALO;
                            int gz = dz * 16 + lz + HALO;
                            if (gx >= 0 && gx < size && gz >= 0 && gz < size) grid[gz * size + gx] = true;
                        }
                    }
                }
            }
            halo = new byte[256];
            boolean any = false;
            boolean[] mine = around[4];
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    if (mine != null && mine[lz * 16 + lx]) continue;
                    int best = 0;
                    for (int d = 1; d <= HALO && best == 0; d++) {
                        for (int oz = -d; oz <= d && best == 0; oz++) {
                            for (int ox = -d; ox <= d; ox++) {
                                if (Math.max(Math.abs(ox), Math.abs(oz)) != d) continue;
                                if (grid[(lz + oz + HALO) * size + lx + ox + HALO]) {
                                    best = d;
                                    break;
                                }
                            }
                        }
                    }
                    if (best > 0) {
                        halo[lz * 16 + lx] = (byte) best;
                        any = true;
                    }
                }
            }
            if (!any) halo = null;
        }
        CHUNKS.put(key, new Cached(me.pixels(), me.open(), halo, generation));
        return halo;
    }

    private static void drawHalo(GuiGraphics g, MapProjection projection, byte[] halo, int cx, int cz) {
        for (int lz = 0; lz < 16; lz++) {
            int wz = cz * 16 + lz;
            int y0 = (int) Math.floor(projection.screenY(wz));
            int y1 = Math.max((int) Math.ceil(projection.screenY(wz + 1)), y0 + 1);
            int run = -1;
            int level = 0;
            for (int lx = 0; lx <= 16; lx++) {
                int here = lx < 16 ? halo[lz * 16 + lx] : 0;
                if (run >= 0 && here != level) {
                    int x0 = (int) Math.floor(projection.screenX(cx * 16 + run));
                    int x1 = (int) Math.ceil(projection.screenX(cx * 16 + lx));
                    double midY = (y0 + y1) / 2.0;
                    int color = (HALO_ALPHA[level] << 24) | 0x090420;
                    if (projection.contains(x0, midY) && projection.contains(x1, midY)) {
                        g.fill(x0, y0, x1, y1, color);
                    } else {
                        for (int wx = cx * 16 + run; wx < cx * 16 + lx; wx++) {
                            int a = (int) Math.floor(projection.screenX(wx));
                            int b = (int) Math.ceil(projection.screenX(wx + 1));
                            if (projection.contains((a + b) / 2.0, midY)) g.fill(a, y0, b, y1, color);
                        }
                    }
                    run = -1;
                }
                if (here > 0 && run < 0) {
                    run = lx;
                    level = here;
                }
            }
        }
    }

    private static void drawChunk(GuiGraphics g, MapProjection projection, float zoom, boolean[] pixels, int cx,
                                  int cz) {
        for (int lz = 0; lz < 16; lz++) {
            int run = -1;
            for (int lx = 0; lx <= 16; lx++) {
                boolean open = lx < 16 && pixels[lz * 16 + lx];
                if (open && run < 0) run = lx;
                if (!open && run >= 0) {
                    int wx0 = cx * 16 + run;
                    int wz = cz * 16 + lz;
                    int x0 = (int) Math.floor(projection.screenX(wx0));
                    int x1 = (int) Math.ceil(projection.screenX(cx * 16 + lx));
                    int y0 = (int) Math.floor(projection.screenY(wz));
                    int y1 = (int) Math.ceil(projection.screenY(wz + 1));
                    int color = ABYSS;
                    int bottom = Math.max(y1, y0 + 1);
                    double midY = (y0 + bottom) / 2.0;
                    if (projection.contains(x0, midY) && projection.contains(x1, midY) &&
                            projection.contains((x0 + x1) / 2.0, midY)) {
                        g.fill(x0, y0, x1, bottom, color);
                    } else {
                        for (int wx = wx0; wx < cx * 16 + lx; wx++) {
                            int a = (int) Math.floor(projection.screenX(wx));
                            int b = (int) Math.ceil(projection.screenX(wx + 1));
                            if (projection.contains((a + b) / 2.0, midY)) g.fill(a, y0, b, bottom, color);
                        }
                    }
                    run = -1;
                }
            }
        }
    }

    private static void stars(GuiGraphics g, MapProjection projection, float zoom, boolean[] open, int cx, int cz,
                              double time) {
        int cell = 8;
        while (cell * zoom < 6.0f && cell < 4096) cell <<= 1;
        int size = zoom >= 3.0f ? 2 : 1;
        int minX = cx * 16;
        int minZ = cz * 16;
        for (int gx = Math.floorDiv(minX, cell); gx <= Math.floorDiv(minX + 15, cell); gx++) {
            for (int gz = Math.floorDiv(minZ, cell); gz <= Math.floorDiv(minZ + 15, cell); gz++) {
                int h = (gx * 73856093) ^ (gz * 19349663) ^ cell * 83492791;
                h ^= h >>> 15;
                h *= 0x2C1B3C6D;
                h ^= h >>> 12;
                if ((h & 3) == 0) continue;
                int wx = gx * cell + Math.floorMod(h >>> 4, cell);
                int wz = gz * cell + Math.floorMod(h >>> 14, cell);
                if (wx < minX || wx > minX + 15 || wz < minZ || wz > minZ + 15) continue;
                if (!open[(wz - minZ) * 16 + (wx - minX)]) continue;
                boolean twinkles = (h & 0x300000) == 0;
                double pulse = twinkles ? 0.5 + 0.5 * Math.sin(time + (h >>> 24 & 255)) : 1.0;
                int alpha = 130 + (int) (110 * pulse);
                int color = (alpha << 24) | ((h & 0x400000) == 0 ? 0xFFFFFF : 0xB48CFF);
                int sx = (int) Math.floor(projection.screenX(wx));
                int sy = (int) Math.floor(projection.screenY(wz));
                if (!projection.contains(sx, sy)) continue;
                g.fill(sx, sy, sx + size, sy + size, color);
            }
        }
    }
}
