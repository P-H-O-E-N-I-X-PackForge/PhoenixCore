package net.phoenix.core.integration.drone.group;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class DroneWorldData extends SavedData {

    private static final String ID = "phoenixcore_drone_config";

    private final Map<BlockPos, DroneConfig> configs = new HashMap<>();

    public static DroneWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DroneWorldData::load, DroneWorldData::new, ID);
    }

    public DroneConfig getOrCreate(BlockPos dronePos) {
        return configs.computeIfAbsent(dronePos, p -> new DroneConfig());
    }

    public void markDirty() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (var entry : configs.entrySet()) {
            CompoundTag entryTag = entry.getValue().save();
            entryTag.putLong("dronePos", entry.getKey().asLong());
            list.add(entryTag);
        }
        tag.put("drones", list);
        return tag;
    }

    public static DroneWorldData load(CompoundTag tag) {
        DroneWorldData data = new DroneWorldData();
        ListTag list = tag.getList("drones", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            data.configs.put(BlockPos.of(entryTag.getLong("dronePos")), DroneConfig.load(entryTag));
        }
        return data;
    }
}
