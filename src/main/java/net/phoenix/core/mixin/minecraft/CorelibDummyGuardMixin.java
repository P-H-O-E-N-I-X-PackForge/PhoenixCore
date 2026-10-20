package net.phoenix.core.mixin.minecraft;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public abstract class CorelibDummyGuardMixin<T> {

    @Inject(
            method = "register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;",
            at = @At("HEAD"),
            cancellable = true)
    private void phoenix$skipRepeatDummy(ResourceKey<T> key, T value, RegistrationInfo info,
                                         CallbackInfoReturnable<Holder.Reference<T>> cir) {
        ResourceLocation loc = key.location();
        if (loc.getNamespace().equals("supermartijn642corelib") && loc.getPath().equals("dummy")) {
            MappedRegistry<T> self = (MappedRegistry<T>) (Object) this;
            if (self.containsKey(key)) {
                cir.setReturnValue(self.getHolder(key).orElseThrow());
            }
        }
    }
}
