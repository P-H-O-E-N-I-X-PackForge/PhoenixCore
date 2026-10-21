package net.phoenix.core.integration.solaris;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.conflux.dimension.worldgen.Landforms;
import net.phoenix.core.network.packet.S2CLandformZonesPacket;
import net.phoenixvine.solaris.client.render.LineRenderer;
import net.phoenixvine.solaris.client.render.MapLayer;
import net.phoenixvine.solaris.client.render.MapLayers;
import net.phoenixvine.solaris.client.render.MapProjection;
import net.phoenixvine.solaris.client.render.SmoothShapes;

/**
 * Draws where the landform physics act on the fullscreen map (updrafts, low gravity, conveyors, bounce pads, slick
 * rock) and a live countdown above each geyser vent. Geyser timing is the same formula the server uses, run on the
 * synced game time. Only reached when Solaris is loaded.
 */
@OnlyIn(Dist.CLIENT)
final class SolarisZoneBridge implements MapLayer {

    // Matches LandformPhysics: a vent erupts for 40 of every 200 ticks and bubbles for the 30 ticks before.
    private static final int GEYSER_PERIOD = 200;
    private static final int GEYSER_ERUPTION = 40;
    private static final int GEYSER_WARNING = 30;

    private static final SolarisZoneBridge INSTANCE = new SolarisZoneBridge();
    private static boolean registered = false;

    private SolarisZoneBridge() {}

    static void register() {
        if (registered) return;
        registered = true;
        MapLayers.register(INSTANCE);
    }

    @Override
    public void render(GuiGraphics g, ResourceLocation dimension, MapProjection projection, float partialTick) {
        if (!PhoenixConfigs.INSTANCE.confluxSolaris.landformZones) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String dimensionId = dimension.toString();
        long time = mc.level.getGameTime();
        float zoom = projection.zoom();

        for (S2CLandformZonesPacket.ZoneDef def : ClientLandformZones.all()) {
            if (!def.dimension().equals(dimensionId)) continue;
            Landforms.Zone zone = def.zone();
            int sx = (int) projection.screenX(zone.x());
            int sy = (int) projection.screenY(zone.z());

            if (zone.type() == Landforms.ZoneType.GEYSER) {
                drawVent(g, mc, zone, sx, sy, zoom, time);
                continue;
            }

            int color = color(zone.type());
            int fill = (0x44 << 24) | color;
            int ring = (0xB0 << 24) | color;
            if (zone.shape() == Landforms.ZoneShape.CIRCLE) {
                int radius = Math.max(2, Math.round((float) zone.a() * zoom));
                SmoothShapes.drawCircle(g, sx, sy, radius, fill);
                SmoothShapes.drawRing(g, sx, sy, radius, ring);
                if (radius >= 22) label(g, mc, name(zone.type()), sx, sy, color);
            } else {
                double dx = Math.cos(zone.theta()) * zone.a();
                double dz = Math.sin(zone.theta()) * zone.a();
                int x1 = (int) projection.screenX(zone.x() - dx);
                int y1 = (int) projection.screenY(zone.z() - dz);
                int x2 = (int) projection.screenX(zone.x() + dx);
                int y2 = (int) projection.screenY(zone.z() + dz);
                int thickness = Math.max(2, Math.round((float) zone.b() * 2f * zoom));
                LineRenderer.drawLine(g, x1, y1, x2, y2, thickness, fill);
                if (zone.a() * zoom >= 26) label(g, mc, name(zone.type()), sx, sy, color);
            }
        }
    }

    private static void drawVent(GuiGraphics g, Minecraft mc, Landforms.Zone zone, int sx, int sy, float zoom,
                                 long time) {
        long phase = Math.floorMod(time + zone.phase(), (long) GEYSER_PERIOD);
        boolean erupting = phase < GEYSER_ERUPTION;
        boolean warning = phase >= GEYSER_PERIOD - GEYSER_WARNING;

        int color = erupting ? 0xFFFFFF : warning ? 0xFFE060 : 0x60E0FF;
        int radius = Math.max(3, Math.round((float) zone.a() * zoom));
        SmoothShapes.drawCircle(g, sx, sy, radius, (0x80 << 24) | color);
        SmoothShapes.drawRing(g, sx, sy, radius + 1, (0xFF << 24) | color);

        if (zoom < 0.25f) return;
        String text;
        if (erupting) {
            text = "Geyser: ERUPTING";
        } else {
            long secondsLeft = (GEYSER_PERIOD - phase + 19) / 20;
            text = "Geyser: " + secondsLeft + "s";
        }
        label(g, mc, text, sx, sy - radius - 11, color);
    }

    private static void label(GuiGraphics g, Minecraft mc, String text, int cx, int y, int rgb) {
        int width = mc.font.width(text);
        g.drawString(mc.font, text, cx - width / 2, y, (0xFF << 24) | rgb, true);
    }

    private static int color(Landforms.ZoneType type) {
        return switch (type) {
            case UPDRAFT -> 0xFF9A3C;
            case LOW_GRAVITY -> 0x9B6BFF;
            case CONVEYOR -> 0xF0D040;
            case BOUNCE -> 0x70E070;
            case SLICK -> 0x70D8FF;
            case GEYSER -> 0x60E0FF;
        };
    }

    private static String name(Landforms.ZoneType type) {
        return switch (type) {
            case UPDRAFT -> "Updraft";
            case LOW_GRAVITY -> "Low gravity";
            case CONVEYOR -> "Conveyor";
            case BOUNCE -> "Bounce";
            case SLICK -> "Slick";
            case GEYSER -> "Geyser";
        };
    }
}
