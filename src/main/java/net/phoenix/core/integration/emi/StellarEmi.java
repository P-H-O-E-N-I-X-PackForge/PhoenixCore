package net.phoenix.core.integration.emi;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeMachines;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;
import net.phoenix.core.common.machine.multiblock.stellar.StellarPlasma;
import net.phoenix.core.common.machine.multiblock.stellar.StellarTint;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
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

    private static final class PlasmaRecipe extends BasicEmiRecipe {

        private final StellarPlasma.Recipe recipe;

        PlasmaRecipe(StellarPlasma.Recipe recipe) {
            super(PLASMA, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/stellar_plasma/" + recipe.id().getNamespace() + "/" + recipe.id().getPath()), 150, 66);
            this.recipe = recipe;
            this.outputs = List.of(fluid(recipe.fluid(), 1000));
            this.catalysts = List.of(forge());
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            boolean hole = recipe.body() == StellarForgeState.Stage.BLACK_HOLE;
            widgets.addSlot(outputs.get(0), 4, 4).large(false).recipeContext(this);
            widgets.addText(Component.literal(recipe.name()), 26, 4, 0xFFFFFF, true);
            widgets.addText(Component.literal(hole ? "from a black hole" : String.format(Locale.ROOT,
                    "from a star, heat %.0f to %.0f", recipe.heatMin(), recipe.heatMax())), 26, 15, 0xAAAAAA, false);
            widgets.addText(Component.literal(String.format(Locale.ROOT, "needs %,.0f M, draws %.1f mB/t",
                    recipe.minMass(), recipe.rate())), 4, 28, 0xAAAAAA, false);
            widgets.addText(Component.literal(String.format(Locale.ROOT, "costs %.2f stability and %.2f M a tick",
                    recipe.damageStability(), recipe.damageMass())).withStyle(ChatFormatting.RED), 4, 40, 0xFF5555,
                    false);
            widgets.addText(Component.literal("pick it with the PLASMA button"), 4, 52, 0x808080, false);
        }
    }

    private static final class DyeRecipe extends BasicEmiRecipe {

        private final StellarTint.Tint tint;

        DyeRecipe(ResourceLocation id, StellarTint.Tint tint) {
            super(DYE, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/stellar_dye/" + id.getNamespace() + "/" + id.getPath()), 150, 36);
            this.tint = tint;
            this.inputs = List.of(fluid(tint.fluid(), tint.amount()));
            this.catalysts = List.of(forge());
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(inputs.get(0), 4, 4).recipeContext(this);
            String target = tint.star() && tint.blackHole() ? "a star or black hole disk"
                    : tint.blackHole() ? "a black hole disk" : "a star";
            widgets.addText(Component.literal(tint.color() < 0 ? "clears the dye on " + target :
                    "dyes " + target), 26, 6, 0xFFFFFF, true);
            if (tint.color() >= 0) {
                widgets.addText(Component.literal(String.format(Locale.ROOT, "#%06x", tint.color() & 0xFFFFFF)), 26,
                        18, 0xFF000000 | tint.color(), false);
            }
            widgets.addText(Component.literal("put in the input hatch, used up"), 4, 26, 0x808080, false);
        }
    }
}
