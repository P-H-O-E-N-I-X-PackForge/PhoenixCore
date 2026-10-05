package net.phoenix.core.integration.conflux.research;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.phoenix.core.PhoenixCore;

import java.util.Collection;

/**
 * Attaches an {@link AxiomResearchCondition} to every already-loaded {@code GTRecipe} matching a
 * {@link GatedRecipeRule} from {@link GatedRecipeRegistry} - called once per recipe reload from
 * {@code RecipeManagerMixin}, right after {@code RecipeManager.apply()} finishes parsing recipes.
 *
 * {@code GTRecipe.conditions} is a plain mutable {@code List<RecipeCondition<?>>} that GTCEu's own
 * {@code RecipeLogic.matchRecipe} already checks for every recipe attempt, so mutating it here is
 * enough to gate a recipe - no mixin into the matching/execution path itself is needed. This is what
 * lets a rule gate GTCEu's own built-in recipes, or another mod's, not just recipes we author.
 */
public final class RecipeGateApplier {

    private RecipeGateApplier() {}

    public static void applyGates(Collection<Recipe<?>> recipes) {
        var rules = GatedRecipeRegistry.INSTANCE.getRules();
        if (rules.isEmpty()) return;

        int gated = 0;
        for (Recipe<?> r : recipes) {
            if (!(r instanceof GTRecipe recipe)) continue;

            for (GatedRecipeRule rule : rules) {
                if (matches(recipe, rule)) {
                    recipe.conditions.add(AxiomResearchCondition.of(rule.flag()));
                    gated++;
                }
            }
        }
        if (gated > 0) {
            PhoenixCore.LOGGER.info("[Conflux] Attached {} research gate(s) to loaded recipes", gated);
        }
    }

    private static boolean matches(GTRecipe recipe, GatedRecipeRule rule) {
        if (rule.recipeIds().contains(recipe.id)) return true;

        if (!rule.recipeTypes().isEmpty()) {
            long voltage = Math.max(recipe.getInputEUt().voltage(), recipe.getOutputEUt().voltage());
            byte recipeTier = GTUtil.getTierByVoltage(voltage);
            for (GatedRecipeRule.TypeTierGate gate : rule.recipeTypes()) {
                if (!gate.recipeType().equals(recipe.recipeType.registryName)) continue;
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
