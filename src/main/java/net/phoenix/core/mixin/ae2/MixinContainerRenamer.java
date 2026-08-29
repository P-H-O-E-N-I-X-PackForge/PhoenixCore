package net.phoenix.core.mixin.ae2;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.network.chat.Component;
import net.phoenix.core.api.CustomNameAccess;

import com.glodblock.github.extendedae.container.ContainerRenamer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(value = ContainerRenamer.class, remap = false)
public abstract class MixinContainerRenamer {

    @Shadow
    @Final
    @Mutable
    private Consumer<String> setter;

    @Shadow
    @Final
    @Mutable
    private Supplier<Component> getter;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void phoenixcore$init(int id, net.minecraft.world.entity.player.Inventory inv, Object host,
                                  CallbackInfo ci) {
        if (!(host instanceof MetaMachine machine) || !(machine instanceof CustomNameAccess access)) {
            return;
        }

        this.setter = access::phoenix$setCustomName;
        this.getter = () -> Component.literal(access.phoenix$getCustomName());
        ((ContainerRenamer) (Object) this).setValidMenu(true);
    }
}
