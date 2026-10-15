package net.phoenix.core.common.data.worldgen;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGenLayers;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils;
import com.gregtechceu.gtceu.api.data.worldgen.generator.indicators.SurfaceIndicatorGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.generator.veins.DikeVeinGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.generator.veins.LayeredVeinGenerator;
import com.gregtechceu.gtceu.api.data.worldgen.generator.veins.VeinedVeinGenerator;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Swaps every ore vein's surface indicators for crystal roses once the vein registry has loaded. GTCEu 1.21 loads veins
 * from data, so this runs as a reload listener instead of the old hook at the end of GTOres.init.
 */
public class CrystalRoseVeinHook extends ContextAwareReloadListener implements PreparableReloadListener {

    public static final CrystalRoseVeinHook INSTANCE = new CrystalRoseVeinHook();

    private CrystalRoseVeinHook() {}

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return stage.wait(null).thenRunAsync(this::apply, gameExecutor);
    }

    private void apply() {
        var lookup = getRegistryLookup().lookupOrThrow(GTRegistries.Keys.ORE_VEIN);
        RandomSource dummyRandom = RandomSource.create();

        for (var holder : lookup.listElements().toList()) {
            GTOreDefinition def = holder.value();
            Material primaryMat = null;

            for (var ig : def.indicatorGenerators()) {
                if (ig instanceof SurfaceIndicatorGenerator sig) {
                    var blockEither = sig.block();
                    if (blockEither != null && blockEither.right().isPresent()) {
                        primaryMat = blockEither.right().get();
                        break;
                    }
                }
            }

            if (primaryMat == null) {
                var generator = def.veinGenerator();

                if (generator instanceof LayeredVeinGenerator layered) {
                    var patterns = layered.getLayerPatterns();
                    if (!patterns.isEmpty()) {
                        for (var pattern : patterns) {
                            for (var layer : pattern.layers) {
                                var rolled = layer.rollBlock(dummyRandom);
                                if (rolled.right().isPresent()) {
                                    primaryMat = rolled.right().get();
                                    break;
                                }
                            }
                            if (primaryMat != null) break;
                        }
                    }
                } else if (generator instanceof DikeVeinGenerator dike) {
                    if (dike.blocks != null && !dike.blocks.isEmpty()) {
                        var rolled = dike.blocks.get(0).block();
                        if (rolled.right().isPresent()) primaryMat = rolled.right().get();
                    }
                } else if (generator instanceof VeinedVeinGenerator veined) {
                    if (veined.oreBlocks != null && !veined.oreBlocks.isEmpty()) {
                        var rolled = veined.oreBlocks.get(0).block();
                        if (rolled.right().isPresent()) primaryMat = rolled.right().get();
                    }
                }
            }

            if (primaryMat != null) {
                def.indicatorGenerators().clear();

                SurfaceIndicatorGenerator.IndicatorPlacement placement;
                if (def.layer() == WorldGenLayers.NETHERRACK) {
                    placement = SurfaceIndicatorGenerator.IndicatorPlacement.BELOW;
                } else {
                    placement = SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE;
                }

                def.indicatorGenerators().add(new CrystalRoseIndicatorGenerator(def)
                        .state(getCrystalRoseState(primaryMat))
                        .placement(placement)
                        .radius(3)
                        .density(0.4f));
            }
        }
        WorldGeneratorUtils.invalidateOreVeinCache();
    }

    private static BlockState getCrystalRoseState(Material material) {
        var roseEntry = GTMaterialBlocks.MATERIAL_BLOCKS.get(PhoenixMaterialFlags.crystal_rose, material);
        return roseEntry != null ? roseEntry.get().defaultBlockState() : Blocks.POPPY.defaultBlockState();
    }
}
