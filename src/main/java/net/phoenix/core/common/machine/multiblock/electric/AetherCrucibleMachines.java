package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.network.chat.Component;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.machine.multiblock.api.PhoenixMultiblockProperties;
import net.phoenix.core.common.machine.multiblock.api.PhoenixPartAppearance;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class AetherCrucibleMachines {

    public static final PartAbility AETHER_CATALYST_ABILITY = new PartAbility("aether_catalyst");

    public static final MachineEntry<MachineDefinition> AETHER_CATALYST_HATCH_EV = registerCatalystHatch(GTValues.EV);
    public static final MachineEntry<MachineDefinition> AETHER_CATALYST_HATCH_IV = registerCatalystHatch(GTValues.IV);

    private static MachineEntry<MachineDefinition> registerCatalystHatch(int tier) {
        return REGISTRATE
                .machine("aether_catalyst_hatch_" + GTValues.VN[tier].toLowerCase(),
                        holder -> new AetherCrucibleMachine.AetherCatalystHatch(holder, tier))
                .langValue(GTValues.VNF[tier] + " Aether Catalyst Hatch")
                .tier(tier)
                .rotationState(RotationState.ALL)
                .abilities(AETHER_CATALYST_ABILITY)
                .tooltips(
                        Component.literal("Lets the Aether Crucible resonate with an attuned catalyst."),
                        Component.literal("§7Resonant: 30% faster, 2× parallel, for as long as the catalyst lasts.§r"))
                .colorOverlayTieredHullModel(net.phoenix.core.PhoenixCore.id("block/machine/ae2/me_tag_input_hatch"))
                .register();
    }

    public static final MachineEntry<MultiblockMachineDefinition> AETHER_CRUCIBLE = REGISTRATE
            .multiblock("aether_crucible", AetherCrucibleMachine::new)
            .langValue("Aether Crucible")
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(net.phoenix.core.common.data.PhoenixRecipeTypes.AETHER_CRUCIBLE_RECIPES)
            .recipeModifiers(
                    GTRecipeModifiers.PARALLEL_HATCH,
                    GTRecipeModifiers.OC_NON_PERFECT,
                    AetherCrucibleMachine::recipeModifier)
            .appearanceBlock(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING)
            .modelProperty(PhoenixMultiblockProperties.FORMATION_TIER, 0)
            .partAppearance(PhoenixPartAppearance.rules()
                    .ownTextureForAbilitiesAtTier(1,
                            PartAbility.IMPORT_ITEMS, PartAbility.EXPORT_ITEMS,
                            PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS)
                    .build(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING))
            .tooltips(
                    Component.literal("Transmutes materials through aetheric resonance."),
                    Component.literal(
                            "§7Aetheric§r: a Fluid Input Hatch makes recipes that burn a drilling fluid 15% faster"),
                    Component.literal(
                            "§7Resonant§r: an Aether Catalyst Hatch lets you attune a catalyst from an input hatch: 30% faster and 2× parallel for a while"),
                    Component.literal("§7Better catalysts last longer but ask for a harder code§r"),
                    Component.literal("§8Bonuses unlock when the structure is re-formed.§r"))
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)

                    .slice("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC")

                    .slice("CCCCC", "G   G", "G   G", "G   G", "CCCCC")

                    .slice("CCCCC", "CCCCC", "CCSCC", "CCCCC", "CCCCC")
                    .where('S', Predicates.controller(definition))
                    .where('C', Predicates.blocks(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING.get())

                            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))

                            .or(Predicates.abilities(AETHER_CATALYST_ABILITY).setMaxGlobalLimited(1)))
                    .where('G', Predicates.blocks(com.gregtechceu.gtceu.common.data.GTBlocks.FUSION_GLASS.get()))
                    .where(' ', Predicates.air())
                    .build())
            .model(com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel(
                    net.phoenix.core.PhoenixCore.id("block/casings/multiblock/machine_casing_source_fiber_mesh"),
                    com.gregtechceu.gtceu.GTCEu.id("block/multiblock/pyrolyse_oven")).andThen(
                            b -> b.addDynamicRenderer(
                                    net.phoenix.core.client.renderer.machine.multiblock.PhoenixDynamicRenderHelpers::getCrucibleRenderer)))
            .hasBER(true)
            .register();

    public static void init() {}

    private AetherCrucibleMachines() {}
}
