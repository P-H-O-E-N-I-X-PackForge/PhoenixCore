package net.phoenix.core.common.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.materials.PhoenixProgressionMaterials;
import net.phoenix.core.configs.PhoenixConfigs;

import org.jetbrains.annotations.NotNull;

public final class PhoenixItemCaps {

    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister
            .create(Registries.DATA_COMPONENT_TYPE, PhoenixCore.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> PAINT = COMPONENTS
            .register("paint_content", () -> DataComponentType.<SimpleFluidContent>builder()
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    private PhoenixItemCaps() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        bus.addListener(PhoenixItemCaps::registerCaps);
    }

    private static void registerCaps(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new FluidHandlerItemStack(PAINT, stack,
                PhoenixConfigs.INSTANCE.features.chameleonSprayCanCapacity) {

            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack fluidStack) {
                return fluidStack.getFluid() == PhoenixProgressionMaterials.PRISMATIC_PAINT.getFluid();
            }
        }, net.phoenix.core.common.data.item.PhoenixItems.CHAMELEON_SPRAY_CAN.get());
        event.registerItem(Capabilities.ItemHandler.ITEM,
                (stack, ctx) -> new net.phoenix.core.common.item.cinder.CinderCoreItem.StockedMaterialsHandler(stack),
                net.phoenix.core.common.item.cinder.CinderItems.CINDER_CORE.get());
    }
}
