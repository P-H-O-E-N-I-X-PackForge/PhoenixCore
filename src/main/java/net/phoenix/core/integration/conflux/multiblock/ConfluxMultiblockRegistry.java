package net.phoenix.core.integration.conflux.multiblock;

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

public final class ConfluxMultiblockRegistry {

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
                            .or(abilities(net.phoenix.core.api.machine.PhoenixPartAbility.CONFLUX_DATA_OUTPUT)
                                    .setMinGlobalLimited(1)
                                    .setMaxGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .workableCasingModel(
                    com.gregtechceu.gtceu.GTCEu.id("block/casings/solid/machine_casing_robust_tungstensteel"),
                    PhoenixCore.id("block/multiblock/conflux_cascade_engine"))
            .register();

    public static final MachineEntry<MultiblockMachineDefinition> RESONANCE_WEB = REGISTRATE
            .multiblock("conflux_resonance_web", ConfluxResonanceWeb::new)
            .recipeType(com.gregtechceu.gtceu.common.data.GTRecipeTypes.DUMMY_RECIPES)
            .langValue("Conflux Resonance Web")
            .tier(GTValues.UV)
            .rotationState(RotationState.NON_Y_AXIS)
            .tooltips(
                    Component.literal("§bPassive all-type data generator.§r"),
                    Component.literal("Scans nearby Conflux pipes and scales output with network size."),
                    Component.literal("Sends its data out through a Conflux Data Output Hatch."),
                    Component.literal("§eDiversity bonus: +25% per distinct pipe type in range.§r"),
                    Component.literal("§7Scan radius: 24 blocks every 5 seconds.§r"))
            .appearanceBlock(GTBlocks.CASING_HSSE_STURDY)

            .pattern(def -> MultiblockPatternBuilder.start()

                    .slice("CCCCCCCCC", "C       C", "C       C", "C       C", "CCCCCCCCC")

                    .slice("CCCCCCCCC", "C       C", "C G   G C", "C       C", "CCCCCCCCC")

                    .slice("CCCCCCCCC", "C  G G  C", "C G   G C", "C  G G  C", "CCCCCCCCC")

                    .slice("CCCCCCCCC", "C       C", "CGG   GGC", "C       C", "CCCCCCCCC")

                    .slice("CCCECCCCC", "C       C", "CGGGEGGGC", "C       C", "CCCECCCCC")

                    .slice("CCCCCCCCC", "C       C", "CGGHCGGGC", "C       C", "CCCCCCCCC")

                    .slice("CCCCCCCCC", "C  G G  C", "C G   G C", "C  G G  C", "CCCCCCCCC")

                    .slice("CCCCCCCCC", "C       C", "C G   G C", "C       C", "CCCCCCCCC")

                    .slice("CCCCCCCCC", "C       C", "C  CMC  C", "C       C", "CCCCCCCCC")
                    .where('H', controller(def))
                    .where('C', blocks(GTBlocks.CASING_HSSE_STURDY.get()))
                    .where('G', blocks(GTBlocks.CASING_HSSE_STURDY.get())
                            .or(blocks(GTBlocks.CASING_LAMINATED_GLASS.get())))
                    .where('E', blocks(GTBlocks.CASING_HSSE_STURDY.get())
                            .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(6)))
                    .where('M', blocks(GTBlocks.CASING_HSSE_STURDY.get())
                            .or(abilities(PartAbility.MAINTENANCE).setMinGlobalLimited(1))
                            .or(abilities(net.phoenix.core.api.machine.PhoenixPartAbility.CONFLUX_DATA_OUTPUT)
                                    .setMinGlobalLimited(1)
                                    .setMaxGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .workableCasingModel(
                    com.gregtechceu.gtceu.GTCEu.id("block/casings/solid/machine_casing_sturdy_hsse"),
                    PhoenixCore.id("block/multiblock/conflux_resonance_web"))
            .register();

    public static final MachineEntry<MultiblockMachineDefinition> HARMONIC_LENS = REGISTRATE
            .multiblock("conflux_harmonic_lens", ConfluxHarmonicLens::new)
            .recipeType(com.gregtechceu.gtceu.common.data.GTRecipeTypes.DUMMY_RECIPES)
            .langValue("Conflux Harmonic Lens")
            .tier(GTValues.ZPM)
            .rotationState(RotationState.NON_Y_AXIS)
            .tooltips(
                    Component.literal("§bConflux data type transmuter.§r"),
                    Component.literal("Converts one Conflux data type to another at 70% base efficiency."),
                    Component.literal("Takes data from a Conflux Data Input Hatch and sends it to an Output Hatch."),
                    Component.literal("§eNearby researchers increase conversion efficiency (+0.5% / 50 nodes).§r"),
                    Component.literal("§7Screwdriver: cycle types. Shift+Screwdriver: cycle input type.§r"))
            .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST)

            .pattern(def -> MultiblockPatternBuilder.start()

                    .slice("XXX", "XHX", "XXX")

                    .slice("XXX", "XSX", "XXX")

                    .slice("XEX", "X X", "XEX")

                    .slice("XXX", "X X", "XXX")

                    .slice("XEX", "X X", "XEX")

                    .slice("XXX", "XMX", "XXX")

                    .slice("XXX", "XXX", "XXX")
                    .where('H', controller(def))
                    .where('X', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()))
                    .where('E', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(4))
                            .or(abilities(net.phoenix.core.api.machine.PhoenixPartAbility.CONFLUX_DATA_OUTPUT)
                                    .setMinGlobalLimited(1)
                                    .setMaxGlobalLimited(1)))
                    .where('S', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(net.phoenix.core.api.machine.PhoenixPartAbility.CONFLUX_DATA_INPUT)
                                    .setMinGlobalLimited(1)
                                    .setMaxGlobalLimited(1)))
                    .where('M', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                            .or(abilities(PartAbility.MAINTENANCE).setMinGlobalLimited(1)))
                    .where(' ', Predicates.air())
                    .build())
            .workableCasingModel(
                    com.gregtechceu.gtceu.GTCEu.id("block/casings/solid/machine_casing_robust_tungstensteel"),
                    PhoenixCore.id("block/multiblock/conflux_harmonic_lens"))
            .register();

    public static final MachineEntry<MultiblockMachineDefinition> PANOPTIC_ENGINE = REGISTRATE
            .multiblock("conflux_panoptic_engine",
                    net.phoenix.core.integration.conflux.panoptic.PanopticEngineMachine::new)
            .recipeType(com.gregtechceu.gtceu.common.data.GTRecipeTypes.DUMMY_RECIPES)
            .langValue("Panoptic Engine")
            .tier(GTValues.UHV)
            .rotationState(RotationState.NON_Y_AXIS)
            .tooltips(
                    Component.literal("§bThe engine that sees everything, including whoever is using it.§r"),
                    Component.literal(
                            "Its puzzle is built from your own research and is different for each discipline."),
                    Component.literal("A tree grows in the open well of the structure as the puzzle is solved."),
                    Component.literal("§7Progress belongs to your team and survives the machine being rebuilt.§r"))
            .appearanceBlock(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST)
            .pattern(def -> {
                String top = "CCCCCCCCCCC";
                String edge = "CGGGGGGGGGC";
                String floor = "CCCCCCCCCCC";
                String ringTop = "C         C";
                String ringMid = "G         G";

                MultiblockPatternBuilder b = MultiblockPatternBuilder.start()
                        .slice(top, edge, edge, edge, edge, edge, floor);
                for (int i = 0; i < 9; i++) {
                    b = b.slice(ringTop, ringMid, ringMid, ringMid, ringMid, ringMid, floor);
                }
                return b.slice(top, edge, edge, edge, edge, "CGGGGHGGGGC", "CEEMDFFFFCC")
                        .where('H', controller(def))
                        .where('C', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()))
                        .where('G', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                                .or(blocks(GTBlocks.CASING_LAMINATED_GLASS.get())))
                        .where('E', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                                .or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                                        .setMaxGlobalLimited(4)))
                        .where('M', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                                .or(abilities(PartAbility.MAINTENANCE).setMinGlobalLimited(1)))
                        .where('D', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                                .or(abilities(net.phoenix.core.api.machine.PhoenixPartAbility.CONFLUX_DATA_INPUT)
                                        .setMinGlobalLimited(1).setMaxGlobalLimited(1)))
                        .where('F', blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                                .or(abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(6)))
                        .where(' ', Predicates.air())
                        .build();
            })

            .modelProperty(com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.RECIPE_LOGIC_STATUS,
                    com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic.Status.IDLE)
            .model(com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel(
                    com.gregtechceu.gtceu.GTCEu.id("block/casings/solid/machine_casing_robust_tungstensteel"),
                    PhoenixCore.id("block/multiblock/conflux_panoptic_engine")))
            .register();

    public static void init() {}

    private ConfluxMultiblockRegistry() {}
}
