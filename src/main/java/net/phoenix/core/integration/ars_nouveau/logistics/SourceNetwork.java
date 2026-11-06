package net.phoenix.core.integration.ars_nouveau.logistics;

import com.gregtechceu.gtceu.api.pipenet.IRoutePath;
import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;
import com.gregtechceu.gtceu.api.pipenet.Node;
import com.gregtechceu.gtceu.api.pipenet.PipeNet;
import com.gregtechceu.gtceu.api.pipenet.PipeNetWalker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.phoenix.core.PhoenixCore;

import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The network of Source pipes, built the way GregTech builds its own: the nodes live in a per-dimension saved store, a
 * walker lists every endpoint reachable from a pipe, and the routes are kept until the network changes.
 */
public final class SourceNetwork {

    private SourceNetwork() {}

    /** One way out from a pipe: the last pipe, the side that touches the endpoint, and the pipes on the way. */
    public static class Route implements IRoutePath<SourceEndpoint> {

        private final BlockPos targetPipePos;
        private final Direction targetFacing;
        private final SourcePipeBlockEntity[] path;
        private final int distance;

        Route(BlockPos targetPipePos, Direction targetFacing, SourcePipeBlockEntity[] path, int distance) {
            this.targetPipePos = targetPipePos;
            this.targetFacing = targetFacing;
            this.path = path;
            this.distance = distance;
        }

        @Override
        public BlockPos getTargetPipePos() {
            return targetPipePos;
        }

        @Override
        public Direction getTargetFacing() {
            return targetFacing;
        }

        @Override
        public int getDistance() {
            return distance;
        }

        public SourcePipeBlockEntity[] getPath() {
            return path;
        }

        @Nullable
        @Override
        public SourceEndpoint getHandler(Level level) {
            return SourceEndpoint.find(level, targetPipePos.relative(targetFacing));
        }

        /** The share of Source this route wastes: every pipe on it costs its tier's loss. */
        public float loss() {
            float kept = 1f;
            for (SourcePipeBlockEntity pipe : path) kept *= 1f - pipe.getPipeType().lossPerBlock();
            return Math.min(0.9f, 1f - kept);
        }

        /** What the route can still pass this tick. */
        public int room(long gameTime) {
            int room = Integer.MAX_VALUE;
            for (SourcePipeBlockEntity pipe : path) room = Math.min(room, pipe.capacityLeft(gameTime));
            return room;
        }

        public void pass(long gameTime, int amount) {
            for (SourcePipeBlockEntity pipe : path) pipe.pass(gameTime, amount);
        }
    }

    public static class Net extends PipeNet<SourcePipeData> {

        private final Map<BlockPos, List<Route>> routes = new HashMap<>();

        public Net(LevelNet world) {
            super(world);
        }

        /** The ways out from this pipe, nearest first. */
        public List<Route> getNetData(BlockPos pipePos) {
            List<Route> data = routes.get(pipePos);
            if (data == null) {
                data = Walker.createNetData(this, pipePos);
                if (data == null) return Collections.emptyList();
                data.sort(Comparator.comparingInt(Route::getDistance));
                routes.put(pipePos, data);
            }
            return data;
        }

        @Override
        public void onNeighbourUpdate(BlockPos fromPos) {
            routes.clear();
        }

        @Override
        public void onPipeConnectionsUpdate() {
            routes.clear();
        }

        @Override
        protected void transferNodeData(Map<BlockPos, Node<SourcePipeData>> transferredNodes,
                                        PipeNet<SourcePipeData> parentNet) {
            super.transferNodeData(transferredNodes, parentNet);
            routes.clear();
            ((Net) parentNet).routes.clear();
        }

        @Override
        protected void writeNodeData(SourcePipeData data, CompoundTag tag) {
            tag.putInt("tier", data.tier.ordinal());
        }

        @Override
        protected SourcePipeData readNodeData(CompoundTag tag) {
            SourcePipeTier[] tiers = SourcePipeTier.values();
            return SourcePipeData.of(tiers[Math.max(0, Math.min(tiers.length - 1, tag.getInt("tier")))]);
        }
    }

    public static class LevelNet extends LevelPipeNet<SourcePipeData, Net> {

        private static final String DATA_ID = "phoenix_source_pipe_net";

        public static LevelNet getOrCreate(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(() -> new LevelNet(level),
                            (tag, provider) -> new LevelNet(level, tag, provider), null),
                    DATA_ID);
        }

        public LevelNet(ServerLevel level) {
            super(level);
        }

        public LevelNet(ServerLevel level, CompoundTag tag, HolderLookup.Provider provider) {
            super(level, tag, provider);
        }

        @Override
        protected Net createNetInstance() {
            return new Net(this);
        }
    }

    static class Walker extends PipeNetWalker<SourcePipeBlockEntity, SourcePipeData, Net> {

        @Nullable
        static List<Route> createNetData(Net net, BlockPos source) {
            try {
                Walker walker = new Walker(net, source, 1, new ArrayList<>());
                walker.traversePipeNet();
                return walker.found;
            } catch (Exception e) {
                PhoenixCore.LOGGER.error("Could not walk a Source pipe network", e);
            }
            return null;
        }

        private final List<Route> found;
        private SourcePipeBlockEntity[] pipes = {};

        Walker(Net net, BlockPos source, int walkedBlocks, List<Route> found) {
            super(net, source, walkedBlocks);
            this.found = found;
        }

        @Override
        protected PipeNetWalker<SourcePipeBlockEntity, SourcePipeData, Net> createSubWalker(Net net, Direction facing,
                                                                                            BlockPos next,
                                                                                            int walkedBlocks) {
            Walker walker = new Walker(net, next, walkedBlocks, found);
            walker.pipes = pipes;
            return walker;
        }

        @Override
        protected void checkPipe(SourcePipeBlockEntity pipe, BlockPos pos) {
            pipes = Arrays.copyOf(pipes, pipes.length + 1);
            pipes[pipes.length - 1] = pipe;
        }

        @Override
        protected void checkNeighbour(SourcePipeBlockEntity pipe, BlockPos pipePos, Direction face,
                                      @Nullable BlockEntity neighbour) {
            if (pipe != pipes[pipes.length - 1]) {
                throw new IllegalStateException("The current pipe is not the last pipe walked");
            }
            if (neighbour == null || neighbour instanceof SourcePipeBlockEntity) return;
            SourceEndpoint endpoint = SourceEndpoint.find(neighbour.getLevel(), neighbour.getBlockPos());
            if (endpoint == null) return;
            found.add(new Route(pipePos.immutable(), face, pipes, getWalkedBlocks()));
        }

        @Override
        protected Class<SourcePipeBlockEntity> getBasePipeClass() {
            return SourcePipeBlockEntity.class;
        }
    }
}
