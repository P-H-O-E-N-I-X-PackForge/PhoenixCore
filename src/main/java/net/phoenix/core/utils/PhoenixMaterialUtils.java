package net.phoenix.core.utils;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class PhoenixMaterialUtils {

    public static final Map<Fluid, Material> FLUID_TO_MATERIAL = new HashMap<>();

    public static void register(Material material) {
        Fluid fluid = material.getFluid();
        if (fluid != null && fluid != Fluids.EMPTY) {
            FLUID_TO_MATERIAL.put(fluid, material);
        }
    }

    public static @Nullable Material getMaterialByFluid(Fluid fluid) {
        return FLUID_TO_MATERIAL.get(fluid);
    }
}
