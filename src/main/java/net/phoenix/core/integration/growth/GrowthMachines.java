package net.phoenix.core.integration.growth;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.integration.growth.machine.BloomvineCoreMachine;
import net.phoenix.core.integration.growth.machine.CrystalGardenMachine;
import net.phoenix.core.integration.growth.machine.VerdantBloomMachine;

import static com.gregtechceu.gtceu.api.multiblock.Predicates.blocks;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class GrowthMachines {

    public static final com.gregtechceu.gtceu.api.machine.multiblock.PartAbility GROWTH_CONTROL_ABILITY = new com.gregtechceu.gtceu.api.machine.multiblock.PartAbility(
            "growth_control");

    public static final MachineEntry<com.gregtechceu.gtceu.api.machine.MachineDefinition> GROWTH_CONTROL_HATCH = REGISTRATE
            .machine("growth_control_hatch", GrowthControlHatch::new)
            .langValue("Growth Control Hatch")
            .tier(com.gregtechceu.gtceu.api.GTValues.HV)
            .rotationState(RotationState.ALL)
            .abilities(GROWTH_CONTROL_ABILITY)
            .tooltips(
                    net.minecraft.network.chat.Component.literal("Fit to a growing machine to take control of it."),
                    net.minecraft.network.chat.Component.literal(
                            "\u00a77Pause it, cap its stage, steer its tendrils, and stop it breaking blocks."),
                    net.minecraft.network.chat.Component.literal(
                            "\u00a78Without one, growth is wild and overrides whatever is in its way."))
            .overlayTieredHullModel("growth_control_hatch")
            .register();

    private static <T extends GrowthMultiblockMachine> MachineEntry<MultiblockMachineDefinition> cubeGrower(
                                                                                                            String id,
                                                                                                            String name,
                                                                                                            java.util.function.Function<com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo, T> factory,
                                                                                                            com.gregtechceu.gtceu.api.registry.registrate.entry.GTRecipeTypeEntry recipeType,
                                                                                                            it.unimi.dsi.fastutil.ints.IntList minBounds,
                                                                                                            it.unimi.dsi.fastutil.ints.IntList maxBounds) {
        return REGISTRATE
                .multiblock(id, info -> factory.apply(info))
                .langValue(name)
                .rotationState(RotationState.ALL)
                .recipeType(recipeType)
                .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT, GrowthMultiblockMachine::stageModifier)
                .appearanceBlock(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY)
                .pattern(definition -> GrowthPatternHelper.getPattern(
                        definition,
                        Predicates.blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get())
                                .or(Predicates.blocks(GrowthBlocks.GROWTH_SHELL.get()))
                                .or(Predicates.abilities(GROWTH_CONTROL_ABILITY).setMaxGlobalLimited(1))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)),
                        minBounds, maxBounds))
                .model(createWorkableCasingMachineModel(
                        PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"),
                        PhoenixCore.id("block/multiblock/tesla_tower")))
                .register();
    }

    public static final MachineEntry<MultiblockMachineDefinition> CRYSTAL_GARDEN = cubeGrower("crystal_garden",
            "Crystal Garden",
            CrystalGardenMachine::new, PhoenixRecipeTypes.GROWTH_RECIPES, CrystalGardenMachine.MIN_BOUNDS,
            CrystalGardenMachine.MAX_BOUNDS);

    public static final MachineEntry<MultiblockMachineDefinition> BLOOMVINE_CORE = cubeGrower("bloomvine_core",
            "Bloomvine Core",
            BloomvineCoreMachine::new, PhoenixRecipeTypes.VINE_GROWTH_RECIPES, BloomvineCoreMachine.MIN_BOUNDS,
            BloomvineCoreMachine.MAX_BOUNDS);

    public static final MachineEntry<MultiblockMachineDefinition> VERDANT_BLOOM = cubeGrower("verdant_bloom",
            "Verdant Bloom",
            VerdantBloomMachine::new, PhoenixRecipeTypes.BOTANICAL_GROWTH_RECIPES, VerdantBloomMachine.MIN_BOUNDS,
            VerdantBloomMachine.MAX_BOUNDS);

    public static void init() {
        BloomvineCoreMachine.registerTendrilShapes();
        VerdantBloomMachine.registerTendrilShapes();
    }
}
