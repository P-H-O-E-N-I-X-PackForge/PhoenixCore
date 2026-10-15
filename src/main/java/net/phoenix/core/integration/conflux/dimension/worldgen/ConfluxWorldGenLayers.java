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

    private static Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> confluxDimensionLocations() {
        var locations = new java.util.HashSet<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>>();
        for (String disciplineId : DISCIPLINE_IDS) {
            locations.add(ConfluxDimensionFactory.getDimensionKey(disciplineId));
        }
        return Set.copyOf(locations);
    }

    public static void init() {}
}
