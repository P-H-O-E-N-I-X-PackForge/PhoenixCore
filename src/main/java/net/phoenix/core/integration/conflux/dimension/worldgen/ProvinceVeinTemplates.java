package net.phoenix.core.integration.conflux.dimension.worldgen;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

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
            new Need(false, Iron, Pyrite),
            new Need(false, Copper, Chalcopyrite, Bornite),
            new Need(false, Tin, Cassiterite),
            new Need(false, Zinc, Sphalerite),
            new Need(false, Lead, Galena),
            new Need(false, Nickel, Pentlandite),
            new Need(false, Gold),
            new Need(false, Silver),
            new Need(false, Bauxite, Aluminium),
            new Need(false, Redstone),
            new Need(false, Sulfur),
            new Need(false, Coal),
            new Need(true, Diamond),
            new Need(false, Lapis),
            new Need(false, NetherQuartz, CertusQuartz),
            new Need(true, Cobalt, Cobaltite),
            new Need(true, Chromium),
            new Need(true, Scheelite),
            new Need(true, Ilmenite),
            new Need(true, Platinum, Cooperite),
            new Need(true, Pyrolusite),
            new Need(true, Molybdenite)
    };

    private static OreLayer layer(int weight, Material material) {
        return new OreLayer(material, weight);
    }

    private static void register(String regionId, int minY, int maxY, OreLayer... layers) {
        TEMPLATES.put(regionId, Template.of(45, 80, minY, maxY, layers));
    }

    static {
        register("phoenix_plains", -48, 100,
                layer(3, Iron), layer(3, Tin), layer(2, Copper), layer(2, Zinc), layer(1, Nickel), layer(1, Cobalt));

        register("phoenix_peaks", -56, 0,
                layer(3, Diamond), layer(2, Scheelite), layer(2, Cooperite), layer(2, Platinum),
                layer(2, Pyrolusite), layer(2, Ilmenite));

        register("phoenix_crimson", -40, 48,
                layer(3, Ruby), layer(3, Gold), layer(2, Cinnabar), layer(2, Redstone), layer(2, Pyrite));

        register("phoenix_ashlands", -40, 80,
                layer(3, Lead), layer(3, Aluminium), layer(2, Bauxite), layer(2, Stibnite), layer(2, Bornite));

        register("phoenix_sulfur_fields", -48, 32,
                layer(3, Sulfur), layer(2, Platinum), layer(2, Palladium), layer(2, Molybdenite), layer(2, Barite));

        register("phoenix_basalt_flats", -40, 48,
                layer(3, Sapphire), layer(2, Chromium), layer(2, NetherQuartz), layer(2, Beryllium), layer(2, Lithium));

        register("phoenix_scorched_grove", -32, 64,
                layer(3, Emerald), layer(2, Lapis), layer(2, Topaz), layer(2, Amethyst), layer(2, Silver));

        register("void_island", -48, 100,
                layer(3, Chalcopyrite), layer(3, Cassiterite), layer(2, Sphalerite), layer(2, Galena),
                layer(2, Pentlandite));

        register("void_cosmic", -56, 8,
                layer(3, Diamond), layer(2, Emerald), layer(2, CertusQuartz), layer(2, Cobaltite), layer(2, Chromium));

        register("void_crystal_field", -40, 48,
                layer(2, Almandine), layer(2, Andradite), layer(2, Grossular), layer(2, Pyrope),
                layer(2, Spessartine), layer(2, Uvarovite));

        register("void_amethyst_grove", -32, 64,
                layer(3, Amethyst), layer(2, Lapis), layer(2, Sapphire), layer(2, GreenSapphire), layer(2, BlueTopaz));

        register("void_starlit_meadow", -48, 40,
                layer(3, Silver), layer(2, Platinum), layer(2, Palladium), layer(1, Cooperite),
                layer(2, Pollucite), layer(2, Spodumene));

        register("void_lavender_fields", -40, 48,
                layer(3, Ruby), layer(2, Topaz), layer(2, Bastnasite), layer(2, Apatite), layer(2, Lepidolite));

        register("void_drift", -48, 32,
                layer(3, Gold), layer(2, NetherQuartz), layer(2, Redstone), layer(2, Pyrite), layer(2, Sulfur));

        register("sculk_forest", -40, 96,
                layer(3, Iron), layer(2, Tin), layer(2, Zinc), layer(2, Nickel), layer(1, Cobalt));

        register("sculk_depths", -56, -8,
                layer(3, Diamond), layer(2, CertusQuartz), layer(2, Chromium), layer(2, Platinum),
                layer(2, Palladium), layer(1, Cooperite));

        register("sculk_shriek", -48, 32,
                layer(3, Amethyst), layer(2, Lapis), layer(2, Redstone), layer(2, Pyrite), layer(2, Cinnabar));

        register("sculk_hollow", -40, 60,
                layer(2, Bornite), layer(2, Galena), layer(2, Sphalerite), layer(2, Pentlandite), layer(2, Apatite));

        register("sculk_marsh", -32, 80,
                layer(2, Barite), layer(2, Stibnite), layer(2, Cassiterite), layer(2, Bauxite), layer(2, Molybdenite));

        register("sculk_thicket", -40, 64,
                layer(3, Emerald), layer(2, Sapphire), layer(2, GreenSapphire), layer(2, Topaz),
                layer(2, Silver), layer(1, Beryllium));

        register("sculk_ridge", -56, 16,
                layer(3, Gold), layer(2, Pyrolusite), layer(2, Scheelite), layer(2, Cooperite), layer(2, Ilmenite));

        register("sealed_industrial", -32, 96,
                layer(3, Iron), layer(3, Copper), layer(2, Tin), layer(2, Zinc), layer(2, Coal));
        register("sealed_vault", -56, 0,
                layer(3, Chromium), layer(2, Platinum), layer(2, Scheelite), layer(2, Ilmenite), layer(1, Diamond));
        register("sealed_scrapyard", -40, 64,
                layer(3, Copper), layer(2, Lead), layer(2, Nickel), layer(2, Silver), layer(1, Cobalt));
        register("sealed_concrete_flats", -32, 64,
                layer(3, Bauxite), layer(2, CertusQuartz), layer(2, Barite), layer(2, Stibnite));
        register("sealed_rust_fields", -32, 48,
                layer(3, Pyrite), layer(2, Iron), layer(2, Pyrolusite), layer(2, Molybdenite));
        register("sealed_overgrown_ruins", -24, 80,
                layer(3, Lapis), layer(2, Redstone), layer(2, Zinc), layer(2, Galena), layer(2, Coal));
        register("sealed_ash_yard", -40, 32,
                layer(3, Coal), layer(3, Sulfur), layer(2, Gold), layer(2, Cinnabar));

        register("sealed_nether", -40, 40,
                layer(3, Gold), layer(3, NetherQuartz), layer(2, Sulfur), layer(2, Redstone));
        register("sealed_warped", -32, 64,
                layer(2, Copper), layer(2, Tin), layer(2, Iron), layer(2, Zinc), layer(2, Lapis));
        register("sealed_crimson_zone", -40, 48,
                layer(3, Iron), layer(2, Cinnabar), layer(2, Redstone), layer(2, Pyrite), layer(2, Coal));
        register("sealed_basalt_wastes", -48, 16,
                layer(3, Scheelite), layer(2, Chromium), layer(2, Ilmenite), layer(2, Pyrolusite), layer(1, Platinum));
        register("sealed_soul_valley", -40, 40,
                layer(3, Nickel), layer(2, Cobalt), layer(2, Lead), layer(2, Silver), layer(1, Molybdenite));
        register("sealed_warped_depths", -56, 0,
                layer(3, Diamond), layer(2, Platinum), layer(2, Palladium), layer(2, Cooperite));
        register("sealed_glowstone_caverns", -32, 56,
                layer(3, Bauxite), layer(2, Coal), layer(2, Sulfur), layer(2, Galena));

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
        byBottom.sort((a, b) -> Integer.compare(TEMPLATES.get(a).minY(), TEMPLATES.get(b).minY()));

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
