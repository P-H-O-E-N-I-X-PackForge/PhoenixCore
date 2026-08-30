package net.phoenix.core.mixin.minecraft;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.WorldDataConfiguration;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.function.Consumer;

@Mixin({ CreateWorldScreen.class })
public abstract class CreateWorldScreenMixin {

    @Shadow
    protected abstract void applyNewPackConfig(PackRepository packRepository,
                                               WorldDataConfiguration worldDataConfiguration,
                                               Consumer<WorldDataConfiguration> consumer);

    @Inject(method = "tryApplyNewDataPacks",
            at = { @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/repository/PackRepository;getRequestedFeatureFlags()Lnet/minecraft/world/flag/FeatureFlagSet;",
                    shift = At.Shift.BEFORE) },
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    public void onTryApplyNewDataPacks(PackRepository packRepository, boolean bo,
                                       Consumer<WorldDataConfiguration> consumer, CallbackInfo callbackInfo,
                                       List listOne, List listTwo, WorldDataConfiguration worldDataConfiguration) {
        applyNewPackConfig(packRepository, worldDataConfiguration, consumer);
        callbackInfo.cancel();
    }
}