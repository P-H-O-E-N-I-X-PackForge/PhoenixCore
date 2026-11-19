package net.phoenix.core.common.machine.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLEnvironment;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class SubstructureTestMachines {

    public static MachineEntry<MultiblockMachineDefinition> SUBSTRUCTURE_TEST = null;

    static {
        if (!FMLEnvironment.production) {
            SUBSTRUCTURE_TEST = REGISTRATE
                    .multiblock("substructure_test", MultiblockControllerMachine::new)
                    .langValue("Substructure Test Multiblock")
                    .rotationState(RotationState.NON_Y_AXIS)
                    .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
                    .tooltips(Component.literal("§7Development only: a box, with an optional tower on top."),
                            Component.literal("§7Its 'extension' structure adds the tower to the box instead of"),
                            Component.literal("§7replacing it."))
                    .pattern(definition -> MultiblockPatternBuilder
                            .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                            .slice("CCC", "CCC", "CCC")
                            .slice("CCC", "CCC", "CCC")
                            .slice("CCC", "CSC", "CCC")
                            .where('S', Predicates.controller(definition))
                            .where('C', Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get()))
                            .build())
                    .additionalPattern("extension", definition -> MultiblockPatternBuilder
                            .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                            .slice("BBB", "BBB", "BBB", "BBB", "BBB")
                            .slice("BTB", "BTB", "BBB", "BBB", "BBB")
                            .slice("BBB", "BBB", "BBB", "BSB", "BBB")
                            .where('S', Predicates.controller(definition))
                            .where('T', Predicates.blocks(net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                    .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gtceu",
                                            "steel_frame"))))
                            .where('B', Predicates.any())
                            .build())
                    .workableCasingModel(GTCEu.id("block/casings/solid/machine_casing_solid_steel"),
                            GTCEu.id("block/multiblock/pyrolyse_oven"))
                    .register();
        }
    }

    public static void init() {}

    private SubstructureTestMachines() {}
}
