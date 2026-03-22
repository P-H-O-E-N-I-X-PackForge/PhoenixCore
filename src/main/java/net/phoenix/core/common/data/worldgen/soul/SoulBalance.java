package net.phoenix.core.common.data.worldgen.soul;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

public class SoulBalance {

    public record SoulProfile(float minSoul, float maxSoul, float regenPerTick) {}

    private static final Map<ResourceLocation, SoulProfile> REGISTRY = new HashMap<>();

    // This is your catch-all for biomes like Plains, Forests, etc.
    private static final SoulProfile DEFAULT = new SoulProfile(0.8f, 1.2f, 0.001f);

    // This is a "Smart Fallback" for modded biomes that have magical tags
    // but aren't manually registered by you yet.
    private static final SoulProfile MAGICAL_FALLBACK = new SoulProfile(1.5f, 2.0f, 0.003f);

    static {
        // Manual Fine-Grained Overrides
        register("minecraft:old_growth_birch_forest", 1.8f, 2.5f, 0.004f);
        register("minecraft:ocean", 0.3f, 0.7f, 0.001f);
        register("ars_nouveau:vibrant_ecosystem", 2.0f, 4.0f, 0.008f);
    }

    private static void register(String id, float min, float max, float regen) {
        REGISTRY.put(new ResourceLocation(id), new SoulProfile(min, max, regen));
    }

    /**
     * Gets the profile for a biome.
     * Priority: Manual Registry -> Biome Tag Check -> Default
     */
    public static SoulProfile get(Holder<Biome> biomeHolder) {
        // 1. Check Manual Registry first
        ResourceLocation id = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
        if (id != null && REGISTRY.containsKey(id)) {
            return REGISTRY.get(id);
        }

        // 2. Check for "Magical" tags (Supports mods like Biomes O' Plenty or Oh The Biomes You'll Go)
        // You can check for Ars Nouveau tags or standard Forge/NeoForge tags
        if (biomeHolder.is(TagKey.create(Registries.BIOME, new ResourceLocation("forge", "is_magical"))) ||
                biomeHolder.is(TagKey.create(Registries.BIOME, new ResourceLocation("ars_nouveau", "vibrant")))) {
            return MAGICAL_FALLBACK;
        }

        // 3. Absolute Fallback
        return DEFAULT;
    }
}
