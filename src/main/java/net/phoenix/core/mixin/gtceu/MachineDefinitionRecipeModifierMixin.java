package net.phoenix.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList;

import net.phoenix.core.integration.conflux.research.ConfluxRecipeEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts Conflux research in every machine's recipe modifier, so research that speeds a machine up (or slows it down, or
 * changes its power draw) reaches every recipe it runs, whatever kind of machine it is.
 */
@Mixin(value = MachineDefinition.class, remap = false)
public abstract class MachineDefinitionRecipeModifierMixin {

    @Unique
    private RecipeModifier phoenix$original;
    @Unique
    private RecipeModifier phoenix$wrapped;

    @Inject(method = "getRecipeModifier", at = @At("RETURN"), cancellable = true)
    private void phoenix$withResearch(CallbackInfoReturnable<RecipeModifier> cir) {
        RecipeModifier original = cir.getReturnValue();
        if (original == null) return;
        if (phoenix$wrapped == null || phoenix$original != original) {
            phoenix$original = original;
            phoenix$wrapped = original == RecipeModifier.NO_MODIFIER ? ConfluxRecipeEffects.MODIFIER :
                    new RecipeModifierList(original, ConfluxRecipeEffects.MODIFIER);
        }
        cir.setReturnValue(phoenix$wrapped);
    }
}
