package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

@SuppressWarnings("removal")
public class PhoenixMaterialRegistry {

    private static final Map<Fluid, Material> FLUID_TO_MATERIAL = new HashMap<>();

    public static void register(Material material) {
        Fluid fluid = material.getFluid();
        if (fluid != null && fluid != Fluids.EMPTY) {
            FLUID_TO_MATERIAL.put(fluid, material);
        }
    }

    public static @Nullable Material getMaterial(@NotNull String fluidId) {
        ResourceLocation loc = new ResourceLocation(fluidId);
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(loc);
        if (fluid == null) return null;
        return getMaterial(fluid);
    }

    public static Material getMaterial(Fluid fluid) {
        return FLUID_TO_MATERIAL.get(fluid);
    }
}
