package net.phoenix.core.common.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.MultiblockPatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.registry.registrate.entry.GTRecipeTypeEntry;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.ActiveTransformerMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.SteamParallelMultiblockMachine;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.PhoenixBlocks;
import net.phoenix.core.common.data.PftRecipeTypes;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class PftMachines {

    private PftMachines() {}

    public static void init() {}

    private static String title(String id) {
        StringBuilder out = new StringBuilder();
        for (String part : id.split("_")) {
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static Block block(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        if (block == Blocks.AIR)
            throw new IllegalStateException("[PFT] pattern needs the block " + id + ", which does not exist");
        return block;
    }

    private static MachineEntry<MachineDefinition>[] simple(String name, GTRecipeTypeEntry type, String hullModel,
                                                            int... tiers) {
        return PhoenixMachines.registerTieredMachines(name,
                (holder, tier) -> new SimpleTieredMachine(holder, tier, GTMachineUtils.defaultTankSizeFunction),
                (tier, builder) -> builder
                        .langValue(GTValues.VNF[tier] + " " + title(name))
                        .rotationState(RotationState.NON_Y_AXIS)
                        .recipeType(type)
                        .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
                        .workableTieredHullModel(ResourceLocation.parse(hullModel))
                        .register(),
                tiers);
    }

    public static final MachineEntry<MachineDefinition>[] EARTH_SIEVE = simple("earth_sieve",
            PftRecipeTypes.EARTH_SIEVE, "gtceu:block/machines/sifter", GTValues.LV, GTValues.MV, GTValues.HV);
    public static final MachineEntry<MachineDefinition>[] THERMO_GENERATOR = simple("thermo_generator",
            PftRecipeTypes.THERMO_GENERATOR, "gtceu:block/machines/thermal_centrifuge", GTValues.LV, GTValues.MV,
            GTValues.HV);
    public static final MachineEntry<MachineDefinition>[] CONFECTIONERY_FABRICATOR = simple("confectionery_fabricator",
            PftRecipeTypes.CONFECTIONERY_FABRICATOR, "gtceu:block/machines/thermal_centrifuge", GTValues.IV,
            GTValues.LuV, GTValues.ZPM);
    public static final MachineEntry<MachineDefinition>[] EMBER_IGNITER = simple("ember_igniter",
            PftRecipeTypes.EMBER_IGNITER, "gtceu:block/machines/sifter", GTValues.LV, GTValues.MV, GTValues.HV);
    public static final MachineEntry<MachineDefinition>[] LAPIDARY = simple("lapidary",
            PftRecipeTypes.LAPIDARY, "gtceu:block/machines/lathe", GTValues.LV, GTValues.MV, GTValues.HV);
    public static final MachineEntry<MachineDefinition>[] WATER_SOURCE = simple("water_source",
            PftRecipeTypes.WATER_SOURCE, "gtceu:block/machines/ore_washer", GTValues.ULV);

    public static final MachineEntry<MultiblockMachineDefinition> GREENHOUSE = REGISTRATE
            .multiblock("greenhouse", WorkableElectricMultiblockMachine::new)
            .langValue("Greenhouse")
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(PftRecipeTypes.GREENHOUSE)
            .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT, GTRecipeModifiers.BATCH_MODE)
            .appearanceBlock(GTBlocks.MACHINE_CASING_ULV)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.LEFT)
                    .slice("0BBB0", "0BBB0", "0BBB0", "0BBB0")
                    .slice("BBBBB", "BDDDB", "B###B", "BGGGB")
                    .slice("BBBBB", "BDDDB", "B###B", "BGGGB")
                    .slice("BBBBB", "BDDDB", "B###B", "BGGGB")
                    .slice("0BBB0", "0BEB0", "0BBB0", "0BBB0")
                    .where('E', Predicates.controller(definition))
                    .where('D', Predicates.blocks(Blocks.DIRT))
                    .where('G', Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                    .where('B', Predicates.blocks(GTBlocks.MACHINE_CASING_ULV.get()).setMinGlobalLimited(5)
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .where('#', Predicates.any())
                    .where('0', Predicates.any())
                    .build())
            .workableCasingModel(ResourceLocation.parse("gtceu:block/casings/voltage/ulv/side"),
                    ResourceLocation.parse("gtceu:block/multiblock/implosion_compressor"))
            .register();

    public static final MachineEntry<MultiblockMachineDefinition> CONSTRUCTION_CORE = REGISTRATE
            .multiblock("construction_core", WorkableElectricMultiblockMachine::new)
            .langValue("Construction Core")
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(PftRecipeTypes.CONSTRUCTION_CORE)
            .recipeModifiers(GTRecipeModifiers.OC_NON_PERFECT, GTRecipeModifiers.BATCH_MODE)
            .appearanceBlock(GTBlocks.MACHINE_CASING_ULV)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.LEFT)
                    .slice("BBB", "GGG", "BBB")
                    .slice("BBB", "GCG", "BBB")
                    .slice("BEB", "GGG", "BBB")
                    .where('E', Predicates.controller(definition))
                    .where('C', Predicates.blocks(GTBlocks.CASING_STEEL_GEARBOX.get()))
                    .where('G', Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                    .where('B', Predicates.blocks(GTBlocks.MACHINE_CASING_LV.get()).setMinGlobalLimited(5)
                            .or(Predicates.autoAbilities(definition.getRecipeTypes())))
                    .build())
            .workableCasingModel(ResourceLocation.parse("gtceu:block/casings/voltage/lv/side"),
                    ResourceLocation.parse("gtceu:block/multiblock/implosion_compressor"))
            .register();

    public static final MachineEntry<MultiblockMachineDefinition> LARGE_STEAM_COMPRESSOR = REGISTRATE
            .multiblock("large_steam_compressor", info -> new SteamParallelMultiblockMachine(info, 4))
            .langValue("Large Steam Compressor")
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeTypes(GTRecipeTypes.COMPRESSOR_RECIPES)
            .recipeModifiers(true, SteamParallelMultiblockMachine::recipeModifier)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.LEFT)
                    .slice("BCCCB", "BBCBB", "BBBBB", "BBBBB")
                    .slice("CDDDC", "BDBDB", "BBDBB", "BBBBB")
                    .slice("CDDDC", "CBBBC", "BDDDB", "BBGBB")
                    .slice("CDDDC", "BDBDB", "BBDBB", "BBBBB")
                    .slice("BCCCB", "BBHBB", "BBBBB", "BBBBB")
                    .where('B', Predicates.any())
                    .where('C', Predicates.blocks(block("gtceu:steam_machine_casing")).setMinGlobalLimited(10)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setExactLimit(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setExactLimit(1)))
                    .where('D', Predicates.blocks(block("gtceu:industrial_steam_casing")))
                    .where('G', Predicates.blocks(block("gtceu:bronze_machine_casing")))
                    .where('H', Predicates.controller(definition))
                    .build())
            .workableCasingModel(ResourceLocation.parse("gtceu:block/casings/steam/bronze/bottom"),
                    ResourceLocation.parse("gtceu:block/machines/compressor"))
            .register();

    public static final MachineEntry<MultiblockMachineDefinition> LARGE_STEAM_SIEVE = REGISTRATE
            .multiblock("large_steam_sieve", info -> new SteamParallelMultiblockMachine(info, 4))
            .langValue("Large Steam Sieve")
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeTypes(PftRecipeTypes.EARTH_SIEVE)
            .recipeModifiers(true, SteamParallelMultiblockMachine::recipeModifier)
            .appearanceBlock(GTBlocks.CASING_PRIMITIVE_BRICKS)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.LEFT)
                    .slice("BBBBB", "CCCCC", "CCCCC", "CCCCC")
                    .slice("BCCCB", "CDDDC", "CDDDC", "CDDDC")
                    .slice("BCCCB", "CDDDC", "CDDDC", "CDDDC")
                    .slice("BCCCB", "CDDDC", "CDDDC", "CDDDC")
                    .slice("BBBBB", "CCECC", "CCCCC", "CCCCC")
                    .where('B', Predicates.blocks(block("gtceu:bronze_firebox_casing")))
                    .where('C', Predicates.blocks(block("gtceu:steam_machine_casing")).setMinGlobalLimited(10)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setMaxGlobalLimited(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setMaxGlobalLimited(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setMaxGlobalLimited(6)))
                    .where('E', Predicates.controller(definition))
                    .where('D', Predicates.blocks(block("gtceu:steel_frame")))
                    .build())
            .workableCasingModel(ResourceLocation.parse("gtceu:block/casings/steam/bronze/bottom"),
                    ResourceLocation.parse("gtceu:block/machines/sifter"))
            .register();

    public static final MachineEntry<MultiblockMachineDefinition> LARGE_STEAM_SIFTER = REGISTRATE
            .multiblock("large_steam_sifter", info -> new SteamParallelMultiblockMachine(info, 4))
            .langValue("Large Steam Sifter")
            .tooltips(Component.translatable("gtultimate.custom.tooltip_large_steam_sifter"))
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeTypes(GTRecipeTypes.SIFTER_RECIPES)
            .recipeModifiers(true, SteamParallelMultiblockMachine::recipeModifier)
            .appearanceBlock(GTBlocks.CASING_PRIMITIVE_BRICKS)
            .pattern(definition -> MultiblockPatternBuilder
                    .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.LEFT)
                    .slice("aaaaa", "bcbcb", "bbbbb", "bbbbb")
                    .slice("addda", "ccdcc", "bcdcb", "bbdbb")
                    .slice("addda", "bdfdb", "bdgdb", "bdgdb")
                    .slice("addda", "ccecc", "bcdcb", "bbdbb")
                    .slice("aaaaa", "bcbcb", "bbbbb", "bbbbb")
                    .where('a', Predicates.blocks(block("gtceu:industrial_steam_casing")))
                    .where('b', Predicates.any())
                    .where('c', Predicates.blocks(block("phoenixcore:fiery_bronze_frame")))
                    .where('d', Predicates.blocks(block("gtceu:steam_machine_casing")).setMinGlobalLimited(4)
                            .or(Predicates.abilities(PartAbility.STEAM_IMPORT_ITEMS).setMaxGlobalLimited(1))
                            .or(Predicates.abilities(PartAbility.STEAM).setMaxGlobalLimited(1))
                            .or(Predicates.abilities(PartAbility.STEAM_EXPORT_ITEMS).setMaxGlobalLimited(1)))
                    .where('e', Predicates.controller(definition))
                    .where('f', Predicates.blocks(GTBlocks.CASING_BRONZE_GEARBOX.get()))
                    .where('g', Predicates.air())
                    .build())
            .workableCasingModel(ResourceLocation.parse("gtceu:block/casings/steam/bronze/bottom"),
                    ResourceLocation.parse("gtceu:block/machines/sifter"))
            .register();

    @EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ActiveTransformerOverride {

        private ActiveTransformerOverride() {}

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                MultiblockMachineDefinition definition = GTMultiMachines.ACTIVE_TRANSFORMER.get();
                java.util.function.Supplier<com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern> pattern = com.google.common.base.Suppliers
                        .memoize(() -> MultiblockPatternBuilder
                                .start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.LEFT)
                                .slice("XXX", "XXX", "XXX")
                                .slice("XXX", "XCX", "XXX")
                                .slice("XXX", "XSX", "XXX")
                                .where('S', Predicates.controller(definition))
                                .where('X', Predicates.blocks(GTBlocks.HIGH_POWER_CASING.get()).setMinGlobalLimited(12)
                                        .or(ActiveTransformerMachine.getHatchPredicates()))
                                .where('C', Predicates.blocks(PhoenixBlocks.CONDUCTIVE_HONEY_COIL.get()))
                                .build());

                try {
                    java.util.Map<String, java.util.function.Supplier<com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern>> replaced = new java.util.LinkedHashMap<>();
                    definition.getStructurePatterns().keySet().forEach(key -> replaced.put(key, pattern));
                    java.lang.reflect.Field field = MultiblockMachineDefinition.class
                            .getDeclaredField("structurePatterns");
                    field.setAccessible(true);
                    field.set(definition, java.util.Collections.unmodifiableMap(replaced));
                } catch (ReflectiveOperationException e) {
                    PhoenixCore.LOGGER.error("[PFT] could not replace the Active Transformer structure", e);
                }
            });
        }
    }
}
