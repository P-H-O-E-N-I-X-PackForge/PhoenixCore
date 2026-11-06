package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;

import java.util.ArrayList;
import java.util.List;

public final class PftCrafting {

    private PftCrafting() {}

    private static boolean itemExists(ResourceLocation id) {
        return BuiltInRegistries.ITEM.containsKey(id);
    }

    private static ResourceLocation resolve(String raw) {
        ResourceLocation id = ResourceLocation.parse(raw);
        if (itemExists(id)) return id;
        if (id.getNamespace().equals("gtceu") || id.getNamespace().equals("kubejs")) {
            ResourceLocation moved = ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, id.getPath());
            if (itemExists(moved)) return moved;
        }
        return null;
    }

    private static String withoutCount(String spec) {
        String s = spec.trim();
        int x = s.indexOf("x ");
        if (x > 0 && s.substring(0, x).matches("[0-9]+")) return s.substring(x + 2).trim();
        return s;
    }

    private static int count(String spec) {
        String s = spec.trim();
        int x = s.indexOf("x ");
        return x > 0 && s.substring(0, x).matches("[0-9]+") ? Integer.parseInt(s.substring(0, x)) : 1;
    }

    private static Object ingredient(String spec, List<String> missing) {
        String s = withoutCount(spec);
        if (s.startsWith("#")) {
            return TagKey.create(Registries.ITEM, ResourceLocation.parse(s.substring(1)));
        }
        ResourceLocation id = resolve(s);
        if (id == null) {
            missing.add(s);
            return null;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    private static ItemStack result(String spec, List<String> missing) {
        String s = spec.trim();
        int count = 1;
        int x = s.indexOf("x ");
        if (x > 0 && s.substring(0, x).matches("\\d+")) {
            count = Integer.parseInt(s.substring(0, x));
            s = s.substring(x + 2).trim();
        }
        ResourceLocation id = resolve(s);
        if (id == null) {
            missing.add(s);
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item, count);
    }

    public static void shaped(RecipeOutput provider, String id, String output, String[] rows, String... keyAndSpec) {
        List<String> missing = new ArrayList<>();
        ItemStack result = result(output, missing);
        List<Object> params = new ArrayList<>(List.of((Object[]) rows));
        for (int i = 0; i + 1 < keyAndSpec.length; i += 2) {
            Object ingredient = ingredient(keyAndSpec[i + 1], missing);
            params.add(keyAndSpec[i].charAt(0));
            params.add(ingredient);
        }
        if (!missing.isEmpty()) {
            PhoenixCore.LOGGER.error("[PFT] skipped crafting recipe {}: missing {}", id, missing);
            PftRecipe.report(id + " (crafting) needs " + String.join(", ", missing));
            return;
        }
        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id(id), result, params.toArray());
    }

    public static void shapeless(RecipeOutput provider, String id, String output, String... ingredients) {
        List<String> missing = new ArrayList<>();
        ItemStack result = result(output, missing);
        List<Object> params = new ArrayList<>();
        for (String spec : ingredients) {
            Object ingredient = ingredient(spec, missing);
            if (ingredient != null) {
                for (int i = 0; i < count(spec); i++) params.add(ingredient);
            }
        }
        if (!missing.isEmpty()) {
            PhoenixCore.LOGGER.error("[PFT] skipped crafting recipe {}: missing {}", id, missing);
            PftRecipe.report(id + " (crafting) needs " + String.join(", ", missing));
            return;
        }
        VanillaRecipeHelper.addShapelessRecipe(provider, id, result, params.toArray());
    }
}
