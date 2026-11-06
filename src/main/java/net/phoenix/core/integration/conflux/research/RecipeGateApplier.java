package net.phoenix.core.integration.conflux.research;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.phoenix.core.PhoenixCore;

import java.util.Collection;

public final class RecipeGateApplier {

    private RecipeGateApplier() {}

    public static void applyGates(Collection<RecipeHolder<?>> recipes) {
        var rules = GatedRecipeRegistry.INSTANCE.getRules();
        var unlockedByNode = ResearchTreeRegistry.INSTANCE.getGatedRecipes();
        if (rules.isEmpty() && unlockedByNode.isEmpty()) return;

        int gated = 0;
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof GTRecipe recipe)) continue;
            recipe.id = holder.id();

            String nodeFlag = unlockedByNode.get(recipe.id);
            if (nodeFlag != null && !alreadyGated(recipe, nodeFlag, "", "")) {
                addCondition(recipe, ConfluxResearchCondition.of(nodeFlag, "", ""));
                gated++;
            }

            for (GatedRecipeRule rule : rules) {
                String only = String.join(",", rule.onlyDisciplines());
                String except = String.join(",", rule.exceptDisciplines());
                if (matches(recipe, rule) && !alreadyGated(recipe, rule.flag(), only, except)) {
                    addCondition(recipe, ConfluxResearchCondition.of(rule.flag(), only, except));
                    gated++;
                }
            }
        }
        if (gated > 0) {
            PhoenixCore.LOGGER.info("[Conflux] Attached {} research gate(s) to loaded recipes", gated);
        }
    }

    private static java.lang.reflect.Field conditionsField;

    private static void addCondition(GTRecipe recipe, ConfluxResearchCondition condition) {
        try {
            if (conditionsField == null) {
                conditionsField = GTRecipe.class.getField("conditions");
                conditionsField.setAccessible(true);
            }
            var copy = new java.util.ArrayList<>(recipe.conditions);
            copy.add(condition);
            conditionsField.set(recipe, copy);
        } catch (ReflectiveOperationException e) {
            PhoenixCore.LOGGER.error("[Conflux] Could not gate recipe {}", recipe.id, e);
        }
    }

    private static boolean alreadyGated(GTRecipe recipe, String flag, String only, String except) {
        for (var condition : recipe.conditions) {
            if (condition instanceof ConfluxResearchCondition conflux && flag.equals(conflux.getFlag()) &&
                    only.equals(conflux.getOnly()) && except.equals(conflux.getExcept()))
                return true;
        }
        return false;
    }

    private static boolean matches(GTRecipe recipe, GatedRecipeRule rule) {
        if (rule.recipeIds().contains(recipe.id)) return true;

        if (!rule.recipeTypes().isEmpty()) {
            long voltage = Math.max(recipe.getInputEUt().voltage(), recipe.getOutputEUt().voltage());
            byte recipeTier = GTUtil.getTierByVoltage(voltage);
            for (GatedRecipeRule.TypeTierGate gate : rule.recipeTypes()) {
                if (!gate.recipeType().equals(recipe.recipeType.id)) continue;
                if (recipeTier >= GTUtil.getTierByName(gate.minTier())) return true;
            }
        }

        if (!rule.outputItems().isEmpty() || !rule.outputTags().isEmpty()) {
            for (var content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
                for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content()).getItems()) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (rule.outputItems().contains(itemId)) return true;
                    for (var tag : rule.outputTags()) {
                        if (stack.is(tag)) return true;
                    }
                }
            }
        }

        return false;
    }
}
