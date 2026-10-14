package net.phoenix.core.integration.growth.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.integration.growth.GrowthBlocks;
import net.phoenix.core.integration.growth.GrowthMultiblockMachine;
import net.phoenix.core.integration.growth.GrowthItems;
import net.phoenix.core.integration.growth.GrowthStage;
import net.phoenix.core.integration.growth.GrowthStages;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;

public class CrystalGardenMachine extends GrowthMultiblockMachine {

    private static final IntList[] SHAPES = {
            GrowthStages.bounds(2, 1, 1, 1, 0, 2),
            GrowthStages.bounds(2, 1, 2, 2, 0, 3),
            GrowthStages.bounds(3, 1, 2, 2, 0, 4),
            GrowthStages.bounds(3, 1, 3, 3, 0, 5),
            GrowthStages.bounds(4, 1, 3, 3, 0, 6) };

    public static final IntList MAX_BOUNDS = SHAPES[SHAPES.length - 1];

    private static final java.util.function.Supplier<List<GrowthStage>> STAGES = com.google.common.base.Suppliers.memoize(CrystalGardenMachine::buildStages);

    private static List<GrowthStage> buildStages() {
        
        return GrowthStages.of(SHAPES).costItems(GrowthItems.CRYSTAL_SEED::get, 1).speedPerStage(0.10f).batchAtMax(2).build();
    }

    public CrystalGardenMachine(BlockEntityCreationInfo holder) {
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
    protected float getGrowthPerRecipe() {
        return 0.1f;
    }
}
