package net.phoenix.core.integration.conflux.research;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * A rule loaded from {@code data/<namespace>/conflux/gated_recipes/*.json} that attaches an
 * {@link AxiomResearchCondition} for {@code flag} onto every already-loaded {@code GTRecipe} it
 * matches - including recipes we don't author (vanilla GTCEu machines, other mods' GT integrations),
 * not just custom recipes that have the condition hand-attached in their own datagen.
 *
 * <pre>
 * {
 *   "flag": "thermal_recursion",
 *   "recipe_ids": ["gtceu:macerator/gregtech_gold_ore"],
 *   "recipe_types": [{"type": "gtceu:macerator", "min_tier": "mv"}],
 *   "output_items": ["gtceu:mv_circuit"],
 *   "output_tags": ["c:circuits/mv"]
 * }
 * </pre>
 *
 * A recipe matches if it matches ANY of the four criteria groups (each group is optional and defaults
 * to empty/no match). A recipe matched by more than one rule gets a condition for each rule's flag.
 */
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

    /**
     * Gates every recipe of {@code recipeType} (a GTRecipeType's registry name, e.g. "gtceu:macerator")
     * at or above {@code minTier} (a GT voltage tier name: "ulv", "lv", "mv", "hv", ...).
     */
    public record TypeTierGate(ResourceLocation recipeType, String minTier) {

        public static final Codec<TypeTierGate> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("type").forGetter(TypeTierGate::recipeType),
                Codec.STRING.fieldOf("min_tier").forGetter(TypeTierGate::minTier))
                .apply(i, TypeTierGate::new));
    }
}
