package net.phoenix.core.integration.solaris;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix_tesla_network.tesla.client.coverage.ClientCoverageData;
import net.phoenix_tesla_network.tesla.network.packet.S2CCoverageSyncPacket;
import net.phoenixvine.solaris.client.render.MapLayer;
import net.phoenixvine.solaris.client.render.MapLayers;
import net.phoenixvine.solaris.client.render.MapProjection;
import net.phoenixvine.solaris.client.render.SmoothShapes;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class SolarisCoverageBridge implements MapLayer {

    private static final SolarisCoverageBridge INSTANCE = new SolarisCoverageBridge();
    private static boolean registered = false;

    private static List<S2CCoverageSyncPacket.Entry> remembered = List.of();
    private static ResourceLocation rememberedDimension;

    private SolarisCoverageBridge() {}

    static void register() {
        if (registered) return;
        registered = true;
        MapLayers.register(INSTANCE);
    }

    static void clear() {
        remembered = List.of();
        rememberedDimension = null;
    }

    @Override
    public void render(GuiGraphics g, ResourceLocation dimension, MapProjection projection, float partialTick) {
        if (!PhoenixConfigs.INSTANCE.confluxSolaris.networkSiteMarkers) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var fresh = ClientCoverageData.fresh();
        if (!fresh.isEmpty()) {
            remembered = fresh;
            rememberedDimension = mc.level.dimension().location();
        }
        if (remembered.isEmpty() || !dimension.equals(rememberedDimension)) return;

        for (S2CCoverageSyncPacket.Entry entry : remembered) {
            if (entry.radius() <= 0) continue; 

            int color = switch (entry.kind()) {
                case S2CCoverageSyncPacket.TOWER -> 0x55E0FF;
                case S2CCoverageSyncPacket.RELAY_ACTIVE -> 0xA070FF;
                default -> 0x888888;
            };
            int sx = (int) projection.screenX(entry.pos().getX() + 0.5);
            int sy = (int) projection.screenY(entry.pos().getZ() + 0.5);
            int radius = (int) Math.abs(projection.screenX(entry.pos().getX() + 0.5 + entry.radius()) - sx);
            if (radius < 3 || radius > 6000) continue;

            boolean live = entry.kind() == S2CCoverageSyncPacket.TOWER ||
                    entry.kind() == S2CCoverageSyncPacket.RELAY_ACTIVE;
            SmoothShapes.drawCircle(g, sx, sy, radius, ((live ? 0x1E : 0x0C) << 24) | color);
            SmoothShapes.drawRing(g, sx, sy, radius, ((live ? 0xB0 : 0x50) << 24) | color);
        }
    }
}
