package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;

import net.phoenix.core.PhoenixCore;

public class AstralMaterials {

    public static Material ASTRAL_FILAMENT;
    public static Material SKEIN;
    public static Material ENSORCELLED_WEAVE;

    public static void register() {
        ASTRAL_FILAMENT = new Material.Builder(PhoenixCore.id("astral_filament"))
                .langValue("§dAstral Filament")
                .dust()
                .color(0x7A4FE0)
                .secondaryColor(0xB99CFF)
                .iconSet(MaterialIconSet.SHINY)
                .buildAndRegister();

        SKEIN = new Material.Builder(PhoenixCore.id("skein"))
                .langValue("§5Skein")
                .dust()
                .fluid()
                .color(0x5C2FB0)
                .secondaryColor(0x9C6CE0)
                .iconSet(MaterialIconSet.SHINY)
                .buildAndRegister();

        ENSORCELLED_WEAVE = new Material.Builder(PhoenixCore.id("ensorcelled_weave"))
                .langValue("§dEnsorcelled Weave")
                .fluid()
                .color(0x2E0F5C)
                .secondaryColor(0xC79CFF)
                .iconSet(MaterialIconSet.SHINY)
                .buildAndRegister();
    }
}
