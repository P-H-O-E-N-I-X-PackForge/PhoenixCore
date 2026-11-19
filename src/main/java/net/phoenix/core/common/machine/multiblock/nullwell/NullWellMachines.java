package net.phoenix.core.common.machine.multiblock.nullwell;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.network.chat.Component;
import net.phoenix.core.common.block.PhoenixBlocks;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

/**
 * The Null Well: a capstone of the Void discipline. It holds a tiny black hole, which is fed matter and gives back
 * Hawking radiation as energy.
 */
public final class NullWellMachines {

    private NullWellMachines() {}

    public static final MachineEntry<MultiblockMachineDefinition> NULL_WELL = REGISTRATE
            .multiblock("null_well", NullWellMachine::new)
            .langValue("Null Well")
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(PhoenixBlocks.GLITCHED_FUSION_CASING)
            .tooltips(
                    Component.literal("§5Holds a black hole the size of a grain, and drinks from it."),
                    Component.literal("§7Pour fluid in and its mass grows; the denser the fluid, the more it weighs."),
                    Component.literal("§7Draw it down and the hole gives off Hawking radiation as energy."),
                    Component.literal("§7Keep it below the horizon limit. Past it the field slips, and if it fails"),
                    Component.literal("§7the hole breaks out and eats the walls around it."),
                    Component.literal("§7The radiation also condenses into neutronium, into an item output bus."),
                    Component.literal("§7Needs an energy hatch in, an energy hatch out and a fluid input hatch."))
            .pattern(definition -> {
                var casing = Predicates.blocks(PhoenixBlocks.GLITCHED_FUSION_CASING.get());
                var glass = Predicates.blocks(GTBlocks.FUSION_GLASS.get());
                var floor = Predicates.blocks(PhoenixBlocks.GLITCHED_FUSION_CASING.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(3))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2));
                String cap = "CCCCCCC";
                String wall = "CGGGGGC";
                String back = "CCCCCCC";
                return MultiblockPatternBuilder
                        .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice(back, wall, wall, wall, wall, wall, cap)
                        .slice("CFFFFFC", "G     G", "G     G", "G     G", "G     G", "G     G", wall)
                        .slice("CFFFFFC", "G     G", "G     G", "G     G", "G     G", "G     G", wall)
                        .slice("CFFFFFC", "G     G", "G     G", "G     G", "G     G", "G     G", wall)
                        .slice("CFFFFFC", "G     G", "G     G", "G     G", "G     G", "G     G", wall)
                        .slice("CFFFFFC", "G     G", "G     G", "G     G", "G     G", "G     G", wall)
                        .slice("CCCSCCC", wall, wall, wall, wall, wall, cap)
                        .where('S', Predicates.controller(definition))
                        .where('C', casing)
                        .where('G', glass)
                        .where('F', floor)
                        .where(' ', Predicates.air())
                        .build();
            })
            .modelProperty(com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.RECIPE_LOGIC_STATUS,
                    com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic.Status.IDLE)
            .model(com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel(
                    net.phoenix.core.PhoenixCore.id("block/glitched_fusion_casing"),
                    GTCEu.id("block/multiblock/pyrolyse_oven")).andThen(
                            b -> b.addDynamicRenderer(
                                    net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers::getNullWellRenderer)))
            .hasBER(true)
            .register();

    public static void init() {}
}
