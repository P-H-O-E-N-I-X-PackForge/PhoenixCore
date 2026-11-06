package net.phoenix.core.integration.conflux.client;

import net.minecraft.world.item.ItemStack;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiReloadManager;

import java.util.ArrayList;
import java.util.List;

public final class ConfluxEmiRefresh {

    private ConfluxEmiRefresh() {}

    public static void reloadRecipes() {
        try {
            EmiReloadManager.reloadRecipes();
        } catch (Throwable ignored) {}
    }

    private static List<ItemStack> outputsOf(EmiRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        for (EmiStack out : recipe.getOutputs()) {
            ItemStack stack = out.getItemStack();
            if (!stack.isEmpty()) outputs.add(stack);
        }
        return outputs;
    }

    private static boolean craftingRecipe(EmiRecipe recipe) {
        var backing = recipe.getBackingRecipe();
        return backing != null && !(backing.value() instanceof com.gregtechceu.gtceu.api.recipe.GTRecipe);
    }

    public static void decorate(EmiRecipe recipe, dev.emi.emi.api.widget.WidgetHolder widgets) {
        List<net.minecraft.network.chat.Component> tip = ConfluxRecipeVisibility.describe(recipe.getId(),
                outputsOf(recipe), craftingRecipe(recipe));
        if (tip.isEmpty()) return;
        int x = recipe.getDisplayWidth() - 9;
        widgets.addDrawable(x, 0, 9, 9, (g, mx, my, delta) -> {
            g.fill(x, 0, x + 9, 9, 0xCC10131C);
            g.renderOutline(x, 0, 9, 9, 0xFF8C6CE0);
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, "\u25c6", x + 1, 1, 0xFFC8A8FF, false);
        });
        widgets.addTooltipText(tip, x, 0, 9, 9);
    }

    public static boolean hide(EmiRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        for (EmiStack out : recipe.getOutputs()) {
            ItemStack stack = out.getItemStack();
            if (!stack.isEmpty()) outputs.add(stack);
        }
        var backing = recipe.getBackingRecipe();
        boolean crafting = backing != null && !(backing.value() instanceof com.gregtechceu.gtceu.api.recipe.GTRecipe);
        return ConfluxRecipeVisibility.hidden(recipe.getId(), outputs, crafting);
    }
}
