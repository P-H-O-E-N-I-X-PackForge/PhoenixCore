package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.phoenix.core.PhoenixCore;
import net.phoenix_tesla_network.tesla.common.machine.PhoenixTeslaMachines;
import net.phoenix_tesla_network.tesla.common.machine.multiblock.electric.TeslaTowerType;
import net.phoenix_tesla_network.tesla.configs.PhoenixTeslaConfigs;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TeslaEmi {

    private TeslaEmi() {}

    private static final int ACCENT = 0xFF58C8FF;
    private static final int W = 220;

    private static EmiStack tower() {
        for (var entry : List.of(PhoenixTeslaMachines.TESLA_TOWER, PhoenixTeslaMachines.TESLA_TOWER_ADVANCED,
                PhoenixTeslaMachines.TESLA_TOWER_BASIC)) {
            if (entry != null) return EmiStack.of(entry.asStack());
        }
        return EmiStack.EMPTY;
    }

    public static EmiRecipeCategory TESLA;

    public static void register(EmiRegistry registry) {
        if (!ModList.get().isLoaded("phoenix_tesla_network")) return;
        TESLA = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "tesla_network"),
                tower()) {

            @Override
            public Component getName() {
                return Component.literal("Tesla Network");
            }
        };
        registry.addCategory(TESLA);
        for (TeslaTowerType type : TeslaTowerType.values()) {
            var entry = switch (type) {
                case BASIC -> PhoenixTeslaMachines.TESLA_TOWER_BASIC;
                case ADVANCED -> PhoenixTeslaMachines.TESLA_TOWER_ADVANCED;
                case ULTIMATE -> PhoenixTeslaMachines.TESLA_TOWER;
            };
            if (entry == null) continue;
            registry.addWorkstation(TESLA, EmiStack.of(entry.asStack()));
            registry.addRecipe(new TowerPage(type, EmiStack.of(entry.asStack())));
        }
        if (PhoenixTeslaMachines.TESLA_RELAY != null) {
            registry.addWorkstation(TESLA, EmiStack.of(PhoenixTeslaMachines.TESLA_RELAY.asStack()));
            registry.addRecipe(new RelayPage(EmiStack.of(PhoenixTeslaMachines.TESLA_RELAY.asStack())));
        }
        addHatchPage(registry, "Energy hatches", "energy_hatches", List.of(
                new Family("Input 2A", PhoenixTeslaMachines.TESLA_INPUT_2A),
                new Family("Input 4A", PhoenixTeslaMachines.TESLA_INPUT_4A),
                new Family("Input 16A", PhoenixTeslaMachines.TESLA_INPUT_16A),
                new Family("Input 64A", PhoenixTeslaMachines.TESLA_INPUT_64A),
                new Family("Output 2A", PhoenixTeslaMachines.TESLA_OUTPUT_2A),
                new Family("Output 4A", PhoenixTeslaMachines.TESLA_OUTPUT_4A),
                new Family("Output 16A", PhoenixTeslaMachines.TESLA_OUTPUT_16A),
                new Family("Output 64A", PhoenixTeslaMachines.TESLA_OUTPUT_64A)));
        List<Family> lasers = new ArrayList<>();
        addLaser(lasers, "256A", PhoenixTeslaMachines.TESLA_LASER_INPUT_256A,
                PhoenixTeslaMachines.TESLA_LASER_OUTPUT_256A);
        addLaser(lasers, "1024A", PhoenixTeslaMachines.TESLA_LASER_INPUT_1024A,
                PhoenixTeslaMachines.TESLA_LASER_OUTPUT_1024A);
        addLaser(lasers, "4096A", PhoenixTeslaMachines.TESLA_LASER_INPUT_4096A,
                PhoenixTeslaMachines.TESLA_LASER_OUTPUT_4096A);
        addLaser(lasers, "16384A", PhoenixTeslaMachines.TESLA_LASER_INPUT_16384A,
                PhoenixTeslaMachines.TESLA_LASER_OUTPUT_16384A);
        addLaser(lasers, "65536A", PhoenixTeslaMachines.TESLA_LASER_INPUT_65536A,
                PhoenixTeslaMachines.TESLA_LASER_OUTPUT_65536A);
        addLaser(lasers, "262144A", PhoenixTeslaMachines.TESLA_LASER_INPUT_262144A,
                PhoenixTeslaMachines.TESLA_LASER_OUTPUT_262144A);
        addLaser(lasers, "1048576A", PhoenixTeslaMachines.TESLA_LASER_INPUT_1048576A,
                PhoenixTeslaMachines.TESLA_LASER_OUTPUT_1048576A);
        addHatchPage(registry, "Laser hatches", "laser_hatches", lasers);
        if (PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER != null) {
            addHatchPage(registry, "Wireless charger", "wireless_charger",
                    List.of(new Family("Charger", PhoenixTeslaMachines.TESLA_WIRELESS_CHARGER)));
        }
    }

    private static void addLaser(List<Family> out, String amps, MachineEntry<MachineDefinition>[] in,
                                 MachineEntry<MachineDefinition>[] output) {
        if (in != null && first(in) >= 0) out.add(new Family("Input " + amps, in));
        if (output != null && first(output) >= 0) out.add(new Family("Output " + amps, output));
    }

    private static void addHatchPage(EmiRegistry registry, String title, String id, List<Family> families) {
        List<Family> real = families.stream().filter(f -> f.machines() != null && first(f.machines()) >= 0).toList();
        if (real.isEmpty()) return;
        registry.addRecipe(new HatchPage(title, id, real));
    }

    private static int first(MachineEntry<?>[] machines) {
        for (int i = 0; i < machines.length; i++) if (machines[i] != null) return i;
        return -1;
    }

    private static int last(MachineEntry<?>[] machines) {
        for (int i = machines.length - 1; i >= 0; i--) if (machines[i] != null) return i;
        return -1;
    }

    private static String tier(int t) {
        return TeslaTowerType.tierName(t);
    }

    private static String pct(double v) {
        return (v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v)) + "%";
    }

    private record Family(String name, MachineEntry<MachineDefinition>[] machines) {}

    private static final class TowerPage extends BasicEmiRecipe {

        private final TeslaTowerType type;
        private final EmiStack stack;

        TowerPage(TeslaTowerType type, EmiStack stack) {
            super(TESLA, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/tesla_network/" +
                    type.registryName()), W, 122);
            this.type = type;
            this.stack = stack;
            this.outputs = List.of(stack);
            this.catalysts = List.of(stack);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            var p = type.profile();
            EmiPage.frame(widgets, W, height, ACCENT);
            widgets.addSlot(stack, 0, 0).drawBack(false);
            EmiPage.title(widgets, type.displayName(), 22, 2, W - 22, 0xFFFFFFFF);
            EmiPage.row(widgets, "A tower of the Tesla Network", 22, 12, W - 22, EmiPage.DIM, null);

            int y = EmiPage.section(widgets, "Reach", 0, 22, W, ACCENT);
            String range = (p.infiniteRange ? "Infinite range" : p.rangeBlocks + " blocks") + "  \u00b7  " +
                    (p.crossDimension ? "reaches other dimensions" : "same dimension only");
            EmiPage.row(widgets, range, 0, y, W, p.infiniteRange ? EmiPage.GOOD : EmiPage.TEXT,
                    List.of(Component
                            .literal(p.infiniteRange ? "No distance limit" : "Extend it with Tesla Range Extenders"),
                            Component.literal(p.crossDimension ? "Other dimensions are covered" :
                                    "Only its own dimension is covered")));
            y += EmiPage.ROW_H + 3;

            y = EmiPage.section(widgets, "Accepts", 0, y, W, ACCENT);
            EmiPage.row(widgets, "Hatches " + tier(p.minHatchTier) + "-" + tier(p.maxHatchTier) + "   Batteries " +
                    tier(p.minBatteryTier) + "-" + tier(p.maxBatteryTier), 0, y, W, EmiPage.TEXT, null);
            y += EmiPage.ROW_H;
            List<String> ok = new ArrayList<>(), no = new ArrayList<>();
            (p.allowEnergyInput ? ok : no).add("energy in");
            (p.allowEnergyOutput ? ok : no).add("energy out");
            (p.allowSubstationInput ? ok : no).add("substation in");
            (p.allowSubstationOutput ? ok : no).add("substation out");
            EmiPage.row(widgets, "\u2714 " + String.join(", ", ok) + (no.isEmpty() ? "" : "   \u25e6 " +
                    String.join(", ", no)), 0, y, W, no.isEmpty() ? EmiPage.GOOD : EmiPage.TEXT,
                    List.of(Component.literal("Hatch types it accepts"),
                            Component.literal("\u2714 " + String.join(", ", ok))));
            y += EmiPage.ROW_H + 3;

            y = EmiPage.section(widgets, "Power lost on the way", 0, y, W, ACCENT);
            var loss = p.loss;
            if (loss == null || !loss.enabled) {
                EmiPage.row(widgets, "\u2714 None", 0, y, W, EmiPage.GOOD, null);
            } else {
                EmiPage.row(widgets, pct(loss.baseLossPercent) + " flat, plus " + pct(loss.lossPercentPerUnit) +
                        " per " + (long) loss.distanceUnitBlocks + " blocks", 0, y, W, EmiPage.TEXT,
                        List.of(Component.literal("The distance term is raised to the power " + loss.exponent)));
                y += EmiPage.ROW_H;
                EmiPage.row(widgets, "At most " + pct(loss.maxLossPercent) + "  \u00b7  " +
                        pct(loss.crossDimensionLossPercent) + " across dimensions", 0, y, W, EmiPage.DIM, null);
            }
        }
    }

    private static final class RelayPage extends BasicEmiRecipe {

        private final EmiStack stack;

        RelayPage(EmiStack stack) {
            super(TESLA, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/tesla_network/range_extender"),
                    W, 90);
            this.stack = stack;
            this.outputs = List.of(stack);
            this.catalysts = List.of(stack);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            widgets.addSlot(stack, 0, 0).drawBack(false);
            EmiPage.title(widgets, "Tesla Range Extender", 22, 2, W - 22, 0xFFFFFFFF);
            EmiPage.row(widgets, "Widens the reach of a tower's network", 22, 13, W - 22, EmiPage.DIM, null);

            int y = EmiPage.section(widgets, "While it works", 0, 24, W, ACCENT);
            EmiPage.row(widgets, "Adds " + PhoenixTeslaConfigs.get().towers.relay.rangeBlocks +
                    " blocks around itself", 0, y, W, EmiPage.GOOD, null);
            y += EmiPage.ROW_H;
            EmiPage.row(widgets, "Uses the power loss of the tower that feeds it", 0, y, W, EmiPage.TEXT,
                    List.of(Component.literal("Where several towers cover a spot,"),
                            Component.literal("the one with the lowest loss is used.")));
            y += EmiPage.ROW_H;
            EmiPage.row(widgets, "Does nothing without a tower powering it", 0, y, W, EmiPage.DIM, null);
        }
    }

    private static final class HatchPage extends BasicEmiRecipe {

        private final String title;
        private final List<Family> families;

        HatchPage(String title, String id, List<Family> families) {
            super(TESLA, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/tesla_network/" + id), W, 40);
            this.title = title;
            this.families = families;
            List<EmiStack> all = new ArrayList<>();
            for (Family f : families) {
                int lo = first(f.machines()), hi = last(f.machines());
                all.add(EmiStack.of(f.machines()[lo].asStack()));
                if (hi != lo) all.add(EmiStack.of(f.machines()[hi].asStack()));
            }
            this.outputs = all;
            this.height = 24 + families.size() * 20 + 4;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, title, 0, 1, W, 0xFFFFFFFF);
            int y = EmiPage.section(widgets, "From the lowest tier to the highest", 0, 13, W, ACCENT);
            for (Family f : families) {
                int lo = first(f.machines()), hi = last(f.machines());
                ItemStack lowest = f.machines()[lo].asStack();
                widgets.addSlot(EmiStack.of(lowest), 0, y).recipeContext(this);
                if (hi != lo) widgets.addSlot(EmiStack.of(f.machines()[hi].asStack()), 20, y).recipeContext(this);
                EmiPage.row(widgets, f.name(), 44, y + 5, 86, EmiPage.TEXT, null);
                EmiPage.row(widgets, lo == hi ? tier(lo) : tier(lo) + " to " + tier(hi), 134, y + 5, W - 134,
                        EmiPage.DIM,
                        List.of(Component.literal(f.name()), Component.literal(GTValues.VN[lo] + " - " +
                                GTValues.VN[hi])));
                y += 20;
            }
        }
    }
}
