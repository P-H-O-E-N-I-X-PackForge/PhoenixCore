package net.phoenix.core.common.machine.multiblock.electric.plasma;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.machine.PhoenixPartAbility;
import net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.machine.multiblock.electric.HighPressurePlasmaArcFurnaceMachine;

import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class PlasmaArcFurnaceMachines {

    private PlasmaArcFurnaceMachines() {}

    static {
        REGISTRATE.creativeModeTab(() -> PhoenixCore.PHOENIX_CREATIVE_TAB);
    }

    public static final MachineEntry<MultiblockMachineDefinition> HIGH_PRESSURE_PLASMA_ARC_FURNACE = REGISTRATE
            .multiblock("high_pressure_plasma_arc_furnace", HighPressurePlasmaArcFurnaceMachine::new)
            .langValue("High-Pressure Plasma Arc Furnace")
            .recipeType(PhoenixRecipeTypes.HIGH_PRESSURE_ARC_FURNACE)
            .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT, GTRecipeModifiers.PARALLEL_HATCH,
                    HighPressurePlasmaArcFurnaceMachine::recipeModifier)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(PhoenixBlocks.SUPER_STABLE_FUSION_CASING)
            .tooltips(
                    Component.literal("§6A capstone furnace. It will not run until its shield is up."),
                    Component.literal("§7Sneak-click with an empty hand to open the containment ring and tune"),
                    Component.literal("§7the coils until the plasma locks. A lock raises the shield."),
                    Component.literal("§7The shield only wears down while the furnace is working, and a spent"),
                    Component.literal("§7shield needs the coils to cool before they can be locked again."),
                    Component.literal("§7A plasma catalyst in a plasma hatch speeds the furnace up."),
                    Component.literal("§8Capstone: it runs on its own, but the Phoenix Forge needs it bound."))
            .pattern(definition -> {
                String wall = "CCCCCCCCC";
                String mid = "C       C";
                MultiblockPatternBuilder b = MultiblockPatternBuilder.start().slice(wall, wall, wall, wall, wall, wall);
                for (int i = 0; i < 7; i++) b = b.slice(wall, mid, mid, mid, mid, wall);
                return b.slice(wall, wall, wall, wall, "CCCCHCCCC", "CEMIOFXPC")
                        .where('H', Predicates.controller(definition))
                        .where('C', Predicates.blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get()))
                        .where('E', Predicates.blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get())
                                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                        .setMaxGlobalLimited(3)))
                        .where('M', Predicates.blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get())
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('I', Predicates.blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get())
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2)))
                        .where('O', Predicates.blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get())
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2)))
                        .where('F', Predicates.blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get())
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2)))
                        .where('X', Predicates.blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get())
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2)))
                        .where('P', Predicates.blocks(PhoenixBlocks.SUPER_STABLE_FUSION_CASING.get())
                                .or(Predicates.abilities(PhoenixPartAbility.PLASMA_INPUT).setMaxGlobalLimited(2)))
                        .where(' ', Predicates.air())
                        .build();
            })
            .model(createWorkableCasingMachineModel(PhoenixCore.id("block/super_stable_fusion_casing"),
                    GTCEu.id("block/multiblock/pyrolyse_oven"))
                    .andThen(b -> b.addDynamicRenderer(PhoenixDynamicRenderHelpers::getPlasmaArcFurnaceRenderer)))
            .hasBER(true)
            .register();

    public static void init() {}

    public static void registerPackets(PayloadRegistrar registrar) {
        registrar.playToClient(S2CPlasmaRingPacket.TYPE, S2CPlasmaRingPacket.STREAM_CODEC,
                S2CPlasmaRingPacket::handle);
        registrar.playToServer(C2SPlasmaRingPacket.TYPE, C2SPlasmaRingPacket.STREAM_CODEC,
                C2SPlasmaRingPacket::handle);
    }
}
