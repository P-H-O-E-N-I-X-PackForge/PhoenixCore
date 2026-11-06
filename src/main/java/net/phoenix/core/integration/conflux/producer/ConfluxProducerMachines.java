package net.phoenix.core.integration.conflux.producer;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.ConfluxDataType;

import java.util.EnumMap;
import java.util.Map;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class ConfluxProducerMachines {

    public static final Map<ConfluxDataType, MachineEntry<MachineDefinition>[]> PRODUCERS = new EnumMap<>(
            ConfluxDataType.class);

    private static final int[] TIERS = {
            GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV,
            GTValues.LuV, GTValues.ZPM, GTValues.UV
    };

    static {
        REGISTRATE.creativeModeTab(() -> PhoenixCore.PHOENIX_CREATIVE_TAB);
        for (ConfluxDataType type : ConfluxDataType.values()) {
            @SuppressWarnings("unchecked")
            MachineEntry<MachineDefinition>[] defs = new MachineEntry[GTValues.TIER_COUNT];
            for (int tier : TIERS) {
                String id = GTValues.VN[tier].toLowerCase() + "_" + type.id() + "_data_producer";
                String lang = GTValues.VN[tier] + " " + type.displayName + " Data Producer";

                defs[tier] = REGISTRATE
                        .machine(id, holder -> new ConfluxProducerMachine(holder, tier, type))
                        .langValue(lang)
                        .tier(tier)
                        .rotationState(RotationState.NON_Y_AXIS)
                        .tooltips(
                                net.minecraft.network.chat.Component.literal(
                                        "Produces §b" + type.displayName + "§r research data."),
                                net.minecraft.network.chat.Component.literal(
                                        "Output: §e" + (4L * tier) + "§r units/t"),
                                net.minecraft.network.chat.Component.literal(
                                        "Needs: §e" + ConfluxProducerMachine.needTitle(type) + "§r"),
                                net.minecraft.network.chat.Component.literal(
                                        "§7" + ConfluxProducerMachine.needDetail(type, tier) + "§r"),
                                net.minecraft.network.chat.Component.literal(type == ConfluxDataType.ENERGETIC ?
                                        "§7The only producer that takes EU.§r" :
                                        "§7Takes no EU.§r"))

                        .overlayTieredHullModel(
                                PhoenixCore.id("block/machine/part/conflux_" + type.id() + "_data_producer"))
                        .register();
            }
            PRODUCERS.put(type, defs);
        }
    }

    public static void init() {}

    private ConfluxProducerMachines() {}
}
