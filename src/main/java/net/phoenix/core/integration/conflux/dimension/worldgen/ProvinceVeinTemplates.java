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

    public record Template(int minRadius, int maxRadius, int minY, int maxY, List<OreLayer> layers, int totalWeight) {
        static Template of(int minRadius, int maxRadius, int minY, int maxY, OreLayer... layers) {
            int total = 0;
            for (OreLayer l : layers) total += l.weight();
            return new Template(minRadius, maxRadius, minY, maxY, List.of(layers), total);
        }
    }

    private static final Map<String, Template> TEMPLATES = new HashMap<>();

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
                layer(3, Chalcopyrite), layer(3, Cassiterite), layer(2, Sphalerite), layer(2, Galena), layer(2, Pentlandite));

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
    }

    public static Template get(String regionId) {
        return TEMPLATES.get(regionId);
    }
}
