package net.phoenix.core.integration.ars_nouveau.conflux;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.network.chat.Component;
import net.phoenix.core.PhoenixCore;

import static com.gregtechceu.gtceu.api.multiblock.Predicates.*;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class ConfluxArcaneMachines {

    static {
        REGISTRATE.creativeModeTab(() -> PhoenixCore.PHOENIX_CREATIVE_TAB);
    }

    public static final MachineEntry<MultiblockMachineDefinition> CASCADE_ENGINE = REGISTRATE
            .multiblock("conflux_cascade_engine", ConfluxCascadeEngine::new)
            .recipeType(com.gregtechceu.gtceu.common.data.GTRecipeTypes.DUMMY_RECIPES)
            .langValue("Conflux Cascade Engine")
            .tier(GTValues.ZPM)
            .rotationState(RotationState.NON_Y_AXIS)
            .tooltips(
                    Component.literal("§bMomentum-based Conflux data generator.§r"),
                    Component.literal("Builds momentum over time — full power takes ~25 seconds."),
                    Component.literal("§cAny power loss resets momentum rapidly!§r"),
                    Component.literal("Screwdriver to cycle output data type."),
                    Component.literal("Sends its data out through a Conflux Data Output Hatch."),
                    Component
                            .literal("§7Eats what its type is made from: material items, biomaterial or circuits from" +
                                    " an input bus, Source from a Source hatch, or EU for Energetic.§r"))
            .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST)

            .pattern(def -> MultiblockPatternBuilder.start()

                    .slice("XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX")

                    .slice("XXXXX", "X   X", "X   X", "X   X", "XXXXX")

                    .slice("XXXXX", "X   X", "X   X", "X   X", "XXXXX")

                    .slice("XXXXX", "X   X", "X   X", "X   X", "XXXXX")

                    .slice("XXXXX", "XEXEX", "XMHMX", "XSXSX", "XXXXX")
                    .where('H', controller(def))
                    .where('X', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()))
                    .where('E', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(4)))
                    .where('S', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2))
                            .or(abilities(net.phoenix.core.api.machine.PhoenixPartAbility.SOURCE_INPUT)
                                    .setMaxGlobalLimited(1)))
                    .where('M', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.MAINTENANCE).setMinGlobalLimited(1))
                            .or(abilities(net.phoenix.conflux.hatch.ConfluxPartAbility.CONFLUX_DATA_OUTPUT)
                                    .setMinGlobalLimited(1)
                                    .setMaxGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .workableCasingModel(
                    com.gregtechceu.gtceu.GTCEu.id("block/casings/solid/machine_casing_robust_tungstensteel"),
                    PhoenixCore.id("block/multiblock/conflux_cascade_engine"))
            .register();

    public static void init() {}

    private ConfluxArcaneMachines() {}
}
