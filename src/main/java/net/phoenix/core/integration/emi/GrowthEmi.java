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
import net.phoenixvine.wiki.theme.PhoenixTheme;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;
import java.util.function.Supplier;

public final class GrowthEmi {

    private GrowthEmi() {}

    public static final EmiRecipeCategory GROWTH = new EmiRecipeCategory(
            new ResourceLocation(PhoenixCore.MOD_ID, "growth_machines"),
            EmiStack.of(GrowthBlocks.GROWTH_SHELL.get()));

    static void register(EmiRegistry registry) {
        registry.addCategory(GROWTH);
        registry.addRecipe(new MachinePage("crystal_garden", "Crystal Garden", GrowthItems.CRYSTAL_SEED::get,
                CrystalGardenMachine::stages,
                "Grows its shell outward with every stage. At full size it runs two harvests at once."));
        registry.addRecipe(new MachinePage("verdant_bloom", "Verdant Bloom", GrowthItems.GROWTH_SEED::get,
                VerdantBloomMachine::stages,
                "Grows its shell outward, getting a little faster and richer with every stage."));
        registry.addRecipe(new MachinePage("bloomvine_core", "Bloomvine Core", GrowthItems.GROWTH_SEED::get,
                BloomvineCoreMachine::stages,
                "Stays small and matures instead; its tendrils do the growing. A double batch at full maturity."));
        registry.addRecipe(new ControlPage());
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

    private static class MachinePage extends BasicEmiRecipe {

        private final String name;
        private final String blurb;
        private final Supplier<List<GrowthStage>> stages;

        MachinePage(String id, String name, Supplier<? extends net.minecraft.world.item.Item> seed,
                    Supplier<List<GrowthStage>> stages, String blurb) {
            super(GROWTH, new ResourceLocation(PhoenixCore.MOD_ID, "/growth_machine/" + id), 300,
                    36 + stages.get().size() * 22);
            this.name = name;
            this.blurb = blurb;
            this.stages = stages;
            this.inputs = List.of(EmiStack.of(seed.get()));
            this.outputs = List.of(EmiStack.of(GrowthBlocks.GROWTH_SHELL.get()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            background(widgets, width, height);
            Palette p = palette();
            widgets.addText(Component.literal(name), 6, 5, p.title(), true);
            widgets.addText(Component.literal(blurb), 6, 17, p.dim(), false);

            List<GrowthStage> list = stages.get();
            for (int i = 0; i < list.size(); i++) {
                GrowthStage stage = list.get(i);
                int y = 32 + i * 22;
                widgets.addText(Component.literal("Stage " + i), 6, y + 5, p.text(), false);
                var b = stage.bounds();
                widgets.addText(Component.literal((b.getInt(2) + b.getInt(3) + 1) + "x" +
                        (b.getInt(0) + b.getInt(1) + 1) + "x" + (b.getInt(4) + b.getInt(5) + 1)), 46, y + 5,
                        p.dim(), false);

                int x = 92;
                for (ItemStack cost : stage.cost()) {
                    widgets.addSlot(EmiStack.of(cost), x, y).drawBack(true);
                    x += 18;
                }
                if (stage.cost().isEmpty()) {
                    widgets.addText(Component.literal(i == 0 ? "start" : "free"), 92, y + 5, p.dim(), false);
                }

                StringBuilder bonus = new StringBuilder();
                if (stage.speedBonus() > 0.0f) bonus.append(Math.round(stage.speedBonus() * 100)).append("% faster  ");
                if (stage.outputBonus() > 0.0f) {
                    bonus.append("+").append(Math.round(stage.outputBonus() * 100)).append("% output  ");
                }
                if (stage.batch() > 1) bonus.append("x").append(stage.batch()).append(" batch");
                widgets.addText(Component.literal(bonus.length() == 0 ? "no bonus" : bonus.toString().trim()), 150,
                        y + 5, bonus.length() == 0 ? p.dim() : 0xFF7AFFB0, false);
            }
        }
    }

    private static class ControlPage extends BasicEmiRecipe {

        ControlPage() {
            super(GROWTH, new ResourceLocation(PhoenixCore.MOD_ID, "/growth_machine/control"), 240, 118);
            this.inputs = List.of(EmiStack.of(GrowthMachines.GROWTH_CONTROL_HATCH.asStack()));
            this.outputs = List.of(EmiStack.of(GrowthBlocks.GROWTH_TENDRIL.get()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            background(widgets, width, height);
            Palette p = palette();
            widgets.addText(Component.literal("Growth control and tendrils"), 6, 5, p.title(), true);
            widgets.addSlot(inputs.get(0), 6, 18).drawBack(true);
            widgets.addText(Component.literal("Fit a growth control hatch to take control of a growing machine."), 30,
                    22, p.text(), false);
            String[] lines = {
                    "Without one, growth is wild: it never stops, and it breaks any block in its way.",
                    "With one: pause it, cap the stage it grows to, switch its tendrils off or",
                    "limit them to chosen sides. Growth then waits for room instead of breaking.",
                    "A stage needs its whole space clear. A tendril may stop part way, and every",
                    "tendril block still standing makes the machine faster and its harvests richer.",
                    "A growth ward keeps tendrils and shells out of the blocks around it."
            };
            for (int i = 0; i < lines.length; i++) {
                widgets.addText(Component.literal(lines[i]), 6, 44 + i * 11, i == 0 ? 0xFFFF9A9A : p.dim(), false);
            }
        }
    }
}
