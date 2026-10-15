package net.phoenix.core.integration.growth.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.growth.GrowthBlocks;
import net.phoenix.core.integration.growth.GrowthItems;
import net.phoenix.core.integration.growth.GrowthMultiblockMachine;
import net.phoenix.core.integration.growth.GrowthStage;
import net.phoenix.core.integration.growth.GrowthStages;
import net.phoenix.core.integration.growth.tendril.TendrilShape;
import net.phoenix.core.integration.growth.tendril.TendrilShapeRegistry;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class VerdantBloomMachine extends GrowthMultiblockMachine {

    private static final IntList[] SHAPES = {
            GrowthStages.bounds(2, 1, 1, 1, 0, 2),
            GrowthStages.bounds(3, 1, 1, 1, 0, 2),
            GrowthStages.bounds(4, 1, 2, 2, 0, 3),
            GrowthStages.bounds(5, 1, 2, 2, 0, 4),
            GrowthStages.bounds(7, 1, 2, 2, 0, 4) };

    public static final IntList MIN_BOUNDS = SHAPES[0];
    public static final IntList MAX_BOUNDS = SHAPES[SHAPES.length - 1];

    private static final java.util.function.Supplier<List<GrowthStage>> STAGES = com.google.common.base.Suppliers
            .memoize(VerdantBloomMachine::buildStages);

    private static List<GrowthStage> buildStages() {
        return GrowthStages.of(SHAPES).costItems(GrowthItems.GROWTH_SEED::get, 2).speedPerStage(0.05f)
                .outputPerStage(0.08f)
                .build();
    }

    public VerdantBloomMachine(BlockEntityCreationInfo holder) {
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
    protected BlockState getShellBlockState() {
        return GrowthBlocks.GROWTH_SHELL.get().defaultBlockState();
    }

    @Override
    protected boolean chargeOnAutoGrowth() {
        return true;
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
        return 6;
    }

    private static final TendrilShape REACHING_SHOOT = TendrilShape.builder(PhoenixCore.id("verdant_reaching_shoot"))
            .repeat(5, b -> b.drift(1, 0))
            .build();

    private static final TendrilShape CURLING_FROND = TendrilShape.generated(
            PhoenixCore.id("verdant_curling_frond"), 8, i -> {
                double angle = i * 0.7;
                int x = (int) Math.round(Math.cos(angle) * 1.2);
                int z = (int) Math.round(Math.sin(angle) * 1.2);
                return new BlockPos(x, i, z);
            });

    private static boolean registered = false;

    public static void registerTendrilShapes() {
        if (registered) return;
        registered = true;

        TendrilShapeRegistry.register(REACHING_SHOOT, (machine, shape) -> 10);

        TendrilShapeRegistry.register(CURLING_FROND,
                (machine, shape) -> machine.getGrowthStage() >= machine.maxStage() ? 12 : 0);
    }
}
