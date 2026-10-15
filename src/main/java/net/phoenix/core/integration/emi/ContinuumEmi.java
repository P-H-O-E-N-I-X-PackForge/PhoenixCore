package net.phoenix.core.integration.emi;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.continuum.ContinuumRegistry;
import net.phoenix.core.integration.continuum.common.RocketRefit;
import net.phoenix.core.integration.continuum.common.RocketStats;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;
import java.util.Locale;

public final class ContinuumEmi {

    private ContinuumEmi() {}

    public static final EmiRecipeCategory BODIES = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "continuum_bodies"),
            EmiStack.of(ContinuumRegistry.ROCKET.get()));

    public static final EmiRecipeCategory REFIT = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "continuum_refit"),
            EmiStack.of(ContinuumRegistry.REFIT_MK2.get()));

    static void register(EmiRegistry registry) {
        registry.addCategory(BODIES);
        registry.addCategory(REFIT);

        for (ContinuumBody body : ContinuumData.allBodies()) {
            if (!body.yields().isEmpty()) registry.addRecipe(new BodyRecipe(body));
        }

        for (int tier = 2; tier <= RocketStats.MAX_TIER; tier++) registry.addRecipe(new HullRecipe(tier));
        for (String upgrade : List.of(RocketStats.OVERDRIVE, RocketStats.REINFORCED_HULL, RocketStats.HULL_INTERLOCK,
                RocketStats.STELLAR_SHIELD, RocketStats.SINGULARITY_SHIELD)) {
            registry.addRecipe(new ModuleRecipe(upgrade));
        }
    }

    private record Palette(int bg, int border, int title, int text, int dim) {}

    private static Palette palette() {
        try {
            PhoenixTheme theme = PhoenixTheme.current();
            return new Palette(theme.panel.getColor() | 0xFF000000, theme.border.getColor() | 0xFF000000,
                    theme.header.getColor() | 0xFF000000, theme.text.getColor() | 0xFF000000,
                    theme.textDim.getColor() | 0xFF000000);
        } catch (RuntimeException | LinkageError e) {
            return new Palette(0xFF0b0a18, 0xFF3a2f7a, 0xFFE8D8FF, 0xFFB8B0D8, 0xFF7a7498);
        }
    }

    private static void background(WidgetHolder widgets, int width, int height) {
        Palette p = palette();
        widgets.addDrawable(0, 0, width, height, (draw, mouseX, mouseY, delta) -> {
            draw.fill(0, 0, width, height, p.bg());
            draw.renderOutline(0, 0, width, height, p.border());
        });
    }

    // ---- text that wraps. EMI panels are narrow, so every line is fitted to the panel and the panel grows to hold it.

    private static final int PANEL_WIDTH = 176;
    private static final int TEXT_WIDTH = PANEL_WIDTH - 12;
    private static final int LINE = 10;

    private enum Tone { TITLE, DIM, TEXT, WARN }

    private record Line(Component text, Tone tone, boolean shadow) {}

    private static Line title(String text) {
        return new Line(Component.literal(text), Tone.TITLE, true);
    }

    private static Line dim(String text) {
        return new Line(Component.literal(text), Tone.DIM, false);
    }

    private static Line text(String text) {
        return new Line(Component.literal(text), Tone.TEXT, false);
    }

    private static Line warn(String text) {
        return new Line(Component.literal(text), Tone.WARN, false);
    }

    private static List<FormattedCharSequence> wrap(Component text) {
        var font = Minecraft.getInstance().font;
        return font == null ? List.of(text.getVisualOrderText()) : font.split(text, TEXT_WIDTH);
    }

    /** How tall these lines are once wrapped. */
    private static int measure(List<Line> lines) {
        int total = 0;
        for (Line line : lines) total += wrap(line.text()).size() * LINE + 1;
        return total;
    }

    /** Draws the lines, wrapped to the panel, from y; returns the y below the last one. */
    private static int draw(WidgetHolder widgets, List<Line> lines, int y) {
        Palette p = palette();
        for (Line line : lines) {
            int color = switch (line.tone()) {
                case TITLE -> p.title();
                case DIM -> p.dim();
                case TEXT -> p.text();
                case WARN -> 0xFFFFD27A;
            };
            for (FormattedCharSequence row : wrap(line.text())) {
                widgets.addText(row, 6, y, color, line.shadow());
                y += LINE;
            }
            y += 1;
        }
        return y;
    }

    private static ItemStack rocket(int tier) {
        ItemStack stack = new ItemStack(ContinuumRegistry.ROCKET.get());
        RocketStats.setTier(stack, tier);
        return stack;
    }

    private static final int COLUMNS = 7;

    private static class BodyRecipe extends BasicEmiRecipe {

        private final ContinuumBody body;
        private final List<Line> head;

        BodyRecipe(ContinuumBody body) {
            this(body, headOf(body));
        }

        private BodyRecipe(ContinuumBody body, List<Line> head) {
            super(BODIES, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/continuum_body/" + body.id().getPath()),
                    PANEL_WIDTH, 8 + measure(head) + 6 + (((body.yields().size() + COLUMNS - 1) / COLUMNS) * 18) + 6);
            this.body = body;
            this.head = head;
            this.inputs = List.of(EmiStack.of(ContinuumRegistry.PROBE.get()));
            this.outputs = body.yields().stream().map(y -> {
                Item item = BuiltInRegistries.ITEM.get(y.item());
                return EmiStack.of(item == null ? Items.BARRIER : item, Math.max(1, y.max()))
                        .setChance(Math.max(0.01f, Math.min(1.0f, y.chance())));
            }).toList();
        }

        private static List<Line> headOf(ContinuumBody body) {
            var system = ContinuumData.system(body.system());
            String where = system != null ? system.name() : body.system().getPath();
            List<Line> lines = new java.util.ArrayList<>();
            lines.add(title(body.name()));
            lines.add(dim(body.type().label() + " in " + where + "  -  tier " + body.minTier()));
            lines.add(text(String.format(Locale.ROOT, "%d min flight   hull %s", body.tripMinutes(),
                    RocketStats.tierName(RocketStats.requiredTier(body)))));
            if (body.transientInfo() != null) {
                lines.add(warn(String.format(Locale.ROOT, "A passing comet: here for about %.1f h, now and then",
                        body.transientInfo().durationMinutes() / 60.0)));
            } else {
                lines.add(dim("Extraction run, per probe"));
            }
            return lines;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            background(widgets, width, height);
            Palette p = palette();
            int y = draw(widgets, head, 5) + 4;

            widgets.addSlot(inputs.get(0), 6, y).drawBack(true);
            widgets.addText(Component.literal("->"), 28, y + 5, p.dim(), false);
            for (int i = 0; i < outputs.size(); i++) {
                var yield = body.yields().get(i);
                String range = yield.min() == yield.max() ? String.valueOf(yield.min()) :
                        yield.min() + "-" + yield.max();
                widgets.addSlot(outputs.get(i), 44 + (i % COLUMNS) * 18, y + (i / COLUMNS) * 18)
                        .drawBack(true)
                        .appendTooltip(Component.literal(range + " per roll, " + Math.round(yield.chance() * 100) +
                                "% of rolls"))
                        .recipeContext(this);
            }
        }
    }

    private static class HullRecipe extends BasicEmiRecipe {

        private final int tier;
        private final List<Line> head;
        private final List<Line> tail;

        HullRecipe(int tier) {
            this(tier, List.of(title("Hull refit: " + RocketStats.tierName(tier))),
                    List.of(dim("In the refit screen: sneak + use the rocket"),
                            text("-5% trips, -10% wear, -12% failure, more module room")));
        }

        private HullRecipe(int tier, List<Line> head, List<Line> tail) {
            super(REFIT, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/continuum_refit/hull_mk" + tier), PANEL_WIDTH,
                    5 + measure(head) + 4 + 18 + 6 + measure(tail) + 6);
            this.tier = tier;
            this.head = head;
            this.tail = tail;
            Item kit = RocketRefit.refitKit(tier);
            this.inputs = List.of(EmiStack.of(rocket(tier - 1)), EmiStack.of(kit == null ? Items.BARRIER : kit));
            this.outputs = List.of(EmiStack.of(rocket(tier)));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            background(widgets, width, height);
            Palette p = palette();
            int y = draw(widgets, head, 5) + 4;
            widgets.addSlot(inputs.get(0), 6, y).drawBack(true);
            widgets.addText(Component.literal("+"), 27, y + 5, p.dim(), false);
            widgets.addSlot(inputs.get(1), 36, y).drawBack(true);
            widgets.addText(Component.literal("->"), 58, y + 5, p.dim(), false);
            widgets.addSlot(outputs.get(0), 76, y).drawBack(true).recipeContext(this);
            draw(widgets, tail, y + 18 + 6);
        }
    }

    private static class ModuleRecipe extends BasicEmiRecipe {

        private final String upgrade;
        private final List<Line> head;
        private final List<Line> tail;

        ModuleRecipe(String upgrade) {
            this(upgrade, headOf(upgrade), tailOf(upgrade));
        }

        private ModuleRecipe(String upgrade, List<Line> head, List<Line> tail) {
            super(REFIT, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/continuum_refit/module_" + upgrade), PANEL_WIDTH,
                    5 + measure(head) + 4 + 18 + 6 + measure(tail) + 6);
            this.upgrade = upgrade;
            this.head = head;
            this.tail = tail;
            Item module = RocketRefit.module(upgrade);
            ItemStack fitted = rocket(RocketStats.MAX_TIER);
            RocketStats.setLevel(fitted, upgrade, 1);
            this.inputs = List.of(EmiStack.of(rocket(RocketStats.MAX_TIER)),
                    EmiStack.of(module == null ? Items.BARRIER : module));
            this.outputs = List.of(EmiStack.of(fitted));
        }

        private static List<Line> headOf(String upgrade) {
            Item module = RocketRefit.module(upgrade);
            return List.of(title("Fit " + (module == null ? upgrade : module.getDescription().getString())));
        }

        /** How many of the module each hull takes: a header, then one line per hull, instead of one long line. */
        private static List<Line> tailOf(String upgrade) {
            List<Line> lines = new java.util.ArrayList<>();
            lines.add(dim("Most you can fit, by hull:"));
            for (int tier = 1; tier <= RocketStats.MAX_TIER; tier++) {
                int cap = RocketStats.upgradeCap(tier, upgrade);
                lines.add(text(RocketStats.tierName(tier) + ":  " + (cap == 0 ? "not at this hull" : cap)));
            }
            return lines;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            background(widgets, width, height);
            Palette p = palette();
            int y = draw(widgets, head, 5) + 4;
            widgets.addSlot(inputs.get(0), 6, y).drawBack(true);
            widgets.addText(Component.literal("+"), 27, y + 5, p.dim(), false);
            widgets.addSlot(inputs.get(1), 36, y).drawBack(true);
            widgets.addText(Component.literal("->"), 58, y + 5, p.dim(), false);
            widgets.addSlot(outputs.get(0), 76, y).drawBack(true).recipeContext(this);
            draw(widgets, tail, y + 18 + 6);
        }
    }
}
