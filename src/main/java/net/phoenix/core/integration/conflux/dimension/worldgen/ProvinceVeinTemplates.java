package net.phoenix.core.integration.conflux.dimension.worldgen;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class ProvinceVeinTemplates {

    public static final TagKey<Block> STONE_TAG = BlockTags.STONE_ORE_REPLACEABLES;

    public record OreLayer(Material material, int weight) {}

    public static java.util.Set<Material> allMaterials() {
        java.util.Set<Material> out = new java.util.LinkedHashSet<>();
        for (Template template : TEMPLATES.values()) {
            for (OreLayer layer : template.layers()) {
                if (layer.material() != null) out.add(layer.material());
            }
        }
        return out;
    }

    public record Template(int minRadius, int maxRadius, int minY, int maxY, List<OreLayer> layers, int totalWeight) {

        static Template of(int minRadius, int maxRadius, int minY, int maxY, OreLayer... layers) {
            int total = 0;
            for (OreLayer l : layers) total += l.weight();
            return new Template(minRadius, maxRadius, minY, maxY, List.of(layers), total);
        }
    }

    private static final Map<String, Template> TEMPLATES = new HashMap<>();

    private record Need(boolean deep, Material... sources) {}

    private static final Need[] PROGRESSION = {
            new Need(false, Iron.get(), Pyrite.get()),
            new Need(false, Copper.get(), Chalcopyrite.get(), Bornite.get()),
            new Need(false, Tin.get(), Cassiterite.get()),
            new Need(false, Zinc.get(), Sphalerite.get()),
            new Need(false, Lead.get(), Galena.get()),
            new Need(false, Nickel.get(), Pentlandite.get()),
            new Need(false, Gold.get()),
            new Need(false, Silver.get()),
            new Need(false, Bauxite.get(), Aluminium.get()),
            new Need(false, Redstone.get()),
            new Need(false, Sulfur.get()),
            new Need(false, Coal.get()),
            new Need(true, Diamond.get()),
            new Need(false, Lapis.get()),
            new Need(false, NetherQuartz.get(), CertusQuartz.get()),
            new Need(true, Cobalt.get(), Cobaltite.get()),
            new Need(true, Chromium.get()),
            new Need(true, Scheelite.get()),
            new Need(true, Ilmenite.get()),
            new Need(true, Platinum.get(), Cooperite.get()),
            new Need(true, Pyrolusite.get()),
            new Need(true, Molybdenite.get())
    };

    private static OreLayer layer(int weight, Material material) {
        return new OreLayer(material, weight);
    }

    private static void register(String regionId, int minY, int maxY, OreLayer... layers) {
        TEMPLATES.put(regionId, Template.of(45, 80, minY, maxY, layers));
    }

    static {
        register("phoenix_plains", -48, 100,
                layer(3, Iron.get()), layer(3, Tin.get()), layer(2, Copper.get()), layer(2, Zinc.get()),
                layer(1, Nickel.get()), layer(1, Cobalt.get()));

        register("phoenix_peaks", -56, 0,
                layer(3, Diamond.get()), layer(2, Scheelite.get()), layer(2, Cooperite.get()), layer(2, Platinum.get()),
                layer(2, Pyrolusite.get()), layer(2, Ilmenite.get()));

        register("phoenix_crimson", -40, 48,
                layer(3, Ruby.get()), layer(3, Gold.get()), layer(2, Cinnabar.get()), layer(2, Redstone.get()),
                layer(2, Pyrite.get()));

        register("phoenix_ashlands", -40, 80,
                layer(3, Lead.get()), layer(3, Aluminium.get()), layer(2, Bauxite.get()), layer(2, Stibnite.get()),
                layer(2, Bornite.get()));

        register("phoenix_sulfur_fields", -48, 32,
                layer(3, Sulfur.get()), layer(2, Platinum.get()), layer(2, Palladium.get()),
                layer(2, Molybdenite.get()), layer(2, Barite.get()));

        register("phoenix_basalt_flats", -40, 48,
                layer(3, Sapphire.get()), layer(2, Chromium.get()), layer(2, NetherQuartz.get()),
                layer(2, Beryllium.get()), layer(2, Lithium.get()));

        register("phoenix_scorched_grove", -32, 64,
                layer(3, Emerald.get()), layer(2, Lapis.get()), layer(2, Topaz.get()), layer(2, Amethyst.get()),
                layer(2, Silver.get()));

        register("void_island", -48, 100,
                layer(3, Chalcopyrite.get()), layer(3, Cassiterite.get()), layer(2, Sphalerite.get()),
                layer(2, Galena.get()),
                layer(2, Pentlandite.get()));

        register("void_cosmic", -56, 8,
                layer(3, Diamond.get()), layer(2, Emerald.get()), layer(2, CertusQuartz.get()),
                layer(2, Cobaltite.get()), layer(2, Chromium.get()));

        register("void_crystal_field", -40, 48,
                layer(2, Almandine.get()), layer(2, Andradite.get()), layer(2, Grossular.get()), layer(2, Pyrope.get()),
                layer(2, Spessartine.get()), layer(2, Uvarovite.get()));

        register("void_amethyst_grove", -32, 64,
                layer(3, Amethyst.get()), layer(2, Lapis.get()), layer(2, Sapphire.get()),
                layer(2, GreenSapphire.get()), layer(2, BlueTopaz.get()));

        register("void_starlit_meadow", -48, 40,
                layer(3, Silver.get()),
                layer(2, Platinum.get()),
                layer(2, Palladium.get()),
                layer(1, Cooperite.get()),
                layer(2, Pollucite.get()),
                layer(2, Spodumene.get()));

        register("void_lavender_fields", -40, 48,
                layer(3, Ruby.get()), layer(2, Topaz.get()), layer(2, Bastnasite.get()), layer(2, Apatite.get()),
                layer(2, Lepidolite.get()));

        register("void_drift", -48, 32,
                layer(3, Gold.get()), layer(2, NetherQuartz.get()), layer(2, Redstone.get()), layer(2, Pyrite.get()),
                layer(2, Sulfur.get()));

        register("sculk_forest", -40, 96,
                layer(3, Iron.get()), layer(2, Tin.get()), layer(2, Zinc.get()), layer(2, Nickel.get()),
                layer(1, Cobalt.get()));

        register("sculk_depths", -56, -8,
                layer(3, Diamond.get()), layer(2, CertusQuartz.get()), layer(2, Chromium.get()),
                layer(2, Platinum.get()),
                layer(2, Palladium.get()), layer(1, Cooperite.get()));

        register("sculk_shriek", -48, 32,
                layer(3, Amethyst.get()), layer(2, Lapis.get()), layer(2, Redstone.get()), layer(2, Pyrite.get()),
                layer(2, Cinnabar.get()));

        register("sculk_hollow", -40, 60,
                layer(2, Bornite.get()), layer(2, Galena.get()), layer(2, Sphalerite.get()),
                layer(2, Pentlandite.get()), layer(2, Apatite.get()));

        register("sculk_marsh", -32, 80,
                layer(2, Barite.get()), layer(2, Stibnite.get()), layer(2, Cassiterite.get()), layer(2, Bauxite.get()),
                layer(2, Molybdenite.get()));

        register("sculk_thicket", -40, 64,
                layer(3, Emerald.get()), layer(2, Sapphire.get()), layer(2, GreenSapphire.get()), layer(2, Topaz.get()),
                layer(2, Silver.get()), layer(1, Beryllium.get()));

        register("sculk_ridge", -56, 16,
                layer(3, Gold.get()), layer(2, Pyrolusite.get()), layer(2, Scheelite.get()), layer(2, Cooperite.get()),
                layer(2, Ilmenite.get()));

        register("sealed_industrial", -32, 96,
                layer(3, Iron.get()), layer(3, Copper.get()), layer(2, Tin.get()), layer(2, Zinc.get()),
                layer(2, Coal.get()));
        register("sealed_vault", -56, 0,
                layer(3, Chromium.get()), layer(2, Platinum.get()), layer(2, Scheelite.get()), layer(2, Ilmenite.get()),
                layer(1, Diamond.get()));
        register("sealed_scrapyard", -40, 64,
                layer(3, Copper.get()), layer(2, Lead.get()), layer(2, Nickel.get()), layer(2, Silver.get()),
                layer(1, Cobalt.get()));
        register("sealed_concrete_flats", -32, 64,
                layer(3, Bauxite.get()), layer(2, CertusQuartz.get()), layer(2, Barite.get()),
                layer(2, Stibnite.get()));
        register("sealed_rust_fields", -32, 48,
                layer(3, Pyrite.get()), layer(2, Iron.get()), layer(2, Pyrolusite.get()), layer(2, Molybdenite.get()));
        register("sealed_overgrown_ruins", -24, 80,
                layer(3, Lapis.get()), layer(2, Redstone.get()), layer(2, Zinc.get()), layer(2, Galena.get()),
                layer(2, Coal.get()));
        register("sealed_ash_yard", -40, 32,
                layer(3, Coal.get()), layer(3, Sulfur.get()), layer(2, Gold.get()), layer(2, Cinnabar.get()));

        register("sealed_nether", -40, 40,
                layer(3, Gold.get()), layer(3, NetherQuartz.get()), layer(2, Sulfur.get()), layer(2, Redstone.get()));
        register("sealed_warped", -32, 64,
                layer(2, Copper.get()), layer(2, Tin.get()), layer(2, Iron.get()), layer(2, Zinc.get()),
                layer(2, Lapis.get()));
        register("sealed_crimson_zone", -40, 48,
                layer(3, Iron.get()), layer(2, Cinnabar.get()), layer(2, Redstone.get()), layer(2, Pyrite.get()),
                layer(2, Coal.get()));
        register("sealed_basalt_wastes", -48, 16,
                layer(3, Scheelite.get()), layer(2, Chromium.get()), layer(2, Ilmenite.get()),
                layer(2, Pyrolusite.get()), layer(1, Platinum.get()));
        register("sealed_soul_valley", -40, 40,
                layer(3, Nickel.get()), layer(2, Cobalt.get()), layer(2, Lead.get()), layer(2, Silver.get()),
                layer(1, Molybdenite.get()));
        register("sealed_warped_depths", -56, 0,
                layer(3, Diamond.get()), layer(2, Platinum.get()), layer(2, Palladium.get()),
                layer(2, Cooperite.get()));
        register("sealed_glowstone_caverns", -32, 56,
                layer(3, Bauxite.get()), layer(2, Coal.get()), layer(2, Sulfur.get()), layer(2, Galena.get()));

        for (String dimension : new String[] { "phoenix", "void", "sculk", "sealed_a", "sealed_b" }) {
            ensureProgression(dimension);
        }
    }

    private static boolean holds(Template template, Material[] sources) {
        for (OreLayer layer : template.layers()) {
            for (Material source : sources) {
                if (layer.material() == source) return true;
            }
        }
        return false;
    }

    private static void ensureProgression(String dimension) {
        java.util.List<String> regions = new java.util.ArrayList<>();
        for (String id : TEMPLATES.keySet()) {
            if (id.startsWith(dimension + "_") && belongsTo(dimension, id)) regions.add(id);
        }
        java.util.Collections.sort(regions);
        if (regions.isEmpty()) return;

        int shallowCursor = 0;
        int deepCursor = 0;

        java.util.List<String> byTop = new java.util.ArrayList<>(regions);
        byTop.sort((a, b) -> Integer.compare(TEMPLATES.get(b).maxY(), TEMPLATES.get(a).maxY()));
        java.util.List<String> byBottom = new java.util.ArrayList<>(regions);
        byBottom.sort(Comparator.comparingInt(a -> TEMPLATES.get(a).minY()));

        for (Need need : PROGRESSION) {
            boolean met = false;
            for (String region : regions) {
                if (holds(TEMPLATES.get(region), need.sources())) {
                    met = true;
                    break;
                }
            }
            if (met) continue;

            String target = need.deep() ? byBottom.get(deepCursor++ % Math.min(3, byBottom.size())) :
                    byTop.get(shallowCursor++ % Math.min(4, byTop.size()));
            Template old = TEMPLATES.get(target);
            java.util.List<OreLayer> layers = new java.util.ArrayList<>(old.layers());
            layers.add(new OreLayer(need.sources()[0], 2));
            TEMPLATES.put(target, Template.of(old.minRadius(), old.maxRadius(), old.minY(), old.maxY(),
                    layers.toArray(new OreLayer[0])));
        }
    }

    private static boolean belongsTo(String dimension, String regionId) {
        return switch (dimension) {
            case "sealed_a" -> regionId.equals("sealed_industrial") || regionId.equals("sealed_vault") ||
                    regionId.equals("sealed_scrapyard") || regionId.equals("sealed_concrete_flats") ||
                    regionId.equals("sealed_rust_fields") || regionId.equals("sealed_overgrown_ruins") ||
                    regionId.equals("sealed_ash_yard");
            case "sealed_b" -> regionId.equals("sealed_nether") || regionId.equals("sealed_warped") ||
                    regionId.equals("sealed_crimson_zone") || regionId.equals("sealed_basalt_wastes") ||
                    regionId.equals("sealed_soul_valley") || regionId.equals("sealed_warped_depths") ||
                    regionId.equals("sealed_glowstone_caverns");
            default -> regionId.startsWith(dimension + "_");
        };
    }

    public static Template get(String regionId) {
        return TEMPLATES.get(regionId);
    }
}
