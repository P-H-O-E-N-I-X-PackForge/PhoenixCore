package net.phoenix.core.common.machine.multiblock.stellar;

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

/** The Stellar Forge multiblock: a glass chamber on a floor of hatches, with the star made in the middle. */
public final class StellarForgeMachines {

    private StellarForgeMachines() {}

    public static final MachineEntry<MultiblockMachineDefinition> STELLAR_FORGE = REGISTRATE
            .multiblock("stellar_forge", StellarForgeMachine::new)
            .langValue("Stellar Forge")
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(PhoenixBlocks.PHOENIX_HEART_CASING)
            .tooltips(
                    Component.literal("§6Makes a star, or a black hole, from nothing."),
                    Component.literal("§7Feed it hydrogen, helium or heavier, and keep three settings in their"),
                    Component.literal("§7moving bands: gas cloud, protostar, star. The bands shift with the mass"),
                    Component.literal("§7and are a little different in every forge."),
                    Component.literal("§7Neutronium is heavy enough to skip the stages: pour in enough, fast, with"),
                    Component.literal("§7the field high, and it falls in as a black hole."),
                    Component.literal("§7A star or black hole gives off oxygen plasma into the output hatch."),
                    Component.literal("§7Needs an energy hatch, a fluid input hatch and a fluid output hatch."))
            .pattern(definition -> {
                var casing = Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get());
                var glass = Predicates.blocks(GTBlocks.FUSION_GLASS.get());
                var floor = Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get())
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(3))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1)
                                .setMaxGlobalLimited(2));
                return MultiblockPatternBuilder
                        .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                        .slice("CCSCC", "CGGGC", "CGGGC", "CGGGC", "CCCCC")
                        .slice("CFFFC", "G   G", "G   G", "G   G", "CGGGC")
                        .slice("CFFFC", "G   G", "G   G", "G   G", "CGGGC")
                        .slice("CFFFC", "G   G", "G   G", "G   G", "CGGGC")
                        .slice("CCCCC", "CGGGC", "CGGGC", "CGGGC", "CCCCC")
                        .where('S', Predicates.controller(definition))
                        .where('C', casing)
                        .where('G', glass)
                        .where('F', floor)
                        .where(' ', Predicates.air())
                        .build();
            })
            .workableCasingModel(net.phoenix.core.PhoenixCore.id("block/phoenix_heart_casing"),
                    GTCEu.id("block/multiblock/pyrolyse_oven"))
            .register();

    public static void init() {}
}
