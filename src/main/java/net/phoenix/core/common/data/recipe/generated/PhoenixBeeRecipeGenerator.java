package net.phoenix.core.common.data.recipe.generated;

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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.common.data.bees.BeeRecipeData;
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
        // generateLumberBeeRecipes(provider);
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

    public static void loadBeeCombProductionRecipes(Consumer<FinishedRecipe> provider) {
        for (FullBeeConfig config : BeeRecipeData.ALL_BEE_CONFIGS.values()) {
            String beeId = config.beeId();

            // preserve your exclusions
            if (beeId.equals("rancher") || beeId.equals("steamy")) continue;

            Material mat = GTMaterials.get(beeId);
            if (mat == null) continue;

            ItemStack combInput = combItemFor(mat);
            if (combInput.isEmpty()) continue;

            String waxDustId = "gtceu:raw_" + beeId + "_wax_dust";
            String honeyedFluidId = "gtceu:honeyed_" + beeId;

            // Decant comb -> wax dust + base comb
            COMB_DECANTING_RECIPES.recipeBuilder(MOD_ID + "/decanting/" + beeId)
                    .EUt(config.decantingEut())
                    .duration(config.decantingDuration())
                    .inputItems(combInput)
                    .outputItems(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(waxDustId))))
                    .outputItems(new ItemStack(
                            ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs:honey_comb_base"))))
                    .save(provider);

            // Melt wax dust -> honeyed fluid
            BREWING_RECIPES.recipeBuilder(MOD_ID + "/wax_melting/" + beeId)
                    .EUt(config.waxEut())
                    .duration(400)
                    .inputItems(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(waxDustId))))
                    .inputFluids(WAX_MELTING_CATALYST.getFluid(100))
                    .outputFluids(getFluidStack(honeyedFluidId + " 1000"))
                    .save(provider);

            // Purify honeyed fluid -> impure honey + item output
            var purifier = CENTRIFUGE_RECIPES.recipeBuilder(MOD_ID + "/honeyed_purifying/" + beeId)
                    .EUt(config.decantingEut())
                    .duration(400)
                    .inputFluids(getFluidStack(honeyedFluidId + " 1000"))
                    .outputFluids(getFluidStack("gtceu:impure_honey 500"));

            if (beeId.equals("water")) {
                purifier.outputItems(
                        new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:salmon"))));
            } else if (beeId.equals("wannabee")) {
                purifier.outputItems(new ItemStack(
                        ForgeRegistries.ITEMS.getValue(new ResourceLocation(config.pollinationInputId())), 2));
            } else {
                purifier.outputItems(config.finalOutputItem());
            }

            purifier.save(provider);

            // Mix comb + sugar water -> honey fluid
            MIXER_RECIPES.recipeBuilder(MOD_ID + "/honey_production/" + beeId)
                    .EUt(config.fluidEut())
                    .duration(config.fluidDuration())
                    .inputItems(combInput)
                    .inputFluids(BeeRecipeData.SUGAR_WATER_MATERIAL.getFluid(BeeRecipeData.SUGAR_WATER_AMOUNT))
                    .outputFluids(
                            getFluidStack(BeeRecipeData.HONEY_FLUID + " " + BeeRecipeData.FINAL_HONEY_OUTPUT_AMOUNT))
                    .save(provider);
        }
    }

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

    public static void generateApisProgenitorRecipes(Consumer<FinishedRecipe> provider) {
        for (ApisProgenitorConfig cfg : BeeRecipeData.UNIQUE_APIS_PROGENITOR_CONFIGS) {

            // Resolve materials
            Material inMat = GTMaterials.get(cfg.inputMaterialId());
            Material outMat = GTMaterials.get(cfg.outputMaterialId());
            if (inMat == null || outMat == null) continue;

            // Resolve bee items via TagPrefix + material
            ItemStack inBee = ChemicalHelper.get(tierPrefix(cfg.inputTier()), inMat);
            ItemStack outBee = ChemicalHelper.get(tierPrefix(cfg.outputTier()), outMat);
            if (inBee.isEmpty() || outBee.isEmpty()) continue;

            var b = APIS_PROGENITOR_RECIPES.recipeBuilder(MOD_ID + "/apis_progenitor/" + cfg.id())
                    .EUt(cfg.EUt())
                    .duration(cfg.duration())
                    .inputItems(inBee)      // consume input bee
                    .outputItems(outBee);   // output upgraded bee

            // extras: item inputs (0..n)
            for (String itemSpec : cfg.extraItemInputs()) {
                if (itemSpec == null || itemSpec.isBlank()) continue;
                applyExtraItemInput(b, itemSpec);
            }

            // extras: fluid inputs (0..n), like "gtceu:water 1000"
            for (String fluidSpec : cfg.extraFluidInputs()) {
                if (fluidSpec == null || fluidSpec.isBlank()) continue;
                b.inputFluids(getFluidStack(fluidSpec));
            }

            b.save(provider);
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
