package net.phoenix.core.integration.conflux.signature;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignatureData extends SavedData {

    private static final String ID = "conflux_signatures";

    private final Map<UUID, Map<String, Double>> values = new HashMap<>();
    private final Map<UUID, Long> lastAdvanced = new HashMap<>();

    public static SignatureData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SignatureData::new, (tag, provider) -> SignatureData.load(tag), null), ID);
    }

    public double get(UUID team, SignatureDefinition def) {
        Double stored = values.getOrDefault(team, Map.of()).get(def.id());
        return stored != null ? stored : def.start();
    }

    public boolean set(UUID team, SignatureDefinition def, double value) {
        double clamped = SignatureDefinition.clamp(value, 0.0, def.max());
        double before = get(team, def);
        if (clamped == before && values.getOrDefault(team, Map.of()).containsKey(def.id())) return false;
        values.computeIfAbsent(team, k -> new HashMap<>()).put(def.id(), clamped);
        setDirty();
        return clamped != before;
    }

    public long lastAdvanced(UUID team, long now) {
        return lastAdvanced.computeIfAbsent(team, k -> {
            setDirty();
            return now;
        });
    }

    public void setLastAdvanced(UUID team, long tick) {
        lastAdvanced.put(team, tick);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag teams = new ListTag();
        for (var entry : values.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("team", entry.getKey());
            CompoundTag meters = new CompoundTag();
            entry.getValue().forEach(meters::putDouble);
            t.put("meters", meters);
            t.putLong("advanced", lastAdvanced.getOrDefault(entry.getKey(), 0L));
            teams.add(t);
        }
        tag.put("teams", teams);
        return tag;
    }

    public static SignatureData load(CompoundTag tag) {
        SignatureData data = new SignatureData();
        ListTag teams = tag.getList("teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag t = teams.getCompound(i);
            UUID team = t.getUUID("team");
            Map<String, Double> meters = new HashMap<>();
            CompoundTag m = t.getCompound("meters");
            for (String key : m.getAllKeys()) meters.put(key, m.getDouble(key));
            data.values.put(team, meters);
            if (t.contains("advanced")) data.lastAdvanced.put(team, t.getLong("advanced"));
        }
        return data;
    }
}
