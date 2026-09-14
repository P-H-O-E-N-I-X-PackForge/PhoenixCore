package net.phoenix.core.common.machine.multiblock.cinder;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.integration.ae2.CinderForgeHatchPartMachine;

import static com.gregtechceu.gtceu.api.GTValues.UHV;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class CinderForgeMachines {

    public static MachineDefinition CINDER_FORGE_HATCH = null;
    public static MachineDefinition CINDER_FORGE = null;

    static {
        if (GTCEu.Mods.isAE2Loaded() || GTCEu.isDataGen()) {

            CINDER_FORGE_HATCH = REGISTRATE
                    .machine("cinder_forge_hatch", CinderForgeHatchPartMachine::new)
                    .tier(UHV)
                    .rotationState(RotationState.ALL)
                    .abilities(PartAbility.IMPORT_ITEMS, PartAbility.EXPORT_ITEMS)

                    .colorOverlayTieredHullModel(net.phoenix.core.PhoenixCore.id("block/machine/ae2/me_tag_input_bus"))
                    .langValue("Cinder Forge ME Interface")
                    .tooltips(
                            Component.literal("§6Autonomous Cinder Core filling"),
                            Component.literal("§7Pulls a configured Core + its needed materials from the ME network,"),
                            Component.literal("§7stocks it, and hands it back once ready."),
                            Component.translatable("gtceu.part_sharing.enabled"))
                    .register();

            CINDER_FORGE = REGISTRATE
                    .multiblock("cinder_forge", CinderForgeMachine::new)
                    .langValue("Cinder Forge")
                    .rotationState(RotationState.NON_Y_AXIS)
                    .appearanceBlock(PhoenixBlocks.PHOENIX_HEART_CASING)
                    .tooltips(
                            Component.literal("Turns a configured Cinder Core plus raw materials into a"),
                            Component.literal("ready-to-place one."),
                            Component.literal("§7Tier 1: small, manual-only, no ME network needed.§r"),
                            Component.literal("§7Tier 2/3: needs one or more Cinder Forge ME Interface hatches.§r"))

                    .pattern(definition -> MultiblockPatternBuilder
                            .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                            .slice("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC")
                            .slice("CCCCC", "C   C", "CSCCC", "C   C", "CCCCC")
                            .slice("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC")
                            .where('S', Predicates.controller(definition))
                            .where('C', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get()))
                            .where(' ', Predicates.air())
                            .build())
                    .pattern("tier1", definition -> MultiblockPatternBuilder
                            .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                            .slice("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC")
                            .slice("CCCCC", "C   C", "CSCCC", "C   C", "CCCCC")
                            .slice("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC")
                            .where('S', Predicates.controller(definition))
                            .where('C', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get()))
                            .where(' ', Predicates.air())
                            .build())
                    .pattern("tier2", definition -> buildHivePattern(definition,
                            Predicates.abilities(PartAbility.IMPORT_ITEMS).setExactLimit(1),
                            Predicates.abilities(PartAbility.EXPORT_ITEMS).setExactLimit(1)))
                    .pattern("tier3", definition -> buildHivePattern(definition,
                            Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(2)
                                    .setMaxGlobalLimited(4),
                            Predicates.abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(2)
                                    .setMaxGlobalLimited(4)))

                    .workableCasingModel(net.phoenix.core.PhoenixCore.id("block/phoenix_heart_casing"),
                            com.gregtechceu.gtceu.GTCEu.id("block/multiblock/pyrolyse_oven"))
                    .register();
        }
    }

    private static IBlockPattern buildHivePattern(MultiblockMachineDefinition definition, MultiPredicate hatchImport,
                                                  MultiPredicate hatchExport) {
        return MultiblockPatternBuilder
                .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)

                .slice("BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB",
                        "BBBBBCBBBBB", "BBBBBCBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB")
                .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBCBBBBB",
                        "BBBBEEEBBBB", "BBBBEEEBBBB", "BBBBBCBBBBB", "BBBBBBBBBBB")
                .slice("BDDDDDDDDDB", "BBFBBBBBFBB", "BBFBBBBBFBB", "BBFBBCBBFBB", "BBFGGGGGFBB",
                        "BBGGAAAGGBB", "BBGGAAAGGBB", "BBBGGGGGBBB", "BBBBBCBBBBB")
                .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBCBCBCBBB", "BBGGGGGGGBB",
                        "BBGAAAAAGBB", "BBGAAAAAGBB", "BBGGGGGGGBB", "BBBBBCBBBBB")
                .slice("BDDDDDDDDDB", "BBBBBCBBBBB", "BBBBBCBBBBB", "BBBBCCCBBBB", "BBGGAAAGGBB",
                        "BEAAAAAAAEB", "BEAAAAAAAEB", "BBGGGAGGGBB", "BBBBBCBBBBB")
                .slice("BDDDDDDDDDB", "BBBBCDCBBBB", "BBBBCDCBBBB", "BBCCCCCCCBB", "BCGAACAAGCB",
                        "CEAAACAAAEC", "CEAAACAAAEC", "BCGGACAGGCB", "BBCCCCCCCBB")
                .slice("BDDDDDDDDDB", "BBBBBCBBBBB", "BBBBBCBBBBB", "BBBBCCCBBBB", "BBGGAAAGGBB",
                        "BEAAAAAAAEB", "BEAAAAAAAEB", "BBGGGAGGGBB", "BBBBBCBBBBB")
                .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBCBCBCBBB", "BBGGGGGGGBB",
                        "BBGAAAAAGBB", "BBGAAAAAGBB", "BBGGGGGGGBB", "BBBBBCBBBBB")
                .slice("BDDDDDDDDDB", "BBFBBBBBFBB", "BBFBBBBBFBB", "BBFBBCBBFBB", "BBFGGGGGFBB",
                        "BBGGAAAGGBB", "BBGGAAAGGBB", "BBBGGGGGBBB", "BBBBBCBBBBB")
                .slice("BDDDDDDDDDB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBCCCBBBB",
                        "BBBCEHECBBB", "BBBCEEECBBB", "BBBBCCCBBBB", "BBBBBBBBBBB")
                .slice("BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB",
                        "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB", "BBBBBBBBBBB")

                .where('A', Predicates.air())
                .where('B', Predicates.any())
                .where('C',
                        Predicates.blocks(ForgeRegistries.BLOCKS
                                .getValue(ResourceLocation.fromNamespaceAndPath("gtceu", "steel_frame"))))
                .where('D', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get()))
                .where('E', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get()))
                .where('F', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get()))
                .where('G', Predicates.blocks(PhoenixBlocks.PHOENIX_HEART_CASING.get())
                        .or(hatchImport)
                        .or(hatchExport))
                .where('H', Predicates.controller(definition))
                .build();
    }

    public static void init() {}

    private CinderForgeMachines() {}
}
