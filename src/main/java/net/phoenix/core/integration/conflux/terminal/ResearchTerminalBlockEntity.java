package net.phoenix.core.integration.conflux.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.pipe.ConfluxMultiHandlerCapability;
import net.phoenix.core.integration.conflux.pipe.IConfluxMultiHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class ResearchTerminalBlockEntity extends BlockEntity {

    public static final long CAPACITY_PER_TYPE = ConfluxDataStore.CAPACITY_PER_TYPE;

    /** The team whose data pool this terminal feeds. Set when placed or first used. */
    private @Nullable UUID ownerTeam;
    /** Data held by terminals from before data moved into {@link ConfluxDataStore}; moved over once owned. */
    private final Map<ConfluxDataType, Long> legacy = new EnumMap<>(ConfluxDataType.class);
    private final LazyOptional<IConfluxMultiHandler> handlerOpt;

    public ResearchTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        handlerOpt = LazyOptional.of(this::buildHandler);
    }

    public @Nullable UUID getOwnerTeam() {
        return ownerTeam;
    }

    /** Takes ownership if nobody has it yet, and moves any legacy contents into the team's pool. */
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

    /** Server side only; the client reads its team's pool from the research sync instead. */
    public long getStored(ConfluxDataType type) {
        ConfluxDataStore store = store();
        return store == null || ownerTeam == null ? 0L : store.stored(ownerTeam, type);
    }

    public long getCapacity(ConfluxDataType type) {
        return CAPACITY_PER_TYPE;
    }

    /** Spends from the owning team's pool. */
    public boolean trySpend(Map<ConfluxDataType, Long> costs) {
        ConfluxDataStore store = store();
        return store != null && ownerTeam != null && store.trySpend(ownerTeam, costs);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ConfluxMultiHandlerCapability.MULTI_DATA) return handlerOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerOpt.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerTeam != null) tag.putUUID("owner", ownerTeam);
        if (!legacy.isEmpty()) {
            CompoundTag data = new CompoundTag();
            legacy.forEach((type, amt) -> data.putLong(type.id(), amt));
            tag.put("data", data);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
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
