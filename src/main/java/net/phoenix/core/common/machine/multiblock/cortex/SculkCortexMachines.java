package net.phoenix.core.common.machine.multiblock.cortex;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.common.block.PhoenixBlocks;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class SculkCortexMachines {

    private SculkCortexMachines() {}

    public static final MachineEntry<MultiblockMachineDefinition> SCULK_CORTEX = REGISTRATE
            .multiblock("sculk_cortex", SculkCortexMachine::new)
            .langValue("Sculk Cortex")
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(PhoenixBlocks.TWISTED_COMPUTER_CASING)
            .tooltips(
                    Component.literal("§3A mind of sculk, and it is always hungry."),
                    Component.literal("§7Feed it items. It digests them into sculk, and grows."),
                    Component.literal("§7Every kind of item it eats it remembers: eaten again, it is digested faster,"),
                    Component.literal("§7for more. The bigger it grows, the more it can remember."),
                    Component.literal("§7What it remembers it turns into thought, which leaves as energy."),
                    Component.literal("§7Let it starve and it withers: it shrinks and forgets."),
                    Component.literal(
                            "§7A 3x3x3 block of sculk in the middle, seen through glass, an energy hatch in and an energy hatch out,"),
                    Component.literal("§7an item input bus and an item output bus."))
            .pattern(definition -> {
                var casing = Predicates.blocks(PhoenixBlocks.TWISTED_COMPUTER_CASING.get());
                var mass = Predicates.blocks(Blocks.SCULK);
                var floor = Predicates.blocks(PhoenixBlocks.TWISTED_COMPUTER_CASING.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2));
                return MultiblockPatternBuilder
                        .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("CCCCC", "CGGGC", "CGGGC", "CGGGC", "CCCCC")
                        .slice("CFFFC", "GKKKG", "GKKKG", "GKKKG", "CCCCC")
                        .slice("CFFFC", "GKKKG", "GKKKG", "GKKKG", "CCCCC")
                        .slice("CFFFC", "GKKKG", "GKKKG", "GKKKG", "CCCCC")
                        .slice("CCSCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC")
                        .where('S', Predicates.controller(definition))
                        .where('C', casing)
                        .where('K', mass)
                        .where('G', Predicates.blocks(com.gregtechceu.gtceu.common.data.GTBlocks.FUSION_GLASS.get()))
                        .where('F', floor)
                        .build();
            })
            .modelProperty(com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.RECIPE_LOGIC_STATUS,
                    com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic.Status.IDLE)
            .model(com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel(
                    net.phoenix.core.PhoenixCore.id("block/twisted_computer_casing"),
                    GTCEu.id("block/multiblock/pyrolyse_oven")).andThen(
                            b -> b.addDynamicRenderer(
                                    net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers::getCortexRenderer)))
            .hasBER(true)
            .register();

    public static void init() {}
}
