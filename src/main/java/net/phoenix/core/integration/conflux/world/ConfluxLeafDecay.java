package net.phoenix.core.integration.conflux.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.phoenix.core.PhoenixCore;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ConfluxLeafDecay {

    private static final int SUPPORT_RANGE = 7;

    private static final int MAX_DELAY = 12;
    private static final int MAX_SCANS_PER_TICK = 8;
    private static final int MAX_REMOVALS_PER_TICK = 256;

    private record Pending(long due, long pos) {}

    private static final class State {

        final ArrayDeque<long[]> scans = new ArrayDeque<>();
        final LongOpenHashSet scanSet = new LongOpenHashSet();
        final PriorityQueue<Pending> removals = new PriorityQueue<>((a, b) -> Long.compare(a.due, b.due));
        final LongOpenHashSet removalSet = new LongOpenHashSet();
    }

    private static final Map<ResourceKey<Level>, State> STATES = new HashMap<>();

    private ConfluxLeafDecay() {}

    private static boolean isConfluxDimension(Level level) {
        return PhoenixCore.MOD_ID.equals(level.dimension().location().getNamespace()) &&
                level.dimension().location().getPath().startsWith("conflux/");
    }

    private static boolean isLeaves(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LEAVES);
    }

    private static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private static void queueScan(ServerLevel level, BlockPos pos) {
        State st = STATES.computeIfAbsent(level.dimension(), k -> new State());
        long key = pos.asLong();
        if (st.scanSet.add(key)) {
            st.scans.add(new long[] { key, level.getGameTime() + 1 });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor accessor = event.getLevel();
        if (!(accessor instanceof ServerLevel level) || !isConfluxDimension(level)) return;
        if (!isLog(event.getState())) return;
        queueScan(level, event.getPos().immutable());
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isConfluxDimension(level)) return;
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (isLog(level.getBlockState(pos))) queueScan(level, pos.immutable());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        State st = STATES.get(level.dimension());
        if (st == null) return;

        long now = level.getGameTime();

        int scans = 0;
        while (scans < MAX_SCANS_PER_TICK && !st.scans.isEmpty() && st.scans.peek()[1] <= now) {
            long[] next = st.scans.poll();
            st.scanSet.remove(next[0]);
            scan(level, st, BlockPos.of(next[0]));
            scans++;
        }

        int removed = 0;
        while (removed < MAX_REMOVALS_PER_TICK && !st.removals.isEmpty() && st.removals.peek().due <= now) {
            Pending p = st.removals.poll();
            st.removalSet.remove(p.pos);
            decay(level, BlockPos.of(p.pos));
            removed++;
        }

        if (st.scans.isEmpty() && st.removals.isEmpty()) STATES.remove(level.dimension());
    }

    private static BlockState stateAt(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) ? level.getBlockState(pos) : net.minecraft.world.level.block.Blocks.AIR
                .defaultBlockState();
    }

    private static void scan(ServerLevel level, State st, BlockPos origin) {
        Long2IntOpenHashMap fromOrigin = new Long2IntOpenHashMap();
        fromOrigin.defaultReturnValue(-1);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        for (Direction dir : Direction.values()) {
            BlockPos n = origin.relative(dir);
            if (isLeaves(stateAt(level, n))) {
                fromOrigin.put(n.asLong(), 1);
                queue.add(n);
            }
        }
        if (queue.isEmpty()) return;

        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = fromOrigin.get(p.asLong());
            if (d >= SUPPORT_RANGE * 2) continue;
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                long key = n.asLong();
                if (fromOrigin.containsKey(key)) continue;
                if (!isLeaves(stateAt(level, n))) continue;
                fromOrigin.put(key, d + 1);
                queue.add(n);
            }
        }

        Long2IntOpenHashMap support = new Long2IntOpenHashMap();
        support.defaultReturnValue(-1);
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();

        for (long key : fromOrigin.keySet()) {
            BlockPos p = BlockPos.of(key);
            for (Direction dir : Direction.values()) {
                if (isLog(stateAt(level, p.relative(dir)))) {
                    support.put(key, 1);
                    frontier.add(p);
                    break;
                }
            }
        }
        while (!frontier.isEmpty()) {
            BlockPos p = frontier.poll();
            int s = support.get(p.asLong());
            if (s >= SUPPORT_RANGE) continue;
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                long key = n.asLong();
                if (!fromOrigin.containsKey(key) || support.containsKey(key)) continue;
                support.put(key, s + 1);
                frontier.add(n);
            }
        }

        long now = level.getGameTime();
        for (Long2IntOpenHashMap.Entry e : fromOrigin.long2IntEntrySet()) {
            if (e.getIntValue() > SUPPORT_RANGE) continue;
            long key = e.getLongKey();
            if (support.containsKey(key)) continue;
            if (!st.removalSet.add(key)) continue;
            st.removals.add(new Pending(now + 1 + level.random.nextInt(MAX_DELAY), key));
        }
    }

    private static void decay(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!isLeaves(state)) return;

        for (Direction dir : Direction.values()) {
            if (isLog(level.getBlockState(pos.relative(dir)))) return;
        }

        Block.dropResources(state, level, pos);
        level.removeBlock(pos, false);
    }
}
