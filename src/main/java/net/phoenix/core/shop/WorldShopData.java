package net.phoenix.core.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorldShopData extends SavedData {

    private static final String ID = "phoenixcore_shop";

    private final List<ShopEntry> entries = new ArrayList<>();

    public static WorldShopData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(WorldShopData::load, WorldShopData::new, ID);
    }

    public List<ShopEntry> getEntries() {
        return List.copyOf(entries);
    }

    public void addEntry(ShopEntry entry) {
        entries.add(entry);
        setDirty();
    }

    public boolean removeEntry(UUID id) {
        boolean removed = entries.removeIf(e -> e.id.equals(id));
        if (removed) setDirty();
        return removed;
    }

    public void replaceEntry(UUID id, ShopEntry replacement) {
        int idx = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id.equals(id)) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) entries.set(idx, replacement);
        else entries.add(replacement);
        setDirty();
    }

    public static WorldShopData load(CompoundTag tag) {
        WorldShopData data = new WorldShopData();
        ListTag list = tag.getList("entries", 10);
        for (int i = 0; i < list.size(); i++) {
            data.entries.add(ShopEntry.load(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ShopEntry entry : entries) list.add(entry.save());
        tag.put("entries", list);
        return tag;
    }
}
