package net.phoenix.core.integration.emi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.item.PhoenixItems;
import net.phoenix.core.common.machine.PhoenixMachines;
import net.phoenix.core.integration.ars_nouveau.common.data.item.SoulLensItem;
import net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source.BioAethericEngineMachine;
import net.phoenix.core.integration.ars_nouveau.common.data.worldgen.soul.SoulBalance;
import net.phoenix.core.saveddata.SoulSavedData;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SoulEmi {

    private SoulEmi() {}

    private static final int ACCENT = 0xFFC48CFF;
    private static final int W = 220;

    private static EmiStack lens() {
        return EmiStack.of(new ItemStack(PhoenixItems.SOUL_LENS.get()));
    }

    public static final EmiRecipeCategory SOUL = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "chunk_soul"), lens()) {

        @Override
        public Component getName() {
            return Component.literal("Chunk Soul");
        }
    };

    private static EmiStack machine(net.minecraft.world.item.ItemStack stack) {
        return stack.isEmpty() ? EmiStack.EMPTY : EmiStack.of(stack);
    }

    public static void register(EmiRegistry registry) {
        registry.addCategory(SOUL);
        registry.addWorkstation(SOUL, lens());
        registry.addRecipe(new ChunkPage());
        registry.addRecipe(new ChangePage());
        registry.addRecipe(new OriginPage());
        registry.addRecipe(new FloraPage());
        registry.addRecipe(new ResonancePage());
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String pretty(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return path.contains("/") ? path.substring(path.lastIndexOf('/') + 1).replace('_', ' ') :
                path.replace('_', ' ');
    }

    private abstract static class TextPage extends BasicEmiRecipe {

        private final String title;
        private final String subtitle;
        private final List<EmiPage.Line> lines;

        TextPage(String id, String title, String subtitle, List<EmiPage.Line> lines, int extra) {
            super(SOUL, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/chunk_soul/" + id), W, 40);
            this.title = title;
            this.subtitle = subtitle;
            this.lines = lines;
            this.catalysts = List.of(lens());
            this.outputs = List.of(lens());
            this.height = 26 + EmiPage.measure(lines, W) + 4 + extra;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, title, 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, subtitle, 0, 12, W, EmiPage.DIM, null);
            int y = EmiPage.draw(widgets, lines, 0, 26, W) + 4;
            more(widgets, y);
        }

        void more(WidgetHolder widgets, int y) {}
    }

    private static final class ChunkPage extends TextPage {

        ChunkPage() {
            super("chunk", "A chunk's soul", "How soul is kept", List.of(
                    EmiPage.Line.of("Every chunk (16 x 16 blocks, at every height) holds soul of its own: an amount, " +
                            "and the most it can hold.", EmiPage.TEXT),
                    EmiPage.Line
                            .of("A chunk is set up the first time a machine or a Soul Lens looks at it. It starts " +
                                    "full, at the biome's base times the dimension's multiplier.", EmiPage.TEXT),
                    EmiPage.Line.of("A machine uses the soul of the chunk it stands in. A neighbouring chunk's soul " +
                            "does not help, though flora is counted around it.", EmiPage.GOOD),
                    EmiPage.Line.of("Running a machine does not use soul up: it is a multiplier, not a fuel.",
                            EmiPage.DIM)),
                    0);
        }
    }

    private static final class ChangePage extends TextPage {

        ChangePage() {
            super("change", "Changing a chunk's soul", "What raises it, and how it recovers", List.of(
                    EmiPage.Line.of("It regrows " + fmt(SoulSavedData.REGEN_PER_MINUTE) + " a minute while it is " +
                            "below the chunk's capacity.", EmiPage.TEXT),
                    EmiPage.Line
                            .of("When a machine finishes a recipe with soul_growth_perm, the chunk's capacity and " +
                                    "amount both rise by that much, for good.", EmiPage.GOOD),
                    EmiPage.Line.of("soul_growth_temp adds to the amount only, up to the capacity.", EmiPage.TEXT)),
                    26);
        }

        @Override
        void more(WidgetHolder widgets, int y) {
            widgets.addSlot(lens(), 0, y);
            EmiPage.row(widgets, "The Soul Lens reads your chunk and maps " + SoulLensItem.VISION_RADIUS +
                    " chunks round you.", 22, y + 1, W - 22, EmiPage.TEXT, null);
            EmiPage.row(widgets,
                    "Vibrant from " + Math.round(SoulLensItem.VIBRANT_AT * 100) + "% of capacity, " + "Stable from " +
                            Math.round(SoulLensItem.STABLE_AT * 100) + "%",
                    22, y + 11, W - 22,
                    EmiPage.DIM, null);
        }
    }

    private static final class OriginPage extends BasicEmiRecipe {

        private final Map<ResourceLocation, Float> dimensions = SoulBalance.dimensionBases();

        OriginPage() {
            super(SOUL, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/chunk_soul/origin"), W, 40);
            this.catalysts = List.of(lens());
            this.outputs = List.of(lens());
            int rows = Math.max(SoulSavedData.BIOME_RULES.size() + 1, dimensions.size() + 1);
            this.height = 26 + 12 + rows * EmiPage.ROW_H + 8;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, "Where a chunk's soul starts", 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, "The biome's base, times the dimension's multiplier", 0, 12, W, EmiPage.DIM, null);

            int col = 108;
            int y = EmiPage.section(widgets, "Biome base", 0, 26, col, ACCENT);
            int ry = y;
            for (SoulSavedData.BiomeRule rule : SoulSavedData.BIOME_RULES) {
                EmiPage.row(widgets, String.join(", ", rule.words()), 0, ry, col - 30, EmiPage.TEXT,
                        List.of(Component.literal("Biomes with these words in their name")));
                EmiPage.row(widgets, fmt(rule.base()), col - 28, ry, 28, rule.base() >= 1.0f ? EmiPage.GOOD :
                        EmiPage.BAD, null);
                ry += EmiPage.ROW_H;
            }
            EmiPage.row(widgets, "anything else", 0, ry, col - 30, EmiPage.DIM, null);
            EmiPage.row(widgets, fmt(SoulSavedData.DEFAULT_BIOME_BASE), col - 28, ry, 28, EmiPage.TEXT, null);

            int x = col + 12, w = W - x;
            EmiPage.section(widgets, "Dimension", x, 26, w, ACCENT);
            int dy = y;
            for (var entry : dimensions.entrySet()) {
                EmiPage.row(widgets, pretty(entry.getKey().toString()), x, dy, w - 34, EmiPage.TEXT,
                        List.of(Component.literal(entry.getKey().toString())));
                EmiPage.row(widgets, "x" + fmt(entry.getValue()), x + w - 32, dy, 32,
                        entry.getValue() >= 1.0f ? EmiPage.GOOD : EmiPage.BAD, null);
                dy += EmiPage.ROW_H;
            }
            EmiPage.row(widgets, "any other", x, dy, w - 34, EmiPage.DIM, null);
            EmiPage.row(widgets, "x1.00", x + w - 32, dy, 32, EmiPage.TEXT, null);
        }
    }

    private static final class FloraPage extends BasicEmiRecipe {

        private final List<Map.Entry<String, Float>> flora = new ArrayList<>(
                BioAethericEngineMachine.FLORA_BOOSTS.entrySet());
        private final List<EmiStack> stacks = new ArrayList<>();

        FloraPage() {
            super(SOUL, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/chunk_soul/flora"), W, 40);
            flora.sort(Map.Entry.<String, Float>comparingByValue().reversed());
            for (var e : flora) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getKey()));
                stacks.add(item == Items.AIR ? EmiStack.EMPTY : EmiStack.of(item));
            }
            this.inputs = stacks.stream().filter(s -> !s.isEmpty()).map(s -> (dev.emi.emi.api.stack.EmiIngredient) s)
                    .toList();
            this.catalysts = List.of(machine(PhoenixMachines.BIO_AETHERIC_ENGINE.asStack()));
            int rows = (flora.size() + 1 + 1) / 2;
            this.height = 26 + 22 + 12 + rows * 20 + 4;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, "Flora bonus", 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, "Flora near a bio-aetheric engine add to its resonance", 0, 12, W, EmiPage.DIM, null);
            EmiPage.row(widgets, "Every block counts: " + BioAethericEngineMachine.SCAN_CHUNKS + " chunks each way,",
                    0, 24, W, EmiPage.TEXT, null);
            EmiPage.row(widgets, BioAethericEngineMachine.SCAN_BLOCKS + " blocks up and down, at most +" +
                    fmt(BioAethericEngineMachine.FLORA_CAP), 0, 35, W, EmiPage.TEXT, null);

            int y = EmiPage.section(widgets, "Each block adds", 0, 48, W, ACCENT);
            int colW = W / 2;
            int i = 0;
            for (; i < flora.size(); i++) {
                var e = flora.get(i);
                int x = (i % 2) * colW, row = y + (i / 2) * 20;
                if (!stacks.get(i).isEmpty()) widgets.addSlot(stacks.get(i), x, row).recipeContext(this);
                EmiPage.row(widgets, pretty(e.getKey()), x + 20, row + 1, colW - 22, EmiPage.TEXT,
                        List.of(Component.literal(e.getKey())));
                EmiPage.row(widgets, "+" + fmt(e.getValue()), x + 20, row + 10, colW - 22, EmiPage.GOOD, null);
            }
            int x = (i % 2) * colW, row = y + (i / 2) * 20;
            EmiPage.row(widgets, "any other flower", x + 20, row + 1, colW - 22, EmiPage.DIM, null);
            EmiPage.row(widgets, "+" + fmt(BioAethericEngineMachine.FLOWER_BOOST), x + 20, row + 10, colW - 22,
                    EmiPage.GOOD, null);
        }
    }

    private static final class ResonancePage extends BasicEmiRecipe {

        private final List<EmiPage.Line> lines = List.of(
                EmiPage.Line.of("Recipe time is divided by 1 + " + BioAethericEngineMachine.DURATION_PER_RESONANCE +
                        " x resonance, and by another " + BioAethericEngineMachine.NIGHT_SPEEDUP + " at night",
                        EmiPage.TEXT),
                EmiPage.Line.of("Energy it makes is multiplied by the resonance", EmiPage.GOOD),
                EmiPage.Line
                        .of("Source it makes is multiplied by 1 + " + BioAethericEngineMachine.SOURCE_PER_RESONANCE +
                                " x resonance", EmiPage.TEXT));

        ResonancePage() {
            super(SOUL, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/chunk_soul/resonance"), W, 40);
            this.catalysts = List.of(machine(PhoenixMachines.BIO_AETHERIC_ENGINE.asStack()),
                    machine(PhoenixMachines.SOURCE_REACTOR.asStack()),
                    machine(PhoenixMachines.ALCHEMICAL_IMBUER.asStack()));
            this.outputs = List.of(machine(PhoenixMachines.BIO_AETHERIC_ENGINE.asStack()));
            this.height = 26 + EmiPage.measure(lines, W) + 4 + EmiPage.ROW_H + 1 + 20;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, "Resonance", 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, "resonance = chunk soul + flora bonus", 0, 12, W, EmiPage.WARM, null);
            int y = EmiPage.draw(widgets, lines, 0, 26, W) + 4;
            y = EmiPage.section(widgets, "Machines that read the soul", 0, y, W, ACCENT);
            int x = 0;
            for (dev.emi.emi.api.stack.EmiIngredient m : catalysts) {
                if (m.isEmpty()) continue;
                widgets.addSlot(m, x, y).catalyst(true);
                x += 18;
            }
        }
    }
}
