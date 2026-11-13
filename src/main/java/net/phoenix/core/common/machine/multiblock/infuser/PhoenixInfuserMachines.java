package net.phoenix.core.common.machine.multiblock.infuser;

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

public final class PhoenixInfuserMachines {

    private PhoenixInfuserMachines() {}

    public static final MachineEntry<MultiblockMachineDefinition> PHOENIX_INFUSER = REGISTRATE
            .multiblock("phoenix_infuser", PhoenixInfuserMachine::new)
            .langValue("Phoenix Infuser")
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(PhoenixBlocks.TRUE_PHOENIX_INFUSED_CASING)
            .tooltips(
                    Component.literal("§6It burns, and then it rises."),
                    Component.literal("§7In BURN mode it takes anything from the input bus and burns it to ash."),
                    Component.literal("§7That gives heat and a store of energy, which it gives out as it can."),
                    Component.literal("§7In RENEW mode it takes only damaged items and, for heat and ash, raises"),
                    Component.literal("§7each of them whole again. The more damaged, the more ash it takes."),
                    Component.literal("§7Needs an energy hatch in and out, an item input bus and an item output bus."))
            .pattern(definition -> {
                var casing = Predicates.blocks(PhoenixBlocks.TRUE_PHOENIX_INFUSED_CASING.get());
                var glass = Predicates.blocks(GTBlocks.FUSION_GLASS.get());
                var floor = Predicates.blocks(PhoenixBlocks.TRUE_PHOENIX_INFUSED_CASING.get())
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
                        .slice("CFFFC", "G   G", "G   G", "G   G", "CGGGC")
                        .slice("CFFFC", "G   G", "G   G", "G   G", "CGGGC")
                        .slice("CFFFC", "G   G", "G   G", "G   G", "CGGGC")
                        .slice("CCSCC", "CGGGC", "CGGGC", "CGGGC", "CCCCC")
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
                    net.phoenix.core.PhoenixCore.id("block/true_phoenix_infused_casing"),
                    GTCEu.id("block/multiblock/pyrolyse_oven")).andThen(
                            b -> b.addDynamicRenderer(
                                    net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers::getInfuserRenderer)))
            .hasBER(true)
            .register();

    public static void init() {}
}
