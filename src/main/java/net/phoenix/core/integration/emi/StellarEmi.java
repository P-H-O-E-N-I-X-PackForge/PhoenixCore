package net.phoenix.core.integration.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.gui.hud.GuiDraw;
import net.phoenix.core.client.stellar.ui.Stellar2D;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachines;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;
import net.phoenix.core.common.machine.multiblock.stellar.StellarPlasma;
import net.phoenix.core.common.machine.multiblock.stellar.StellarTint;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The Stellar Forge's exotic plasmas and dyes in EMI. */
public final class StellarEmi {

    private StellarEmi() {}

    private static EmiStack forge() {
        return EmiStack.of(StellarForgeMachines.STELLAR_FORGE.asStack());
    }

    public static final EmiRecipeCategory PLASMA = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "stellar_plasma"), forge()) {

        @Override
        public Component getName() {
            return Component.literal("Stellar Forge: exotic plasma");
        }
    };

    public static final EmiRecipeCategory DYE = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "stellar_dye"), forge()) {

        @Override
        public Component getName() {
            return Component.literal("Stellar Forge: dye");
        }
    };

    private static EmiStack fluid(ResourceLocation id, long amount) {
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        return EmiStack.of(fluid == null ? Fluids.EMPTY : fluid, amount);
    }

    public static void register(EmiRegistry registry) {
        registry.addCategory(PLASMA);
        registry.addCategory(DYE);
        registry.addWorkstation(PLASMA, forge());
        registry.addWorkstation(DYE, forge());
        for (StellarPlasma.Recipe recipe : StellarPlasma.forDisplay()) {
            registry.addRecipe(new PlasmaRecipe(recipe));
        }
        for (Map.Entry<ResourceLocation, StellarTint.Tint> entry : StellarTint.forDisplay()) {
            StellarTint.Tint tint = entry.getValue();
            if (tint.fluid() != null) registry.addRecipe(new DyeRecipe(entry.getKey(), tint));
        }
    }

    private static final int W = 170;

    /** The body drawn flat on a dark panel. */
    private static final class Picture extends Widget {

        private final Bounds bounds;
        private final StellarForgeState.Stage stage;
        private final int rgb;
        private final int holeRgb;

        Picture(int x, int y, int w, int h, StellarForgeState.Stage stage, int rgb, int holeRgb) {
            this.bounds = new Bounds(x, y, w, h);
            this.stage = stage;
            this.rgb = rgb;
            this.holeRgb = holeRgb;
        }

        @Override
        public Bounds getBounds() {
            return bounds;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            int x = bounds.x();
            int y = bounds.y();
            g.fill(x, y, x + bounds.width(), y + bounds.height(), 0xFF000004);
            g.renderOutline(x, y, bounds.width(), bounds.height(), 0xFF30344A);
            float r = Math.min(bounds.width(), bounds.height()) * 0.42f;
            GuiDraw.enableScissor(g, x + 1, y + 1, x + bounds.width() - 1, y + bounds.height() - 1);
            Stellar2D.draw(g, stage, rgb, x + bounds.width() / 2.0f, y + bounds.height() / 2.0f, r, Stellar2D.time(),
                    0.3f, holeRgb, 0.6f);
            g.disableScissor();
            Font font = Minecraft.getInstance().font;
            String label = stage.label;
            g.drawString(font, label, x + (bounds.width() - font.width(label)) / 2, y + bounds.height() - 11,
                    0xFFB0B8D0, false);
        }
    }

    /** Lines of text that wrap inside their width rather than running off the edge. */
    private static final class Lines extends Widget {

        private final Bounds bounds;
        private final List<Component> lines;
        private final List<Integer> colors;

        Lines(int x, int y, int w, int h, List<Component> lines, List<Integer> colors) {
            this.bounds = new Bounds(x, y, w, h);
            this.lines = lines;
            this.colors = colors;
        }

        @Override
        public Bounds getBounds() {
            return bounds;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            Font font = Minecraft.getInstance().font;
            int y = bounds.y();
            for (int i = 0; i < lines.size(); i++) {
                for (FormattedCharSequence part : font.split(lines.get(i), bounds.width())) {
                    g.drawString(font, part, bounds.x(), y, colors.get(i), false);
                    y += 10;
                }
                y += 2;
            }
        }
    }

    private static final class PlasmaRecipe extends BasicEmiRecipe {

        private final StellarPlasma.Recipe recipe;

        PlasmaRecipe(StellarPlasma.Recipe recipe) {
            super(PLASMA, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/stellar_plasma/" + recipe.id().getNamespace() + "/" + recipe.id().getPath()), W, 96);
            this.recipe = recipe;
            this.outputs = List.of(fluid(recipe.fluid(), 1000));
            this.catalysts = List.of(forge());
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            boolean hole = recipe.body() == StellarForgeState.Stage.BLACK_HOLE;
            float mid = (recipe.heatMin() + recipe.heatMax()) / 2.0f;
            widgets.add(new Picture(0, 0, 60, 96, recipe.body(), StellarForgeState.colorOf(mid) & 0xFFFFFF, -1));
            widgets.addSlot(outputs.get(0), 66, 0).recipeContext(this);
            widgets.add(new Lines(88, 1, W - 88, 20, List.of(Component.literal(recipe.name())), List.of(0xFFFFFFFF)));
            widgets.add(new Lines(66, 24, W - 66, 72, List.of(
                    Component.literal(hole ? "From a black hole" : String.format(Locale.ROOT,
                            "From a star, heat %.0f to %.0f", recipe.heatMin(), recipe.heatMax())),
                    Component.literal(String.format(Locale.ROOT, "Needs %,.0f M, draws %.1f mB/t", recipe.minMass(),
                            recipe.rate())),
                    Component.literal(String.format(Locale.ROOT, "Costs %.2f stability and %.2f M every tick",
                            recipe.damageStability(), recipe.damageMass())),
                    Component.literal("Pick it with the PLASMA button")),
                    List.of(0xFFC8CCDC, 0xFFC8CCDC, 0xFFFF6A6A, 0xFF808898)));
        }
    }

    private static final class DyeRecipe extends BasicEmiRecipe {

        private final StellarTint.Tint tint;

        DyeRecipe(ResourceLocation id, StellarTint.Tint tint) {
            super(DYE, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/stellar_dye/" + id.getNamespace() + "/" + id.getPath()), W, 70);
            this.tint = tint;
            this.inputs = List.of(fluid(tint.fluid(), tint.amount()));
            this.catalysts = List.of(forge());
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            boolean clears = tint.color() < 0;
            StellarForgeState.Stage shown = tint.star() ? StellarForgeState.Stage.STAR :
                    StellarForgeState.Stage.BLACK_HOLE;
            int rgb = clears ? 0xFFD060 : tint.color() & 0xFFFFFF;
            widgets.add(new Picture(0, 0, 60, 70, shown, rgb, clears ? -1 : rgb));
            widgets.addSlot(inputs.get(0), 66, 0).recipeContext(this);
            String target = tint.star() && tint.blackHole() ? "a star or a black hole's disk" :
                    tint.blackHole() ? "a black hole's disk" : "a star";
            widgets.add(new Lines(88, 1, W - 88, 20, List.of(Component.literal(clears ? "Clears the dye" : "Dye")),
                    List.of(0xFFFFFFFF)));
            widgets.add(new Lines(66, 24, W - 66, 46, List.of(
                    Component.literal((clears ? "Puts the colour of " : "Colours ") + target +
                            (clears ? " back to the heat's own" : String.format(Locale.ROOT, " #%06x", tint.color() & 0xFFFFFF))),
                    Component.literal("Put it in the input hatch, it is used up")),
                    List.of(clears ? 0xFFC8CCDC : 0xFF000000 | tint.color(), 0xFF808898)));
        }
    }
}
