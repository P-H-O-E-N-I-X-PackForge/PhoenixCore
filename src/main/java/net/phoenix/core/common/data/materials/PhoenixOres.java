package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.common.data.GTElements;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.phoenix.core.PhoenixCore;

public class PhoenixOres {

    public static Material NEVVONIAN_IRON;
    public static Material FLUORITE;
    public static Material IGNISIUM;
    public static Material POLARITY_FLIPPED_BISMUTHITE;
    public static Material VOIDGLASS_SHARD;
    public static Material CRYSTALLIZED_FLUXSTONE;
    public static Material PERMAFROST, NAQUADATITE;

    public static Material DORMANT_EMBER;

    public static void register() {
        NEVVONIAN_IRON = new Material.Builder(PhoenixCore.id("nevvonian_iron"))
                
                .ingot()
                .ore()
                .color(0x7A687F)
                .iconSet(MaterialIconSet.DIAMOND)
                .buildAndRegister();

        VOIDGLASS_SHARD = new Material.Builder(PhoenixCore.id("voidglass_shard"))
                
                .gem()
                .ore()
                .color(0x6A00AA)
                .iconSet(MaterialIconSet.DIAMOND)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        NAQUADATITE = new Material.Builder(PhoenixCore.id("naquadatite"))
                .ore()
                .dust()
                .color(0x0C0A00).secondaryColor(0xBF00FF)
                .iconSet(MaterialIconSet.DIAMOND)
                .element(GTElements.Nq)
                .buildAndRegister();

        POLARITY_FLIPPED_BISMUTHITE = new Material.Builder(PhoenixCore.id("polarity_flipped_bismuthite"))
                
                .dust()
                .ore()
                .color(0xE4D6FF)
                .iconSet(MaterialIconSet.DIAMOND)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        PERMAFROST = new Material.Builder(PhoenixCore.id("permafrost"))
                
                .dust()
                .ore()
                .color(0xA7D1EB)
                .iconSet(MaterialIconSet.DIAMOND)
                .buildAndRegister();

        FLUORITE = new Material.Builder(PhoenixCore.id("fluorite"))
                
                .gem()
                .ore()
                .color(0x0C9949)
                .iconSet(MaterialIconSet.DIAMOND)
                .components(GTMaterials.Fluorine, 2, GTMaterials.Calcium, 1)
                .buildAndRegister();

        IGNISIUM = new Material.Builder(PhoenixCore.id("ignisium"))
                
                .dust()
                .ore()
                .color(0xFF4500)
                .iconSet(MaterialIconSet.DIAMOND)
                .buildAndRegister();

        CRYSTALLIZED_FLUXSTONE = new Material.Builder(PhoenixCore.id("crystallized_fluxstone"))
                
                .dust()
                .ore()
                .color(0xD4BFFF)
                .iconSet(MaterialIconSet.DIAMOND)
                .buildAndRegister();

        DORMANT_EMBER = new Material.Builder(PhoenixCore.id("dormant_ember"))
                .ingot().ore().color(0x704214).iconSet(MaterialIconSet.SHINY)
                .addOreByproducts(GTMaterials.Iron)
                .element(PhoenixElements.EMBER)
                .buildAndRegister();

    }
}
