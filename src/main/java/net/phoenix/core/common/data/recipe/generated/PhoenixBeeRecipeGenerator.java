package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.common.data.bees.BeeRecipeData;
import net.phoenix.core.common.data.materials.PhoenixBeeMaterials;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;
import net.phoenix.core.common.data.recipe.records.ApisProgenitorConfig;
import net.phoenix.core.common.data.recipe.records.FullBeeConfig;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static net.phoenix.core.common.data.PhoenixRecipeTypes.*;
import static net.phoenix.core.common.data.bees.BeeRecipeData.MOD_ID;
import static net.phoenix.core.common.data.materials.PhoenixBeeMaterials.WAX_MELTING_CATALYST;
import static net.phoenix.core.common.data.materials.PhoenixMaterialFlags.crystal_rose;

@SuppressWarnings("All")
public class PhoenixBeeRecipeGenerator {

    // Shared base comb (non-NBT input)
    private static final ItemStack HONEY_COMB_BASE = new ItemStack(
            ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", "honey_comb_base")), 1);

    public static void loadBeeRecipes(Consumer<FinishedRecipe> provider) {
        // These two are now fully TagPrefix based:
        generateSimulatedColonyRecipes(provider);
        loadBeeCombProductionRecipes(provider);

        // The original ApisProgenitor / SwarmNurturing / Lumber recipes were tightly
        // coupled to ProductiveBees NBT/entity items.
        // If you want them TagPrefix-only too, you need to redesign what they consume/output.
        //
        generateApisProgenitorRecipes(provider);
        // generateSwarmNurturingRecipes(provider);
        generateLumberBeeRecipes(provider);
    }

    /*
     * -----------------------------
     * Tier selection
     * -----------------------------
     */

    private static TagPrefix tierPrefix(int tier) {
        return switch (tier) {
            case 1 -> PhoenixMaterialFlags.tier_one_bee;
            case 2 -> PhoenixMaterialFlags.tier_two_bee;
            case 3 -> PhoenixMaterialFlags.tier_three_bee;
            default -> throw new IllegalArgumentException("Unsupported bee tier: " + tier);
        };
    }

    private static ItemStack beeItemFor(FullBeeConfig config, Material mat) {
        TagPrefix prefix = tierPrefix(config.tier()); // <-- you choose tier per bee in config
        return ChemicalHelper.get(prefix, mat);
    }

    private static ItemStack combItemFor(Material mat) {
        return ChemicalHelper.get(PhoenixMaterialFlags.honeycomb, mat);
    }

    /*
     * -----------------------------
     * Fluids
     * -----------------------------
     */

    private static FluidStack getFluidStack(String fluidString) {
        String[] parts = fluidString.split(" ");
        ResourceLocation id = new ResourceLocation(parts[0]);
        int amount = Integer.parseInt(parts[1]);
        return new FluidStack(ForgeRegistries.FLUIDS.getValue(id), amount);
    }

    /*
     * -----------------------------
     * Simulated Colony (produces combs)
     * -----------------------------
     */

    public static void generateSimulatedColonyRecipes(Consumer<FinishedRecipe> provider) {
        for (FullBeeConfig config : BeeRecipeData.ALL_BEE_CONFIGS.values()) {
            String beeId = config.beeId();
            Material mat = GTMaterials.get(beeId);

            // If a beeId doesn't map to a GT material, it can't use TagPrefix unification.
            if (mat == null) continue;

            ItemStack beeCatalyst = beeItemFor(config, mat);
            if (beeCatalyst.isEmpty()) continue;

            ItemStack combOutput = combItemFor(mat);
            if (combOutput.isEmpty()) continue;

            ItemStack resourceBlock = new ItemStack(
                    ForgeRegistries.ITEMS.getValue(new ResourceLocation(config.pollinationInputId())));

            FluidStack sugarWater = BeeRecipeData.SUGAR_WATER_MATERIAL.getFluid(BeeRecipeData.SUGAR_WATER_AMOUNT);

            SIMULATED_COLONY_RECIPES.recipeBuilder(MOD_ID + "/simulated_colony/" + beeId)
                    .EUt(config.decantingEut())
                    .duration(config.decantingDuration())
                    .notConsumable(beeCatalyst)
                    .inputItems(HONEY_COMB_BASE)
                    .inputItems(resourceBlock)
                    .inputFluids(sugarWater)
                    .outputItems(combOutput, 1)
                    .save(provider);

            // Boosted: crystal rose (still TagPrefix-based)
            ItemStack crystalRose = ChemicalHelper.get(crystal_rose, mat);
            if (!crystalRose.isEmpty()) {
                SIMULATED_COLONY_RECIPES.recipeBuilder(MOD_ID + "/simulated_colony_boosted/" + beeId)
                        .EUt(config.boostedDecantingEut())
                        .duration(config.boostedDecantingDuration())
                        .notConsumable(beeCatalyst)
                        .inputItems(HONEY_COMB_BASE)
                        .inputItems(resourceBlock)
                        .inputFluids(sugarWater)
                        .inputItems(crystalRose.copyWithCount(1))
                        .outputItems(combOutput, 2)
                        .save(provider);
            }
        }
    }

    /*
     * -----------------------------
     * Comb processing (decanting/honey)
     * -----------------------------
     */

    private static record CountAndId(int count, String id) {}

    private static CountAndId parseCountedId(String spec) {
        // Supports: "4x minecraft:lapis_block" OR "minecraft:lapis_block"
        String s = spec.trim();
        int count = 1;
        String id = s;

        int xIdx = s.indexOf("x ");
        if (xIdx > 0) {
            String maybeCount = s.substring(0, xIdx).trim();
            try {
                count = Integer.parseInt(maybeCount);
                id = s.substring(xIdx + 2).trim();
            } catch (NumberFormatException ignored) {
                // fall back to treating whole string as id
                count = 1;
                id = s;
            }
        }
        return new CountAndId(count, id);
    }

    /**
     * Apis Progenitor: Upgrades bees using the new TagPrefix items.
     */
    public static void generateApisProgenitorRecipes(Consumer<FinishedRecipe> provider) {
        for (ApisProgenitorConfig cfg : BeeRecipeData.UNIQUE_APIS_PROGENITOR_CONFIGS) {

            // Resolve Materials from the material ID strings in the record
            Material inMat = GTMaterials.get(cfg.inputMaterialId());
            Material outMat = GTMaterials.get(cfg.outputMaterialId());

            if (inMat == null || outMat == null) continue;

            // Resolve the "Bee" items using your tierPrefix helper and ChemicalHelper
            ItemStack inBee = ChemicalHelper.get(tierPrefix(cfg.inputTier()), inMat);
            ItemStack outBee = ChemicalHelper.get(tierPrefix(cfg.outputTier()), outMat);

            if (inBee.isEmpty() || outBee.isEmpty()) continue;

            var builder = APIS_PROGENITOR_RECIPES.recipeBuilder(MOD_ID + "/apis_progenitor/" + cfg.id())
                    .EUt(cfg.EUt())
                    .duration(cfg.duration())
                    .inputItems(inBee)      // Consumes the source bee
                    .outputItems(outBee);   // Produces the upgraded bee

            // Process the List of extra item inputs (e.g., "4x minecraft:lapis_block")
            for (String itemSpec : cfg.extraItemInputs()) {
                applyExtraItemInput(builder, itemSpec);
            }

            // Process the List of extra fluid inputs (e.g., "gtceu:liquid_extra 1000")
            for (String fluidSpec : cfg.extraFluidInputs()) {
                builder.inputFluids(getFluidStack(fluidSpec));
            }

            builder.save(provider);
        }
    }

    /**
     * Lumber Bee: Uses the Lumber Bee material as a Tier 2 catalyst for wood processing.
     */
    public static void generateLumberBeeRecipes(Consumer<FinishedRecipe> provider) {
        Material lumberMat = GTMaterials.get("lumber");
        if (lumberMat == null) return;

        // Lumber Bee is treated as a Tier 2 Bee Catalyst
        ItemStack lumberBee = ChemicalHelper.get(PhoenixMaterialFlags.tier_two_bee, lumberMat);
        if (lumberBee.isEmpty()) return;

        for (String logId : BeeRecipeData.LUMBER_LOG_TYPES) {
            Item logItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(logId));
            if (logItem == null) continue;

            SIMULATED_COLONY_RECIPES.recipeBuilder(MOD_ID + "/simulated_colony/lumber_" + logId.replace(":", "_"))
                    .EUt(GTValues.VA[GTValues.IV])
                    .duration(1200)
                    .notConsumable(lumberBee) // The bee is not consumed
                    .inputItems(new ItemStack(logItem))
                    .inputFluids(PhoenixBeeMaterials.SUGAR_WATER.getFluid(BeeRecipeData.SUGAR_WATER_AMOUNT))
                    .outputItems(new ItemStack(logItem, 64))
                    .save(provider);
        }
    }

    /**
     * Comb Production: Now uses the Maps from PhoenixBeeMaterials for type-safety.
     */
    public static void loadBeeCombProductionRecipes(Consumer<FinishedRecipe> provider) {
        for (FullBeeConfig config : BeeRecipeData.ALL_BEE_CONFIGS.values()) {
            String beeId = config.beeId();
            if (beeId.equals("rancher") || beeId.equals("steamy")) continue;

            Material mat = GTMaterials.get(beeId);
            if (mat == null) continue;

            // 1. Get the Honeycomb Item
            ItemStack combInput = ChemicalHelper.get(PhoenixMaterialFlags.honeycomb, mat);

            // 2. Retrieve your Java-defined materials from the Maps
            Material honeyedFluidMat = PhoenixBeeMaterials.HONEYED_MATERIALS.get(beeId);
            Material rawWaxDustMat = PhoenixBeeMaterials.RAW_WAX_MATERIALS.get(beeId);

            if (combInput.isEmpty() || honeyedFluidMat == null || rawWaxDustMat == null) continue;

            // Decanting: Comb -> Raw Wax Dust + Base Comb
            COMB_DECANTING_RECIPES.recipeBuilder(MOD_ID + "/decanting/" + beeId)
                    .EUt(config.decantingEut()).duration(config.decantingDuration())
                    .inputItems(combInput)
                    .outputItems(ChemicalHelper.get(TagPrefix.dust, rawWaxDustMat))
                    .outputItems(HONEY_COMB_BASE)
                    .save(provider);

            // Melting: Raw Wax Dust + Catalyst -> Honeyed Fluid
            BREWING_RECIPES.recipeBuilder(MOD_ID + "/wax_melting/" + beeId)
                    .EUt(config.waxEut()).duration(400)
                    .inputItems(TagPrefix.dust, rawWaxDustMat)
                    .inputFluids(PhoenixBeeMaterials.WAX_MELTING_CATALYST.getFluid(100))
                    .outputFluids(honeyedFluidMat.getFluid(1000))
                    .save(provider);

            // Purifying: Honeyed Fluid -> Impure Honey + Resource
            var purifier = CENTRIFUGE_RECIPES.recipeBuilder(MOD_ID + "/honeyed_purifying/" + beeId)
                    .EUt(config.decantingEut()).duration(400)
                    .inputFluids(honeyedFluidMat.getFluid(1000))
                    .outputFluids(PhoenixBeeMaterials.IMPURE_HONEY.getFluid(500));

            // Resource logic (Salmon/Wannabee/Standard)
            if (beeId.equals("water")) {
                purifier.outputItems(new ItemStack(Items.SALMON));
            } else if (beeId.equals("wannabee")) {
                purifier.outputItems(new ItemStack(
                        ForgeRegistries.ITEMS.getValue(new ResourceLocation(config.pollinationInputId())), 2));
            } else {
                purifier.outputItems(config.finalOutputItem());
            }
            purifier.save(provider);
        }
    }

    private static void applyExtraItemInput(Object recipeBuilder, String itemSpec) {
        if (itemSpec == null || itemSpec.isBlank()) return;

        CountAndId parsed = parseCountedId(itemSpec);
        int count = parsed.count();
        String id = parsed.id();

        // Tag input: "#minecraft:logs"
        if (id.startsWith("#")) {
            ResourceLocation tagId = new ResourceLocation(id.substring(1));
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            // GTCEu builders commonly accept Ingredient
            ((GTRecipeBuilder) recipeBuilder)
                    .inputItems(Ingredient.of(tag), count);
            return;
        }

        // Item input: "minecraft:lapis_block"
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null) return;

        ((GTRecipeBuilder) recipeBuilder)
                .inputItems(new ItemStack(item, count));
    }
}
