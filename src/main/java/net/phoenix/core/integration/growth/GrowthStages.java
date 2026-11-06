package net.phoenix.core.integration.growth;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class GrowthStages {

    private GrowthStages() {}

    public static IntList bounds(int up, int down, int left, int right, int front, int back) {
        return IntList.of(up, down, left, right, front, back);
    }

    public static Builder of(IntList... shapes) {
        return new Builder(shapes.length - 1, stage -> shapes[Math.min(stage, shapes.length - 1)]);
    }

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

        public Builder cost(IntFunction<List<ItemStack>> cost) {
            this.cost = cost;
            return this;
        }

        public Builder costItems(java.util.function.Supplier<? extends ItemLike> item, int perStage) {
            return cost(stage -> stage == 0 ? List.of() : List.of(new ItemStack(item.get(), perStage * stage)));
        }

        public Builder costItems(ItemLike item, int perStage) {
            return cost(stage -> stage == 0 ? List.of() : List.of(new ItemStack(item, perStage * stage)));
        }

        public Builder speedPerStage(float share) {
            this.speedPerStage = share;
            return this;
        }

        public Builder outputPerStage(float share) {
            this.outputPerStage = share;
            return this;
        }

        public Builder batchFrom(int stage, int batch) {
            this.batchFrom = stage;
            this.batch = batch;
            return this;
        }

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
