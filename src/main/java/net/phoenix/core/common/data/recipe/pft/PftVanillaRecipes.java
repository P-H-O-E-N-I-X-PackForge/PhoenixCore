package net.phoenix.core.common.data.recipe.pft;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.PftRecipe;

import java.util.Map;

public final class PftVanillaRecipes {

    private PftVanillaRecipes() {}

    public static void init(RecipeOutput provider) {
        PftRecipe.gt(provider, "chemical_reactor", "gtceu:ghast_tear")
                .itemInputs("#c:tiny_dusts/potassium", "#c:tiny_dusts/lithium")
                .inputFluids("gtceu:salt_water 1000")
                .itemOutputs("minecraft:ghast_tear")
                .duration(400)
                .EUt(30)
                .save();

        PftRecipe.gt(provider, "assembler", "gtceu:skeleton_skull")
                .itemInputs("minecraft:bone")
                .itemOutputs("skeleton_skull")
                .duration(100)
                .EUt(4)
                .save();

        PftRecipe.gt(provider, "extractor", "gtceu:bones")
                .itemInputs("#minecraft:dirt")
                .itemOutputs("bone")
                .duration(100)
                .EUt(16)
                .save();

        PftRecipe.gt(provider, "autoclave", "gtceu:prismarine")
                .itemInputs("minecraft:prismarine_crystals")
                .inputFluids("minecraft:water 100")
                .itemOutputs("prismarine_shard")
                .duration(1200)
                .EUt(24)
                .save();

        PftRecipe.gt(provider, "autoclave", "gtceu:prismarine_distilled")
                .itemInputs("minecraft:prismarine_crystals")
                .inputFluids("gtceu:distilled_water 100")
                .itemOutputs("prismarine_shard")
                .duration(600)
                .EUt(24)
                .save();

        PftRecipe.gt(provider, "macerator", "gtceu:prismarine_crushed")
                .itemInputs("minecraft:prismarine_shard")
                .itemOutputs("prismarine_crystals")
                .duration(120)
                .EUt(2)
                .save();

        PftRecipe.gt(provider, "chemical_bath", "gtceu:slimeball_bath")
                .itemInputs("#c:dusts/asbestos")
                .inputFluids("gtceu:glue 250")
                .itemOutputs("slime_ball")
                .duration(200)
                .EUt(24)
                .save();

        PftRecipe.gt(provider, "centrifuge", "gtceu:slimeballs_centrifuge")
                .itemInputs("slime_ball")
                .outputFluids("gtceu:glue 250")
                .itemOutputs("gtceu:asbestos_dust")
                .duration(200)
                .EUt(24)
                .save();

        PftRecipe.gt(provider, "compressor", "gtceu:spongebob")
                .itemInputs("2x #c:foils/polycaprolactam")
                .itemOutputs("sponge")
                .duration(200)
                .EUt(2)
                .save();

        PftRecipe.gt(provider, "large_chemical_reactor", "gtceu:bottle_o_enchanting")
                .itemInputs("glass_bottle",
                        "#c:tiny_dusts/gold",
                        "#c:tiny_dusts/sugar")
                .inputFluids("water 100")
                .itemOutputs("experience_bottle")
                .duration(20)
                .EUt(256)
                .save();

        PftRecipe.gt(provider, "chemical_reactor", "gtceu:nether_star")
                .itemInputs("#c:dusts/diamond", "#c:dusts/iridium")
                .inputFluids("gtceu:rocket_fuel 1000", "gtceu:nether_air 8000")
                .itemOutputs("2x gtceu:nether_star_dust")
                .duration(200)
                .EUt(7680)
                .save();

        PftRecipe.gt(provider, "alloy_smelter", "gtceu:better_netherite")
                .itemInputs("2x netherite_scrap", "2x #c:ingots/gold")
                .itemOutputs("1x netherite_ingot")
                .duration(150)
                .EUt(100)
                .save();

        componentRecipes(provider);
    }

    private static void componentRecipes(RecipeOutput provider) {
        var water = DataComponentIngredient.of(true, PotionContents.createItemStack(Items.POTION, Potions.WATER));
        potion(provider, "fire_resistance_potion", Potions.FIRE_RESISTANCE, water, "c:dusts/sugar", "c:dusts/stone",
                "c:dusts/lead");
        potion(provider, "long_fire_resistance_potion", Potions.LONG_FIRE_RESISTANCE, water, "c:dusts/redstone",
                "c:dusts/sugar", "c:dusts/stone", "c:dusts/lead");

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var silkTouch = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SILK_TOUCH);
        ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(silkTouch, 1));
        Map<Character, Ingredient> key = Map.of(
                'R', tag("c:plates/rubber"),
                'W', tag("minecraft:wool"),
                'B', Ingredient.of(Items.BOOK),
                'm', tag("c:tools/mallet"));
        provider.accept(PhoenixCore.id("silk_touch_book_pft"), new ShapedRecipe("", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(key, "RWR", "WBW", "RmR"), book), null);
    }

    private static Ingredient tag(String id) {
        return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(id)));
    }

    private static void potion(RecipeOutput provider, String id, Holder<Potion> potion, Ingredient water,
                               String... tags) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(water);
        for (String t : tags) ingredients.add(tag(t));
        provider.accept(PhoenixCore.id(id + "_pft"), new ShapelessRecipe("", CraftingBookCategory.MISC,
                PotionContents.createItemStack(Items.POTION, potion), ingredients), null);
    }
}
