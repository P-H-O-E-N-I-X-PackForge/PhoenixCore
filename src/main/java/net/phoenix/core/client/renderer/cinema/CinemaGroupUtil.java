package net.phoenix.core.client.renderer.cinema;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import net.phoenix.core.common.block.cinema.CinemaScreenBlock;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class CinemaGroupUtil {

    private CinemaGroupUtil() {}

    public record GroupLayout(int col, int row, int width, int height) {
        
        public boolean isCenterCell() {
            return col == (width - 1) / 2 && row == (height - 1) / 2;
        }
    }

    private static final GroupLayout SOLO = new GroupLayout(0, 0, 1, 1);
    private static final long REFRESH_INTERVAL_MS = 500;

    private static final int MAX_GROUP_SIZE = 64;

    private static final Map<BlockPos, GroupLayout> cache = new HashMap<>();
    private static final Map<BlockPos, Long> lastComputed = new HashMap<>();

    public static GroupLayout getLayout(Level level, BlockPos pos) {
        long now = System.currentTimeMillis();
        Long last = lastComputed.get(pos);
        if (last != null && now - last < REFRESH_INTERVAL_MS) {
            return cache.getOrDefault(pos, SOLO);
        }

        Map<BlockPos, GroupLayout> computed = computeGroup(level, pos);
        for (Map.Entry<BlockPos, GroupLayout> entry : computed.entrySet()) {
            cache.put(entry.getKey(), entry.getValue());
            lastComputed.put(entry.getKey(), now);
        }
        return computed.getOrDefault(pos, SOLO);
    }

    private static Map<BlockPos, GroupLayout> computeGroup(Level level, BlockPos origin) {
        if (!(level.getBlockEntity(origin) instanceof CinemaScreenBlockEntity originBE)) {
            return Map.of(origin, SOLO);
        }
        Direction facing = originBE.getBlockState().getValue(CinemaScreenBlock.FACING);
        Direction right = facing.getClockWise();

        Map<BlockPos, int[]> offsets = new HashMap<>();
        offsets.put(origin, new int[] { 0, 0 });
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);

        while (!queue.isEmpty() && offsets.size() < MAX_GROUP_SIZE) {
            BlockPos cur = queue.poll();
            int[] cc = offsets.get(cur);
            tryLink(level, facing, cur, right, cc[0] + 1, cc[1], offsets, queue);
            tryLink(level, facing, cur, right.getOpposite(), cc[0] - 1, cc[1], offsets, queue);
            tryLink(level, facing, cur, Direction.UP, cc[0], cc[1] + 1, offsets, queue);
            tryLink(level, facing, cur, Direction.DOWN, cc[0], cc[1] - 1, offsets, queue);
        }

        int minRight = Integer.MAX_VALUE, maxRight = Integer.MIN_VALUE;
        int minUp = Integer.MAX_VALUE, maxUp = Integer.MIN_VALUE;
        for (int[] c : offsets.values()) {
            minRight = Math.min(minRight, c[0]);
            maxRight = Math.max(maxRight, c[0]);
            minUp = Math.min(minUp, c[1]);
            maxUp = Math.max(maxUp, c[1]);
        }
        int width = maxRight - minRight + 1;
        int height = maxUp - minUp + 1;

        Map<BlockPos, GroupLayout> result = new HashMap<>();
        for (Map.Entry<BlockPos, int[]> entry : offsets.entrySet()) {
            int col = entry.getValue()[0] - minRight;
            int rowFromTop = maxUp - entry.getValue()[1]; 
            result.put(entry.getKey(), new GroupLayout(col, rowFromTop, width, height));
        }
        return result;
    }

    private static void tryLink(Level level, Direction facing, BlockPos from, Direction dir,
                                 int newRightSteps, int newUpSteps,
                                 Map<BlockPos, int[]> offsets, Deque<BlockPos> queue) {
        BlockPos next = from.relative(dir);
        if (offsets.containsKey(next)) return;
        if (!(level.getBlockEntity(next) instanceof CinemaScreenBlockEntity nextBE)) return;
        if (nextBE.getBlockState().getValue(CinemaScreenBlock.FACING) != facing) return;

        offsets.put(next, new int[] { newRightSteps, newUpSteps });
        queue.add(next);
    }
}
