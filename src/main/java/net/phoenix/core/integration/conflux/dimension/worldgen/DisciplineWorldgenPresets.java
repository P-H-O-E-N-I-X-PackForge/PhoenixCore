package net.phoenix.core.integration.conflux.dimension.worldgen;

import java.util.*;

public class DisciplineWorldgenPresets {

    private static final Map<String, WorldgenProfile> PRESETS = new HashMap<>();

    static {
        
        PRESETS.put("phoenix", createPhoenixWorldgen());
        PRESETS.put("sculk", createSculkWorldgen());
        PRESETS.put("void", createVoidWorldgen());
        PRESETS.put("sealed_a", createSealedAWorldgen());
        PRESETS.put("sealed_b", createSealedBWorldgen());
    }

    public static WorldgenProfile getPreset(String disciplineId) {
        return PRESETS.getOrDefault(disciplineId, createDefaultWorldgen(disciplineId));
    }

    private static WorldgenProfile createPhoenixWorldgen() {
        return new WorldgenProfileBuilder("phoenix")
            .terrainFull(
                32, 128,           
                72,                
                2.0f,              
                1.5f,              
                0.7f,              
                false, true, false 
            )
            .biomes(
                Arrays.asList(
                    new WorldgenProfile.BiomeDefinition(
                        "phoenix_plains", "Ashfield Plains",
                        1.2f, 0.3f,
                        0xD4A574, 0xB8860B, 0xA0522D,
                        "minecraft:grass_block", "minecraft:dirt",
                        0.2f, Arrays.asList("phoenix_plains")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "phoenix_peaks", "Obsidian Peaks",
                        0.5f, 0.0f,
                        0x2F4F4F, 0x1C1C1C, 0x0A0A0A,
                        "minecraft:obsidian", "minecraft:blackstone",
                        0.0f, Arrays.asList("phoenix_peaks")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "phoenix_crimson", "Crimson Wastes",
                        2.0f, 0.0f,
                        0xDC143C, 0x8B0000, 0x4B0000,
                        "minecraft:crimson_nylium", "minecraft:netherrack",
                        0.0f, Arrays.asList("phoenix_crimson")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "phoenix_ashlands", "Ashlands",
                        1.4f, 0.1f,
                        0x707070, 0x505050, 0x606060,
                        "minecraft:gravel", "minecraft:stone",
                        0.0f, Arrays.asList("phoenix_ashlands")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "phoenix_sulfur_fields", "Sulfur Fields",
                        1.6f, 0.0f,
                        0xE0C040, 0xC89020, 0xB08010,
                        "minecraft:yellow_terracotta", "minecraft:orange_terracotta",
                        0.0f, Arrays.asList("phoenix_sulfur_fields")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "phoenix_basalt_flats", "Basalt Flats",
                        1.3f, 0.0f,
                        0x4A4A4A, 0x3A3A3A, 0x2A2A2A,
                        "minecraft:basalt", "minecraft:smooth_basalt",
                        0.0f, Arrays.asList("phoenix_basalt_flats")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "phoenix_scorched_grove", "Scorched Grove",
                        1.1f, 0.25f,
                        0x8B7355, 0x6B5335, 0x5B4325,
                        "minecraft:coarse_dirt", "minecraft:dirt",
                        0.15f, Arrays.asList("phoenix_scorched_grove")
                    )
                ),
                "phoenix_plains",
                0xD4A574, 0xB8860B, 0xA0522D, 0xFF6347, 0xFF8C00
            )
            .decorations(
                Arrays.asList(
                    new WorldgenProfile.TreeConfig("phoenix_oak", 4, 7, 0.5f),
                    new WorldgenProfile.TreeConfig("dead_tree", 4, 7, 0.2f),
                    new WorldgenProfile.TreeConfig("fire_tree", 5, 9, 0.15f),
                    new WorldgenProfile.TreeConfig("rubber_tree", 5, 8, 0.12f)
                ),
                Arrays.asList("fire_flower", "lava_rose"),
                Arrays.asList("ash_shrub"),
                Arrays.asList("lava_pool", "crystal_formation"),
                0.7f, 0.4f, 0.5f
            )
            .cavesFull(
                true, 0.8f, 0.7f,
                8, 20,
                true, true, false,  
                0.3f                
            )
            .liquidsFull(
                0.05f, 0.15f,       
                64, 32,
                false, true,        
                0.05f, 0.2f
            )
            .colors(0xD4A574, 0xB8860B, 0xA0522D, 0xFF6347, 0xFF8C00)
            .build();
    }

    private static WorldgenProfile createSculkWorldgen() {
        return new WorldgenProfileBuilder("sculk")
            .terrainFull(
                0, 64,             
                32,                
                0.8f,              
                0.8f,              
                0.9f,              
                false, false, true 
            )
            .biomes(
                Arrays.asList(
                    new WorldgenProfile.BiomeDefinition(
                        "sculk_forest", "Sculk Forest",
                        0.0f, 1.0f,
                        0x2F4F4F, 0x1C1C1C, 0x000080,
                        "minecraft:moss_block", "minecraft:dirt",
                        0.8f, Arrays.asList("sculk_forest")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sculk_depths", "Void Depths",
                        -0.5f, 0.8f,
                        0x1A1A2E, 0x0F3460, 0x0A0A1A,
                        "minecraft:deepslate", "minecraft:tuff",
                        0.6f, Arrays.asList("sculk_depths")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sculk_shriek", "Shrieker Cavern",
                        0.1f, 0.9f,
                        0x4B0082, 0x8B00FF, 0x2F0854,
                        "minecraft:sculk_shrieker", "minecraft:sculk_catalyst",
                        0.9f, Arrays.asList("sculk_shriek")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sculk_hollow", "Fungal Hollow",
                        0.2f, 0.9f,
                        0x4A3B2A, 0x3A2B1A, 0x2A1B0A,
                        "minecraft:podzol", "minecraft:dirt",
                        0.7f, Arrays.asList("sculk_hollow")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sculk_marsh", "Sculk Marsh",
                        0.3f, 1.0f,
                        0x4A5A3A, 0x3A4A2A, 0x2A3A1A,
                        "minecraft:mud", "minecraft:clay",
                        0.9f, Arrays.asList("sculk_marsh")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sculk_thicket", "Deep Thicket",
                        0.1f, 1.0f,
                        0x2A4A2A, 0x1A3A1A, 0x0A2A0A,
                        "minecraft:moss_block", "minecraft:rooted_dirt",
                        0.85f, Arrays.asList("sculk_thicket")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sculk_ridge", "Deepslate Ridge",
                        -0.3f, 0.6f,
                        0x3A3A3A, 0x2A2A2A, 0x1A1A1A,
                        "minecraft:deepslate_tiles", "minecraft:deepslate",
                        0.5f, Arrays.asList("sculk_ridge")
                    )
                ),
                "sculk_forest",
                0x2F4F4F, 0x1C1C1C, 0x000080, 0x1A1A2E, 0x0F3460
            )
            .decorations(
                Arrays.asList(
                    new WorldgenProfile.TreeConfig("moss_oak", 4, 7, 0.5f),
                    new WorldgenProfile.TreeConfig("sculk_tree", 3, 6, 0.15f),
                    new WorldgenProfile.TreeConfig("rubber_tree", 5, 8, 0.12f)
                ),
                Arrays.asList("sculk_flower", "glowing_vine"),
                Arrays.asList("sculk_moss", "sculk_carpet"),
                Arrays.asList("sculk_shrieker", "sculk_vein"),
                0.6f, 0.5f, 0.6f
            )
            .cavesFull(
                true, 1.0f, 0.9f,  
                4, 18,
                false, true, true, 
                0.05f              
            )
            .liquidsFull(
                0.2f, 0.01f,       
                32, 16,
                true, false,       
                0.3f, 0.01f
            )
            .colors(0x2F4F4F, 0x1C1C1C, 0x000080, 0x1A1A2E, 0x0F3460)
            .build();
    }

    private static WorldgenProfile createVoidWorldgen() {
        return new WorldgenProfileBuilder("void")
            .terrainFull(
                40, 120,           
                80,                
                2.5f,              
                2.0f,              
                0.8f,              
                false, false, false
            )
            .biomes(
                Arrays.asList(
                    new WorldgenProfile.BiomeDefinition(
                        "void_island", "Ethereal Meadow",
                        0.5f, 0.4f,
                        0x4B0082, 0x9932CC, 0x1A0033,
                        "minecraft:grass_block", "minecraft:dirt",
                        0.3f, Arrays.asList("void_island")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "void_cosmic", "Cosmic Void",
                        -0.7f, 0.0f,
                        0x00008B, 0x000080, 0x000033,
                        "minecraft:blackstone", "minecraft:deepslate",
                        0.0f, Arrays.asList("void_cosmic")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "void_crystal_field", "Crystal Field",
                        0.4f, 0.2f,
                        0xC0C0D0, 0xA0A0C0, 0x8080A0,
                        "minecraft:calcite", "minecraft:diorite",
                        0.1f, Arrays.asList("void_crystal_field")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "void_amethyst_grove", "Amethyst Grove",
                        0.5f, 0.3f,
                        0x9370DB, 0x8360CB, 0x7350BB,
                        "minecraft:quartz_block", "minecraft:diorite",
                        0.2f, Arrays.asList("void_amethyst_grove")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "void_starlit_meadow", "Starlit Meadow",
                        0.3f, 0.4f,
                        0xE0E0F0, 0xC0C0E0, 0xA0A0D0,
                        "minecraft:snow_block", "minecraft:packed_ice",
                        0.4f, Arrays.asList("void_starlit_meadow")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "void_lavender_fields", "Lavender Fields",
                        0.6f, 0.35f,
                        0xB19CD9, 0x9B7FC7, 0x8560B5,
                        "minecraft:purple_terracotta", "minecraft:purple_concrete",
                        0.25f, Arrays.asList("void_lavender_fields")
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "void_drift", "Cosmic Drift",
                        -0.6f, 0.0f,
                        0xD8D8C8, 0xC8C8B8, 0xB8B8A8,
                        "minecraft:end_stone", "minecraft:end_stone_bricks",
                        0.0f, Arrays.asList("void_drift")
                    )
                ),
                "void_island",
                0x4B0082, 0x9932CC, 0x1A0033, 0x191970, 0x000033
            )
            .decorations(
                Arrays.asList(
                    new WorldgenProfile.TreeConfig("cherry_tree", 5, 8, 0.45f),
                    new WorldgenProfile.TreeConfig("crystalline_tree", 6, 12, 0.15f),
                    new WorldgenProfile.TreeConfig("rubber_tree", 5, 8, 0.12f)
                ),
                Arrays.asList("void_flower", "ethereal_crystal"),
                Arrays.asList("void_shard"),
                Arrays.asList("floating_island", "cosmic_formation"),
                0.5f, 0.3f, 0.35f
            )
            .cavesFull(
                true, 0.3f, 0.4f,
                6, 14,
                false, false, true, 
                0.0f               
            )
            .liquidsFull(
                0.0f, 0.0f,        
                80, 50,
                false, false,
                0.0f, 0.0f
            )
            .colors(0x4B0082, 0x9932CC, 0x1A0033, 0x191970, 0x000033)
            .build();
    }

    private static WorldgenProfile createSealedAWorldgen() {
        return new WorldgenProfileBuilder("sealed_a")
            .terrainFull(
                30, 100,
                68,
                1.2f, 1.0f, 0.6f,
                false, false, false
            )
            .biomes(
                Arrays.asList(
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_industrial", "Overgrown Industrial Zone",
                        0.8f, 0.3f,
                        0x7F7F7F, 0x696969, 0xA9A9A9,
                        "minecraft:grass_block", "minecraft:dirt",
                        0.3f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_vault", "Vault",
                        0.5f, 0.0f,
                        0x4F4F4F, 0x2F2F2F, 0x808080,
                        "minecraft:iron_block", "minecraft:raw_iron_block",
                        0.0f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_scrapyard", "Scrapyard",
                        0.75f, 0.1f,
                        0xB87333, 0x9C6428, 0x805520,
                        "minecraft:copper_block", "minecraft:cut_copper",
                        0.1f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_concrete_flats", "Concrete Flats",
                        0.7f, 0.15f,
                        0x9F9F9F, 0x8F8F8F, 0x7F7F7F,
                        "minecraft:gray_concrete", "minecraft:light_gray_concrete",
                        0.15f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_rust_fields", "Rust Fields",
                        0.65f, 0.2f,
                        0x6E8B6E, 0x5E7B5E, 0x4E6B4E,
                        "minecraft:oxidized_copper", "minecraft:weathered_copper",
                        0.2f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_overgrown_ruins", "Overgrown Ruins",
                        0.8f, 0.4f,
                        0x5A7A4A, 0x4A6A3A, 0x3A5A2A,
                        "minecraft:moss_block", "minecraft:stone_bricks",
                        0.35f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_ash_yard", "Ash Yard",
                        0.6f, 0.05f,
                        0x2F2F2F, 0x1F1F1F, 0x0F0F0F,
                        "minecraft:blackstone", "minecraft:polished_blackstone",
                        0.0f
                    )
                ),
                "sealed_industrial",
                0x7F7F7F, 0x696969, 0xA9A9A9, 0x808080, 0x696969
            )
            .decorations(
                Arrays.asList(
                    new WorldgenProfile.TreeConfig("birch_grove", 5, 8, 0.45f),
                    new WorldgenProfile.TreeConfig("metal_tree", 5, 8, 0.15f),
                    new WorldgenProfile.TreeConfig("rubber_tree", 5, 8, 0.12f)
                ),
                Arrays.asList("copper_flower"),
                Arrays.asList("copper_moss"),
                Arrays.asList("copper_formation"),
                0.6f, 0.2f, 0.4f
            )
            .cavesFull(
                true, 0.6f, 0.5f,
                5, 12,
                true, true, true,
                0.1f
            )
            .liquidsFull(
                0.08f, 0.04f,
                64, 32,
                false, false,
                0.08f, 0.04f
            )
            .colors(0x7F7F7F, 0x696969, 0xA9A9A9, 0x808080, 0x696969)
            .build();
    }

    private static WorldgenProfile createSealedBWorldgen() {
        return new WorldgenProfileBuilder("sealed_b")
            .terrainFull(
                20, 110,
                65,
                1.5f, 1.2f, 0.7f,
                false, false, false
            )
            .biomes(
                Arrays.asList(
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_nether", "Netherlike",
                        1.5f, 0.0f,
                        0x8B4513, 0xA0522D, 0x654321,
                        "minecraft:soul_sand", "minecraft:netherrack",
                        0.0f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_warped", "Warped Zone",
                        0.9f, 0.1f,
                        0x00CED1, 0x20B2AA, 0x008B8B,
                        "minecraft:warped_nylium", "minecraft:netherrack",
                        0.3f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_crimson_zone", "Crimson Zone",
                        1.4f, 0.05f,
                        0x8B1A1A, 0x6B0A0A, 0x4B0000,
                        "minecraft:crimson_nylium", "minecraft:netherrack",
                        0.0f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_basalt_wastes", "Basalt Wastes",
                        1.3f, 0.0f,
                        0x3A3A3A, 0x2A2A2A, 0x1A1A1A,
                        "minecraft:basalt", "minecraft:blackstone",
                        0.0f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_soul_valley", "Soul Valley",
                        1.0f, 0.0f,
                        0x4A6A6A, 0x3A5A5A, 0x2A4A4A,
                        "minecraft:soul_soil", "minecraft:soul_sand",
                        0.0f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_warped_depths", "Warped Depths",
                        0.85f, 0.15f,
                        0x1E9090, 0x0E7070, 0x005050,
                        "minecraft:warped_wart_block", "minecraft:warped_nylium",
                        0.35f
                    ),
                    new WorldgenProfile.BiomeDefinition(
                        "sealed_glowstone_caverns", "Fungal Caverns",
                        1.1f, 0.1f,
                        0xA05050, 0x804040, 0x603030,
                        "minecraft:nether_wart_block", "minecraft:netherrack",
                        0.1f
                    )
                ),
                "sealed_warped",
                0x8B4513, 0xA0522D, 0x654321, 0xD2691E, 0xA0522D
            )
            .decorations(
                Arrays.asList(
                    new WorldgenProfile.TreeConfig("warped_tree", 4, 8, 0.5f),
                    new WorldgenProfile.TreeConfig("rubber_tree", 5, 8, 0.12f)
                ),
                Arrays.asList("warped_flower", "soul_lantern"),
                Arrays.asList("warped_moss", "soul_vines"),
                Arrays.asList("warped_formation"),
                0.6f, 0.4f, 0.5f
            )
            .cavesFull(
                true, 0.7f, 0.6f,
                6, 16,
                true, true, false,
                0.15f
            )
            .liquidsFull(
                0.1f, 0.12f,
                64, 34,
                false, true,
                0.1f, 0.12f
            )
            .colors(0x8B4513, 0xA0522D, 0x654321, 0xD2691E, 0xA0522D)
            .build();
    }

    private static WorldgenProfile createDefaultWorldgen(String disciplineId) {
        return new WorldgenProfileBuilder(disciplineId).build();
    }
}
