package net.phoenix.core.integration.conflux.dimension.worldgen;

import com.gregtechceu.gtceu.api.data.worldgen.SimpleWorldGenLayer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.dimension.ConfluxDimensionFactory;

import java.util.Set;

/**
 * GT's own {@code WorldGenLayers.STONE} is hardcoded to {@code Set.of(Level.OVERWORLD.location())} - checked in
 * {@code OreGenerator#getEntries} before a vein's own {@code dimensions()} filter is ever consulted. Every
 * discipline vein in {@code DefaultDisciplineOres} using that layer was therefore filtered out before candidate
 * selection even began, no matter how correctly its own dimension set was configured - GT never even looked at
 * Conflux dimensions for a "stone" layer.
 * <p>
 * This registers our own layer, using the same replaceable-block target (Conflux terrain is real
 * {@code Blocks.STONE}), but applicable to every discipline's shared dimension instead.
 */
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

    /** Forces this class to load (and its fields' constructors, which self-register into GT's registry) to run. */
    public static void init() {}
}
