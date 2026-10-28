package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;
import com.gregtechceu.gtceu.common.data.GTElements;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import static net.phoenix.core.PhoenixCore.PHOENIX_REGISTRATE;

public class PhoenixOres {

    public static MaterialRegistryEntry NEVVONIAN_IRON;
    public static MaterialRegistryEntry FLUORITE;
    public static MaterialRegistryEntry IGNISIUM;
    public static MaterialRegistryEntry POLARITY_FLIPPED_BISMUTHITE;
    public static MaterialRegistryEntry VOIDGLASS_SHARD;
    public static MaterialRegistryEntry CRYSTALLIZED_FLUXSTONE;
    public static MaterialRegistryEntry PERMAFROST, NAQUADATITE;

    public static MaterialRegistryEntry DORMANT_EMBER;

    public static void register() {
        NEVVONIAN_IRON = PHOENIX_REGISTRATE.material("nevvonian_iron", builder -> builder

                .ingot()
                .ore()
                .color(0x7A687F)
                .iconSet(MaterialIconSet.DIAMOND));

        VOIDGLASS_SHARD = PHOENIX_REGISTRATE.material("voidglass_shard", builder -> builder

                .gem()
                .ore()
                .color(0x6A00AA)
                .iconSet(MaterialIconSet.DIAMOND)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION));

        NAQUADATITE = PHOENIX_REGISTRATE.material("naquadatite", builder -> builder
                .ore()
                .dust()
                .color(0x0C0A00).secondaryColor(0xBF00FF)
                .iconSet(MaterialIconSet.DIAMOND)
                .element(GTElements.Nq));

        POLARITY_FLIPPED_BISMUTHITE = PHOENIX_REGISTRATE.material("polarity_flipped_bismuthite", builder -> builder

                .dust()
                .ore()
                .color(0xE4D6FF)
                .iconSet(MaterialIconSet.DIAMOND)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION));

        PERMAFROST = PHOENIX_REGISTRATE.material("permafrost", builder -> builder

                .dust()
                .ore()
                .color(0xA7D1EB)
                .iconSet(MaterialIconSet.DIAMOND));

        FLUORITE = PHOENIX_REGISTRATE.material("fluorite", builder -> builder

                .gem()
                .ore()
                .color(0x0C9949)
                .iconSet(MaterialIconSet.DIAMOND)
                .components(GTMaterials.Fluorine, 2, GTMaterials.Calcium, 1));

        IGNISIUM = PHOENIX_REGISTRATE.material("ignisium", builder -> builder

                .dust()
                .ore()
                .color(0xFF4500)
                .iconSet(MaterialIconSet.DIAMOND));

        CRYSTALLIZED_FLUXSTONE = PHOENIX_REGISTRATE.material("crystallized_fluxstone", builder -> builder

                .dust()
                .ore()
                .color(0xD4BFFF)
                .iconSet(MaterialIconSet.DIAMOND));

        DORMANT_EMBER = PHOENIX_REGISTRATE.material("dormant_ember", builder -> builder
                .ingot().ore().color(0x704214).iconSet(MaterialIconSet.SHINY)
                .oreByproducts(GTMaterials.Iron)
                .element(PhoenixElements.EMBER));
    }
}
