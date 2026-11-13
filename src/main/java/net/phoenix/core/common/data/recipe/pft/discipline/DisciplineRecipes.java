package net.phoenix.core.common.data.recipe.pft.discipline;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.PftRecipe;
import net.phoenix.core.common.data.recipeConditions.DisciplineCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DisciplineRecipes {

    private DisciplineRecipes() {}

    public static final String CRUCIBLE = "aether_crucible";

    public static PftRecipe recipe(RecipeOutput provider, String recipeType, Discipline discipline, String name) {
        return PftRecipe.gt(provider, recipeType, recipeType + "/" + discipline.id() + "/" + name)
                .condition(new DisciplineCondition(discipline.id()));
    }

    public static PftRecipe crucible(RecipeOutput provider, Discipline discipline, String name) {
        return PftRecipe.gt(provider, CRUCIBLE, "crucible/" + discipline.id() + "/" + name)
                .condition(new DisciplineCondition(discipline.id()));
    }

    public static PftRecipe crucibleAny(RecipeOutput provider, String name) {
        return PftRecipe.gt(provider, CRUCIBLE, "crucible/" + name);
    }

    public static PftRecipe commonOres(RecipeOutput provider, Discipline discipline, int perWeight) {
        PftRecipe recipe = crucible(provider, discipline, "common_ores");
        recipe.itemOutputs(oreOutputs(discipline, perWeight).toArray());
        return recipe;
    }

    public static List<String> oreOutputs(Discipline discipline, int perWeight) {
        List<String> out = new ArrayList<>();
        for (Discipline.Ore ore : discipline.commonOres()) {
            String item = rawItem(ore.name());
            if (item == null) {
                PhoenixCore.LOGGER.warn("[PFT] no raw ore item for '{}' (discipline {}), left out", ore.name(),
                        discipline.id());
                continue;
            }
            out.add(ore.weight() * perWeight + "x " + item);
        }
        return out;
    }

    private static final Map<String, String> MINERAL = Map.ofEntries(
            Map.entry("tin", "gtceu:raw_cassiterite"),
            Map.entry("zinc", "gtceu:raw_sphalerite"),
            Map.entry("lead", "gtceu:raw_galena"),
            Map.entry("nickel", "gtceu:raw_pentlandite"),
            Map.entry("bauxite", "gtceu:raw_bauxite"),
            Map.entry("iron", "minecraft:raw_iron"),
            Map.entry("copper", "minecraft:raw_copper"),
            Map.entry("gold", "minecraft:raw_gold"),
            Map.entry("dormant_ember", "phoenixcore:raw_dormant_ember"),
            Map.entry("permafrost", "phoenixcore:raw_permafrost"));

    public static String rawItem(String name) {
        String id = MINERAL.getOrDefault(name, "gtceu:raw_" + name);
        ResourceLocation rl = ResourceLocation.parse(id);
        if (BuiltInRegistries.ITEM.containsKey(rl)) return id;
        ResourceLocation moved = ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, rl.getPath());
        return BuiltInRegistries.ITEM.containsKey(moved) ? PhoenixCore.MOD_ID + ":" + rl.getPath() : null;
    }

    public static final long DEFAULT_EUT = GTValues.VA[GTValues.EV] / 2;
    public static final int DEFAULT_DURATION = 6000;
}
