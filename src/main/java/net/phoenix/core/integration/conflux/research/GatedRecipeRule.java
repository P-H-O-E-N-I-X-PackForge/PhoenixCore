package net.phoenix.core.integration.conflux.research;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record GatedRecipeRule(String flag, List<ResourceLocation> recipeIds, List<TypeTierGate> recipeTypes,
                              List<ResourceLocation> outputItems, List<TagKey<Item>> outputTags) {

    public static final Codec<GatedRecipeRule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("flag").forGetter(GatedRecipeRule::flag),
            ResourceLocation.CODEC.listOf().optionalFieldOf("recipe_ids", List.of())
                    .forGetter(GatedRecipeRule::recipeIds),
            TypeTierGate.CODEC.listOf().optionalFieldOf("recipe_types", List.of())
                    .forGetter(GatedRecipeRule::recipeTypes),
            ResourceLocation.CODEC.listOf().optionalFieldOf("output_items", List.of())
                    .forGetter(GatedRecipeRule::outputItems),
            TagKey.codec(Registries.ITEM).listOf().optionalFieldOf("output_tags", List.of())
                    .forGetter(GatedRecipeRule::outputTags))
            .apply(i, GatedRecipeRule::new));

    public record TypeTierGate(ResourceLocation recipeType, String minTier) {

        public static final Codec<TypeTierGate> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(TypeTierGate::recipeType),
                Codec.STRING.fieldOf("min_tier").forGetter(TypeTierGate::minTier))
                .apply(i, TypeTierGate::new));
    }
}
