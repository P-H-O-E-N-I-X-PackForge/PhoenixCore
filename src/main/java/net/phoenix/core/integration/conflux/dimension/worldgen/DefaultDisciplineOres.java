package net.phoenix.core.integration.conflux.dimension.worldgen;

import com.gregtechceu.gtceu.api.data.worldgen.GTLayerPattern;
import com.gregtechceu.gtceu.api.data.worldgen.generator.indicators.SurfaceIndicatorGenerator;

import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class DefaultDisciplineOres {

    private static final RuleTest[] STONE_RULES = new RuleTest[] { new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES) };

    public static void registerAll() {
        registerPhoenixOres();
        registerSculkOres();
        registerVoidOres();
        registerSealedAOres();
        registerSealedBOres();
    }

    private static void registerPhoenixOres() {
        GTVeinPlacement.registerVein("phoenix", "iron_tin", def -> def
                .clusterSize(UniformInt.of(30, 45)).density(0.8f).weight(100)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 96)
                .layeredVeinGenerator(gen -> gen.withLayerPattern(() -> GTLayerPattern.builder(STONE_RULES)
                        .layer(l -> l.weight(2).mat(Iron).size(2, 4))
                        .layer(l -> l.weight(1).mat(Tin).size(1, 3))
                        .build()))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Iron)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("phoenix", "copper", def -> def
                .clusterSize(UniformInt.of(24, 36)).density(0.6f).weight(70)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-16, 80)
                .dikeVeinGenerator(gen -> gen.withBlock(Copper, 1, -16, 80))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Copper)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("phoenix", "gold", def -> def
                .clusterSize(UniformInt.of(16, 24)).density(0.4f).weight(35)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 40)
                .veinedVeinGenerator(gen -> gen.oreBlock(Gold, 1))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Gold)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("phoenix", "ruby", def -> def
                .clusterSize(UniformInt.of(6, 10)).density(0.3f).weight(15)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 32)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Ruby).size(2))
                        .middle(b -> b.mat(Ruby).size(3))
                        .bottom(b -> b.mat(Ruby).size(2))
                        .spread(b -> b.mat(Ruby).size(1)))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Ruby)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("phoenix", "sapphire", def -> def
                .clusterSize(UniformInt.of(6, 10)).density(0.3f).weight(15)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 32)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Sapphire).size(2))
                        .middle(b -> b.mat(Sapphire).size(3))
                        .bottom(b -> b.mat(Sapphire).size(2))
                        .spread(b -> b.mat(Sapphire).size(1)))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Sapphire)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));
    }

    private static void registerSculkOres() {
        GTVeinPlacement.registerVein("sculk", "copper", def -> def
                .clusterSize(UniformInt.of(28, 40)).density(0.6f).weight(80)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-40, 60)
                .veinedVeinGenerator(gen -> gen.oreBlock(Copper, 1))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Copper)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sculk", "tin_lead", def -> def
                .clusterSize(UniformInt.of(30, 45)).density(0.7f).weight(70)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-48, 40)
                .layeredVeinGenerator(gen -> gen.withLayerPattern(() -> GTLayerPattern.builder(STONE_RULES)
                        .layer(l -> l.weight(2).mat(Tin).size(2, 4))
                        .layer(l -> l.weight(1).mat(Lead).size(1, 3))
                        .build()))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Tin)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sculk", "nickel", def -> def
                .clusterSize(UniformInt.of(18, 28)).density(0.4f).weight(30)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-48, 16)
                .dikeVeinGenerator(gen -> gen.withBlock(Nickel, 1, -48, 16))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Nickel)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sculk", "diamond", def -> def
                .clusterSize(UniformInt.of(6, 10)).density(0.25f).weight(12)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-56, -16)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Diamond).size(2))
                        .middle(b -> b.mat(Diamond).size(3))
                        .bottom(b -> b.mat(Diamond).size(2))
                        .spread(b -> b.mat(Diamond).size(1)))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Diamond)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sculk", "emerald", def -> def
                .clusterSize(UniformInt.of(6, 10)).density(0.25f).weight(12)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-40, 8)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Emerald).size(2))
                        .middle(b -> b.mat(Emerald).size(3))
                        .bottom(b -> b.mat(Emerald).size(2))
                        .spread(b -> b.mat(Emerald).size(1)))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Emerald)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));
    }

    private static void registerVoidOres() {
        GTVeinPlacement.registerVein("void", "platinum", def -> def
                .clusterSize(UniformInt.of(16, 24)).density(0.35f).weight(30)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 48)
                .dikeVeinGenerator(gen -> gen.withBlock(Platinum, 1, -32, 48))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Platinum)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("void", "iridium", def -> def
                .clusterSize(UniformInt.of(10, 16)).density(0.2f).weight(15)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-48, 24)
                .veinedVeinGenerator(gen -> gen.oreBlock(Iridium, 1)));

        GTVeinPlacement.registerVein("void", "palladium", def -> def
                .clusterSize(UniformInt.of(10, 16)).density(0.2f).weight(15)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-48, 24)
                .veinedVeinGenerator(gen -> gen.oreBlock(Palladium, 1))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Palladium)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("void", "diamond", def -> def
                .clusterSize(UniformInt.of(6, 10)).density(0.25f).weight(10)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-56, -16)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Diamond).size(2))
                        .middle(b -> b.mat(Diamond).size(3))
                        .bottom(b -> b.mat(Diamond).size(2))
                        .spread(b -> b.mat(Diamond).size(1)))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Diamond)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("void", "emerald", def -> def
                .clusterSize(UniformInt.of(6, 10)).density(0.25f).weight(10)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-40, 8)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Emerald).size(2))
                        .middle(b -> b.mat(Emerald).size(3))
                        .bottom(b -> b.mat(Emerald).size(2))
                        .spread(b -> b.mat(Emerald).size(1)))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Emerald)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("void", "chromium", def -> def
                .clusterSize(UniformInt.of(6, 10)).density(0.2f).weight(10)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 32)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Chromium).size(2))
                        .middle(b -> b.mat(Chromium).size(3))
                        .bottom(b -> b.mat(Chromium).size(2))
                        .spread(b -> b.mat(Chromium).size(1))));
    }

    private static void registerSealedAOres() {
        GTVeinPlacement.registerVein("sealed_a", "iron_copper", def -> def
                .clusterSize(UniformInt.of(30, 45)).density(0.8f).weight(100)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 96)
                .layeredVeinGenerator(gen -> gen.withLayerPattern(() -> GTLayerPattern.builder(STONE_RULES)
                        .layer(l -> l.weight(2).mat(Iron).size(2, 4))
                        .layer(l -> l.weight(1).mat(Copper).size(1, 3))
                        .build()))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Iron)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sealed_a", "tin_zinc", def -> def
                .clusterSize(UniformInt.of(24, 36)).density(0.5f).weight(50)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-16, 80)
                .dikeVeinGenerator(gen -> gen
                        .withBlock(Tin, 1, -16, 80)
                        .withBlock(Zinc, 1, -16, 80))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Tin)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sealed_a", "aluminium", def -> def
                .clusterSize(UniformInt.of(10, 16)).density(0.2f).weight(15)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 32)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Aluminium).size(2))
                        .middle(b -> b.mat(Aluminium).size(3))
                        .bottom(b -> b.mat(Aluminium).size(2))
                        .spread(b -> b.mat(Aluminium).size(1)))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Aluminium)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sealed_a", "bauxite", def -> def
                .clusterSize(UniformInt.of(24, 36)).density(0.5f).weight(30)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-16, 64)
                .layeredVeinGenerator(gen -> gen.withLayerPattern(() -> GTLayerPattern.builder(STONE_RULES)
                        .layer(l -> l.weight(1).mat(Bauxite).size(2, 4))
                        .build()))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Bauxite)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));
    }

    private static void registerSealedBOres() {
        GTVeinPlacement.registerVein("sealed_b", "gold", def -> def
                .clusterSize(UniformInt.of(8, 14)).density(0.25f).weight(20)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 32)
                .cuboidVeinGenerator(gen -> gen
                        .top(b -> b.mat(Gold).size(2))
                        .middle(b -> b.mat(Gold).size(3))
                        .bottom(b -> b.mat(Gold).size(2))
                        .spread(b -> b.mat(Gold).size(1)))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Gold)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sealed_b", "quartz", def -> def
                .clusterSize(UniformInt.of(24, 36)).density(0.6f).weight(60)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-16, 80)
                .layeredVeinGenerator(gen -> gen.withLayerPattern(() -> GTLayerPattern.builder(STONE_RULES)
                        .layer(l -> l.weight(1).mat(NetherQuartz).size(2, 4))
                        .build()))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(NetherQuartz)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sealed_b", "sulfur", def -> def
                .clusterSize(UniformInt.of(16, 24)).density(0.35f).weight(35)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-32, 48)
                .veinedVeinGenerator(gen -> gen.oreBlock(Sulfur, 1))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Sulfur)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));

        GTVeinPlacement.registerVein("sealed_b", "tungsten", def -> def
                .clusterSize(UniformInt.of(14, 20)).density(0.3f).weight(20)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-40, 16)
                .dikeVeinGenerator(gen -> gen.withBlock(Tungsten, 1, -40, 16)));

        GTVeinPlacement.registerVein("sealed_b", "nickel", def -> def
                .clusterSize(UniformInt.of(14, 20)).density(0.3f).weight(20)
                .layer(ConfluxWorldGenLayers.CONFLUX_STONE)
                .heightRangeUniform(-40, 16)
                .dikeVeinGenerator(gen -> gen.withBlock(Nickel, 1, -40, 16))
                .surfaceIndicatorGenerator(ind -> ind.surfaceRock(Nickel)
                        .placement(SurfaceIndicatorGenerator.IndicatorPlacement.ABOVE)));
    }
}
