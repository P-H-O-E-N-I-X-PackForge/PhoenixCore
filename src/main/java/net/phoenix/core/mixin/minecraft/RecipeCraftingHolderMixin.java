package net.phoenix.core.mixin.minecraft;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.conflux.research.ConfluxRecipeGate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeCraftingHolder.class)
public interface RecipeCraftingHolderMixin {

    @Inject(method = "setRecipeUsed(Lnet/minecraft/world/level/Level;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void phoenix$gate(Level level, ServerPlayer player, RecipeHolder<?> recipe,
                              CallbackInfoReturnable<Boolean> cir) {
        if (ConfluxRecipeGate.blocks(player, recipe)) cir.setReturnValue(false);
    }
}
