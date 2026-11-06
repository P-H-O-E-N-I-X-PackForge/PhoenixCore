package net.phoenix.core.common.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.phoenix.core.common.machine.electric.AutoChargerMachine;

public final class PftChargers {

    private PftChargers() {}

    public static final MachineEntry<MachineDefinition>[] AUTO_CHARGER_4 = register(4);

    private static MachineEntry<MachineDefinition>[] register(int slots) {
        return PhoenixMachines.registerTieredMachines("auto_charger_" + slots + "x",
                (info, tier) -> new AutoChargerMachine(info, tier, slots),
                (tier, builder) -> builder
                        .rotationState(RotationState.ALL)
                        .modelProperty(GTMachineModelProperties.CHARGER_STATE, BatteryBufferMachine.State.IDLE)
                        .model(GTMachineModels.createChargerModel())
                        .langValue("%s %sx Auto Turbo Charger".formatted(
                                GTValues.VCF[tier] + GTValues.VOLTAGE_NAMES[tier] + ChatFormatting.RESET, slots))
                        .tooltips(Component.translatable("gtceu.universal.tooltip.item_storage_capacity", slots),
                                Component.translatable("gtceu.universal.tooltip.voltage_in_out",
                                        FormattingUtil.formatNumbers(GTValues.V[tier]), GTValues.VNF[tier]),
                                Component.translatable("gtceu.universal.tooltip.amperage_in_till",
                                        slots * BatteryBufferMachine.AMPS_PER_BATTERY_CHARGER),
                                Component.translatable("tooltip.phoenixcore.auto_charger.output"))
                        .register(),
                GTValues.tiersBetween(GTValues.ULV, GTValues.UHV));
    }

    public static void init() {}
}
