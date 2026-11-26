package net.phoenix.core.common.fission;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.phoenix_fission.api.PhoenixAPI;
import net.phoenix.phoenix_fission.api.block.FissionBlanketBlock;
import net.phoenix.phoenix_fission.api.block.FissionCoolerBlock;
import net.phoenix.phoenix_fission.api.block.FissionFuelRodBlock;
import net.phoenix.phoenix_fission.api.block.FissionModeratorBlock;
import net.phoenix.phoenix_fission.api.block.iface.IFissionBlanketType;
import net.phoenix.phoenix_fission.api.block.iface.IFissionCoolerType;
import net.phoenix.phoenix_fission.api.block.iface.IFissionFuelRodType;
import net.phoenix.phoenix_fission.api.block.iface.IFissionModeratorType;
import net.phoenix.phoenix_fission.datagen.models.PhoenixFissionMachineModels;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class PftFissionComponents {

    private PftFissionComponents() {}

    private static ResourceLocation fission(String path) {
        return ResourceLocation.fromNamespaceAndPath("phoenix_fission", path);
    }

    private record Rod(String name, String display, int heat, String fuel, String output, int ticks, int tint)
            implements IFissionFuelRodType {

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public int getTintColor() {
            return tint;
        }

        @Override
        public int getBaseHeatProduction() {
            return heat;
        }

        @Override
        public @NotNull String getFuelKey() {
            return fuel;
        }

        @Override
        public @NotNull String getOutputKey() {
            return output;
        }

        @Override
        public int getDurationTicks() {
            return ticks;
        }

        @Override
        public int getAmountPerCycle() {
            return 1;
        }

        @Override
        public ResourceLocation getTexture() {
            return fission("block/fission/fuel_rods/" + name);
        }
    }

    private record Cooler(String name, String display, int ratedHUt, String coolantIn, String coolantOut, int perTick,
                          int temperature, int tint)
            implements IFissionCoolerType {

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public int getTintColor() {
            return tint;
        }

        @Override
        public int getCoolerTemperature() {
            return temperature;
        }

        @Override
        public @NotNull String getRequiredCoolantMaterialId() {
            return coolantIn;
        }

        @Override
        public @NotNull String getOutputCoolantFluidId() {
            return coolantOut;
        }

        @Override
        public int getCoolantUsagePerTick() {
            return perTick;
        }

        @Override
        public double getRatedCoolingHUt() {
            return ratedHUt;
        }

        @Override
        public ResourceLocation getTexture() {
            return fission("block/fission/coolers/" + name);
        }
    }

    private record Moderator(String name, String display, int euBoost, int fuelDiscount, int tint)
            implements IFissionModeratorType {

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public int getTintColor() {
            return tint;
        }

        @Override
        public int getEUBoost() {
            return euBoost;
        }

        @Override
        public int getFuelDiscount() {
            return fuelDiscount;
        }

        @Override
        public double getHeatMultiplier() {
            return 1.0;
        }

        @Override
        public int getParallelBonus() {
            return 0;
        }

        @Override
        public ResourceLocation getTexture() {
            return fission("block/fission/moderators/" + name);
        }
    }

    private record Blanket(String name, String display, int tier, int tint, String requiredFuel, int ticks,
                           String input, List<IFissionBlanketType.BlanketOutput> outputs)
            implements IFissionBlanketType {

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public int getTintColor() {
            return tint;
        }

        @Override
        public @NotNull String getRequiredFuelKey() {
            return requiredFuel;
        }

        @Override
        public int getDurationTicks() {
            return ticks;
        }

        @Override
        public int getAmountPerCycle() {
            return 1;
        }

        @Override
        public @NotNull String getInputKey() {
            return input;
        }

        @Override
        public @NotNull List<BlanketOutput> getOutputs() {
            return outputs;
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            return fission("block/fission/blankets/" + name);
        }
    }

    private static IFissionBlanketType.BlanketOutput out(String key, int weight, int instability) {
        return new IFissionBlanketType.BlanketOutput(key, weight, instability);
    }

    public static void init() {
        List<Rod> rods = List.of(
                new Rod("t1_fuel_rod", "Tier 1 Fuel Rod", 10, "gtceu:uranium_238_dust", "gtceu:lead_dust", 1200,
                        0xFF8FBF5A),
                new Rod("t2_fuel_rod", "Tier 2 Fuel Rod", 25, "gtceu:uranium_235_dust", "gtceu:uranium_238_dust", 1000,
                        0xFF5ABF8F),
                new Rod("t3_fuel_rod", "Tier 3 Fuel Rod", 60, "phoenixcore:u235_fuel_pellet",
                        "gtceu:plutonium_239_dust", 900, 0xFF5A9FBF),
                new Rod("t4_fuel_rod", "Tier 4 Fuel Rod", 150, "phoenixcore:plutonium_241_fuel_pellet",
                        "gtceu:plutonium_239_dust", 800, 0xFF8F5ABF),
                new Rod("t5_fuel_rod", "Tier 5 Fuel Rod", 380, "phoenixcore:u236_fuel_pellet",
                        "gtceu:plutonium_241_dust", 700, 0xFFBF5A5A));
        for (Rod type : rods) {
            var entry = REGISTRATE.block(type.name(), p -> new FissionFuelRodBlock(p, type))
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                    .blockstate(PhoenixFissionMachineModels.createFuelRodModel(type))
                    .lang(type.display())
                    .item(BlockItem::new)
                    .build()
                    .register();
            PhoenixAPI.FISSION_FUEL_RODS.put(type, entry::get);
        }

        List<Cooler> coolers = List.of(
                new Cooler("cooler_basic", "Basic Cooler", 20, "minecraft:water", "gtceu:steam", 4, 373, 0xFFBBBBBB),
                new Cooler("cooler_ev", "EV Cooler", 300, "gtceu:liquid_oxygen", "gtceu:oxygen", 3, 90, 0xFFAA66FF),
                new Cooler("cooler_iv", "IV Cooler", 750, "gtceu:liquid_helium", "gtceu:helium", 2, 4, 0xFF66AACC),
                new Cooler("cooler_luv", "LuV Cooler", 1900, "gtceu:helium_3", "gtceu:helium", 2, 3, 0xFFFF66AA));
        for (Cooler type : coolers) {
            var entry = REGISTRATE.block(type.name(), p -> new FissionCoolerBlock(p, type))
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                    .blockstate(PhoenixFissionMachineModels.createActiveCoolerModel(type))
                    .lang(type.display())
                    .item(BlockItem::new)
                    .build()
                    .register();
            PhoenixAPI.FISSION_COOLERS.put(type, entry::get);
        }

        List<Moderator> moderators = List.of(
                new Moderator("graphite_moderator", "Graphite Moderator", 10, 5, 0xFF555555),
                new Moderator("beryllium_moderator", "Beryllium Moderator", 20, 10, 0xFF99BB88),
                new Moderator("heavy_water_moderator", "Heavy Water Moderator", 35, 15, 0xFF5599DD),
                new Moderator("niobium_sic_moderator", "Niobium-SiC Moderator", 50, 25, 0xFF8866CC));
        for (Moderator type : moderators) {
            var entry = REGISTRATE.block(type.name(), p -> new FissionModeratorBlock(p, type))
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                    .blockstate(PhoenixFissionMachineModels.createFissionModeratorModel(type))
                    .lang(type.display())
                    .item(BlockItem::new)
                    .build()
                    .register();
            PhoenixAPI.FISSION_MODERATORS.put(type, entry::get);
        }

        List<Blanket> blankets = List.of(
                new Blanket("thorium_blanket", "Thorium Blanket", 1, 0xFF6B6B6B, "gtceu:uranium_238_dust", 2400,
                        "gtceu:thorium_dust", List.of(out("phoenixcore:uranium_233_dust", 100, 1))),
                new Blanket("uranium_blanket", "Uranium Blanket", 2, 0xFF5ABF8F, "gtceu:uranium_235_dust", 2400,
                        "gtceu:uranium_238_dust",
                        List.of(out("gtceu:plutonium_239_dust", 70, 1), out("gtceu:plutonium_241_dust", 20, 3),
                                out("gtceu:neptunium_dust", 10, 4))),
                new Blanket("neptunium_blanket", "Neptunium Blanket", 3, 0xFF5A9FBF, "phoenixcore:u235_fuel_pellet",
                        2000, "gtceu:neptunium_dust",
                        List.of(out("gtceu:plutonium_239_dust", 60, 1), out("gtceu:americium_dust", 40, 3))),
                new Blanket("plutonium_blanket", "Plutonium Blanket", 4, 0xFF8F5ABF,
                        "phoenixcore:plutonium_241_fuel_pellet", 1800, "gtceu:plutonium_239_dust",
                        List.of(out("gtceu:plutonium_241_dust", 60, 1), out("gtceu:americium_dust", 30, 3),
                                out("gtceu:curium_dust", 10, 4))),
                new Blanket("americium_blanket", "Americium Blanket", 5, 0xFFBF5A5A, "phoenixcore:u236_fuel_pellet",
                        1600, "gtceu:americium_dust", List.of(out("gtceu:curium_dust", 100, 2))));
        for (Blanket type : blankets) {
            var entry = REGISTRATE.block(type.name(), p -> new FissionBlanketBlock(p, type))
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                    .blockstate(PhoenixFissionMachineModels.createBlanketRodModel(type))
                    .lang(type.display())
                    .item(BlockItem::new)
                    .build()
                    .register();
            PhoenixAPI.FISSION_BLANKETS.put(type, entry::get);
        }
    }
}
