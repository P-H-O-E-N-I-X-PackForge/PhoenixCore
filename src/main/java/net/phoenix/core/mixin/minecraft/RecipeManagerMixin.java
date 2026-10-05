package net.phoenix.core.mixin.minecraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.phoenix.core.api.RecipeBlacklist;
import net.phoenix.core.integration.conflux.research.RecipeGateApplier;

import com.google.gson.JsonElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RecipeManager.class, priority = 100)
public abstract class RecipeManagerMixin {

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void phoenix$onApplyHead(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
                                     ProfilerFiller profiler, CallbackInfo ci) {
        RecipeBlacklist.load();

        map.entrySet().removeIf(entry -> RecipeBlacklist.shouldRemoveRaw(entry.getKey(), entry.getValue()));
    }

    // Runs after every recipe reload, once all recipes (ours, GTCEu's own, other mods') are fully
    // parsed and stored - see RecipeGateApplier for why mutating them here, rather than a mixin into
    // the recipe-matching path itself, is enough to gate them.
    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL"))
    private void phoenix$onApplyTail(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
                                     ProfilerFiller profiler, CallbackInfo ci) {
        RecipeGateApplier.applyGates(((RecipeManager) (Object) this).getRecipes());
    }
}
