package net.phoenix.core.integration.conflux.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class HookState extends SavedData {

    private static final String DATA_NAME = "phoenixcore_conflux_hooks";

    private final Set<String> fired = new HashSet<>();

    public static HookState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(HookState::new, HookState::load), DATA_NAME);
    }

    public boolean has(String key) {
        return fired.contains(key);
    }

    public void mark(String key) {
        if (fired.add(key)) setDirty();
    }

    private static HookState load(CompoundTag tag, HolderLookup.Provider registries) {
        HookState state = new HookState();
        ListTag list = tag.getList("Fired", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) state.fired.add(list.getString(i));
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (String s : fired) list.add(StringTag.valueOf(s));
        tag.put("Fired", list);
        return tag;
    }
}
