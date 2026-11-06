package net.phoenix.core.integration.drone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.phoenix.core.integration.drone.network.DroneTargetView;
import net.phoenix.core.integration.drone.network.S2CDroneOverlayPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DroneOverlayCache {

    public record Entry(int range, List<DroneTargetView> targets, long receivedTick) {}

    private static final long EXPIRE_TICKS = 100;
    private static final Map<BlockPos, Entry> ENTRIES = new HashMap<>();

    private DroneOverlayCache() {}

    public static void accept(S2CDroneOverlayPacket msg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        ENTRIES.put(msg.controller(), new Entry(msg.range(), msg.targets(), level.getGameTime()));
    }

    public static Map<BlockPos, Entry> live() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            ENTRIES.clear();
            return ENTRIES;
        }
        long now = level.getGameTime();
        ENTRIES.values().removeIf(e -> now - e.receivedTick() > EXPIRE_TICKS);
        return ENTRIES;
    }
}
