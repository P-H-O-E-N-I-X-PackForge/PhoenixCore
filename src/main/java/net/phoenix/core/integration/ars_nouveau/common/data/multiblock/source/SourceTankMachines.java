package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source;



import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.machine.PhoenixPartAbility;
import net.phoenix.core.common.block.PhoenixBlocks;

import java.util.function.Function;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.frameGt;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.DUMMY_RECIPES;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.SOURCE_IMBUED_TITANIUM;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class SourceTankMachines {

    private SourceTankMachines() {}

    public static final MultiblockMachineDefinition SOURCE_TANK_T1 = register("source_tank_t1", "Source Tank Mk I",
            5_000_000, 100, SourceTankMachines::patternT1);
    public static final MachineEntry<MultiblockMachineDefinition> SOURCE_TANK_T2 = register("source_tank_t2", "Source Tank Mk II",
            50_000_000, 1_000, SourceTankMachines::patternT2);
    public static final MachineEntry<MultiblockMachineDefinition> SOURCE_TANK_T3 = register("source_tank_t3", "Source Tank Mk III",
            500_000_000, 10_000, SourceTankMachines::patternT3);

    public static void init() {}

    private static MultiblockMachineDefinition register(String id, String name, int capacity, int maxConsumption,
                                                        Function<MultiblockMachineDefinition, com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern> pattern) {
        return REGISTRATE
                .multiblock(id, holder -> new SourceMultiblockTankMachine(holder, capacity, maxConsumption))
                .langValue(name)
                .tooltips(
                        Component.translatable("phoenixcore.machine.multiblock.source_tank.tooltip"),
                        Component.translatable("phoenixcore.universal.tooltip.source_storage_capacity", capacity))
                .rotationState(RotationState.ALL)
                .recipeType(DUMMY_RECIPES)
                .pattern(pattern::apply)
                .appearanceBlock(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING)
                .workableCasingModel(
                        ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "block/casings/multiblock/machine_casing_source_fiber_mesh"),
                        com.gregtechceu.gtceu.GTCEu.id("block/multiblock/multiblock_tank"))
                .register();
    }

    private static MultiblockPatternBuilder start() {
        return MultiblockPatternBuilder.start(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT);
    }

    private static MultiPredicate named(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("Missing block for the source tank pattern: " + id);
        }
        return Predicates.blocks(block);
    }

    private static MultiPredicate frame() {
        return Predicates.blocks(ChemicalHelper.getBlock(frameGt, SOURCE_IMBUED_TITANIUM));
    }

    private static MultiPredicate fiberWithHatches() {
        return Predicates.blocks(PhoenixBlocks.SOURCE_FIBER_MACHINE_CASING.get())
                .or(Predicates.abilities(PhoenixPartAbility.SOURCE_INPUT).setExactLimit(1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                .or(Predicates.abilities(PhoenixPartAbility.SOURCE_OUTPUT).setExactLimit(1));
    }

    private static com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern patternT1(
                                                                                        MultiblockMachineDefinition definition) {
        return start()
                .slice("aba", "ccc", "cfc", "ccc")
                .slice("bbb", "ccc", "ded", "ccc")
                .slice("aba", "ccc", "cdc", "ccc")
                .where('a', frame())
                .where('b', Predicates.air())
                .where('c', fiberWithHatches())
                .where('d', Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                .where('e', named("ars_nouveau:void_prism"))
                .where('f', Predicates.controller(definition))
                .build();
    }

    private static com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern patternT2(
                                                                                        MultiblockMachineDefinition definition) {
        return start()
                .slice("aabaa", "ccicc", "cdddc", "cdddc", "cdddc", "ccecc")
                .slice("abbba", "ccccc", "dfgfd", "dfgfd", "dfgfd", "ceeec")
                .slice("bbcbb", "ccccc", "dghgd", "dghgd", "dghgd", "eeeee")
                .slice("abbba", "ccccc", "dfgfd", "dfgfd", "dfgfd", "ceeec")
                .slice("aabaa", "ccccc", "cdddc", "cdddc", "cdddc", "ccecc")
                .where('a', frame())
                .where('b', Predicates.air())
                .where('c', fiberWithHatches())
                .where('d', Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                .where('e', Predicates.blocks(GTBlocks.CASING_TITANIUM_STABLE.get()))
                .where('f', named("ars_nouveau:void_prism"))
                .where('g', named("ars_nouveau:sourcestone"))
                .where('h', Predicates.blocks(GTBlocks.CASING_TITANIUM_PIPE.get()))
                .where('i', Predicates.controller(definition))
                .build();
    }

    private static com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern patternT3(
                                                                                        MultiblockMachineDefinition definition) {
        return start()
                .slice("bbbbbbb", "bbbbbbb", "bbbbbbb", "bbbbbbb", "bbbbbbb", "bbbbbbb", "bbcccbb", "bbdddbb",
                        "bbbbbba")
                .slice("bbbebbb", "bbbebbb", "bbdkdbb", "bbcfcbb", "bbcfcbb", "bbcccbb", "bcccccb", "bdddddb",
                        "bbgggbb")
                .slice("bbbbbbb", "bbbbbbb", "bdddddb", "bchghcb", "bchghcb", "bcgggcb", "ccebecc", "ddebedd",
                        "bghhhgb")
                .slice("bebebeb", "bebebeb", "bdddddb", "bfgegfb", "bfgegfb", "bcgggcb", "ccbibcc", "ddbibdd",
                        "bghjhgb")
                .slice("bbbbbbb", "bbbbbbb", "bdddddb", "bchghcb", "bchghcb", "bcgggcb", "ccebecc", "ddebedd",
                        "bghhhgb")
                .slice("bbbebbb", "bbbebbb", "bbdddbb", "bbcfcbb", "bbcfcbb", "bbcccbb", "bcccccb", "bdddddb",
                        "bbgggbb")
                .slice("abbbbbb", "bbbbbbb", "bbbbbbb", "bbbbbbb", "bbbbbbb", "bbbbbbb", "bbcccbb", "bbdddbb",
                        "bbbbbbb")
                .where('a', Predicates.blocks(Blocks.GRASS_BLOCK))
                .where('b', Predicates.air())
                .where('c', Predicates.blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()))
                .where('d', fiberWithHatches())
                .where('e', frame())
                .where('f', Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                .where('g', named("ars_nouveau:sourcestone"))
                .where('h', named("ars_nouveau:void_prism"))
                .where('i', Predicates.blocks(GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get()))
                .where('j', named("ars_nouveau:vitalic_sourcelink"))
                .where('k', Predicates.controller(definition))
                .build();
    }
}
