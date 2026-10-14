package net.phoenix.core.integration.emi;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
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
            new ResourceLocation(PhoenixCore.MOD_ID, "continuum_bodies"),
            EmiStack.of(ContinuumRegistry.ROCKET.get()));

    public static final EmiRecipeCategory REFIT = new EmiRecipeCategory(
            new ResourceLocation(PhoenixCore.MOD_ID, "continuum_refit"),
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

    private static ItemStack rocket(int tier) {
        ItemStack stack = new ItemStack(ContinuumRegistry.ROCKET.get());
        RocketStats.setTier(stack, tier);
        return stack;
    }

    private static final int COLUMNS = 7;

    private static class BodyRecipe extends BasicEmiRecipe {

        private final ContinuumBody body;

        BodyRecipe(ContinuumBody body) {
            super(BODIES, new ResourceLocation(PhoenixCore.MOD_ID, "/continuum_body/" + body.id().getPath()), 168,
                    62 + ((body.yields().size() + COLUMNS - 1) / COLUMNS) * 18);
            this.body = body;
            this.inputs = List.of(EmiStack.of(ContinuumRegistry.PROBE.get()));
            this.outputs = body.yields().stream().map(y -> {
                Item item = ForgeRegistries.ITEMS.getValue(y.item());
                return EmiStack.of(item == null ? Items.BARRIER : item, Math.max(1, y.max()))
                        .setChance(Math.max(0.01f, Math.min(1.0f, y.chance())));
            }).toList();
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            background(widgets, width, height);
            Palette p = palette();

            var system = ContinuumData.system(body.system());
            String where = system != null ? system.name() : body.system().getPath();
            widgets.addText(Component.literal(body.name()), 6, 5, p.title(), true);
            widgets.addText(Component.literal(body.type().label() + " in " + where + "  -  tier " +
                    body.minTier()), 6, 17, p.dim(), false);
            widgets.addText(Component.literal(String.format(Locale.ROOT, "%d min flight   hull %s", body.tripMinutes(),
                    RocketStats.tierName(RocketStats.requiredTier(body)))), 6, 28, p.text(), false);
            if (body.transientInfo() != null) {
                var t = body.transientInfo();
                widgets.addText(Component.literal(String.format(Locale.ROOT,
                        "A passing comet: here for about %.1f h, now and then", t.durationMinutes() / 60.0)), 6, 39,
                        0xFFFFD27A, false);
            } else {
                widgets.addText(Component.literal("Extraction run, per probe"), 6, 39, p.dim(), false);
            }

            widgets.addSlot(inputs.get(0), 6, 50).drawBack(true);
            widgets.addText(Component.literal("->"), 28, 55, p.dim(), false);
            for (int i = 0; i < outputs.size(); i++) {
                var yield = body.yields().get(i);
                String range = yield.min() == yield.max() ? String.valueOf(yield.min()) :
                        yield.min() + "-" + yield.max();
                widgets.addSlot(outputs.get(i), 44 + (i % COLUMNS) * 18, 50 + (i / COLUMNS) * 18)
                        .drawBack(true)
                        .appendTooltip(Component.literal(range + " per roll, " + Math.round(yield.chance() * 100) +
                                "% of rolls"))
                        .recipeContext(this);
            }
        }
    }

    private static class HullRecipe extends BasicEmiRecipe {

        private final int tier;

        HullRecipe(int tier) {
            super(REFIT, new ResourceLocation(PhoenixCore.MOD_ID, "/continuum_refit/hull_mk" + tier), 168, 62);
            this.tier = tier;
            Item kit = RocketRefit.refitKit(tier);
            this.inputs = List.of(EmiStack.of(rocket(tier - 1)), EmiStack.of(kit == null ? Items.BARRIER : kit));
            this.outputs = List.of(EmiStack.of(rocket(tier)));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            background(widgets, width, height);
            Palette p = palette();
            widgets.addText(Component.literal("Hull refit: " + RocketStats.tierName(tier)), 6, 5, p.title(), true);
            widgets.addSlot(inputs.get(0), 6, 20).drawBack(true);
            widgets.addText(Component.literal("+"), 27, 25, p.dim(), false);
            widgets.addSlot(inputs.get(1), 36, 20).drawBack(true);
            widgets.addText(Component.literal("->"), 58, 25, p.dim(), false);
            widgets.addSlot(outputs.get(0), 76, 20).drawBack(true).recipeContext(this);
            widgets.addText(Component.literal("In the refit screen: sneak + use the rocket"), 6, 42, p.dim(), false);
            widgets.addText(Component.literal("-5% trips, -10% wear, -12% failure, more module room"), 6, 52,
                    p.text(), false);
        }
    }

    private static class ModuleRecipe extends BasicEmiRecipe {

        private final String upgrade;

        ModuleRecipe(String upgrade) {
            super(REFIT, new ResourceLocation(PhoenixCore.MOD_ID, "/continuum_refit/module_" + upgrade), 168, 62);
            this.upgrade = upgrade;
            Item module = RocketRefit.module(upgrade);
            ItemStack fitted = rocket(RocketStats.MAX_TIER);
            RocketStats.setLevel(fitted, upgrade, 1);
            this.inputs = List.of(EmiStack.of(rocket(RocketStats.MAX_TIER)),
                    EmiStack.of(module == null ? Items.BARRIER : module));
            this.outputs = List.of(EmiStack.of(fitted));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            background(widgets, width, height);
            Palette p = palette();
            Item module = RocketRefit.module(upgrade);
            widgets.addText(Component.literal("Fit " + (module == null ? upgrade : module.getDescription().getString())),
                    6, 5, p.title(), true);
            widgets.addSlot(inputs.get(0), 6, 20).drawBack(true);
            widgets.addText(Component.literal("+"), 27, 25, p.dim(), false);
            widgets.addSlot(inputs.get(1), 36, 20).drawBack(true);
            widgets.addText(Component.literal("->"), 58, 25, p.dim(), false);
            widgets.addSlot(outputs.get(0), 76, 20).drawBack(true).recipeContext(this);

            StringBuilder caps = new StringBuilder("Most by hull:  ");
            for (int tier = 1; tier <= RocketStats.MAX_TIER; tier++) {
                int cap = RocketStats.upgradeCap(tier, upgrade);
                caps.append(RocketStats.tierName(tier)).append(' ').append(cap == 0 ? "-" : cap);
                if (tier < RocketStats.MAX_TIER) caps.append("   ");
            }
            widgets.addText(Component.literal(caps.toString()), 6, 46, p.text(), false);
        }
    }
}
