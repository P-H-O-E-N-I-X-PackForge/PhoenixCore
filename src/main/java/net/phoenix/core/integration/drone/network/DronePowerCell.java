package net.phoenix.core.integration.drone.network;

import net.phoenix.core.client.gui.hud.HudModel;

public final class DronePowerCell {

    private DronePowerCell() {}

    public static HudModel.Cell cell(long draw, long generation) {
        return HudModel.cell("POWER", compact(draw) + " EU/t", "makes " + compact(generation) + " EU/t",
                draw > 0 ? HudModel.GOLD : HudModel.DIM);
    }

    private static String compact(long v) {
        if (v >= 1_000_000_000L) return String.format("%.1fG", v / 1e9);
        if (v >= 1_000_000L) return String.format("%.1fM", v / 1e6);
        if (v >= 10_000L) return String.format("%.1fk", v / 1e3);
        return String.valueOf(v);
    }
}
