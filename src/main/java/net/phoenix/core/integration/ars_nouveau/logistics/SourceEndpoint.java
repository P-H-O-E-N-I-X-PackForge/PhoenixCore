package net.phoenix.core.integration.ars_nouveau.logistics;

import com.gregtechceu.gtceu.api.capability.recipe.IO;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source.SourceHatchPartMachine;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import org.jetbrains.annotations.Nullable;

public final class SourceEndpoint {

    public enum Kind {

        RECEIVER,

        PROVIDER,

        BUFFER
    }

    private final BlockEntity entity;
    private final ISourceTile tile;
    private final Kind kind;

    private SourceEndpoint(BlockEntity entity, ISourceTile tile, Kind kind) {
        this.entity = entity;
        this.tile = tile;
        this.kind = kind;
    }

    @Nullable
    public static SourceEndpoint find(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be instanceof SourcePipeBlockEntity) return null;

        if (be instanceof SourceRelayMachine relay) {
            return new SourceEndpoint(be, relay.getSource(), relay.isSender() ? Kind.RECEIVER : Kind.PROVIDER);
        }
        if (be instanceof net.phoenix.core.integration.conflux.producer.ConfluxProducerMachine producer &&
                producer.takesSource()) {

            return new SourceEndpoint(be, producer.getSource(), Kind.RECEIVER);
        }
        if (be instanceof SourceHatchPartMachine hatch) {
            return new SourceEndpoint(be, hatch.getSource(), hatch.getIo() == IO.IN ? Kind.RECEIVER : Kind.PROVIDER);
        }
        if (be instanceof ISourceProviderCapability provider && provider.getSource() != null) {
            return new SourceEndpoint(be, provider.getSource(), Kind.BUFFER);
        }
        if (be instanceof ISourceTile tile) {
            return new SourceEndpoint(be, tile, Kind.BUFFER);
        }
        return null;
    }

    public BlockPos pos() {
        return entity.getBlockPos();
    }

    public Kind kind() {
        return kind;
    }

    public boolean canGive() {
        return kind != Kind.RECEIVER && tile.getSource() > 0;
    }

    public boolean canTake() {
        return kind != Kind.PROVIDER && tile.canAcceptSource() && space() > 0;
    }

    public int stored() {
        return tile.getSource();
    }

    public int space() {
        return Math.max(0, tile.getMaxSource() - tile.getSource());
    }

    public float fill() {
        int max = tile.getMaxSource();
        return max <= 0 ? 0f : tile.getSource() / (float) max;
    }

    public int give(int amount) {
        int before = tile.getSource();
        tile.removeSource(amount);
        int taken = before - tile.getSource();
        if (taken > 0) changed();
        return taken;
    }

    public int take(int amount) {
        int before = tile.getSource();
        tile.addSource(amount);
        int added = tile.getSource() - before;
        if (added > 0) changed();
        return added;
    }

    private void changed() {
        entity.setChanged();
        Level level = entity.getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(), 3);
        }
    }

    public static Direction facing(BlockPos from, BlockPos to) {
        for (Direction d : Direction.values()) if (from.relative(d).equals(to)) return d;
        return Direction.UP;
    }
}
