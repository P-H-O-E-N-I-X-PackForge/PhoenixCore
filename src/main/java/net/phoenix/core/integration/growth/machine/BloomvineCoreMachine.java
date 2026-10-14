package net.phoenix.core.integration.growth.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.integration.growth.GrowthBlocks;
import net.phoenix.core.integration.growth.GrowthMultiblockMachine;
import net.phoenix.core.integration.growth.GrowthItems;
import net.phoenix.core.integration.growth.GrowthStage;
import net.phoenix.core.integration.growth.GrowthStages;
import net.phoenix.core.integration.growth.tendril.TendrilShape;
import net.phoenix.core.integration.growth.tendril.TendrilShapeRegistry;
import net.phoenix.core.integration.growth.tendril.TendrilWeightCondition;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class BloomvineCoreMachine extends GrowthMultiblockMachine {

    private static final int MAX_MATURITY = 5;
    
    public static final IntList MAX_BOUNDS = GrowthStages.bounds(2, 0, 3, 3, 0, 6);
    private static final java.util.function.Supplier<List<GrowthStage>> STAGES = com.google.common.base.Suppliers.memoize(BloomvineCoreMachine::buildMaturityTicks);

    private static List<GrowthStage> buildMaturityTicks() {
        
        return GrowthStages.fixed(MAX_BOUNDS, MAX_MATURITY).costItems(GrowthItems.GROWTH_SEED::get, 1)
                .speedPerStage(0.04f).outputPerStage(0.10f)
                .batchAtMax(2).build();
    }

    public BloomvineCoreMachine(BlockEntityCreationInfo holder) {
        super(holder);
    }

    @Override
    protected List<GrowthStage> getGrowthStages() {
        return STAGES.get();
    }

    public static List<GrowthStage> stages() {
        return STAGES.get();
    }

    @Override
    protected boolean chargeOnAutoGrowth() {
        return true;
    }

    @Override
    protected BlockState getShellBlockState() {
        return PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get().defaultBlockState();
    }

    @Override
    protected BlockState getTendrilBlockState() {
        return GrowthBlocks.GROWTH_TENDRIL.get().defaultBlockState();
    }

    @Override
    protected List<BlockPos> getTendrilAnchors() {
        return roofAnchors();
    }

    @Override
    protected int getMaxTendrils() {
        return 4;
    }

    @Override
    protected int getMaxTendrilLength() {
        return 8;
    }

    private static final TendrilShape SPIKE = TendrilShape.builder(PhoenixCore.id("bloomvine_spike"))
            .repeat(6, b -> b.up(1))
            .build();

    private static final TendrilShape CREEPER_VINE = TendrilShape
            .builder(PhoenixCore.id("bloomvine_creeper_vine"))
            .drift(1, 0).drift(1, 0).drift(1, 0)
            .drift(0, 1).drift(1, 1).drift(0, 1).drift(1, 1)
            .build();

    private static final TendrilShape SPIRAL_FROND = TendrilShape.generated(
            PhoenixCore.id("bloomvine_spiral_frond"), 10, i -> {
                double angle = i * 0.9;
                int x = (int) Math.round(Math.cos(angle) * 1.5);
                int z = (int) Math.round(Math.sin(angle) * 1.5);
                return new BlockPos(x, i, z);
            });

    private static boolean registered = false;

    public static void registerTendrilShapes() {
        if (registered) return;
        registered = true;

        TendrilShapeRegistry.register(SPIKE, TendrilWeightCondition.constant(10));

        TendrilShapeRegistry.register(CREEPER_VINE,
                (machine, shape) -> 4 + machine.getGrowthStage() * 3);

        TendrilShapeRegistry.register(SPIRAL_FROND,
                (machine, shape) -> machine.getGrowthStage() >= MAX_MATURITY ? 14 : 0);
    }
}
