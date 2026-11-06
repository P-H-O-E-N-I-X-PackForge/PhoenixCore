package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.client.ConfluxTheme;
import net.phoenix.core.integration.conflux.dimension.DisciplineChunkGenerator;
import net.phoenix.core.integration.conflux.dimension.worldgen.ProvinceVeinTemplates;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OreEmi {

    private OreEmi() {}

    private static final int W = 220;
    private static final String[] DIMENSIONS = { "phoenix", "void", "sculk", "sealed" };

    private static EmiStack icon() {
        ItemStack ore = ChemicalHelper.get(TagPrefix.ore, GTMaterials.Iron, 1);
        return ore.isEmpty() ? EmiStack.EMPTY : EmiStack.of(ore);
    }

    public static final EmiRecipeCategory ORES = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "conflux_ores"), icon()) {

        @Override
        public Component getName() {
            return Component.literal("Conflux Ores");
        }
    };

    public static void register(EmiRegistry registry) {
        registry.addCategory(ORES);
        registry.addRecipe(new OverviewPage());
        Map<String, ProvinceVeinTemplates.Template> all = ProvinceVeinTemplates.all();
        for (String dimension : DIMENSIONS) {
            for (var entry : all.entrySet()) {
                if (!entry.getKey().startsWith(dimension + "_")) continue;
                RegionPage page = RegionPage.of(dimension, entry.getKey(), entry.getValue());
                if (page != null) registry.addRecipe(page);
            }
        }
    }

    private static String pretty(String id) {
        String spaced = id.replace('_', ' ');
        return spaced.isEmpty() ? id : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static String dimensionName(String dimension) {
        return pretty(dimension);
    }

    private static int accent(String dimension) {
        return ConfluxTheme.disciplineAccent("sealed".equals(dimension) ? "sealed_a" : dimension);
    }

    private static final class RegionPage extends BasicEmiRecipe {

        private final String dimension;
        private final String region;
        private final ProvinceVeinTemplates.Template template;
        private final List<EmiStack> ores;
        private final List<ProvinceVeinTemplates.OreLayer> layers;
        private final int accent;

        private RegionPage(String dimension, String id, ProvinceVeinTemplates.Template template,
                           List<ProvinceVeinTemplates.OreLayer> layers, List<EmiStack> ores,
                           List<EmiStack> everyVariant) {
            super(ORES, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/conflux_ores/" + id), W, 40);
            this.dimension = dimension;
            this.region = pretty(id.substring(dimension.length() + 1));
            this.template = template;
            this.layers = layers;
            this.ores = ores;
            this.accent = accent(dimension);
            // looking up any of an ore's stone variants (stone, deepslate, netherrack...) finds the region
            this.outputs = new ArrayList<>(everyVariant);
            this.inputs = List.<EmiIngredient>of();
            this.height = 26 + 12 + ((layers.size() + 1) / 2) * 18 + 4 + 2 * EmiPage.LINE_H;
        }

        static RegionPage of(String dimension, String id, ProvinceVeinTemplates.Template template) {
            List<ProvinceVeinTemplates.OreLayer> layers = new ArrayList<>();
            List<EmiStack> ores = new ArrayList<>();
            List<EmiStack> every = new ArrayList<>();
            for (var layer : template.layers()) {
                Material m = layer.material();
                if (m == null) continue;
                // the ore comes in a variant for each kind of stone: the generator picks one by what it replaces
                EmiStack shown = null;
                for (TagPrefix prefix : TagPrefix.ORES.keySet()) {
                    ItemStack stack = ChemicalHelper.get(prefix, m, 1);
                    if (stack.isEmpty()) continue;
                    EmiStack e = EmiStack.of(stack);
                    if (shown == null) shown = e;
                    every.add(e);
                }
                if (shown == null) continue;
                layers.add(layer);
                ores.add(shown);
            }
            return layers.isEmpty() ? null : new RegionPage(dimension, id, template, layers, ores, every);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, accent);
            EmiPage.title(widgets, region, 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, dimensionName(dimension) + "  -  Y " + template.minY() + " to " + template.maxY() +
                    "  -  clusters " + template.minRadius() + " to " + template.maxRadius() + " blocks wide", 0, 12, W,
                    EmiPage.DIM, null);

            int y = EmiPage.section(widgets, "Ores in a cluster", 0, 25, W, accent);
            int colW = W / 2;
            int total = Math.max(1, template.totalWeight());
            for (int i = 0; i < layers.size(); i++) {
                int x = (i % 2) * colW;
                int row = y + (i / 2) * 18;
                widgets.addSlot(ores.get(i), x, row).drawBack(false).recipeContext(this);
                var layer = layers.get(i);
                EmiPage.row(widgets, layer.material().getLocalizedName().getString(), x + 20, row + 1, colW - 24,
                        EmiPage.TEXT, null);
                double share = layer.weight() / (double) total;
                EmiPage.bar(widgets, x + 20, row + 11, colW - 28, 3, Math.min(1.0, share * 2.5), accent);
            }
            int footerY = y + ((layers.size() + 1) / 2) * 18 + 3;
            EmiPage.wrap(widgets,
                    "A great lode of one ore, ringed by smaller ones. The heart of a cluster is one huge" +
                            " body of a single ore, and which ore it is varies.",
                    0, footerY, W, EmiPage.DIM);
        }
    }

    private static final class OverviewPage extends BasicEmiRecipe {

        private final List<EmiPage.Line> lines = List.of(
                EmiPage.Line
                        .of("Ore is not scattered through the stone. It sits in clusters: one huge lode of a single " +
                                "ore at the heart, with smaller lodes of the others ringed around it.", EmiPage.TEXT),
                EmiPage.Line.of("A lode is a solid lump, packed at the heart and ragged at the rim, with plain stone " +
                        "between one lode and the next. A cluster is a lot of ore, enough to keep coming back to.",
                        EmiPage.TEXT),
                EmiPage.Line.of("Dig into the heart for the bulk of it, then work round the ring for the other ores.",
                        EmiPage.GOOD),
                EmiPage.Line.of("A cluster holds up to " + (DisciplineChunkGenerator.MAX_LODES - 1) +
                        " lodes around its heart. Clusters of " +
                        "the same kind stay at least " + (int) DisciplineChunkGenerator.SAME_KIND_GAP +
                        " blocks apart, so the clusters around you are different kinds.", EmiPage.DIM),
                EmiPage.Line.of("On the surface above a cluster, crystal roses show which ores are below.",
                        EmiPage.DIM));

        OverviewPage() {
            super(ORES, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/conflux_ores/overview"), W, 40);
            ItemStack rose = ChemicalHelper.get(TagPrefix.ore, GTMaterials.Iron, 1);
            this.outputs = rose.isEmpty() ? List.of() : List.of(EmiStack.of(rose));
            this.height = 26 + EmiPage.measure(lines, W) + 4;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            int accent = 0xFFB0B8C8;
            EmiPage.frame(widgets, W, height, accent);
            EmiPage.title(widgets, "How ore is laid out", 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, "In every Conflux dimension", 0, 12, W, EmiPage.DIM, null);
            EmiPage.draw(widgets, lines, 0, 26, W);
        }
    }

    @SuppressWarnings("unused")
    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
