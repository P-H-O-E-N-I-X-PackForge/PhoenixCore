package net.phoenix.core.mixin.gtceu;

import com.gregtechceu.gtceu.common.cover.ConveyorCover;

import net.phoenix.core.common.data.CoverRates;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ConveyorCover.class, remap = false)
public abstract class ConveyorCoverRateMixin {

    @Shadow
    @Final
    public int tier;
    @Shadow
    @Final
    @Mutable
    public int maxItemTransferRate;
    @Shadow
    protected int transferRate;
    @Shadow
    protected int itemsLeftToTransferLastSecond;

    @Inject(method = "<init>(Lcom/gregtechceu/gtceu/api/cover/CoverDefinition;Lcom/gregtechceu/gtceu/api/capability/ICoverable;Lnet/minecraft/core/Direction;II)V",
            at = @At("TAIL"))
    private void phoenix$scaleRate(CallbackInfo ci) {
        if (tier < 1) return;
        maxItemTransferRate *= CoverRates.GT_MULTIPLIER;
        transferRate = maxItemTransferRate;
        itemsLeftToTransferLastSecond = transferRate;
    }
}
