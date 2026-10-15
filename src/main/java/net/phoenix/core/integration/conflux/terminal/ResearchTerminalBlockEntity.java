package net.phoenix.core.integration.conflux.terminal;


import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.integration.conflux.pipe.IConfluxMultiHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class ResearchTerminalBlockEntity extends BlockEntity implements ConfluxMultiHandlerCapability.Holder {

    public static final long CAPACITY_PER_TYPE = ConfluxDataStore.CAPACITY_PER_TYPE;

    private @Nullable UUID ownerTeam;

    private final Map<ConfluxDataType, Long> legacy = new EnumMap<>(ConfluxDataType.class);
    private final IConfluxMultiHandler handler;

    public ResearchTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        handler = buildHandler();
    }

    public @Nullable UUID getOwnerTeam() {
        return ownerTeam;
    }

    public void adopt(UUID team) {
        if (ownerTeam == null) {
            ownerTeam = team;
            setChanged();
        }
        migrateLegacy();
    }

    private void migrateLegacy() {
        if (ownerTeam == null || legacy.isEmpty() || !(level instanceof ServerLevel server)) return;
        ConfluxDataStore store = ConfluxDataStore.get(server);
        legacy.forEach((type, amt) -> store.insert(ownerTeam, type, amt));
        legacy.clear();
        setChanged();
    }

    private @Nullable ConfluxDataStore store() {
        return level instanceof ServerLevel server ? ConfluxDataStore.get(server) : null;
    }

    private IConfluxMultiHandler buildHandler() {
        return new IConfluxMultiHandler() {

            @Override
            public long insert(ConfluxDataType type, long amount) {
                ConfluxDataStore store = store();
                if (store == null || ownerTeam == null) return 0;
                migrateLegacy();
                return store.insert(ownerTeam, type, amount);
            }

            @Override
            public long extract(ConfluxDataType type, long amount) {
                ConfluxDataStore store = store();
                if (store == null || ownerTeam == null) return 0;
                return store.extract(ownerTeam, type, amount);
            }

            @Override
            public long getStored(ConfluxDataType type) {
                return ResearchTerminalBlockEntity.this.getStored(type);
            }

            @Override
            public long getCapacity(ConfluxDataType type) {
                return CAPACITY_PER_TYPE;
            }
        };
    }

    public long getStored(ConfluxDataType type) {
        ConfluxDataStore store = store();
        return store == null || ownerTeam == null ? 0L : store.stored(ownerTeam, type);
    }

    public long getCapacity(ConfluxDataType type) {
        return CAPACITY_PER_TYPE;
    }

    public boolean trySpend(Map<ConfluxDataType, Long> costs) {
        ConfluxDataStore store = store();
        return store != null && ownerTeam != null && store.trySpend(ownerTeam, costs);
    }

    @Override
    public @Nullable IConfluxMultiHandler getConfluxMultiHandler(@Nullable Direction side) {
        return handler;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerTeam != null) tag.putUUID("owner", ownerTeam);
        if (!legacy.isEmpty()) {
            CompoundTag data = new CompoundTag();
            legacy.forEach((type, amt) -> data.putLong(type.id(), amt));
            tag.put("data", data);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("owner")) ownerTeam = tag.getUUID("owner");
        legacy.clear();
        if (tag.contains("data")) {
            CompoundTag data = tag.getCompound("data");
            for (ConfluxDataType type : ConfluxDataType.values()) {
                long amt = data.getLong(type.id());
                if (amt > 0) legacy.put(type, amt);
            }
        }
    }
}
