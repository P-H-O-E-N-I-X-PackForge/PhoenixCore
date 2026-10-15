package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.integration.conflux.ConfluxDataType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfluxPipeBlockEntity extends PipeBlockEntity<ConfluxPipeType, ConfluxPipeData>
        implements ConfluxDataCapability.Holder {

    public static final long THROUGHPUT = 64L;
    public static final long BUFFER = 256L;

    @SaveField(nbtKey = "stored")
    private long stored = 0L;
    private final IConfluxDataHandler handler;
    @Nullable
    private TickableSubscription tickSub;

    public ConfluxPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.handler = buildHandler();
    }

    @Override
    public @Nullable IConfluxDataHandler getConfluxDataHandler(@Nullable Direction side) {
        if (side != null && !isConnected(side)) return null;
        return handler;
    }

    private IConfluxDataHandler buildHandler() {
        return new IConfluxDataHandler() {

            @Override
            public ConfluxDataType getDataType() {
                return getPipeType().dataType();
            }

            @Override
            public long insert(long amount) {
                long accepted = Math.min(amount, BUFFER - stored);
                if (accepted <= 0) return 0;
                stored += accepted;
                setChanged();
                scheduleTickIfNeeded();
                return accepted;
            }

            @Override
            public long extract(long amount) {
                long given = Math.min(amount, stored);
                stored -= given;
                if (given > 0) setChanged();
                return given;
            }

            @Override
            public long getStored() {
                return stored;
            }

            @Override
            public long getCapacity() {
                return BUFFER;
            }
        };
    }

    @Override
    public boolean canAttachTo(Direction side) {
        if (level == null) return false;
        BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(side));
        if (neighbor == null || neighbor instanceof ConfluxPipeBlockEntity) return false;

        ConfluxDataType dt = getPipeType().dataType();
        if (ConfluxMultiHandlerCapability.find(neighbor, side.getOpposite()) != null) {
            return true;
        }
        IConfluxDataHandler h = ConfluxDataCapability.find(neighbor, side.getOpposite());
        return h != null && h.getDataType() == dt;
    }

    private void scheduleTickIfNeeded() {
        if (tickSub == null || !tickSub.isStillSubscribed()) {
            tickSub = subscribeServerTick(this::doServerTick);
        }
    }

    private void doServerTick() {
        if (stored == 0 || level == null) {
            unsubscribeTick();
            return;
        }

        long budget = Math.min(stored, THROUGHPUT);
        ConfluxDataType dt = getPipeType().dataType();

        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            if (!isConnected(dir)) continue;

            BlockPos npos = getBlockPos().relative(dir);
            BlockEntity be = level.getBlockEntity(npos);
            if (be == null) continue;

            if (be instanceof ConfluxPipeBlockEntity peer && peer.stored >= stored) continue;

            IConfluxDataHandler h = ConfluxDataCapability.find(be, dir.getOpposite());
            if (h != null) {
                if (h.getDataType() != dt) continue;
                long accepted = h.insert(budget);
                if (accepted > 0) {
                    stored -= accepted;
                    budget -= accepted;
                    setChanged();
                }
                continue;
            }

            IConfluxMultiHandler multi = ConfluxMultiHandlerCapability.find(be, dir.getOpposite());
            if (multi != null) {
                long accepted = multi.insert(dt, budget);
                if (accepted > 0) {
                    stored -= accepted;
                    budget -= accepted;
                    setChanged();
                }
            }
        }

        if (stored == 0) unsubscribeTick();
    }

    private void unsubscribeTick() {
        unsubscribe(tickSub);
        tickSub = null;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (stored > 0) scheduleTickIfNeeded();
    }

    public ConfluxDataType getDataType() {
        return getPipeType().dataType();
    }

    public long getStored() {
        return stored;
    }
}
