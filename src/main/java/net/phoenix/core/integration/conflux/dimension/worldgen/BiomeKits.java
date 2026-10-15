package net.phoenix.core.integration.conflux.dimension.worldgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BiomeKits {

    public record Kit(List<WorldgenProfile.TreeConfig> trees, List<String> flowers, List<String> shrubs,
                      float treeMul, float flowerMul, float shrubMul) {}

    private static final Map<String, Kit> KITS = new HashMap<>();

    private static WorldgenProfile.TreeConfig tree(String type, int min, int max, float freq) {
        return new WorldgenProfile.TreeConfig(type, min, max, freq);
    }

    private static void kit(String biomeId, List<WorldgenProfile.TreeConfig> trees, List<String> flowers,
                            List<String> shrubs, float treeMul, float flowerMul, float shrubMul) {
        KITS.put(biomeId, new Kit(trees, flowers, shrubs, treeMul, flowerMul, shrubMul));
    }

    static {

        kit("phoenix_plains", List.of(tree("phoenix_oak", 4, 7, 0.6f), tree("fire_tree", 5, 8, 0.12f)),
                List.of("fire_flower", "minecraft:poppy", "minecraft:orange_tulip"),
                List.of("minecraft:grass", "minecraft:fern"), 0.5f, 1.6f, 1.6f);
        kit("phoenix_peaks", List.of(tree("obsidian_spire", 5, 12, 0.5f)), List.of(), List.of(), 0.45f, 0f, 0f);
        kit("phoenix_crimson", List.of(tree("fire_tree", 6, 10, 0.5f), tree("dead_tree", 4, 8, 0.2f)),
                List.of("minecraft:crimson_roots", "minecraft:crimson_fungus"), List.of("minecraft:nether_sprouts"),
                1.3f, 1.4f, 1.2f);
        kit("phoenix_ashlands", List.of(tree("charred_stump", 2, 5, 0.6f)), List.of(), List.of(), 0.7f, 0f, 0f);
        kit("phoenix_sulfur_fields", List.of(), List.of(), List.of("ash_shrub"), 0f, 0f, 1.8f);
        kit("phoenix_basalt_flats", List.of(tree("basalt_column", 4, 11, 0.5f)), List.of(), List.of(), 0.5f, 0f, 0f);
        kit("phoenix_scorched_grove", List.of(tree("dead_tree", 5, 9, 0.6f), tree("phoenix_oak", 4, 7, 0.3f)),
                List.of("lava_rose"), List.of("ash_shrub", "minecraft:fern"), 2.0f, 0.5f, 1.2f);

        kit("sculk_forest", List.of(tree("moss_oak", 5, 8, 0.6f), tree("sculk_tree", 4, 7, 0.15f)),
                List.of("minecraft:flowering_azalea", "minecraft:lily_of_the_valley"),
                List.of("minecraft:fern", "sculk_moss"), 1.5f, 1.2f, 1.4f);
        kit("sculk_depths", List.of(tree("sculk_spire", 4, 10, 0.5f)), List.of(), List.of(), 0.45f, 0f, 0f);
        kit("sculk_shriek", List.of(tree("sculk_spire", 3, 8, 0.4f)), List.of(), List.of("sculk_carpet"), 0.3f, 0f,
                1.5f);
        kit("sculk_hollow", List.of(tree("mushroom_tree", 5, 9, 0.6f)),
                List.of("minecraft:red_mushroom", "minecraft:brown_mushroom"), List.of("minecraft:fern"), 1.4f, 1.6f,
                0.8f);
        kit("sculk_marsh", List.of(tree("marsh_tree", 4, 7, 0.5f)),
                List.of("minecraft:blue_orchid", "minecraft:brown_mushroom"), List.of("minecraft:fern"), 0.9f, 1.4f,
                1.0f);
        kit("sculk_thicket", List.of(tree("thicket_pine", 7, 12, 0.7f), tree("moss_oak", 4, 7, 0.2f)),
                List.of("minecraft:fern", "minecraft:flowering_azalea"),
                List.of("minecraft:fern", "minecraft:moss_carpet"),
                2.3f, 0.6f, 1.8f);
        kit("sculk_ridge", List.of(tree("sculk_spire", 3, 7, 0.3f)), List.of(), List.of(), 0.25f, 0f, 0f);

        kit("void_island", List.of(tree("cherry_tree", 5, 8, 0.6f)),
                List.of("void_flower", "minecraft:azure_bluet", "minecraft:cornflower"),
                List.of("minecraft:grass", "minecraft:fern"), 0.9f, 1.8f, 1.4f);
        kit("void_cosmic", List.of(tree("crystalline_tree", 7, 14, 0.5f)), List.of(), List.of("void_shard"), 0.4f, 0f,
                0.5f);
        kit("void_crystal_field", List.of(tree("crystalline_tree", 5, 11, 0.4f)), List.of("ethereal_crystal"),
                List.of("ethereal_crystal"), 0.6f, 1.6f, 1.0f);
        kit("void_amethyst_grove", List.of(tree("amethyst_spire", 5, 10, 0.7f)), List.of("ethereal_crystal"),
                List.of("ethereal_crystal"), 1.5f, 2.0f, 1.2f);
        kit("void_starlit_meadow", List.of(tree("frost_pine", 5, 9, 0.6f)), List.of(), List.of(), 0.9f, 0f, 0f);
        kit("void_lavender_fields", List.of(), List.of("ethereal_crystal"), List.of("ethereal_crystal"), 0f, 2.0f,
                1.5f);
        kit("void_drift", List.of(), List.of(), List.of("minecraft:chorus_flower"), 0f, 0f, 0.7f);

        kit("sealed_industrial", List.of(tree("birch_grove", 5, 8, 0.5f), tree("metal_tree", 5, 8, 0.15f),
                tree("rubber_tree", 5, 8, 0.12f)), List.of("copper_flower", "minecraft:dandelion"),
                List.of("minecraft:grass", "copper_moss"), 0.8f, 1.0f, 1.2f);
        kit("sealed_vault", List.of(tree("antenna", 6, 14, 0.5f)), List.of(), List.of(), 0.3f, 0f, 0f);
        kit("sealed_scrapyard", List.of(tree("rusted_pylon", 3, 8, 0.6f), tree("metal_tree", 4, 7, 0.1f)), List.of(),
                List.of(), 0.7f, 0f, 0f);
        kit("sealed_concrete_flats", List.of(tree("antenna", 5, 10, 0.4f)), List.of(), List.of(), 0.15f, 0f, 0f);
        kit("sealed_rust_fields", List.of(tree("rusted_pylon", 2, 5, 0.5f)), List.of(), List.of(), 0.5f, 0f, 0f);
        kit("sealed_overgrown_ruins", List.of(tree("birch_grove", 5, 9, 0.5f), tree("rubber_tree", 5, 8, 0.3f)),
                List.of("minecraft:lily_of_the_valley", "minecraft:azure_bluet", "minecraft:dandelion"),
                List.of("minecraft:fern", "minecraft:moss_carpet"), 2.0f, 1.6f, 1.6f);
        kit("sealed_ash_yard", List.of(tree("charred_stump", 2, 5, 0.6f)), List.of(), List.of(), 0.6f, 0f, 0f);

        kit("sealed_nether", List.of(tree("bone_tree", 4, 9, 0.5f)), List.of(), List.of("soul_vines"), 0.5f, 0f, 0.6f);
        kit("sealed_warped", List.of(tree("warped_tree", 5, 9, 0.6f)),
                List.of("minecraft:warped_roots", "minecraft:warped_fungus"), List.of("minecraft:nether_sprouts"), 1.2f,
                1.5f, 1.3f);
        kit("sealed_crimson_zone", List.of(tree("fire_tree", 5, 9, 0.5f), tree("dead_tree", 4, 7, 0.2f)),
                List.of("minecraft:crimson_roots", "minecraft:crimson_fungus"), List.of(), 1.1f, 1.5f, 0f);
        kit("sealed_basalt_wastes", List.of(tree("basalt_column", 5, 14, 0.6f)), List.of(), List.of(), 0.8f, 0f, 0f);
        kit("sealed_soul_valley", List.of(tree("bone_tree", 5, 11, 0.5f)),
                List.of("minecraft:soul_fire", "soul_lantern"), List.of("minecraft:nether_sprouts"), 0.8f, 0.6f, 1.0f);
        kit("sealed_warped_depths", List.of(tree("warped_tree", 7, 13, 0.7f)), List.of(), List.of(), 1.8f, 0f, 0f);
        kit("sealed_glowstone_caverns", List.of(tree("glow_tree", 5, 9, 0.6f)), List.of(), List.of(), 1.3f, 0f, 0f);
    }

    private BiomeKits() {}

    public static Kit get(String biomeId) {
        return biomeId == null ? null : KITS.get(biomeId);
    }
}
