package net.phoenix.core.integration.conflux.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.integration.conflux.ConfluxDataType;

import org.jetbrains.annotations.Nullable;

public final class ConfluxEndpoint {

    @Nullable
    private final IConfluxDataHandler single;
    @Nullable
    private final IConfluxMultiHandler multi;

    private ConfluxEndpoint(@Nullable IConfluxDataHandler single, @Nullable IConfluxMultiHandler multi) {
        this.single = single;
        this.multi = multi;
    }

    @Nullable
    public static ConfluxEndpoint find(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be instanceof ConfluxPipeBlockEntity) return null;
        IConfluxDataHandler single = ConfluxDataCapability.find(be, side);
        IConfluxMultiHandler multi = ConfluxMultiHandlerCapability.find(be, side);
        if (single == null && multi == null) return null;
        return new ConfluxEndpoint(single, multi);
    }

    public boolean accepts(ConfluxDataType type) {
        if (single != null) return single.getDataType() == type;
        return multi != null;
    }

    public long insert(ConfluxDataType type, long amount) {
        if (single != null) {
            return single.getDataType() == type ? single.insert(amount) : 0L;
        }
        return multi != null ? multi.insert(type, amount) : 0L;
    }
}
