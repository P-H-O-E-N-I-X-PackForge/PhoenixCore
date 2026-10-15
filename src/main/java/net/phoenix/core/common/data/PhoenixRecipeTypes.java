package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.registrate.entry.GTRecipeTypeEntry;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.client.resources.language.I18n;
import net.phoenix.core.api.gui.PhoenixGuiTextures;
import net.phoenix.core.integration.ars_nouveau.api.capability.SourceRecipeCapability;

import brachy.modularui.drawable.progress.ProgressDrawable;
import brachy.modularui.widgets.ProgressWidget;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixRecipeTypes {

    public static GTRecipeTypeEntry HONEY_CHAMBER_RECIPES;
    public static GTRecipeTypeEntry PLEASE;
    public static GTRecipeTypeEntry TESLA_TOWER;
    public static GTRecipeTypeEntry JUKEBLOCK;
    public static GTRecipeTypeEntry HIGH_PERFORMANCE_BREEDER_REACTOR_RECIPES;
    public static GTRecipeTypeEntry ADVANCED_PRESSURIZED_FISSION_REACTOR_RECIPES;
    public static GTRecipeTypeEntry PRESSURIZED_FISSION_REACTOR_RECIPES;
    public static GTRecipeTypeEntry HEAT_EXCHANGER_RECIPES;
    public static GTRecipeTypeEntry SIMULATED_COLONY_RECIPES;
    public static GTRecipeTypeEntry COMB_DECANTING_RECIPES;
    public static GTRecipeTypeEntry SWARM_NURTURING_RECIPES;
    public static GTRecipeTypeEntry APIS_PROGENITOR_RECIPES;
    public static GTRecipeTypeEntry MELLIFERIOUS_MATRIX_RECIPES;
    public static GTRecipeTypeEntry SOURCE_IMBUEMENT_RECIPES, SOURCE_REACTOR_RECIPES, BIO_ENGINE_RECIPES;
    public static GTRecipeTypeEntry SOURCE_EXTRACTION_RECIPES;
    public static GTRecipeTypeEntry HIGH_PRESSURE_ARC_FURNACE;
    public static GTRecipeTypeEntry PHOENIXWARE_FUSION_MK1;
    public static GTRecipeTypeEntry DIMENSIONAL_ANCHORING_RECIPES, AETHERIAL_FABIRCATION_RECIPES;
    
    public static GTRecipeTypeEntry GROWTH_RECIPES, BOTANICAL_GROWTH_RECIPES, VINE_GROWTH_RECIPES;
    public static GTRecipeTypeEntry ASTRAL_WEAVING_RECIPES;
    public static GTRecipeTypeEntry SANCTUM_WARD_RECIPES;

    public static void init() {
        GROWTH_RECIPES = REGISTRATE.recipeType("growth", MULTIBLOCK)
                .setMaxIOSize(4, 3, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER)
                .register();
        BOTANICAL_GROWTH_RECIPES = REGISTRATE.recipeType("botanical_growth", MULTIBLOCK)
                .setMaxIOSize(4, 3, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER)
                .register();
        VINE_GROWTH_RECIPES = REGISTRATE.recipeType("vine_growth", MULTIBLOCK)
                .setMaxIOSize(4, 3, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER)
                .register();

        ASTRAL_WEAVING_RECIPES = REGISTRATE.recipeType("astral_weaving", MULTIBLOCK)
                .setMaxIOSize(2, 2, 2, 2)
                .setMaxSize(IO.IN, net.phoenix.core.integration.astral.api.capability.AstralThreadRecipeCapability.CAP,
                        1)
                .setMaxSize(IO.OUT, net.phoenix.core.integration.astral.api.capability.AstralThreadRecipeCapability.CAP,
                        1)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER)
                .register();

        AETHERIAL_FABIRCATION_RECIPES = REGISTRATE.recipeType("aetherial_fabrication", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(3, 25, 1, 0)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC)
                .register();

        JUKEBLOCK = REGISTRATE.recipeType("jukeblock", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(3, 25, 1, 0)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC)
                .register();

        DIMENSIONAL_ANCHORING_RECIPES = REGISTRATE.recipeType("dimensional_anchoring", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(3, 30, 1, 0)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC)
                .register();

        PHOENIXWARE_FUSION_MK1 = REGISTRATE.recipeType("phoenixware_fusion_mk1", MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(0, 0, 2, 1)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC)
                .register();

        PLEASE = REGISTRATE.recipeType("please", MULTIBLOCK)
                .setMaxIOSize(4, 1, 8, 4)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_COMPRESS))
                .setSound(GTSoundEntries.FORGE_HAMMER)
                .register();

        SWARM_NURTURING_RECIPES = REGISTRATE.recipeType("swarm_nurturing", MULTIBLOCK)
                .setMaxIOSize(3, 1, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER)
                .register();

        TESLA_TOWER = REGISTRATE.recipeType("tesla_tower", MULTIBLOCK)
                .setMaxIOSize(1, 1, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .register();

        HONEY_CHAMBER_RECIPES = REGISTRATE.recipeType("honey_chamber", MULTIBLOCK)
                .setMaxIOSize(4, 4, 4, 4)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_COMPRESS))
                .setSound(GTSoundEntries.BATH)
                .register();

        SANCTUM_WARD_RECIPES = REGISTRATE.recipeType("sanctum_ward", MULTIBLOCK)
                .setMaxIOSize(4, 0, 1, 0)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER)
                .register();

        SIMULATED_COLONY_RECIPES = REGISTRATE.recipeType("simulated_colony", MULTIBLOCK)
                .setMaxIOSize(4, 1, 2, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_COMPRESS))
                .setSound(GTSoundEntries.FORGE_HAMMER)
                .register();

        COMB_DECANTING_RECIPES = REGISTRATE.recipeType("comb_decanting", MULTIBLOCK)
                .setMaxIOSize(1, 2, 1, 2)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_MACERATE))
                .setSound(GTSoundEntries.CENTRIFUGE)
                .register();

        MELLIFERIOUS_MATRIX_RECIPES = REGISTRATE.recipeType("melliferous_matrix", MULTIBLOCK)
                .setMaxIOSize(1, 2, 1, 2)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_MACERATE))
                .setSound(GTSoundEntries.CENTRIFUGE)
                .register();

        HIGH_PRESSURE_ARC_FURNACE = REGISTRATE.recipeType("high_pressure_arc_furnace", MULTIBLOCK)
                .setMaxIOSize(2, 2, 2, 2)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.ARC)
                .register();

        SOURCE_REACTOR_RECIPES = REGISTRATE.recipeType("source_reactor", MULTIBLOCK)
                .setMaxIOSize(3, 2, 2, 2)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.REPLICATOR)
                .register();

        BIO_ENGINE_RECIPES = REGISTRATE.recipeType("bio_engine", MULTIBLOCK)
                .setMaxIOSize(1, 1, 0, 1)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setMaxSize(IO.OUT, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.REPLICATOR)
                .register();

        SOURCE_IMBUEMENT_RECIPES = REGISTRATE.recipeType("source_imbuement", MULTIBLOCK)
                .setMaxIOSize(3, 3, 1, 0)
                .setMaxSize(IO.IN, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.CHEMICAL)
                .register();

        SOURCE_EXTRACTION_RECIPES = REGISTRATE.recipeType("source_extraction", MULTIBLOCK)
                .setMaxIOSize(2, 1, 1, 0)
                .setMaxSize(IO.OUT, SourceRecipeCapability.CAP, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
                .setSound(GTSoundEntries.CHEMICAL)
                .register();

        APIS_PROGENITOR_RECIPES = REGISTRATE.recipeType("apis_progenitor", MULTIBLOCK)
                .setMaxIOSize(3, 1, 1, 1)
                .setEUIO(IO.IN)
                .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_EXTRACT))
                .setSound(GTSoundEntries.MIXER)
                .register();

        HIGH_PERFORMANCE_BREEDER_REACTOR_RECIPES = REGISTRATE.recipeType("high_performance_breeder_reactor", MULTIBLOCK)
                .setMaxIOSize(2, 2, 2, 2)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBarSupplier((layout, value, machine) -> new ProgressWidget()
                        .value(value)
                        .name("progressBar")
                        .texture(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressDrawable.Direction.RIGHT)
                        .size(20)))
                .setSound(GTSoundEntries.CHEMICAL)
                .register();

        PRESSURIZED_FISSION_REACTOR_RECIPES = REGISTRATE.recipeType("pressurized_fission_reactor", MULTIBLOCK)
                .setMaxIOSize(1, 1, 0, 0)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBarSupplier((layout, value, machine) -> new ProgressWidget()
                        .value(value)
                        .name("progressBar")
                        .texture(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressDrawable.Direction.RIGHT)
                        .size(20)))
                .setSound(GTSoundEntries.CHEMICAL)
                .register();

        ADVANCED_PRESSURIZED_FISSION_REACTOR_RECIPES = REGISTRATE.recipeType("advanced_pressurized_fission_reactor", MULTIBLOCK)
                .setMaxIOSize(1, 1, 1, 1)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBarSupplier((layout, value, machine) -> new ProgressWidget()
                        .value(value)
                        .name("progressBar")
                        .texture(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressDrawable.Direction.RIGHT)
                        .size(20)))
                .setSound(GTSoundEntries.CHEMICAL)
                .register();

        HEAT_EXCHANGER_RECIPES = REGISTRATE.recipeType("heat_exchanging", MULTIBLOCK)
                .setMaxIOSize(1, 0, 1, 1)
                .setEUIO(IO.OUT)
                .UI(builder -> builder.setProgressBarSupplier((layout, value, machine) -> new ProgressWidget()
                        .value(value)
                        .name("progressBar")
                        .texture(PhoenixGuiTextures.PROGRESS_BAR_FISSION, ProgressDrawable.Direction.RIGHT)
                        .size(20)))
                .setSound(GTSoundEntries.MIXER)
                .register();
    }

    /** Runs once the recipe type registry is filled. */
    public static void postInit() {
        for (GTRecipeType growing : new GTRecipeType[] { GROWTH_RECIPES.get(), BOTANICAL_GROWTH_RECIPES.get(),
                VINE_GROWTH_RECIPES.get() }) {
            growing.getDataInfos().add(data -> data.contains("min_growth_stage") ?
                    "Needs growth stage " + data.getInt("min_growth_stage") : null);
        }
        HIGH_PERFORMANCE_BREEDER_REACTOR_RECIPES.get().getDataInfos().add(data -> {
            int cooling = data.getInt("required_cooling");
            return cooling > 0 ? I18n.get("emi_info.phoenixcore.required_cooling", cooling) : "";
        });
        PRESSURIZED_FISSION_REACTOR_RECIPES.get().getDataInfos().add(data -> {
            int cooling = data.getInt("required_cooling");
            return cooling > 0 ? I18n.get("emi_info.phoenixcore.required_cooling", cooling) : "";
        });
        ADVANCED_PRESSURIZED_FISSION_REACTOR_RECIPES.get().getDataInfos().add(data -> {
            int cooling = data.getInt("required_cooling");
            return cooling > 0 ? I18n.get("emi_info.phoenixcore.required_cooling", cooling) : "";
        });
    }
}
