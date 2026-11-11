package net.phoenix.core.integration.conflux.hatch;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;

import net.minecraft.network.chat.Component;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.machine.PhoenixPartAbility;

import static net.phoenix.core.common.machine.PhoenixMachines.registerTieredMachines;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class ConfluxHatches {

    private ConfluxHatches() {}

    private static final int[] TIER = { GTValues.IV };

    public static MachineEntry<MachineDefinition>[] DATA_INPUT;
    public static MachineEntry<MachineDefinition>[] DATA_OUTPUT;

    public static void init() {
        REGISTRATE.creativeModeTab(() -> PhoenixCore.PHOENIX_CREATIVE_TAB);
        DATA_INPUT = register("conflux_data_input_hatch", "Conflux Data Input Hatch", IO.IN,
                PhoenixPartAbility.CONFLUX_DATA_INPUT, "block/machine/part/conflux_data_input_hatch",
                "Takes Conflux data in from pipes and producers for the machine to use.");
        DATA_OUTPUT = register("conflux_data_output_hatch", "Conflux Data Output Hatch", IO.OUT,
                PhoenixPartAbility.CONFLUX_DATA_OUTPUT, "block/machine/part/conflux_data_output_hatch",
                "Holds what the machine makes and sends it into the pipes touching it.");
    }

    private static MachineEntry<MachineDefinition>[] register(String id, String name, IO io,
                                                              com.gregtechceu.gtceu.api.machine.multiblock.PartAbility ability,
                                                              String overlayModel, String tooltip) {
        return registerTieredMachines(id,
                (holder, tier) -> new ConfluxDataHatchPartMachine(holder, tier, io),
                (tier, builder) -> builder
                        .langValue(name)
                        .abilities(ability)
                        .rotationState(RotationState.ALL)
                        .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                        .tooltips(Component.literal(tooltip),
                                Component.literal("§7Holds all five kinds of Conflux data, " +
                                        ConfluxDataHatchPartMachine.CAPACITY + " of each.§r"))
                        .overlayTieredHullModel(PhoenixCore.id(overlayModel))
                        .register(),
                TIER);
    }
}
