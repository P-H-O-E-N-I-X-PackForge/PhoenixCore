package net.phoenix.core.common.machine.multiblock.ward;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class MobWardRegistry {

    private static final Map<ResourceKey<Level>, Set<SanctumWardMachine>> ACTIVE = new ConcurrentHashMap<>();

    private MobWardRegistry() {}

    public static void register(SanctumWardMachine ward) {
        Level level = ward.getLevel();
        if (level == null) return;
        ACTIVE.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet()).add(ward);
    }

    public static void unregister(SanctumWardMachine ward) {
        Level level = ward.getLevel();
        if (level == null) return;
        Set<SanctumWardMachine> set = ACTIVE.get(level.dimension());
        if (set != null) set.remove(ward);
    }

    public static boolean isWarded(Level level, double x, double y, double z) {
        Set<SanctumWardMachine> set = ACTIVE.get(level.dimension());
        if (set == null || set.isEmpty()) return false;
        for (SanctumWardMachine ward : set) {
            if (ward.isWardActive() && ward.contains(x, y, z)) return true;
        }
        return false;
    }
}
