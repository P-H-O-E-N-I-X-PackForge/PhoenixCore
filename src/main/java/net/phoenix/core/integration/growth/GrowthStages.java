package net.phoenix.core.integration.growth;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Declares a growing machine's stages in a few lines instead of a loop in every machine:
 *
 * <pre>
 * GrowthStages.of(bounds(2, 1, 1, 1, 0, 2), bounds(2, 1, 2, 2, 0, 3), ...).costItems(Items.EMERALD, 4).build()
 * </pre>
 *
 * {@link #of} gives each stage its own shape of shell, so a machine can grow wide, or tall, or both; {@link #fixed}
 * keeps the shell the same size and the machine only matures. Stage 0 never costs anything and has no bonus.
 */
public final class GrowthStages {

    private GrowthStages() {}

    /**
     * Bounds for one stage's shell: how far it reaches up, down, to the left, to the right, to the front and to the back,
     * measured from the controller. The controller sits in the shell, so a front of 0 puts it on the front face (the
     * way a machine you can walk up to is built) and a down of 1 puts it one block above the floor.
     */
    public static IntList bounds(int up, int down, int left, int right, int front, int back) {
        return IntList.of(up, down, left, right, front, back);
    }

    /** Stages that grow the shell: one shape for each stage, from the smallest to the full size. */
    public static Builder of(IntList... shapes) {
        return new Builder(shapes.length - 1, stage -> shapes[Math.min(stage, shapes.length - 1)]);
    }

    /** Stages that never change the shell: the machine only matures. */
    public static Builder fixed(IntList bounds, int maxStage) {
        return new Builder(maxStage, stage -> bounds);
    }

    public static final class Builder {

        private final int maxStage;
        private final IntFunction<IntList> bounds;
        private IntFunction<List<ItemStack>> cost = stage -> List.of();
        private float speedPerStage;
        private float outputPerStage;
        private int batchFrom = Integer.MAX_VALUE;
        private int batch = 1;

        private Builder(int maxStage, IntFunction<IntList> bounds) {
            this.maxStage = maxStage;
            this.bounds = bounds;
        }

        /** What reaching each stage costs, by stage number. */
        public Builder cost(IntFunction<List<ItemStack>> cost) {
            this.cost = cost;
            return this;
        }

        /** Each stage costs {@code perStage x stage} of this item, looked up when the stages are built (not before). */
        public Builder costItems(java.util.function.Supplier<? extends ItemLike> item, int perStage) {
            return cost(stage -> stage == 0 ? List.of() : List.of(new ItemStack(item.get(), perStage * stage)));
        }

        /** Each stage costs {@code perStage x stage} of this item. */
        public Builder costItems(ItemLike item, int perStage) {
            return cost(stage -> stage == 0 ? List.of() : List.of(new ItemStack(item, perStage * stage)));
        }

        /** Each stage runs this much faster than the last (0.1 = 10% of the time per stage). */
        public Builder speedPerStage(float share) {
            this.speedPerStage = share;
            return this;
        }

        /** Each stage adds this share of extra output (0.1 = 10% more per stage). */
        public Builder outputPerStage(float share) {
            this.outputPerStage = share;
            return this;
        }

        /** From this stage on, run this many recipes at once. */
        public Builder batchFrom(int stage, int batch) {
            this.batchFrom = stage;
            this.batch = batch;
            return this;
        }

        /** At the last stage only, run this many recipes at once. */
        public Builder batchAtMax(int batch) {
            return batchFrom(maxStage, batch);
        }

        public List<GrowthStage> build() {
            List<GrowthStage> stages = new ArrayList<>();
            for (int stage = 0; stage <= maxStage; stage++) {
                stages.add(new GrowthStage(bounds.apply(stage), cost.apply(stage), speedPerStage * stage,
                        outputPerStage * stage, stage >= batchFrom ? batch : 1));
            }
            return List.copyOf(stages);
        }
    }
}
