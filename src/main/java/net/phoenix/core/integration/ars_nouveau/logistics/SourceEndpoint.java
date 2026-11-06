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

/**
 * Whatever holds Source beside a pipe: an Ars jar, one of our hatches, or a relay. What it is decides which way Source
 * may go: an output hatch or a receiving relay only gives, an input hatch or a sending relay only takes, and a jar does
 * both.
 */
public final class SourceEndpoint {

    public enum Kind {
        /** Only takes Source in. */
        RECEIVER,
        /** Only gives Source out. */
        PROVIDER,
        /** Holds Source and does either. */
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

    /** The endpoint at {@code pos}, or null when nothing there holds Source. */
    @Nullable
    public static SourceEndpoint find(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be instanceof SourcePipeBlockEntity) return null;

        if (be instanceof SourceRelayMachine relay) {
            return new SourceEndpoint(be, relay.getSource(), relay.isSender() ? Kind.RECEIVER : Kind.PROVIDER);
        }
        if (be instanceof net.phoenix.core.integration.conflux.producer.ConfluxProducerMachine producer &&
                producer.takesSource()) {
            // an arcane data producer eats Source: it only takes it
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

    /** How full it is, 0 to 1. */
    public float fill() {
        int max = tile.getMaxSource();
        return max <= 0 ? 0f : tile.getSource() / (float) max;
    }

    /** Takes up to {@code amount} out; returns what came out. */
    public int give(int amount) {
        int before = tile.getSource();
        tile.removeSource(amount);
        int taken = before - tile.getSource();
        if (taken > 0) changed();
        return taken;
    }

    /** Puts up to {@code amount} in; returns what went in. */
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

    /** Which side of the endpoint faces the pipe at {@code from}, for the particles. */
    public static Direction facing(BlockPos from, BlockPos to) {
        for (Direction d : Direction.values()) if (from.relative(d).equals(to)) return d;
        return Direction.UP;
    }
}
