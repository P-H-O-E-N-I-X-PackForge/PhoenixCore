package net.phoenix.core.mixin.ae2;

import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.inventories.InternalInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MEPatternBufferPartMachine.class, remap = false)
public interface MEPatternBufferResizeAccessor {

    @Mutable
    @Accessor("internalInventory")
    void phoenix$setInternalInventory(MEPatternBufferPartMachine.InternalSlot[] slots);

    @Mutable
    @Accessor("patternSlotDetails")
    void phoenix$setPatternSlotDetails(IPatternDetails[] details);

    @Mutable
    @Accessor("internalPatternInventory")
    void phoenix$setInternalPatternInventory(InternalInventory inventory);

    @Accessor("proxies")
    java.util.Set<net.minecraft.core.BlockPos> phoenix$getProxyPositions();

    @Invoker("onSlotChanged")
    void phoenix$onSlotChanged();
}
