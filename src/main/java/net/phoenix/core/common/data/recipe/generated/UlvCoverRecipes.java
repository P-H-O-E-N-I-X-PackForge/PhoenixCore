package net.phoenix.core.common.data.recipe.generated;


import net.minecraft.data.recipes.RecipeOutput;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.item.PhoenixItems;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public final class UlvCoverRecipes {

    private UlvCoverRecipes() {}

    public static void init(RecipeOutput provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, PhoenixCore.id("ulv_electric_motor"),
                PhoenixItems.ULV_ELECTRIC_MOTOR.asStack(),
                "CWR",
                "WMW",
                "RWC",
                'C', new MaterialEntry(cableGtSingle, Lead),
                'W', new MaterialEntry(wireGtSingle, Tin),
                'R', new MaterialEntry(rod, Copper),
                'M', new MaterialEntry(rod, IronMagnetic));

        VanillaRecipeHelper.addShapedRecipe(provider, PhoenixCore.id("ulv_electric_piston"),
                PhoenixItems.ULV_ELECTRIC_PISTON.asStack(),
                "PPP",
                "CRR",
                "CMG",
                'P', new MaterialEntry(plate, Copper),
                'C', new MaterialEntry(cableGtSingle, Lead),
                'R', new MaterialEntry(rod, Copper),
                'M', PhoenixItems.ULV_ELECTRIC_MOTOR.asStack(),
                'G', new MaterialEntry(gearSmall, Copper));

        VanillaRecipeHelper.addShapedRecipe(provider, PhoenixCore.id("ulv_conveyor_module"),
                PhoenixItems.ULV_CONVEYOR_MODULE.asStack(),
                "RRR",
                "MCM",
                "RRR",
                'R', GTItems.STICKY_RESIN.asStack(),
                'M', PhoenixItems.ULV_ELECTRIC_MOTOR.asStack(),
                'C', new MaterialEntry(cableGtSingle, Lead));

        VanillaRecipeHelper.addShapedRecipe(provider, PhoenixCore.id("ulv_electric_pump"),
                PhoenixItems.ULV_ELECTRIC_PUMP.asStack(),
                "SFR",
                "sPw",
                "RMC",
                'S', new MaterialEntry(screw, Copper),
                'R', new MaterialEntry(ring, Rubber),
                'F', new MaterialEntry(rotor, Copper),
                'P', new MaterialEntry(pipeNormalFluid, Copper),
                'M', PhoenixItems.ULV_ELECTRIC_MOTOR.asStack(),
                'C', new MaterialEntry(cableGtSingle, Lead));

        VanillaRecipeHelper.addShapedRecipe(provider, PhoenixCore.id("ulv_robot_arm"),
                PhoenixItems.ULV_ROBOT_ARM.asStack(),
                "CCC",
                "MRM",
                "PVR",
                'R', new MaterialEntry(rod, Copper),
                'C', new MaterialEntry(cableGtSingle, Lead),
                'M', PhoenixItems.ULV_ELECTRIC_MOTOR.asStack(),
                'P', PhoenixItems.ULV_ELECTRIC_PISTON.asStack(),
                'V', CustomTags.ULV_CIRCUITS);

        VanillaRecipeHelper.addShapedRecipe(provider, PhoenixCore.id("ulv_fluid_regulator"),
                PhoenixItems.ULV_FLUID_REGULATOR.asStack(),
                "SFR",
                "sPw",
                "RMC",
                'S', new MaterialEntry(screw, Copper),
                'R', CustomTags.ULV_CIRCUITS,
                'F', new MaterialEntry(rotor, Copper),
                'P', PhoenixItems.ULV_ELECTRIC_PUMP.asStack(),
                'M', PhoenixItems.ULV_ELECTRIC_MOTOR.asStack(),
                'C', new MaterialEntry(cableGtSingle, Lead));
    }
}
