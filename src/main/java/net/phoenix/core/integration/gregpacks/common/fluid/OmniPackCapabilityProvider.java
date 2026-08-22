package net.phoenix.core.integration.gregpacks.common.fluid;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.phoenix.core.integration.gregpacks.common.energy.OmniPackEnergyHandler;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OmniPackCapabilityProvider implements ICapabilityProvider {

    private final OmniPackFluidHandler fluidHandler;
    private final OmniPackEnergyHandler energyHandler;

    private final LazyOptional<OmniPackFluidHandler> fluidOptional;
    private final LazyOptional<OmniPackEnergyHandler> energyOptional;

    public OmniPackCapabilityProvider(ItemStack stack, OmniPackTier tier) {
        this.fluidHandler = new OmniPackFluidHandler(stack, tier);
        this.energyHandler = new OmniPackEnergyHandler(stack, tier);
        this.fluidOptional = LazyOptional.of(() -> fluidHandler);
        this.energyOptional = LazyOptional.of(() -> energyHandler);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
                                                      @NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) return fluidOptional.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyOptional.cast();
        return LazyOptional.empty();
    }
}
