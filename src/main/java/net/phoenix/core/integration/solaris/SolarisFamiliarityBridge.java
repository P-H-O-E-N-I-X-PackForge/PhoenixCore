package net.phoenix.core.integration.solaris;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenixvine.solaris.client.render.MapLayer;
import net.phoenixvine.solaris.client.render.MapLayers;
import net.phoenixvine.solaris.client.render.MapProjection;

import com.mojang.blaze3d.systems.RenderSystem;

@OnlyIn(Dist.CLIENT)
final class SolarisFamiliarityBridge implements MapLayer {

    private static final ResourceLocation SCULK = ResourceLocation.fromNamespaceAndPath("phoenixcore", "conflux/sculk");
    private static final int MAX_CHUNKS = 20000;

    private static final SolarisFamiliarityBridge INSTANCE = new SolarisFamiliarityBridge();
    private static boolean registered = false;

    private SolarisFamiliarityBridge() {}

    static void register() {
        if (registered) return;
        registered = true;
        MapLayers.register(INSTANCE);
        MapLayers.registerMinimap(INSTANCE);
    }

    @Override
    public void render(GuiGraphics g, ResourceLocation dimension, MapProjection projection, float partialTick) {
        if (!SCULK.equals(dimension) || !PhoenixConfigs.INSTANCE.confluxSolaris.familiarityOverlay) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float zoom = projection.zoom();
        if (zoom <= 0.0f) return;
        double originX = projection.screenX(0.0);
        double originY = projection.screenY(0.0);
        int minCx = Math.floorDiv((int) Math.floor((0 - originX) / zoom), 16);
        int maxCx = Math.floorDiv((int) Math.ceil((mc.getWindow().getGuiScaledWidth() - originX) / zoom), 16);
        int minCz = Math.floorDiv((int) Math.floor((0 - originY) / zoom), 16);
        int maxCz = Math.floorDiv((int) Math.ceil((mc.getWindow().getGuiScaledHeight() - originY) / zoom), 16);
        if ((long) (maxCx - minCx + 1) * (maxCz - minCz + 1) > MAX_CHUNKS) return;

        String id = dimension.toString();
        double pulse = 0.9 + 0.1 * Math.sin((System.currentTimeMillis() % 600000L) / 1400.0);
        int sub = zoom >= 1.0f ? 4 : zoom >= 0.5f ? 2 : 1;
        int[] near = new int[9];
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                boolean any = false;
                for (int i = 0; i < 9; i++) {
                    near[i] = ClientFamiliarity.level(id, cx + i / 3 - 1, cz + i % 3 - 1);
                    any |= near[i] > 0;
                }
                if (!any) continue;
                drawChunk(g, projection, near, cx, cz, sub, pulse);
            }
        }
        RenderSystem.disableBlend();
    }

    private static void drawChunk(GuiGraphics g, MapProjection projection, int[] near, int cx, int cz, int sub,
                                  double pulse) {
        for (int i = 0; i < sub; i++) {
            for (int j = 0; j < sub; j++) {
                double u = (i + 0.5) / sub;
                double v = (j + 0.5) / sub;
                int ox = u < 0.5 ? 0 : 1;
                int oz = v < 0.5 ? 0 : 1;
                double fx = u < 0.5 ? u + 0.5 : u - 0.5;
                double fz = v < 0.5 ? v + 0.5 : v - 0.5;
                double l00 = near[ox * 3 + oz];
                double l10 = near[(ox + 1) * 3 + oz];
                double l01 = near[ox * 3 + oz + 1];
                double l11 = near[(ox + 1) * 3 + oz + 1];
                double top = l00 + (l10 - l00) * fx;
                double bottom = l01 + (l11 - l01) * fx;
                double level = top + (bottom - top) * fz;
                int alpha = (int) Math.min(0x48, level * 0x10 * pulse);
                if (alpha < 3) continue;
                double wx0 = cx * 16 + i * 16.0 / sub;
                double wz0 = cz * 16 + j * 16.0 / sub;
                int x0 = (int) Math.floor(projection.screenX(wx0));
                int x1 = (int) Math.ceil(projection.screenX(wx0 + 16.0 / sub));
                int y0 = (int) Math.floor(projection.screenY(wz0));
                int y1 = (int) Math.ceil(projection.screenY(wz0 + 16.0 / sub));
                if (!projection.contains((x0 + x1) / 2.0, (y0 + y1) / 2.0)) continue;
                g.fill(x0, y0, x1, y1, (alpha << 24) | 0x2EE6C0);
            }
        }
    }
}
