package net.phoenix.core.common.machine.multiblock.forge;

import com.gregtechceu.gtceu.GTCEu;
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
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PhoenixRecipeTypes;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class PhoenixForgeMachines {

    private PhoenixForgeMachines() {}

    static {
        REGISTRATE.creativeModeTab(() -> PhoenixCore.PHOENIX_CREATIVE_TAB);
    }

    public static final MachineEntry<MultiblockMachineDefinition> PHOENIX_FORGE = REGISTRATE
            .multiblock("phoenix_forge", PhoenixForgeMachine::new)
            .langValue("Phoenix Forge")
            .recipeType(PhoenixRecipeTypes.PHOENIX_FORGE_RECIPES)
            .tier(GTValues.UXV)
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(PhoenixBlocks.PHOENIX_HEART_CASING)
            .tooltips(
                    Component
                            .literal("§6The last multiblock. Every capstone feeds it, and it runs on none of its own."),
                    Component.literal("§7Attune a Rebirth Binder to it, then bind the Tesla Tower, the Honey"),
                    Component.literal("§7Crystallization Chamber, the High-Pressure Plasma Arc Furnace, the"),
                    Component.literal(
                            "§7Continuum Launch Complex and the Panoptic Engine to it, each by winning its binding rite. The capstones work without the Forge; it does not work without them."),
                    Component.literal("§7Then put the five into the ring in the right order: the Forge only says"),
                    Component.literal("§7how many sit where it wants them."),
                    Component.literal("§7A closed ring lets the Forge run its recipes, and lets you light it."),
                    Component.literal("§8Right-click for the Forge. Sneak-click for the machine panel."))
            .pattern(definition -> {
                String roof = "CCCCCCCCCCC";
                String wall = "CCCCCCCCCCC";
                String glassRoof = "CCGGGGGGGCC";
                String ring = "C         C";
                String floor = "CCCCCCCCCCC";
                MultiblockPatternBuilder b = MultiblockPatternBuilder.start().slice(roof, wall, wall, wall, wall, wall,
                        floor);
                for (int i = 0; i < 9; i++) b = b.slice(glassRoof, ring, ring, ring, ring, ring, floor);
                return b.slice(roof, wall, wall, wall, wall, "CCCCCHCCCCC", "CEEMIIOOFXC")
                        .where('H', Predicates.controller(definition))
                        .where('C', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get()))
                        .where('G', Predicates.blocks(GTBlocks.FUSION_GLASS.get()))
                        .where('E', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get())
                                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                        .setMaxGlobalLimited(4)))
                        .where('M', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get())
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .where('I', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get())
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2)))
                        .where('O', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get())
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2)))
                        .where('F', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get())
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(3)))
                        .where('X', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get())
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2)))
                        .where(' ', Predicates.air())
                        .build();
            })
            .workableCasingModel(PhoenixCore.id("block/phoenix_heart_casing"),
                    GTCEu.id("block/multiblock/pyrolyse_oven"))
            .register();

    public static void init() {}
}
