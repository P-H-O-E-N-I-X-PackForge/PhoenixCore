package net.phoenix.core.util;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;

import org.jetbrains.annotations.Nullable;

/**
 * Looks materials up by bare name, as GTCEu 1.20 did. Materials now live under their mod's namespace.
 */
public final class Mats {

    private Mats() {}

    @Nullable
    public static Material byName(String name) {
        if (name == null) return null;
        ResourceLocation direct = name.contains(":") ? ResourceLocation.tryParse(name) : null;
        if (direct != null) return GTRegistries.MATERIALS.get(direct);
        Material m = GTRegistries.MATERIALS.get(GTCEu.id(name));
        if (m == null) m = GTRegistries.MATERIALS.get(PhoenixCore.id(name));
        return m;
    }
}
