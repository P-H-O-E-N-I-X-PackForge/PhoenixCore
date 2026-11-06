package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.registry.registrate.entry.GTRecipeTypeEntry;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ELECTRIC;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.GENERATOR;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.MULTIBLOCK;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public final class PftRecipeTypes {

    private PftRecipeTypes() {}

    public static GTRecipeTypeEntry TEST_TURBINE;
    public static GTRecipeTypeEntry EARTH_SIEVE;
    public static GTRecipeTypeEntry EMBER_IGNITER;
    public static GTRecipeTypeEntry CONFECTIONERY_FABRICATOR;
    public static GTRecipeTypeEntry THERMO_GENERATOR;
    public static GTRecipeTypeEntry WATER_SOURCE;
    public static GTRecipeTypeEntry GREENHOUSE;
    public static GTRecipeTypeEntry CONSTRUCTION_CORE;
    public static GTRecipeTypeEntry LAPIDARY;

    public static void init() {
        TEST_TURBINE = REGISTRATE.recipeType("test_turbine", MULTIBLOCK)
                .setMaxIOSize(2, 2, 2, 2)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC)
                .register();
        EARTH_SIEVE = REGISTRATE.recipeType("earth_sieve", ELECTRIC)
                .setMaxIOSize(1, 16, 0, 0)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.FORGE_HAMMER)
                .register();
        EMBER_IGNITER = REGISTRATE.recipeType("ember_igniter", ELECTRIC)
                .setMaxIOSize(2, 1, 2, 0)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.FURNACE)
                .register();
        CONFECTIONERY_FABRICATOR = REGISTRATE.recipeType("confectionery_fabricator", ELECTRIC)
                .setMaxIOSize(2, 1, 5, 0)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.SCIENCE)
                .register();
        THERMO_GENERATOR = REGISTRATE.recipeType("thermo_generator", GENERATOR)
                .setMaxIOSize(0, 0, 1, 1)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.SCIENCE)
                .register();
        WATER_SOURCE = REGISTRATE.recipeType("water_source", ELECTRIC)
                .setMaxIOSize(1, 0, 0, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.BATH)
                .register();
        GREENHOUSE = REGISTRATE.recipeType("greenhouse", MULTIBLOCK)
                .setMaxIOSize(3, 4, 1, 0)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.COOLING)
                .register();
        CONSTRUCTION_CORE = REGISTRATE.recipeType("construction_core", MULTIBLOCK)
                .setMaxIOSize(2, 1, 1, 0)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.COOLING)
                .register();
        LAPIDARY = REGISTRATE.recipeType("lapidary", ELECTRIC)
                .setMaxIOSize(2, 1, 1, 0)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.CUT)
                .register();
    }
}
