package net.phoenix.core.integration.conflux.dimension;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DisciplineTheme {
    public final String disciplineId;
    public final String displayName;
    public final BiomePalette biomePalette;
    public final StructureThemeSet structures;
    public final SkyboxProfile skybox;
    public final ColorProgression[] colorProgression;
    public final net.phoenix.core.integration.conflux.dimension.worldgen.DisciplineWorldgenConfig worldgenConfig;

    public DisciplineTheme(
            String disciplineId,
            String displayName,
            BiomePalette biomePalette,
            StructureThemeSet structures,
            SkyboxProfile skybox,
            ColorProgression[] colorProgression,
            net.phoenix.core.integration.conflux.dimension.worldgen.DisciplineWorldgenConfig worldgenConfig) {
        this.disciplineId = disciplineId;
        this.displayName = displayName;
        this.biomePalette = biomePalette;
        this.structures = structures;
        this.skybox = skybox;
        this.colorProgression = colorProgression;
        this.worldgenConfig = worldgenConfig;
    }

    public static DisciplineTheme fromJson(JsonObject json, String disciplineId) {
        String displayName = json.get("display_name").getAsString();

        BiomePalette biomePalette = BiomePalette.fromJson(json.getAsJsonObject("biome_palette"));
        StructureThemeSet structures = StructureThemeSet.fromJson(json.getAsJsonObject("structures"));
        SkyboxProfile skybox = SkyboxProfile.fromJson(json.getAsJsonObject("skybox"));

        JsonArray colorProgArray = json.getAsJsonArray("color_progression");
        ColorProgression[] colorProgression = new ColorProgression[colorProgArray.size()];
        for (int i = 0; i < colorProgArray.size(); i++) {
            colorProgression[i] = ColorProgression.fromJson(colorProgArray.get(i).getAsJsonObject());
        }

        net.phoenix.core.integration.conflux.dimension.worldgen.DisciplineWorldgenConfig worldgenConfig = null;
        if (json.has("worldgen_config")) {
            worldgenConfig = net.phoenix.core.integration.conflux.dimension.worldgen.DisciplineWorldgenConfig.fromJson(
                    json.getAsJsonObject("worldgen_config"), disciplineId);
        }

        return new DisciplineTheme(
                disciplineId,
                displayName,
                biomePalette,
                structures,
                skybox,
                colorProgression,
                worldgenConfig);
    }

    public static class BiomePalette {
        public final List<StageEntry> stages;
        public final Map<String, ResourceLocation> defaultBiomes;

        public BiomePalette(List<StageEntry> stages, Map<String, ResourceLocation> defaultBiomes) {
            this.stages = stages;
            this.defaultBiomes = defaultBiomes;
        }

        public static BiomePalette fromJson(JsonObject json) {
            List<StageEntry> stages = new ArrayList<>();
            JsonArray stagesArray = json.getAsJsonArray("stages");
            for (JsonElement elem : stagesArray) {
                stages.add(StageEntry.fromJson(elem.getAsJsonObject()));
            }

            Map<String, ResourceLocation> defaultBiomes = new HashMap<>();
            if (json.has("default_biomes")) {
                JsonObject defaultObj = json.getAsJsonObject("default_biomes");
                for (String key : defaultObj.keySet()) {
                    defaultBiomes.put(key, new ResourceLocation(defaultObj.get(key).getAsString()));
                }
            }

            return new BiomePalette(stages, defaultBiomes);
        }

        public static class StageEntry {
            public final String stageName;
            public final List<ResourceLocation> biomes;

            public StageEntry(String stageName, List<ResourceLocation> biomes) {
                this.stageName = stageName;
                this.biomes = biomes;
            }

            public static StageEntry fromJson(JsonObject json) {
                String stageName = json.get("stage").getAsString();
                List<ResourceLocation> biomes = new ArrayList<>();
                for (JsonElement elem : json.getAsJsonArray("biomes")) {
                    biomes.add(new ResourceLocation(elem.getAsString()));
                }
                return new StageEntry(stageName, biomes);
            }
        }
    }

    public static class StructureThemeSet {
        public final Map<String, String> structureTextures;
        public final float particleDensity;

        public StructureThemeSet(Map<String, String> structureTextures, float particleDensity) {
            this.structureTextures = structureTextures;
            this.particleDensity = particleDensity;
        }

        public static StructureThemeSet fromJson(JsonObject json) {
            Map<String, String> textures = new HashMap<>();
            if (json.has("texture_overrides")) {
                JsonObject overrides = json.getAsJsonObject("texture_overrides");
                for (String key : overrides.keySet()) {
                    textures.put(key, overrides.get(key).getAsString());
                }
            }
            float particleDensity = json.has("particle_density") ? json.get("particle_density").getAsFloat() : 1.0f;
            return new StructureThemeSet(textures, particleDensity);
        }
    }

    public static class SkyboxProfile {
        public final int skyColor;
        public final int fogColor;
        public final float fogDensity;
        public final String renderMode;

        public SkyboxProfile(int skyColor, int fogColor, float fogDensity, String renderMode) {
            this.skyColor = skyColor;
            this.fogColor = fogColor;
            this.fogDensity = fogDensity;
            this.renderMode = renderMode;
        }

        public static SkyboxProfile fromJson(JsonObject json) {
            int skyColor = parseHexColor(json.get("sky_color").getAsString());
            int fogColor = parseHexColor(json.get("fog_color").getAsString());
            float fogDensity = json.get("fog_density").getAsFloat();
            String renderMode = json.has("render_mode") ? json.get("render_mode").getAsString() : "default";
            return new SkyboxProfile(skyColor, fogColor, fogDensity, renderMode);
        }

        private static int parseHexColor(String hex) {
            return (int) Long.parseLong(hex.replace("#", ""), 16);
        }
    }

    public static class ColorProgression {
        public final String milestone;
        public final int grassColor;
        public final int waterColor;
        public final int foliageColor;

        public ColorProgression(String milestone, int grassColor, int waterColor, int foliageColor) {
            this.milestone = milestone;
            this.grassColor = grassColor;
            this.waterColor = waterColor;
            this.foliageColor = foliageColor;
        }

        public static ColorProgression fromJson(JsonObject json) {
            String milestone = json.get("milestone").getAsString();
            int grassColor = parseHexColor(json.get("grass_color").getAsString());
            int waterColor = parseHexColor(json.get("water_color").getAsString());
            int foliageColor = parseHexColor(json.get("foliage_color").getAsString());
            return new ColorProgression(milestone, grassColor, waterColor, foliageColor);
        }

        private static int parseHexColor(String hex) {
            return (int) Long.parseLong(hex.replace("#", ""), 16);
        }
    }
}
