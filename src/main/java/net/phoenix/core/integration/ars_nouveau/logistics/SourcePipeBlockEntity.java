package net.phoenix.core.integration.ars_nouveau.logistics;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A Source pipe. Beside a jar, an output hatch or a relay that has Source to give, it draws some out each tick and
 * sends it down the network to whatever beside the network takes Source in: input hatches and sending relays first,
 * then jars that are emptier than where it came from. A tier's throughput limits every pipe on the way, and each pipe
 * wastes a little.
 */
public class SourcePipeBlockEntity extends PipeBlockEntity<SourcePipeTier, SourcePipeData> {

    private static final float JAR_MARGIN = 0.05f;

    /** A pipe works every few ticks, each time carrying that many ticks of its throughput. */
    private static final int PERIOD = 4;

    private WeakReference<SourceNetwork.Net> currentNet = new WeakReference<>(null);

    private long passedTick = Long.MIN_VALUE;
    private int passed = 0;

    public SourcePipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) subscribeServerTick(this::pump);
    }

    @Nullable
    private SourceNetwork.Net getNet() {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        SourceNetwork.Net net = currentNet.get();
        if (net != null && net.isValid() && net.containsNode(getBlockPos())) return net;
        net = SourceNetwork.LevelNet.getOrCreate(serverLevel).getNetFromPos(getBlockPos());
        if (net != null) currentNet = new WeakReference<>(net);
        return net;
    }

    int capacityLeft(long gameTime) {
        int limit = getPipeType().throughput() * PERIOD;
        return passedTick == gameTime ? Math.max(0, limit - passed) : limit;
    }

    void pass(long gameTime, int amount) {
        if (passedTick != gameTime) {
            passedTick = gameTime;
            passed = 0;
        }
        passed += amount;
    }

    @Override
    public boolean canAttachTo(Direction side) {
        if (level == null) return false;
        BlockEntity neighbour = level.getBlockEntity(getBlockPos().relative(side));
        if (neighbour == null || neighbour instanceof SourcePipeBlockEntity) return false;
        return SourceEndpoint.find(level, neighbour.getBlockPos()) != null;
    }

    @Override
    public int getDefaultPaintingColor() {
        return getPipeType().color() & 0xFFFFFF;
    }

    // ------------------------------------------------------------------------------------------ moving Source

    private void pump() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long now = serverLevel.getGameTime();
        // spread the pipes over the ticks so they do not all work at once
        if ((now + (getBlockPos().asLong() & (PERIOD - 1))) % PERIOD != 0) return;
        SourceNetwork.Net net = getNet();
        if (net == null) return;

        for (Direction side : Direction.values()) {
            if (!isConnected(side)) continue;
            SourceEndpoint source = SourceEndpoint.find(serverLevel, getBlockPos().relative(side));
            if (source == null || !source.canGive()) continue;

            int want = Math.min(source.stored(), capacityLeft(now));
            if (want <= 0) continue;

            List<SourceNetwork.Route> routes = net.getNetData(getBlockPos());
            int remaining = want;
            // receivers first, then storage
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (SourceNetwork.Route route : routes) {
                    if (remaining <= 0) break;
                    // never back into where it came from
                    if (getBlockPos().equals(route.getTargetPipePos()) && side == route.getTargetFacing()) continue;
                    SourceEndpoint dest = route.getHandler(serverLevel);
                    if (dest == null || dest.pos().equals(source.pos()) || !dest.canTake()) continue;
                    boolean receiver = dest.kind() == SourceEndpoint.Kind.RECEIVER;
                    if (pass == 0 && !receiver) continue;
                    if (pass == 1 && receiver) continue;
                    // storage only fills from storage that is fuller than it, so jars do not trade back and forth
                    if (!receiver && source.fill() < dest.fill() + JAR_MARGIN) continue;

                    int room = Math.min(remaining, route.room(now));
                    if (room <= 0) continue;
                    int arriving = (int) Math.floor(room * (1f - route.loss()));
                    arriving = Math.min(arriving, dest.space());
                    if (arriving <= 0) continue;
                    // take what it costs, put in what arrives
                    int cost = Math.min(room, (int) Math.ceil(arriving / (1f - route.loss())));
                    int taken = source.give(cost);
                    if (taken <= 0) break;
                    int delivered = dest.take(Math.min(arriving, (int) Math.floor(taken * (1f - route.loss()))));
                    route.pass(now, taken);
                    remaining -= taken;
                    if (delivered > 0 && now % 10 == 0) {
                        ParticleUtil.spawnFollowProjectile(serverLevel, source.pos(), dest.pos(),
                                ParticleColor.defaultParticleColor());
                    }
                }
            }
        }
    }
}
