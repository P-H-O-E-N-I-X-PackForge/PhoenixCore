package net.phoenix.core.integration.conflux.research;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record GatedRecipeRule(String flag, List<ResourceLocation> recipeIds, List<TypeTierGate> recipeTypes,
                              List<ResourceLocation> outputItems, List<TagKey<Item>> outputTags,
                              List<String> onlyDisciplines, List<String> exceptDisciplines, boolean crafting) {

    public static final Codec<GatedRecipeRule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("flag", "").forGetter(GatedRecipeRule::flag),
            ResourceLocation.CODEC.listOf().optionalFieldOf("recipe_ids", List.of())
                    .forGetter(GatedRecipeRule::recipeIds),
            TypeTierGate.CODEC.listOf().optionalFieldOf("recipe_types", List.of())
                    .forGetter(GatedRecipeRule::recipeTypes),
            ResourceLocation.CODEC.listOf().optionalFieldOf("output_items", List.of())
                    .forGetter(GatedRecipeRule::outputItems),
            TagKey.codec(Registries.ITEM).listOf().optionalFieldOf("output_tags", List.of())
                    .forGetter(GatedRecipeRule::outputTags),
            Codec.STRING.listOf().optionalFieldOf("only_disciplines", List.of())
                    .forGetter(GatedRecipeRule::onlyDisciplines),
            Codec.STRING.listOf().optionalFieldOf("except_disciplines", List.of())
                    .forGetter(GatedRecipeRule::exceptDisciplines),
            Codec.BOOL.optionalFieldOf("crafting", false).forGetter(GatedRecipeRule::crafting))
            .apply(i, GatedRecipeRule::new));

    public boolean inScope(@org.jetbrains.annotations.Nullable String discipline) {
        boolean listedOnly = onlyDisciplines.isEmpty() || (discipline != null && onlyDisciplines.contains(discipline));
        boolean listedExcept = discipline != null && exceptDisciplines.contains(discipline);
        return listedOnly && !listedExcept;
    }

    public boolean blocks(@org.jetbrains.annotations.Nullable String discipline,
                          java.util.function.Predicate<String> hasFlag) {
        return inScope(discipline) && (flag.isEmpty() || !hasFlag.test(flag));
    }

    public boolean hides() {
        return flag.isEmpty() && (!onlyDisciplines.isEmpty() || !exceptDisciplines.isEmpty());
    }

    public boolean namesCraftingId(ResourceLocation id) {
        return crafting && recipeIds.contains(id);
    }

    public boolean namesCraftingRecipe(ResourceLocation id, net.minecraft.world.item.ItemStack result) {
        if (namesCraftingId(id)) return true;
        if (!crafting) return false;
        if (result.isEmpty()) return false;
        if (outputItems.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()))) {
            return true;
        }
        for (var tag : outputTags) {
            if (result.is(tag)) return true;
        }
        return false;
    }

    public record TypeTierGate(ResourceLocation recipeType, String minTier) {

        public static final Codec<TypeTierGate> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(TypeTierGate::recipeType),
                Codec.STRING.fieldOf("min_tier").forGetter(TypeTierGate::minTier))
                .apply(i, TypeTierGate::new));
    }
}
