package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.materials.AstralMaterials;
import net.phoenix.core.integration.astral.AstralItems;
import net.phoenix.core.integration.astral.AstralMachines;
import net.phoenix.core.integration.astral.item.AstralThreadCellItem;
import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;
import net.phoenix.core.integration.astral.puzzle.StarChart;
import net.phoenix.core.integration.astral.skein.AstralConfluenceHatchMachine;
import net.phoenix.core.integration.astral.skein.AstralThreadHatchPartMachine;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;
import java.util.Locale;

public final class AstralEmi {

    private AstralEmi() {}

    private static final int ACCENT = 0xFFB07CFF;
    private static final int W = 220;

    private static EmiStack icon() {
        return stack(AstralMachines.ASTRAL_LOOM.asStack());
    }

    private static EmiStack stack(ItemStack s) {
        return s == null || s.isEmpty() ? EmiStack.EMPTY : EmiStack.of(s);
    }

    public static final EmiRecipeCategory ASTRAL = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "astral_weaving"), icon()) {

        @Override
        public Component getName() {
            return Component.literal("Astral Weaving");
        }
    };

    public static void register(EmiRegistry registry) {
        registry.addCategory(ASTRAL);
        registry.addWorkstation(ASTRAL, stack(AstralMachines.ASTRAL_SPINNING_WHEEL.asStack()));
        registry.addWorkstation(ASTRAL, stack(AstralMachines.ASTRAL_LOOM.asStack()));
        registry.addRecipe(new ChainPage());
        registry.addRecipe(
                new MachinePage("spinning_wheel", "Astral Spinning Wheel", AstralMachines.ASTRAL_SPINNING_WHEEL
                        .asStack(), StarChart.Kind.CHAIN, "Spins filament into thread, and thread into skein"));
        registry.addRecipe(new MachinePage("loom", "Astral Loom", AstralMachines.ASTRAL_LOOM.asStack(),
                StarChart.Kind.WEB, "Weaves skein into Ensorcelled Weave"));
        registry.addRecipe(new CalibrationPage());
        registry.addRecipe(new HatchPage());
    }

    private static String num(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    private static String tier(int t) {
        return GTValues.VN[t];
    }

    private static final class ChainPage extends BasicEmiRecipe {

        private final EmiStack filament = stack(ChemicalHelper.get(TagPrefix.dust, AstralMaterials.ASTRAL_FILAMENT, 4));
        private final EmiStack skein = AstralMaterials.SKEIN == null ? EmiStack.EMPTY :
                EmiStack.of(AstralMaterials.SKEIN.getFluid(), 144);
        private final EmiStack weave = AstralMaterials.ENSORCELLED_WEAVE == null ? EmiStack.EMPTY :
                EmiStack.of(AstralMaterials.ENSORCELLED_WEAVE.getFluid(), 144);

        ChainPage() {
            super(ASTRAL, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/astral_weaving/chain"), W, 130);
            this.inputs = List.of(filament);
            this.outputs = List.of(weave);
            this.catalysts = List.of(stack(AstralMachines.ASTRAL_SPINNING_WHEEL.asStack()),
                    stack(AstralMachines.ASTRAL_LOOM.asStack()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, "From filament to weave", 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, "Three steps, each on a better machine", 0, 12, W, EmiPage.DIM, null);

            int y = EmiPage.section(widgets, "The chain", 0, 24, W, ACCENT);
            step(widgets, y, filament, "Astral thread  -  Spinning Wheel " + tier(GTValues.IV) + "+",
                    "4 filament dust -> 1,000 thread");
            step(widgets, y + 24, skein, "Skein  -  Spinning Wheel " + tier(GTValues.LuV) + "+",
                    "1,000 thread -> 144 mB skein");
            step(widgets, y + 48, weave, "Ensorcelled Weave  -  Loom " + tier(GTValues.ZPM) + "+",
                    "4,608 mB skein -> 144 mB (" + tier(GTValues.UV) + ": 576 mB)");
            EmiPage.row(widgets, "200 source with the dust makes 1,500 thread, in half the time", 0, y + 74, W,
                    EmiPage.GOOD, null);
        }

        private void step(WidgetHolder widgets, int y, EmiStack icon, String what, String detail) {
            if (!icon.isEmpty()) widgets.addSlot(icon, 0, y).recipeContext(this);
            EmiPage.row(widgets, what, 22, y + 1, W - 22, EmiPage.TEXT, null);
            EmiPage.row(widgets, detail, 22, y + 12, W - 22, EmiPage.DIM, null);
        }
    }

    private static final class MachinePage extends BasicEmiRecipe {

        private final String title;
        private final String sub;
        private final EmiStack machine;
        private final StarChart.Kind kind;

        MachinePage(String id, String title, ItemStack stack, StarChart.Kind kind, String sub) {
            super(ASTRAL, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/astral_weaving/" + id), W, 104);
            this.title = title;
            this.sub = sub;
            this.machine = stack(stack);
            this.kind = kind;
            this.outputs = List.of(machine);
            this.catalysts = List.of(machine);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            widgets.addSlot(machine, 0, 0).drawBack(false);
            EmiPage.title(widgets, title, 22, 2, W - 22, 0xFFFFFFFF);
            EmiPage.row(widgets, sub, 22, 12, W - 22, EmiPage.DIM, null);

            int y = EmiPage.section(widgets, "Calibration", 0, 24, W, ACCENT);
            boolean chain = kind == StarChart.Kind.CHAIN;
            EmiPage.row(widgets, "Wants a " + (chain ? "chain" : "web") + " of stars", 0, y, W, EmiPage.TEXT,
                    List.of(Component.literal(chain ? "One unbroken line through the stars" :
                            "Stars joined into a web, loops allowed")));
            y += EmiPage.ROW_H;
            EmiPage.row(widgets, "Up to " + StarChart.MAX_LINES + " lines on a " + StarChart.GRID + " x " +
                    StarChart.GRID + " sky", 0, y, W, EmiPage.DIM, null);
            y += EmiPage.ROW_H + 3;

            y = EmiPage.section(widgets, "Needs", 0, y, W, ACCENT);
            EmiPage.row(widgets, "Maintenance hatch (exactly one)", 0, y, W, EmiPage.TEXT, null);
            y += EmiPage.ROW_H;
            EmiPage.row(widgets, "Astral thread input hatch, " + tier(GTValues.IV) + "-" + tier(GTValues.ZPM), 0, y, W,
                    EmiPage.TEXT, null);
            y += EmiPage.ROW_H;
            EmiPage.row(widgets, "Item, fluid and energy hatches as usual", 0, y, W, EmiPage.DIM, null);
            y += EmiPage.ROW_H + 3;
            EmiPage.row(widgets, "Aligned with the moon: x" + AstralCalibratedMachine.ALIGNED_DURATION + " duration",
                    0, y, W, EmiPage.GOOD, null);
        }
    }

    private static final class CalibrationPage extends BasicEmiRecipe {

        private final List<EmiPage.Line> lines = List.of(
                EmiPage.Line.of("Both machines are calibrated before they run, by drawing a chart of stars.",
                        EmiPage.TEXT),
                EmiPage.Line.of("Survey the sky with the Astral Wand to learn the stars, then use a ritual " +
                        "pedestal to reveal lines of the chart.", EmiPage.TEXT),
                EmiPage.Line.of("A chart that lines up with the moon's phase runs x" +
                        AstralCalibratedMachine.ALIGNED_DURATION + " as long.", EmiPage.GOOD),
                EmiPage.Line.of("A wrong line costs a short cooldown before you may try again.", EmiPage.BAD));

        CalibrationPage() {
            super(ASTRAL, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/astral_weaving/calibration"), W,
                    40);
            this.catalysts = List.of(stack(new ItemStack(AstralItems.ASTRAL_WAND.get())));
            this.outputs = List.of(stack(new ItemStack(AstralItems.ASTRAL_SURVEY.get())));
            this.height = 26 + EmiPage.measure(lines, W) + 4 + 40;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, "Calibrating a loom", 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, "Draw the chart, then it runs faster", 0, 12, W, EmiPage.DIM, null);
            int y = EmiPage.draw(widgets, lines, 0, 26, W) + 4;
            widgets.addSlot(catalysts.get(0), 0, y);
            EmiPage.row(widgets, "Astral Wand: survey the sky", 22, y + 1, W - 22, EmiPage.TEXT, null);
            widgets.addSlot(outputs.get(0), 0, y + 20).recipeContext(this);
            EmiPage.row(widgets, "Survey: what the wand records", 22, y + 21, W - 22, EmiPage.TEXT, null);
        }
    }

    private static final class HatchPage extends BasicEmiRecipe {

        HatchPage() {
            super(ASTRAL, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/astral_weaving/hatches"), W, 40);
            var in = firstHatch();
            this.catalysts = List.of(in);
            this.outputs = List.of(in);
            int rows = GTValues.ZPM - GTValues.IV + 1;
            this.height = 26 + 12 + rows * EmiPage.ROW_H * 2 + 28;
        }

        private static EmiStack firstHatch() {
            for (var e : AstralMachines.ASTRAL_THREAD_HATCH_INPUT) {
                if (e != null) return stack(e.asStack());
            }
            return EmiStack.EMPTY;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, "Thread hatches", 0, 1, W, 0xFFFFFFFF);
            EmiPage.row(widgets, "How thread gets into and out of the machines", 0, 12, W, EmiPage.DIM, null);

            int y = EmiPage.section(widgets, "Thread hatch", 0, 24, W, ACCENT);
            EmiPage.row(widgets, "Tier", 0, y, 40, EmiPage.DIM, null);
            EmiPage.row(widgets, "Holds", 60, y, 70, EmiPage.DIM, null);
            EmiPage.row(widgets, "Moves a tick", 140, y, W - 140, EmiPage.DIM, null);
            y += EmiPage.ROW_H;
            for (int t = GTValues.IV; t <= GTValues.ZPM; t++) {
                EmiPage.row(widgets, tier(t), 0, y, 56, EmiPage.TEXT, null);
                EmiPage.row(widgets, num(AstralThreadHatchPartMachine.getMaxCapacity(t)), 60, y, 76, EmiPage.TEXT,
                        null);
                EmiPage.row(widgets, num(AstralThreadHatchPartMachine.getMaxConsumption(t)), 140, y, W - 140,
                        EmiPage.GOOD, null);
                y += EmiPage.ROW_H;
            }
            y += 3;
            y = EmiPage.section(widgets, "Confluence hatch (" + tier(GTValues.IV) + ")", 0, y, W, ACCENT);
            int t = GTValues.IV;
            EmiPage.row(widgets, "Thread " + num(AstralConfluenceHatchMachine.getThreadCapacity(t)) + ", " +
                    num(AstralConfluenceHatchMachine.getThreadTransferRate(t)) + " a tick", 0, y, W, EmiPage.TEXT,
                    null);
            y += EmiPage.ROW_H;
            EmiPage.row(widgets, "Source " + num(AstralConfluenceHatchMachine.getSourceCapacity(t)) + ", " +
                    num(AstralConfluenceHatchMachine.getSourceTransferRate(t)) + " a tick", 0, y, W, EmiPage.TEXT,
                    null);
            y += EmiPage.ROW_H + 3;
            EmiPage.row(widgets, "Thread Cell carries " + num(AstralThreadCellItem.CAPACITY) + " by hand", 0, y, W,
                    EmiPage.DIM, null);
        }
    }
}
