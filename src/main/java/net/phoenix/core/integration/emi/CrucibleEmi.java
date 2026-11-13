package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.common.data.recipeConditions.DisciplineCondition;
import net.phoenix.core.common.machine.multiblock.electric.AetherCrucibleMachines;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;

public final class CrucibleEmi {

    private CrucibleEmi() {}

    private static final int WIDTH = 176;
    private static final int ACCENT = 0xFF58C8E8;
    private static final int COLS = 6;
    private static final int OUT_X = 64;

    public static EmiRecipeCategory CATEGORY;

    private static EmiStack crucible() {
        return EmiStack.of(AetherCrucibleMachines.AETHER_CRUCIBLE.asStack());
    }

    public static boolean replaced(dev.emi.emi.api.recipe.EmiRecipe recipe) {
        if (CATEGORY == null || recipe.getCategory() == CATEGORY) return false;
        var backing = recipe.getBackingRecipe();
        return backing != null && backing.value() instanceof GTRecipe gt &&
                gt.recipeType == PhoenixRecipeTypes.AETHER_CRUCIBLE_RECIPES.get();
    }

    public static void register(EmiRegistry registry) {
        if (AetherCrucibleMachines.AETHER_CRUCIBLE == null) return;
        CATEGORY = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "aether_crucible"),
                crucible()) {

            @Override
            public Component getName() {
                return Component.literal("Aether Crucible");
            }
        };
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, crucible());
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (RecipeHolder<GTRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(PhoenixRecipeTypes.AETHER_CRUCIBLE_RECIPES.get())) {
            registry.addRecipe(new Page(holder));
        }
    }

    private static final class Page extends BasicEmiRecipe {

        private final RecipeHolder<GTRecipe> holder;
        private final GTRecipe gt;
        private final List<EmiIngredient> ins = new ArrayList<>();
        private final List<EmiStack> outs = new ArrayList<>();
        private final String discipline;

        Page(RecipeHolder<GTRecipe> holder) {
            super(CATEGORY, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/aether_crucible/" + holder.id().getPath()), WIDTH, 40);
            this.holder = holder;
            this.gt = holder.value();
            String found = null;
            for (var condition : gt.conditions) {
                if (condition instanceof DisciplineCondition d) found = d.getDiscipline();
            }
            this.discipline = found;
            try {
                for (var c : gt.getInputContents(ItemRecipeCapability.CAP)) {
                    var sized = ItemRecipeCapability.CAP.of(c.content());
                    ins.add(EmiIngredient.of(sized.ingredient(), sized.count()));
                }
                for (var c : gt.getInputContents(FluidRecipeCapability.CAP)) {
                    for (var fluid : FluidRecipeCapability.CAP.of(c.content()).getFluids()) {
                        ins.add(EmiStack.of(fluid.getFluid(), fluid.getAmount()));
                        break;
                    }
                }
                for (var c : gt.getOutputContents(ItemRecipeCapability.CAP)) {
                    for (ItemStack stack : ItemRecipeCapability.CAP.of(c.content()).getItems()) {
                        if (!stack.isEmpty()) {
                            outs.add(EmiStack.of(stack));
                            break;
                        }
                    }
                }
                for (var c : gt.getOutputContents(FluidRecipeCapability.CAP)) {
                    for (var fluid : FluidRecipeCapability.CAP.of(c.content()).getFluids()) {
                        outs.add(EmiStack.of(fluid.getFluid(), fluid.getAmount()));
                        break;
                    }
                }
            } catch (Throwable t) {
                PhoenixCore.LOGGER.warn("[PFT] could not read Crucible recipe {} for EMI", holder.id(), t);
            }
            this.inputs = new ArrayList<>(ins);
            this.outputs = new ArrayList<>(outs);
            this.catalysts = List.of(crucible());
            int rows = Math.max(Math.max(1, (outs.size() + COLS - 1) / COLS), (ins.size() + 1) / 2);
            this.height = 14 + rows * 18 + 4 + 22;
        }

        @Override
        public RecipeHolder<?> getBackingRecipe() {
            return holder;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, WIDTH, height, ACCENT);
            EmiPage.title(widgets,
                    discipline == null ? "Aether Crucible" : "Aether Crucible: " + discipline.replace('_',
                            ' '),
                    0, 1, WIDTH, 0xFFFFFFFF);

            for (int i = 0; i < ins.size(); i++) {
                widgets.addSlot(ins.get(i), (i % 2) * 18, 14 + (i / 2) * 18);
            }
            int rows = Math.max(1, (outs.size() + COLS - 1) / COLS);
            widgets.addTexture(dev.emi.emi.api.render.EmiTexture.EMPTY_ARROW, 40, 14 + (rows * 18) / 2 - 8);
            for (int i = 0; i < outs.size(); i++) {
                widgets.addSlot(outs.get(i), OUT_X + (i % COLS) * 18, 14 + (i / COLS) * 18).recipeContext(this);
            }

            int y = height - 20;
            long eut = 0;
            try {
                var list = gt.tickInputs.get(com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability.CAP);
                if (list != null && !list.isEmpty()) {
                    var energy = com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability.CAP
                            .of(list.get(0).content());
                    eut = energy.voltage() * energy.amperage();
                }
            } catch (Throwable ignored) {}
            EmiPage.row(widgets, eut + " EU/t for " + String.format("%.1f", gt.duration / 20.0) + " s", 0, y,
                    WIDTH, EmiPage.DIM, null);
            if (discipline != null) {
                EmiPage.pill(widgets, "Only for " + discipline.replace('_', ' '), 0, y + 11, 0xFFB890FF);
            }
        }
    }
}
