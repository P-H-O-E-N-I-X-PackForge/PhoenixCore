package net.phoenix.core.saveddata;

import com.gregtechceu.gtceu.common.machine.owner.FTBOwner;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreativeDataAccessSavedData extends SavedData {

    public static String DATA_NAME = "creativeDataAccessData";
    public static String ENERGY_OWNERS = "creativeDataOwners";

    private final Map<UUID, Boolean> ownersMap = new HashMap<>();

    private CreativeDataAccessSavedData() {}

    public CreativeDataAccessSavedData(CompoundTag tag) {
        var ownerList = tag.getList(ENERGY_OWNERS, CompoundTag.TAG_COMPOUND);
        for (Tag t : ownerList) {
            CompoundTag ownerTag = (CompoundTag) t;
            long ownerUUIDMSB = ownerTag.getLong("ownerUUIDMSB");
            long ownerUUIDLSB = ownerTag.getLong("ownerUUIDLSB");
            boolean enabled = ownerTag.getBoolean("enabled");
            UUID ownerUUID = new UUID(ownerUUIDMSB, ownerUUIDLSB);
            ownersMap.put(ownerUUID, enabled);
        }
    }

    public static CreativeDataAccessSavedData getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(CreativeDataAccessSavedData::new,
                        (tag, provider) -> new CreativeDataAccessSavedData(tag), null),
                DATA_NAME);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag ownerList = new ListTag();
        for (UUID ownerUUID : ownersMap.keySet()) {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putLong("ownerUUIDMSB", ownerUUID.getMostSignificantBits());
            ownerTag.putLong("ownerUUIDLSB", ownerUUID.getLeastSignificantBits());
            ownerTag.putBoolean("enabled", ownersMap.get(ownerUUID));
            ownerList.add(ownerTag);
        }
        nbt.put(ENERGY_OWNERS, ownerList);
        return nbt;
    }

    public boolean isEnabledFor(UUID uuid) {
        FTBOwner owner = new FTBOwner(uuid);
        return ownersMap.keySet().stream().filter(ownersMap::get).anyMatch(owner::isPlayerInTeam);
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        boolean isEnabled = isEnabledFor(uuid);
        if (isEnabled != enabled) {
            ownersMap.put(uuid, enabled);
            setDirty();
        }
    }
}
