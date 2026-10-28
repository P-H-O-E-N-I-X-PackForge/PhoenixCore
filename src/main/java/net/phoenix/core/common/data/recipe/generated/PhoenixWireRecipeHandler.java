package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.WireProperties;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.Util;
import net.minecraft.data.recipes.RecipeOutput;
import net.phoenix.core.PhoenixCore;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.GTValues.EV;
import static com.gregtechceu.gtceu.api.GTValues.L;
import static com.gregtechceu.gtceu.api.GTValues.LuV;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.foil;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

public class PhoenixWireRecipeHandler {

    private static final Reference2IntMap<TagPrefix> INSULATION_AMOUNT = Util.make(new Reference2IntOpenHashMap<>(),
            map -> {
                map.put(cableGtSingle.get(), 1);
                map.put(cableGtDouble.get(), 1);
                map.put(cableGtQuadruple.get(), 2);
                map.put(cableGtOctal.get(), 3);
                map.put(cableGtHex.get(), 5);
            });

    private static final TagPrefix[] wireSizes = { wireGtDouble.get(), wireGtQuadruple.get(), wireGtOctal.get(),
            wireGtHex.get() };

    private PhoenixWireRecipeHandler() {}

    public static void run(@NotNull RecipeOutput provider, @NotNull Material material) {
        WireProperties property = material.getProperty(PropertyKey.WIRE);
        if (property == null) {
            return;
        }

        generateCableCovering(provider, property, wireGtSingle.get(), material);
        generateCableCovering(provider, property, wireGtDouble.get(), material);
        generateCableCovering(provider, property, wireGtQuadruple.get(), material);
        generateCableCovering(provider, property, wireGtOctal.get(), material);
        generateCableCovering(provider, property, wireGtHex.get(), material);
    }

    private static void generateCableCovering(@NotNull RecipeOutput provider,
                                              @NotNull WireProperties property,
                                              @NotNull TagPrefix prefix, @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || property.isSuperconductor()) {

            return;
        }

        int cableAmount = (int) (prefix.getMaterialAmount(material) * 2 / M);
        TagPrefix cablePrefix = GTRegistries.TAG_PREFIXES.get(PhoenixCore.id("cable" + prefix.name().substring(4)));
        int voltageTier = GTUtil.getTierByVoltage(property.getVoltage());
        int insulationAmount = INSULATION_AMOUNT.getInt(cablePrefix);

        GTRecipeBuilder builder = ASSEMBLER_RECIPES
                .recipeBuilder("cover_" + material.getName() + "_" + prefix + "_polyamide_imide")
                .EUt(VA[ULV]).duration(100)
                .inputItems(prefix, material)
                .outputItems(cablePrefix, material);

        if (voltageTier >= LuV) {
            builder.inputItems(foil, PolyphenyleneSulfide, insulationAmount);
        }

        if (voltageTier >= EV) {
            builder.inputItems(foil, PolyvinylChloride, insulationAmount);
        }

        builder.inputFluids(Water.getFluid(L * insulationAmount / 2))
                .save(provider);
    }
}
