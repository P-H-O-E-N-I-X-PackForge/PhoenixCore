package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.integration.conflux.ConfluxDataType;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.EnumMap;

public class ConfluxPipeBlockEntity extends PipeBlockEntity<ConfluxPipeType, ConfluxPipeData>
                                    implements ConfluxDataCapability.Holder {

    public static final long THROUGHPUT = 64L;

    private WeakReference<ConfluxPipeNet> currentNet = new WeakReference<>(null);
    private final EnumMap<Direction, NetHandler> handlers = new EnumMap<>(Direction.class);
    private NetHandler defaultHandler;

    private long passedTick = Long.MIN_VALUE;
    private long passed = 0L;

    private long lastSecondTotal = 0L;
    private long secondTotal = 0L;
    private long secondStart = Long.MIN_VALUE;

    public ConfluxPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    private ConfluxPipeNet getNet() {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        ConfluxPipeNet net = currentNet.get();
        if (net != null && net.isValid() && net.containsNode(getBlockPos())) return net;
        net = LevelConfluxPipeNet.getOrCreate(serverLevel).getNetFromPos(getBlockPos());
        if (net != null) currentNet = new WeakReference<>(net);
        return net;
    }

    @Override
    public @Nullable IConfluxDataHandler getConfluxDataHandler(@Nullable Direction side) {
        if (side != null && !isConnected(side)) return null;
        if (defaultHandler == null) {
            for (Direction facing : Direction.values()) handlers.put(facing, new NetHandler(facing));
            defaultHandler = new NetHandler(null);
        }
        return side == null ? defaultHandler : handlers.get(side);
    }

    long capacityLeft(long gameTime) {
        return passedTick == gameTime ? Math.max(0L, THROUGHPUT - passed) : THROUGHPUT;
    }

    void pass(long gameTime, long amount) {
        if (passedTick != gameTime) {
            passedTick = gameTime;
            passed = 0L;
        }
        passed += amount;
        if (secondStart == Long.MIN_VALUE || gameTime - secondStart >= 20) {
            lastSecondTotal = secondTotal;
            secondTotal = 0L;
            secondStart = gameTime;
        }
        secondTotal += amount;
    }

    public long getFlowPerSecond() {
        return level != null && level.getGameTime() - secondStart > 40 ? 0L : lastSecondTotal;
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

    public ConfluxDataType getDataType() {
        return getPipeType().dataType();
    }

    @Override
    public int getDefaultPaintingColor() {
        return net.phoenix.core.integration.conflux.client.ConfluxTheme.typeColor(getPipeType().dataType()) & 0xFFFFFF;
    }

    private final class NetHandler implements IConfluxDataHandler {

        @Nullable
        private final Direction side;
        private boolean routing;

        NetHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public ConfluxDataType getDataType() {
            return getPipeType().dataType();
        }

        @Override
        public long insert(long amount) {
            if (routing || amount <= 0 || !(level instanceof ServerLevel serverLevel)) return 0L;
            ConfluxPipeNet net = getNet();
            if (net == null) return 0L;

            ConfluxDataType type = getPipeType().dataType();
            long gameTime = serverLevel.getGameTime();
            long remaining = amount;
            routing = true;
            try {
                for (ConfluxRoutePath route : net.getNetData(getBlockPos())) {
                    if (remaining <= 0) break;

                    if (side != null && getBlockPos().equals(route.getTargetPipePos()) &&
                            side == route.getTargetFacing()) {
                        continue;
                    }
                    long room = remaining;
                    for (ConfluxPipeBlockEntity pipe : route.getPath()) {
                        room = Math.min(room, pipe.capacityLeft(gameTime));
                    }
                    if (room <= 0) continue;

                    ConfluxEndpoint destination = route.getHandler(serverLevel);
                    if (destination == null) continue;
                    long accepted = destination.insert(type, room);
                    if (accepted <= 0) continue;

                    for (ConfluxPipeBlockEntity pipe : route.getPath()) pipe.pass(gameTime, accepted);
                    remaining -= accepted;
                }
            } finally {
                routing = false;
            }
            return amount - remaining;
        }

        @Override
        public long extract(long amount) {
            return 0L;
        }

        @Override
        public long getStored() {
            return 0L;
        }

        @Override
        public long getCapacity() {
            return THROUGHPUT;
        }
    }
}
