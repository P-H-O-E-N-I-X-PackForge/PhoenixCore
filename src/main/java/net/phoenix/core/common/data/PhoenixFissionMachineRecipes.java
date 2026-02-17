package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.FinishedRecipe;
import net.phoenix.core.common.block.PhoenixFissionBlocks;
import net.phoenix.core.common.machine.PhoenixFissionMachines;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static net.phoenix.core.PhoenixCore.id;
import static net.phoenix.core.common.data.materials.PhoenixFissionMaterials.*;
import static net.phoenix.core.common.data.materials.PhoenixProgressionMaterials.FROST;

public class PhoenixFissionMachineRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        var assembler = GTRecipeTypes.ASSEMBLER_RECIPES;
        var electro = GTRecipeTypes.ELECTROLYZER_RECIPES;

        assembler.recipeBuilder("fissile_heat_safe_casing")
                .inputItems(TagPrefix.plate, ZIRCALLOY, 6)
                .inputItems(TagPrefix.frameGt, StainlessSteel, 1)
                .inputItems(TagPrefix.pipeLargeFluid, Aluminium, 2)
                .circuitMeta(2)
                .inputFluids(FROST.getFluid(100))
                .outputItems(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.asItem(), 2)
                .duration(160)
                .EUt(GTValues.HV / 2)
                .save(provider);

        assembler.recipeBuilder("fissile_reaction_safe_casing")
                .inputItems(TagPrefix.plate, ZIRCALLOY, 3)
                .inputItems(TagPrefix.plate, StainlessSteel, 2)
                .inputItems(TagPrefix.frameGt, ZIRCALLOY, 1)
                .circuitMeta(6)
                .inputFluids(StainlessSteel.getFluid(250))
                .outputItems(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.asItem(), 2)
                .duration(145)
                .EUt(GTValues.HV / 2)
                .save(provider);

        assembler.recipeBuilder("fisile_safe_gearbox_casing")
                .inputItems(TagPrefix.plate, ZIRCALLOY, 4)
                .inputItems(TagPrefix.gear, ZIRCALLOY, 2)
                .inputItems(TagPrefix.frameGt, ZIRCALLOY, 1)
                .circuitMeta(4)
                .inputFluids(Gold, 1000)
                .outputItems(PhoenixFissionBlocks.FISSILE_SAFE_GEARBOX_CASING.asItem(), 2)
                .duration(120)
                .EUt(GTValues.LV / 2)
                .save(provider);

        assembler.recipeBuilder("presurized_fission_reactor")
                .inputItems(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING, 4)
                .inputItems(TagPrefix.pipeLargeFluid, StainlessSteel, 8)
                .inputItems(TagPrefix.frameGt, ZIRCALLOY, 4)
                .inputItems(CustomTags.HV_CIRCUITS)
                .inputFluids(SolderingAlloy, 613)
                .outputItems(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR, 1)
                .duration(320)
                .EUt(GTValues.HV / 2)
                .save(provider);

        assembler.recipeBuilder("heat_exhanger")
                .inputItems(PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING.asItem(), 2)
                .inputItems(PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING.asItem(), 8)
                .inputItems(TagPrefix.plateDense, ZIRCALLOY, 2)
                .inputItems(CustomTags.EV_CIRCUITS)
                .inputItems(TagPrefix.pipeLargeFluid, Titanium, 2)
                .inputFluids(SolderingAlloy, 613)
                .outputItems(PhoenixFissionMachines.HEAT_EXCHANGER.asStack(), 1)
                .duration(180)
                .EUt(GTValues.EV / 2)
                .save(provider);

        assembler.recipeBuilder("high_performnce_breeder_reactor")
                .inputItems(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR.asStack(), 1)
                .inputItems(TagPrefix.rotor, BORON_CARBIDE, 2)
                .inputItems(CustomTags.IV_CIRCUITS)
                .inputItems(TagPrefix.gear, ZIRCALLOY, 2)
                .inputItems(GTMachines.HULL[IV])
                .inputItems(TagPrefix.rod, Uranium238, 16)
                .inputFluids(CRYO_GRAPHITE_BINDING_SOLUTION, 6000)
                .outputItems(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack(), 1)
                .duration(800)
                .EUt(IV / 2)
                .save(provider);

        assembler.recipeBuilder("graphite_moderator")
                .inputItems(TagPrefix.plate, Steel, 4)
                .inputItems(TagPrefix.dust, Graphite, 16)
                .inputItems(TagPrefix.frameGt, Steel, 1)
                .inputItems(GTItems.VOLTAGE_COIL_MV, 1)
                .inputItems(CustomTags.MV_CIRCUITS)
                .inputFluids(Steel, 576)
                .outputItems(PhoenixFissionBlocks.MODERATOR_GRAPHITE.asItem(), 1)
                .duration(450)
                .EUt(VA[EV])
                .save(provider);

        assembler.recipeBuilder("basic_fission_cooler")
                .inputItems(TagPrefix.frameGt, Steel, 2)
                .inputItems(TagPrefix.rodLong, Steel, 3)
                .inputItems(TagPrefix.rotor, Steel, 1)
                .inputItems(CustomTags.HV_CIRCUITS)
                .inputFluids(DistilledWater.getFluid(576))
                .outputItems(PhoenixFissionBlocks.COOLER_BASIC.asItem(), 1)
                .duration(450)
                .EUt(VA[HV])
                .save(provider);

        GTRecipeTypes.CHEMICAL_BATH_RECIPES.recipeBuilder(id("zirconium_dust_to_hafnium_chloride"))
                .inputItems(TagPrefix.dust, Zirconium, 4)
                .inputFluids(HydrochloricAcid, 1000)
                .outputFluids(HAFNIUM_CHLORIDE.getFluid(2000))
                .duration(200)
                .EUt(VA[HV])
                .save(provider);

        GTRecipeTypes.CENTRIFUGE_RECIPES.recipeBuilder(id("zircon_dust_processing"))
                .inputItems(TagPrefix.dust, ZIRCON, 20)
                .outputItems(TagPrefix.dust, IMPURE_ZIRCONIUM, 8)
                .outputItems(TagPrefix.dust, IMPURE_HAFNIUM, 4)
                .duration(400)
                .EUt(VA[HV])
                .save(provider);

        GTRecipeTypes.ELECTROLYZER_RECIPES.recipeBuilder(id("hafnium_chloride"))
                .inputFluids(HAFNIUM_CHLORIDE.getFluid(500))
                .outputFluids(HydrochloricAcid.getFluid(200))
                .outputItems(TagPrefix.dust, Hafnium, 1)
                .duration(100)
                .EUt(VA[HV])
                .save(provider);

        GTRecipeTypes.MIXER_RECIPES.recipeBuilder(id("zircalloy_dust"))
                .inputItems(TagPrefix.dust, Zirconium, 5)
                .inputItems(TagPrefix.dustSmall, Bismuth, 1)
                .inputItems(TagPrefix.dust, Hafnium, 2)
                .outputItems(TagPrefix.dust, ZIRCALLOY, 7)
                .circuitMeta(15)
                .duration(100)
                .EUt(VA[HV])
                .save(provider);
    }
}
