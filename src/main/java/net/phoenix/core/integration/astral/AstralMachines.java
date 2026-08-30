package net.phoenix.core.integration.astral;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.machine.PhoenixPartAbility;
import net.phoenix.core.integration.astral.skein.AstralConfluenceHatchMachine;
import net.phoenix.core.integration.astral.skein.AstralLoomMachine;
import net.phoenix.core.integration.astral.skein.AstralSpinningWheelMachine;
import net.phoenix.core.integration.astral.skein.AstralThreadHatchPartMachine;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;

import static com.gregtechceu.gtceu.api.multiblock.Predicates.blocks;
import static com.gregtechceu.gtceu.api.multiblock.Predicates.controller;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.machine.PhoenixMachines.registerTieredMachines;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class AstralMachines {

    public static final MachineDefinition[] ASTRAL_CONFLUENCE_HATCH = registerTieredMachines(
            "astral_confluence_hatch",
            (holder, tier) -> new AstralConfluenceHatchMachine(holder, tier),
            (tier, builder) -> builder
                    .langValue(GTValues.VNF[tier] + " Astral Confluence Hatch")
                    .rotationState(RotationState.ALL)
                    .recipeType(PhoenixRecipeTypes.ASTRAL_WEAVING_RECIPES)
                    .overlayTieredHullModel("source_hatch")
                    .register(),
            new int[] { GTValues.IV });

    public static final MachineDefinition[] ASTRAL_THREAD_HATCH_INPUT = registerTieredMachines(
            "astral_thread_hatch_input",
            (holder, tier) -> new AstralThreadHatchPartMachine(holder, tier, IO.IN),
            (tier, builder) -> builder
                    .langValue(GTValues.VNF[tier] + " Astral Thread Input Hatch")
                    .rotationState(RotationState.ALL)
                    .abilities(PhoenixPartAbility.ASTRAL_THREAD_INPUT)
                    .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                    .overlayTieredHullModel("source_hatch")
                    .register(),
            GTValues.tiersBetween(GTValues.IV, GTValues.ZPM));

    public static final MachineDefinition[] ASTRAL_THREAD_HATCH_OUTPUT = registerTieredMachines(
            "astral_thread_hatch_output",
            (holder, tier) -> new AstralThreadHatchPartMachine(holder, tier, IO.OUT),
            (tier, builder) -> builder
                    .langValue(GTValues.VNF[tier] + " Astral Thread Output Hatch")
                    .rotationState(RotationState.ALL)
                    .abilities(PhoenixPartAbility.ASTRAL_THREAD_OUTPUT)
                    .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                    .overlayTieredHullModel("source_hatch")
                    .register(),
            GTValues.tiersBetween(GTValues.IV, GTValues.ZPM));

    public static final MultiblockMachineDefinition ASTRAL_SPINNING_WHEEL = REGISTRATE
            .multiblock("astral_spinning_wheel", AstralSpinningWheelMachine::new)
            .langValue("Astral Spinning Wheel")
            .rotationState(RotationState.ALL)
            .recipeType(PhoenixRecipeTypes.ASTRAL_WEAVING_RECIPES)
            .appearanceBlock(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                    .slice("CCC", "CCC", "CCC")
                    .slice("CHC", "COC", "CTC")
                    .slice("CCC", "CCC", "CCC")
                    .where(' ', Predicates.any())
                    .where('C', blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get()))
                    .where('H', Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                    .where('T', blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get())
                            .or(Predicates.abilities(PhoenixPartAbility.ASTRAL_THREAD_INPUT).setPreviewCount(1))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where('O', controller(blocks(definition.get())))
                    .build())
            .model(createWorkableCasingMachineModel(
                    PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"),
                    PhoenixCore.id("block/multiblock/tesla_tower")))
            .register();

    public static final MultiblockMachineDefinition ASTRAL_LOOM = REGISTRATE
            .multiblock("astral_loom", AstralLoomMachine::new)
            .langValue("Astral Loom")
            .rotationState(RotationState.ALL)
            .recipeType(PhoenixRecipeTypes.ASTRAL_WEAVING_RECIPES)
            .appearanceBlock(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                    .slice("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC")
                    .slice("CCCCC", "CHIOC", "CIIIC", "CITIC", "CCCCC")
                    .slice("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC")
                    .where(' ', Predicates.any())
                    .where('C', blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get()))
                    .where('I', Predicates.air())
                    .where('H', Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                    .where('T', blocks(PhoenixBlocks.MACHINE_CASING_NAQUADAH_ALLOY.get())
                            .or(Predicates.abilities(PhoenixPartAbility.ASTRAL_THREAD_INPUT).setPreviewCount(1))
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where('O', controller(blocks(definition.get())))
                    .build())
            .model(createWorkableCasingMachineModel(
                    PhoenixCore.id("block/casings/multiblock/machine_casing_invariant_naquadah_alloy"),
                    PhoenixCore.id("block/multiblock/tesla_tower")))
            .register();

    public static void init() {}
}
