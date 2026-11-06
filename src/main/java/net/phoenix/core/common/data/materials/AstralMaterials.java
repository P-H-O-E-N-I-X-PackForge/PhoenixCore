package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;

import static net.phoenix.core.PhoenixCore.PHOENIX_REGISTRATE;

public class AstralMaterials {

    public static MaterialRegistryEntry ASTRAL_FILAMENT;
    public static MaterialRegistryEntry SKEIN;
    public static MaterialRegistryEntry ENSORCELLED_WEAVE;

    public static void register() {
        ASTRAL_FILAMENT = PHOENIX_REGISTRATE.material("astral_filament", builder -> builder

                .dust()
                .color(0x7A4FE0)
                .secondaryColor(0xB99CFF)
                .iconSet(MaterialIconSet.SHINY));

        SKEIN = PHOENIX_REGISTRATE.material("skein", builder -> builder

                .dust()
                .fluid()
                .color(0x5C2FB0)
                .secondaryColor(0x9C6CE0)
                .iconSet(MaterialIconSet.SHINY));

        ENSORCELLED_WEAVE = PHOENIX_REGISTRATE.material("ensorcelled_weave", builder -> builder

                .fluid()
                .color(0x2E0F5C)
                .secondaryColor(0xC79CFF)
                .iconSet(MaterialIconSet.SHINY));
    }
}
