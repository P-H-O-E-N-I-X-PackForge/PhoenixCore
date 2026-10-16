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
import net.phoenix.core.PhoenixAPI;
import net.phoenix.core.common.data.materials.PhoenixMaterialFlags;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
            try {
                processVein(holder.value(), dummyRandom);
            } catch (Exception e) {

                PhoenixAPI.LOGGER.error("CrystalRoseVeinHook failed for vein '{}'", holder.key().location(), e);
            }
        }
        WorldGeneratorUtils.invalidateOreVeinCache();
    }

    private void processVein(GTOreDefinition def, RandomSource dummyRandom) {
        Material primaryMat = findPrimaryMaterial(def, dummyRandom);
        if (primaryMat == null) return;

        SurfaceIndicatorGenerator.IndicatorPlacement placement = def.layer() == WorldGenLayers.NETHERRACK ?
                SurfaceIndicatorGenerator.IndicatorPlacement.BELOW : SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE;

        var rose = new CrystalRoseIndicatorGenerator()
                .state(getCrystalRoseState(primaryMat))
                .placement(placement)
                .radius(3)
                .density(0.4f);

        setIndicators(def, rose);
    }

    private static Material findPrimaryMaterial(GTOreDefinition def, RandomSource dummyRandom) {
        for (var ig : def.indicatorGenerators()) {
            if (ig instanceof SurfaceIndicatorGenerator sig) {
                var blockEither = sig.block();
                if (blockEither != null && blockEither.right().isPresent()) {
                    return blockEither.right().get();
                }
            }
        }

        var generator = def.veinGenerator();

        if (generator instanceof LayeredVeinGenerator layered) {
            var patterns = layered.getLayerPatterns();
            if (patterns != null) {
                for (var pattern : patterns) {
                    for (var layer : pattern.layers) {
                        var rolled = layer.rollBlock(dummyRandom);
                        if (rolled.right().isPresent()) {
                            return rolled.right().get();
                        }
                    }
                }
            }
        } else if (generator instanceof DikeVeinGenerator dike) {
            if (dike.blocks != null && !dike.blocks.isEmpty()) {
                var rolled = dike.blocks.get(0).block();
                if (rolled.right().isPresent()) return rolled.right().get();
            }
        } else if (generator instanceof VeinedVeinGenerator veined) {
            if (veined.oreBlocks != null && !veined.oreBlocks.isEmpty()) {
                var rolled = veined.oreBlocks.get(0).block();
                if (rolled.right().isPresent()) return rolled.right().get();
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setIndicators(GTOreDefinition def, Object indicator) {
        List current = def.indicatorGenerators();
        try {
            current.clear();
            current.add(indicator);
            return;
        } catch (UnsupportedOperationException ignored) {

        }

        try {
            Field f = GTOreDefinition.class.getDeclaredField("indicatorGenerators");
            f.setAccessible(true);
            List replacement = new ArrayList();
            replacement.add(indicator);
            f.set(def, replacement);
        } catch (ReflectiveOperationException | RuntimeException e) {
            PhoenixAPI.LOGGER.error(
                    "CrystalRoseVeinHook: could not replace indicator generators " +
                            "(GTOreDefinition may be a record; use an accessor mixin instead)",
                    e);
        }
    }

    private static BlockState getCrystalRoseState(Material material) {
        var roseEntry = GTMaterialBlocks.MATERIAL_BLOCKS.get(PhoenixMaterialFlags.crystal_rose, material);
        return roseEntry != null ? roseEntry.get().defaultBlockState() : Blocks.POPPY.defaultBlockState();
    }
}
