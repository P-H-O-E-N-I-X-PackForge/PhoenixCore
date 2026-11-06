package net.phoenix.core.integration.emi;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.growth.GrowthBlocks;
import net.phoenix.core.integration.growth.GrowthItems;
import net.phoenix.core.integration.growth.GrowthMachines;
import net.phoenix.core.integration.growth.GrowthStage;
import net.phoenix.core.integration.growth.machine.BloomvineCoreMachine;
import net.phoenix.core.integration.growth.machine.CrystalGardenMachine;
import net.phoenix.core.integration.growth.machine.VerdantBloomMachine;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class GrowthEmi {

    private GrowthEmi() {}

    private static final int ACCENT = 0xFF5CE08A;
    private static final int BONUS = 0xFF7AFFB0;

    public static final EmiRecipeCategory GROWTH = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "growth_machines"),
            EmiStack.of(GrowthBlocks.GROWTH_SHELL.get()));

    static void register(EmiRegistry registry) {
        registry.addCategory(GROWTH);
        registry.addWorkstation(GROWTH, EmiStack.of(GrowthBlocks.GROWTH_SHELL.get()));
        registry.addRecipe(new GrowerPage("crystal_garden", "Crystal Garden", GrowthMachines.CRYSTAL_GARDEN.asStack(),
                0xFF7AE0FF, "Grows outward, one stage at a time.", GrowthItems.CRYSTAL_SEED::get,
                CrystalGardenMachine::stages, false, "At full size it runs two harvests at once."));
        registry.addRecipe(new GrowerPage("verdant_bloom", "Verdant Bloom", GrowthMachines.VERDANT_BLOOM.asStack(),
                0xFF5CE08A, "Grows outward, and gets a little faster and richer with each stage.",
                GrowthItems.GROWTH_SEED::get, VerdantBloomMachine::stages, false, "Its tendrils help it along."));
        registry.addRecipe(new GrowerPage("bloomvine_core", "Bloomvine Core", GrowthMachines.BLOOMVINE_CORE.asStack(),
                0xFFFF7AB8, "Stays the same size and matures instead; its tendrils do the growing.",
                GrowthItems.GROWTH_SEED::get, BloomvineCoreMachine::stages, true,
                "A double batch once fully mature."));
        registry.addRecipe(new ControlPage());
    }

    private static final int W = 300;

    private static int cells(GrowthStage stage, int axis) {
        var b = stage.bounds();
        return switch (axis) {
            case 0 -> b.getInt(2) + b.getInt(3) + 1;
            case 1 -> b.getInt(0) + b.getInt(1) + 1;
            default -> b.getInt(4) + b.getInt(5) + 1;
        };
    }

    private static final class GrowerPage extends BasicEmiRecipe {

        private final String name;
        private final String role;
        private final String extra;
        private final EmiStack controller;
        private final int accent;
        private final List<GrowthStage> stages;

        private final boolean matures;
        private final int maxW, maxH, maxD;

        GrowerPage(String id, String name, ItemStack controller, int accent, String role,
                   Supplier<? extends net.minecraft.world.item.Item> seed, Supplier<List<GrowthStage>> stages,
                   boolean matures, String extra) {
            super(GROWTH, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/growth_machine/" + id), W, 118);
            this.name = name;
            this.role = role;
            this.extra = extra;
            this.controller = EmiStack.of(controller);
            this.accent = 0xFF000000 | accent;
            this.stages = stages.get();
            this.matures = matures;
            int w = 1, h = 1, d = 1;
            for (GrowthStage st : this.stages) {
                w = Math.max(w, cells(st, 0));
                h = Math.max(h, cells(st, 1));
                d = Math.max(d, cells(st, 2));
            }
            this.maxW = w;
            this.maxH = h;
            this.maxD = d;
            this.inputs = List.of(EmiStack.of(seed.get()));
            this.outputs = List.of(this.controller, EmiStack.of(GrowthBlocks.GROWTH_SHELL.get()));
            this.catalysts = List.of(this.controller);
        }

        private int shownStage() {
            long step = System.currentTimeMillis() / 900L;
            return (int) (step % (stages.size() + 1)) >= stages.size() ? stages.size() - 1 :
                    (int) (step % (stages.size() + 1));
        }

        private void shape(net.minecraft.client.gui.GuiGraphics g, int size, boolean top) {
            GrowthStage st = stages.get(shownStage());
            int mw = maxW, mv = top ? maxD : maxH;
            float cell = Math.max(2f, Math.min(size / (float) mw, size / (float) mv));
            int w = cells(st, 0), v = top ? cells(st, 2) : cells(st, 1);
            var b = st.bounds();
            int ox = Math.round((size - mw * cell) / 2f), oy = Math.round((size - mv * cell) / 2f);

            g.renderOutline(ox, oy, Math.round(mw * cell), Math.round(mv * cell), EmiPage.withAlpha(accent, 0x40));

            int cx = (mw - w) / 2, cy = (mv - v) / 2;
            int x0 = ox + Math.round(cx * cell), y0 = oy + Math.round(cy * cell);
            g.fill(x0, y0, x0 + Math.round(w * cell), y0 + Math.round(v * cell),
                    EmiPage.withAlpha(accent, matures ? 0x30 + 0x18 * shownStage() : 0x60));
            g.renderOutline(x0, y0, Math.round(w * cell), Math.round(v * cell), accent);
            int mx = x0 + Math.round((b.getInt(2) + 0.5f) * cell);
            int my = y0 + Math.round(((top ? b.getInt(4) : b.getInt(0)) + 0.5f) * cell);
            g.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFFFFFFFF);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, accent);
            widgets.addSlot(controller, 0, 0).drawBack(false);
            EmiPage.title(widgets, name, 22, 2, W - 22, 0xFFFFFFFF);
            EmiPage.row(widgets, role, 22, 12, W - 22, EmiPage.DIM, null);

            EmiPage.section(widgets, matures ? "The shell" : "How it grows", 0, 24, 104, accent);
            widgets.addDrawable(0, 38, 48, 48, (g, mx, my, delta) -> {
                g.fill(0, 0, 48, 48, 0xFF0A0D14);
                shape(g, 48, true);
            });
            widgets.addDrawable(54, 38, 48, 48, (g, mx, my, delta) -> {
                g.fill(0, 0, 48, 48, 0xFF0A0D14);
                shape(g, 48, false);
            });
            EmiPage.row(widgets, "from above", 0, 88, 52, EmiPage.DIM, null);
            EmiPage.row(widgets, "from the side", 54, 88, 52, EmiPage.DIM, null);
            widgets.addDrawable(0, 99, 104, 10, (g, mx, my, delta) -> {
                GrowthStage st = stages.get(shownStage());
                var font = net.minecraft.client.Minecraft.getInstance().font;
                String text = (matures ? "Maturity " : "Stage ") + shownStage() + "  " + cells(st, 0) + "x" +
                        cells(st, 1) + "x" + cells(st, 2);
                g.drawString(font, text, 0, 0, EmiPage.WARM, false);
            });
            widgets.addTooltipText(List.of(Component.literal("Width x height x depth, in blocks"),
                    Component.literal("The white dot is the controller")), 0, 99, 104, 10);

            int x0 = 112, w = W - x0;
            EmiPage.section(widgets, matures ? "Maturity" : "Stages", x0, 24, w, accent);
            int chip = Math.min(32, w / stages.size());
            for (int i = 0; i < stages.size(); i++) {
                GrowthStage st = stages.get(i);
                int cx = x0 + i * chip;
                EmiPage.row(widgets, String.valueOf(i), cx + 6, 37, chip - 4, EmiPage.DIM, null);
                if (st.cost().isEmpty()) {
                    EmiPage.row(widgets, i == 0 ? "start" : "free", cx, 50, chip, EmiPage.DIM, null);
                } else {
                    widgets.addSlot(EmiStack.of(st.cost().get(0)), cx - 1, 47).drawBack(true);
                }
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal((matures ? "Maturity " : "Stage ") + i));
                tip.add(Component.literal(cells(st, 0) + "x" + cells(st, 1) + "x" + cells(st, 2) + " blocks"));
                if (st.speedBonus() > 0f) tip.add(Component.literal(Math.round(st.speedBonus() * 100) + "% faster"));
                if (st.outputBonus() > 0f) {
                    tip.add(Component.literal("+" + Math.round(st.outputBonus() * 100) + "% output"));
                }
                if (st.batch() > 1) tip.add(Component.literal("x" + st.batch() + " batch"));
                widgets.addTooltipText(tip, cx, 36, chip, 32);
            }

            GrowthStage full = stages.get(stages.size() - 1);
            int y = EmiPage.section(widgets, "At full growth", x0, 72, w, accent);
            List<String> bonus = new ArrayList<>();
            if (full.speedBonus() > 0f) bonus.add(Math.round(full.speedBonus() * 100) + "% faster");
            if (full.outputBonus() > 0f) bonus.add("+" + Math.round(full.outputBonus() * 100) + "% output");
            if (full.batch() > 1) bonus.add("x" + full.batch() + " batch");
            EmiPage.row(widgets, bonus.isEmpty() ? "no bonus" : String.join("   ", bonus), x0, y, w,
                    bonus.isEmpty() ? EmiPage.DIM : BONUS, null);
            EmiPage.row(widgets, extra, x0, y + EmiPage.ROW_H, w, EmiPage.TEXT, null);
        }
    }

    private static class ControlPage extends BasicEmiRecipe {

        private static final int W = 240;

        private final List<EmiPage.Line> lines = List.of(
                EmiPage.Line.of("Without one, growth is wild: it never stops, and it breaks any block in its way.",
                        0xFFFF9A9A),
                EmiPage.Line
                        .of("With one: pause it, cap the stage it grows to, switch its tendrils off or limit them " +
                                "to chosen sides. Growth then waits for room instead of breaking.", EmiPage.TEXT),
                EmiPage.Line.of("A stage needs its whole space clear. A tendril may stop part way, and every tendril " +
                        "block still standing makes the machine faster and its harvests richer.", EmiPage.DIM),
                EmiPage.Line.of("A growth ward keeps tendrils and shells out of the blocks around it.", EmiPage.DIM));

        ControlPage() {
            super(GROWTH, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "/growth_machine/control"), W, 40);
            this.inputs = List.of(EmiStack.of(GrowthMachines.GROWTH_CONTROL_HATCH.asStack()));
            this.outputs = List.of(EmiStack.of(GrowthBlocks.GROWTH_TENDRIL.get()));
            this.height = 36 + EmiPage.measure(lines, W) + 4;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, W, height, ACCENT);
            EmiPage.title(widgets, "Growth control and tendrils", 0, 1, W, 0xFFFFFFFF);
            widgets.addSlot(inputs.get(0), 0, 13).drawBack(true);
            EmiPage.row(widgets, "Fit a growth control hatch to take control of a growing machine.", 22, 18, W - 22,
                    EmiPage.TEXT, null);
            EmiPage.draw(widgets, lines, 0, 36, W);
        }
    }
}
