package net.phoenix.core.common.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.phoenix.core.PhoenixCore;

public final class PftCooking {

    private PftCooking() {}

    private static ResourceLocation resolve(String raw) {
        ResourceLocation id = ResourceLocation.parse(raw);
        if (BuiltInRegistries.ITEM.containsKey(id)) return id;
        if (id.getNamespace().equals("gtceu") || id.getNamespace().equals("kubejs")) {
            ResourceLocation moved = ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, id.getPath());
            if (BuiltInRegistries.ITEM.containsKey(moved)) return moved;
        }
        return null;
    }

    private interface Factory {

        Recipe<?> make(Ingredient input, ItemStack result);
    }

    private static void save(RecipeOutput out, String id, String output, String input, Factory factory) {
        String o = output.trim();
        int count = 1;
        int x = o.indexOf("x ");
        if (x > 0 && o.substring(0, x).matches("\\d+")) {
            count = Integer.parseInt(o.substring(0, x));
            o = o.substring(x + 2).trim();
        }
        ResourceLocation resultId = resolve(o);
        Ingredient ingredient;
        String in = input.trim().replaceFirst("^[0-9]+x +", "");
        if (in.startsWith("#")) {
            ingredient = Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(in.substring(1))));
        } else {
            ResourceLocation inputId = resolve(in);
            ingredient = inputId == null ? null : Ingredient.of(BuiltInRegistries.ITEM.get(inputId));
        }
        if (resultId == null || ingredient == null) {
            String what = resultId == null ? o : in;
            PhoenixCore.LOGGER.error("[PFT] skipped cooking recipe {}: missing {}", id, what);
            PftRecipe.report(id + " (cooking) needs " + what);
            return;
        }
        Item result = BuiltInRegistries.ITEM.get(resultId);
        out.accept(PhoenixCore.id(id), factory.make(ingredient, new ItemStack(result, count)), null);
    }

    public static void smelting(RecipeOutput out, String id, String output, String input) {
        save(out, id, output, input, (in, r) -> new SmeltingRecipe("", CookingBookCategory.MISC, in, r, 0.1f, 200));
    }

    public static void blasting(RecipeOutput out, String id, String output, String input) {
        save(out, id, output, input, (in, r) -> new BlastingRecipe("", CookingBookCategory.MISC, in, r, 0.1f, 100));
    }

    public static void smoking(RecipeOutput out, String id, String output, String input) {
        save(out, id, output, input, (in, r) -> new SmokingRecipe("", CookingBookCategory.MISC, in, r, 0.1f, 100));
    }

    public static void campfireCooking(RecipeOutput out, String id, String output, String input) {
        save(out, id, output, input,
                (in, r) -> new CampfireCookingRecipe("", CookingBookCategory.MISC, in, r, 0.1f, 600));
    }

    public static void stonecutting(RecipeOutput out, String id, String output, String input) {
        save(out, id, output, input, (in, r) -> new StonecutterRecipe("", in, r));
    }
}
