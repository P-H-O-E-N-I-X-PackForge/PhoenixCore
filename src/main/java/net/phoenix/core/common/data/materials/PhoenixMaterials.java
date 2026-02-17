package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.*;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.phoenix.core.api.item.tool.PhoenixToolType;
import net.phoenix.core.common.data.recipe.generated.BeePrefixHelper;
import net.phoenix.core.common.data.recipe.generated.CrystalRoseHelper;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static net.phoenix.core.common.data.materials.PhoenixMaterialHelpers.*;

public class PhoenixMaterials {

    public static void register() {
        GTMaterials.Francium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Technetium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Radium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Actinium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Polonium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Protactinium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Neptunium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Curium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Berkelium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Californium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Einsteinium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Fermium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Mendelevium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Nobelium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Lawrencium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Strontium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Zirconium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Hafnium.setProperty(PropertyKey.INGOT, new IngotProperty());
        GTMaterials.Hafnium.setProperty(PropertyKey.ORE, new OreProperty());
        GTMaterials.Zirconium.setProperty(PropertyKey.ORE, new OreProperty());

        addFluid(GTMaterials.Iodine, FluidStorageKeys.GAS);
        addFluid(GTMaterials.Oganesson, FluidStorageKeys.GAS);
    }

    public static void addFluid(Material m, FluidStorageKey key) {
        FluidProperty prop = new FluidProperty();

        prop.getStorage().enqueueRegistration(key, new FluidBuilder());

        m.setProperty(PropertyKey.FLUID, prop);
    }

    public static void modifyMaterials() {
        CrystalRoseHelper.addCrystalRoseFlags(
                // --- Basic & Base Metals ---
                Amethyst, Apatite, Bauxite, Cinnabar, Cobalt, Cobaltite, Copper, Diamond,
                Electrotine, Emerald, Galena, Gold, Ilmenite, Invar, Iron, Lapis,
                Lead, Lepidolite, Malachite, Nickel, Opal, Pitchblende, Pyrope, Realgar,
                Ruby, Salt, Sapphire, Scheelite, Silicon, Silver, Steel, Stibnite, Topaz,
                TricalciumPhosphate, Tungstate, Zinc,

                // --- LuV & Specialized Materials ---
                Barite, Bastnasite, Bismuth, Chromite, Graphite, Molybdenum, Oilsands, Platinum,
                Pyrochlore, Pyrolusite, Sphalerite, Sulfur, Tantalite, Tetrahedrite, Thorium,
                Titanium, VanadiumMagnetite,

                // --- Custom & External Materials from your Configs ---
                // PhoenixOres.FLUORITE,
                NetherQuartz,
                RockSalt,
                Sodalite,

                // --- Missing GT Materials from BeeRecipeData ---
                Coal,             // For Coal Bee
                Redstone,         // For Redstone Bee
                Tin,              // For Tin Bee
                Obsidian,         // For Obsidian Bee
                Netherite,        // For Netherite Bee
                CertusQuartz     // For Spacial Bee
        );
        BeePrefixHelper.addBeeCombFlag(
                // --- Basic & Base Metals ---
                Amethyst, Apatite, Bauxite, Cinnabar, Cobalt, Cobaltite, Copper, Diamond,
                Electrotine, Emerald, Galena, Gold, Ilmenite, Invar, Iron, Lapis,
                Lead, Lepidolite, Malachite, Nickel, Opal, Pitchblende, Pyrope, Realgar,
                Ruby, Salt, Sapphire, Scheelite, Silicon, Silver, Steel, Stibnite, Topaz,
                TricalciumPhosphate, Tungstate, Zinc,

                // --- LuV & Specialized Materials ---
                Barite, Bastnasite, Bismuth, Chromite, Graphite, Molybdenum, Oilsands, Platinum,
                Pyrochlore, Pyrolusite, Sphalerite, Sulfur, Tantalite, Tetrahedrite, Thorium,
                Titanium, VanadiumMagnetite,

                // --- Custom & External Materials from your Configs ---
                // PhoenixOres.FLUORITE,
                NetherQuartz,
                RockSalt,
                Sodalite,

                // --- Missing GT Materials from BeeRecipeData ---
                Coal,             // For Coal Bee
                Redstone,         // For Redstone Bee
                Tin,              // For Tin Bee
                Obsidian,         // For Obsidian Bee
                Netherite,        // For Netherite Bee
                CertusQuartz     // For Spacial Bee
        );

        BeePrefixHelper.addTierOneBeeFlag(
                // --- Basic & Base Metals ---
                Amethyst, Apatite, Bauxite, Cinnabar, Cobalt, Cobaltite, Copper, Diamond,
                Electrotine, Emerald, Galena, Gold, Ilmenite, Invar, Iron, Lapis,
                Lead, Lepidolite, Malachite, Nickel, Opal, Pitchblende, Pyrope, Realgar,
                Ruby, Salt, Sapphire, Scheelite, Silicon, Silver, Steel, Stibnite, Topaz,
                TricalciumPhosphate, Tungstate, Zinc,

                // --- LuV & Specialized Materials ---
                Barite, Bastnasite, Bismuth, Chromite, Graphite, Molybdenum, Oilsands, Platinum,
                Pyrochlore, Pyrolusite, Sphalerite, Sulfur, Tantalite, Tetrahedrite, Thorium,
                Titanium, VanadiumMagnetite,

                // --- Custom & External Materials from your Configs ---
                // PhoenixOres.FLUORITE,
                NetherQuartz,
                RockSalt,
                Sodalite,

                // --- Missing GT Materials from BeeRecipeData ---
                Coal,             // For Coal Bee
                Redstone,         // For Redstone Bee
                Tin,              // For Tin Bee
                Obsidian,         // For Obsidian Bee
                Netherite,        // For Netherite Bee
                CertusQuartz     // For Spacial Bee
        );

        BeePrefixHelper.addTierTwoBeeFlag(
                // --- Basic & Base Metals ---
                Amethyst, Apatite, Bauxite, Cinnabar, Cobalt, Cobaltite, Copper, Diamond,
                Electrotine, Emerald, Galena, Gold, Ilmenite, Invar, Iron, Lapis,
                Lead, Lepidolite, Malachite, Nickel, Opal, Pitchblende, Pyrope, Realgar,
                Ruby, Salt, Sapphire, Scheelite, Silicon, Silver, Steel, Stibnite, Topaz,
                TricalciumPhosphate, Tungstate, Zinc,

                // --- LuV & Specialized Materials ---
                Barite, Bastnasite, Bismuth, Chromite, Graphite, Molybdenum, Oilsands, Platinum,
                Pyrochlore, Pyrolusite, Sphalerite, Sulfur, Tantalite, Tetrahedrite, Thorium,
                Titanium, VanadiumMagnetite,

                // --- Custom & External Materials from your Configs ---
                // PhoenixOres.FLUORITE,
                NetherQuartz,
                RockSalt,
                Sodalite,

                // --- Missing GT Materials from BeeRecipeData ---
                Coal,             // For Coal Bee
                Redstone,         // For Redstone Bee
                Tin,              // For Tin Bee
                Obsidian,         // For Obsidian Bee
                Netherite,        // For Netherite Bee
                CertusQuartz     // For Spacial Bee
        );

        BeePrefixHelper.addTierThreeBeeFlag(
                // --- Basic & Base Metals ---
                Amethyst, Apatite, Bauxite, Cinnabar, Cobalt, Cobaltite, Copper, Diamond,
                Electrotine, Emerald, Galena, Gold, Ilmenite, Invar, Iron, Lapis,
                Lead, Lepidolite, Malachite, Nickel, Opal, Pitchblende, Pyrope, Realgar,
                Ruby, Salt, Sapphire, Scheelite, Silicon, Silver, Steel, Stibnite, Topaz,
                TricalciumPhosphate, Tungstate, Zinc,

                // --- LuV & Specialized Materials ---
                Barite, Bastnasite, Bismuth, Chromite, Graphite, Molybdenum, Oilsands, Platinum,
                Pyrochlore, Pyrolusite, Sphalerite, Sulfur, Tantalite, Tetrahedrite, Thorium,
                Titanium, VanadiumMagnetite,

                // --- Custom & External Materials from your Configs ---
                // PhoenixOres.FLUORITE,
                NetherQuartz,
                RockSalt,
                Sodalite,

                // --- Missing GT Materials from BeeRecipeData ---
                Coal,             // For Coal Bee
                Redstone,         // For Redstone Bee
                Tin,              // For Tin Bee
                Obsidian,         // For Obsidian Bee
                Netherite,        // For Netherite Bee
                CertusQuartz     // For Spacial Bee
        );

        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            ToolProperty toolProperty = material.getProperty(PropertyKey.TOOL);

            if (toolProperty != null && toolProperty.hasType(GTToolType.SCREWDRIVER_LV)) {
                toolProperty.addTypes(PhoenixToolType.SCREWDRIVER_MV);
                toolProperty.addTypes(PhoenixToolType.SCREWDRIVER_EV);
                toolProperty.addTypes(PhoenixToolType.SCREWDRIVER_LuV);
                toolProperty.addTypes(PhoenixToolType.SCREWDRIVER_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.BUZZSAW)) {
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_MV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_HV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_EV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_IV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_LuV);
                toolProperty.addTypes(PhoenixToolType.BUZZSAW_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.CHAINSAW_LV)) {
                toolProperty.addTypes(PhoenixToolType.CHAINSAW_MV);
                toolProperty.addTypes(PhoenixToolType.CHAINSAW_EV);
                toolProperty.addTypes(PhoenixToolType.CHAINSAW_LuV);
                toolProperty.addTypes(PhoenixToolType.CHAINSAW_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.WIRE_CUTTER_LV)) {
                toolProperty.addTypes(PhoenixToolType.WIRE_CUTTER_MV);
                toolProperty.addTypes(PhoenixToolType.WIRE_CUTTER_EV);
                toolProperty.addTypes(PhoenixToolType.WIRE_CUTTER_LuV);
                toolProperty.addTypes(PhoenixToolType.WIRE_CUTTER_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.WRENCH_LV)) {
                toolProperty.addTypes(PhoenixToolType.WRENCH_MV);
                toolProperty.addTypes(PhoenixToolType.WRENCH_EV);
                toolProperty.addTypes(PhoenixToolType.WRENCH_LuV);
                toolProperty.addTypes(PhoenixToolType.WRENCH_ZPM);
            }

            if (toolProperty != null && toolProperty.hasType(GTToolType.DRILL_LV)) {
                toolProperty.addTypes(PhoenixToolType.DRILL_LUV, PhoenixToolType.DRILL_ZPM);
            }

        }
    }
}
