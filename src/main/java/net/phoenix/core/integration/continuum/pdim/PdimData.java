package net.phoenix.core.integration.continuum.pdim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PdimData extends SavedData {

    private static final String ID = "continuum_pdim";

    public record Entry(float gravity, int plot) {}

    public record Return(ResourceLocation dimension, double x, double y, double z, float yaw, float pitch) {}

    private final Map<UUID, Map<ResourceLocation, Entry>> entries = new HashMap<>();
    private final Map<ResourceLocation, Integer> nextPlot = new HashMap<>();
    private final Map<UUID, Return> returns = new HashMap<>();

    public static PdimData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PdimData::load, PdimData::new, ID);
    }

    public @Nullable Entry entry(UUID player, ResourceLocation body) {
        return entries.getOrDefault(player, Map.of()).get(body);
    }

    public Map<ResourceLocation, Entry> entries(UUID player) {
        return entries.getOrDefault(player, Map.of());
    }

    public Entry create(UUID player, ResourceLocation body, float gravity) {
        Entry existing = entry(player, body);
        if (existing != null) return existing;

        int plot = nextPlot.merge(body, 1, Integer::sum) - 1;
        Entry entry = new Entry(PdimDimensions.clampGravity(gravity), plot);
        entries.computeIfAbsent(player, p -> new HashMap<>()).put(body, entry);
        setDirty();
        return entry;
    }

    public boolean delete(UUID player, ResourceLocation body) {
        Map<ResourceLocation, Entry> mine = entries.get(player);
        if (mine == null || mine.remove(body) == null) return false;
        setDirty();
        return true;
    }

    public @Nullable Return returnPoint(UUID player) {
        return returns.get(player);
    }

    public void setReturn(UUID player, Return point) {
        returns.put(player, point);
        setDirty();
    }

    public void clearReturn(UUID player) {
        if (returns.remove(player) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (var player : entries.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Player", player.getKey());
            ListTag list = new ListTag();
            for (var entry : player.getValue().entrySet()) {
                CompoundTag e = new CompoundTag();
                e.putString("Body", entry.getKey().toString());
                e.putFloat("Gravity", entry.getValue().gravity());
                e.putInt("Plot", entry.getValue().plot());
                list.add(e);
            }
            playerTag.put("Entries", list);
            players.add(playerTag);
        }
        tag.put("Players", players);

        CompoundTag plots = new CompoundTag();
        nextPlot.forEach((body, next) -> plots.putInt(body.toString(), next));
        tag.put("NextPlot", plots);

        ListTag back = new ListTag();
        for (var entry : returns.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putUUID("Player", entry.getKey());
            Return r = entry.getValue();
            e.putString("Dimension", r.dimension().toString());
            e.putDouble("X", r.x());
            e.putDouble("Y", r.y());
            e.putDouble("Z", r.z());
            e.putFloat("Yaw", r.yaw());
            e.putFloat("Pitch", r.pitch());
            back.add(e);
        }
        tag.put("Returns", back);
        return tag;
    }

    public static PdimData load(CompoundTag tag) {
        PdimData data = new PdimData();
        for (Tag t : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag playerTag = (CompoundTag) t;
            Map<ResourceLocation, Entry> map = new HashMap<>();
            for (Tag e : playerTag.getList("Entries", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) e;
                map.put(new ResourceLocation(entry.getString("Body")),
                        new Entry(entry.getFloat("Gravity"), entry.getInt("Plot")));
            }
            data.entries.put(playerTag.getUUID("Player"), map);
        }
        CompoundTag plots = tag.getCompound("NextPlot");
        for (String key : plots.getAllKeys()) data.nextPlot.put(new ResourceLocation(key), plots.getInt(key));
        for (Tag t : tag.getList("Returns", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            data.returns.put(e.getUUID("Player"), new Return(new ResourceLocation(e.getString("Dimension")),
                    e.getDouble("X"), e.getDouble("Y"), e.getDouble("Z"), e.getFloat("Yaw"), e.getFloat("Pitch")));
        }
        return data;
    }
}
