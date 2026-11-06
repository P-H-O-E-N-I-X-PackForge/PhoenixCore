package net.phoenix.core.integration.drone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.phoenix.core.integration.drone.network.S2CDroneFleetPacket;

import java.util.HashMap;
import java.util.Map;

public final class DroneFleetCache {

    public record Entry(int drones, java.util.List<S2CDroneFleetPacket.Target> targets, long receivedTick) {}

    private static final long EXPIRE_TICKS = 100;
    private static final Map<BlockPos, Entry> ENTRIES = new HashMap<>();

    private DroneFleetCache() {}

    public static void accept(S2CDroneFleetPacket msg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        ENTRIES.put(msg.controller(), new Entry(msg.drones(), msg.targets(), level.getGameTime()));
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
