package net.phoenix.core.integration.conflux.dimension.worldgen;

import com.gregtechceu.gtceu.api.data.worldgen.SimpleWorldGenLayer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.dimension.ConfluxDimensionFactory;

import java.util.Set;

public class ConfluxWorldGenLayers {

    private static final String[] DISCIPLINE_IDS = { "phoenix", "sculk", "void", "sealed_a", "sealed_b" };

    public static final SimpleWorldGenLayer CONFLUX_STONE = new SimpleWorldGenLayer(
            PhoenixCore.id("conflux_stone"),
            () -> new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
            confluxDimensionLocations());

    private static Set<ResourceLocation> confluxDimensionLocations() {
        var locations = new java.util.HashSet<ResourceLocation>();
        for (String disciplineId : DISCIPLINE_IDS) {
            locations.add(ConfluxDimensionFactory.getDimensionKey(disciplineId).location());
        }
        return Set.copyOf(locations);
    }

    public static void init() {}
}
