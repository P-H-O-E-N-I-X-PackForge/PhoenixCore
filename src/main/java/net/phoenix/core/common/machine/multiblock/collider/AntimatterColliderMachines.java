package net.phoenix.core.common.machine.multiblock.collider;

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

public final class AntimatterColliderMachines {

    private AntimatterColliderMachines() {}

    private static String[] slice(int z) {
        StringBuilder floor = new StringBuilder();
        StringBuilder mid = new StringBuilder();
        StringBuilder roof = new StringBuilder();
        for (int x = -3; x <= 3; x++) {
            int m = Math.max(Math.abs(x), Math.abs(z));
            floor.append(m == 3 && !(z == 3 && x == 0) ? 'F' : 'C');
            mid.append(m >= 2 ? 'G' : ' ');
            roof.append('C');
        }
        if (z == 3) floor.setCharAt(3, 'S');
        return new String[] { floor.toString(), mid.toString(), roof.toString() };
    }

    public static final MachineEntry<MultiblockMachineDefinition> ANTIMATTER_COLLIDER = REGISTRATE
            .multiblock("antimatter_collider", AntimatterColliderMachine::new)
            .langValue("Antimatter Collider")
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(PhoenixBlocks.PHOENIX_ENRICHED_NEUTRONIUM_CASING)
            .tooltips(
                    Component.literal("§dMatter one way, antimatter the other, and a place for them to meet."),
                    Component
                            .literal("§7Fluid fed in is the matter. Each collision makes antimatter, which is held in"),
                    Component.literal("§7a trap that leaks if the power fails and loses what does not fit."),
                    Component.literal("§7In POWER mode the trap is annihilated into a great deal of energy."),
                    Component.literal("§7In ITEMS mode it is condensed into rare materials in the output bus."),
                    Component.literal("§7Shift-click with an empty hand to switch mode."),
                    Component.literal("§7Needs energy in and out, a fluid input hatch and an item output bus."))
            .pattern(definition -> {
                var casing = Predicates.blocks(PhoenixBlocks.PHOENIX_ENRICHED_NEUTRONIUM_CASING.get());
                var glass = Predicates.blocks(GTBlocks.FUSION_GLASS.get());
                var floor = Predicates.blocks(PhoenixBlocks.PHOENIX_ENRICHED_NEUTRONIUM_CASING.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2));
                var builder = MultiblockPatternBuilder
                        .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT);
                for (int z = -3; z <= 3; z++) {
                    String[] s = slice(z);
                    builder.slice(s[0], s[1], s[2]);
                }
                return builder
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
                    net.phoenix.core.PhoenixCore.id("block/phoenix_enriched_neutronium_casing"),
                    GTCEu.id("block/multiblock/fusion_reactor")).andThen(
                            b -> b.addDynamicRenderer(
                                    net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers::getColliderRenderer)))
            .hasBER(true)
            .register();

    public static void init() {}
}
