package net.phoenix.core.integration.astral.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.phoenix.core.integration.astral.api.capability.AstralThreadIngredient;
import net.phoenix.core.integration.astral.api.capability.AstralThreadRecipeCapability;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NotifiableAstralThreadContainer extends NotifiableRecipeHandlerTrait<AstralThreadIngredient> {

    @SaveField
    @SyncToClient
    private long currentThread;

    @SaveField
    private long maxThread;

    @SaveField
    private int maxConsumption;

    private final IO handlerIO;

    public NotifiableAstralThreadContainer(IO io, int maxCapacity, int maxConsumption) {
        super();
        this.maxThread = maxCapacity;
        this.maxConsumption = maxConsumption;
        this.handlerIO = io;
    }

    @Override
    public IO getHandlerIO() {
        return handlerIO;
    }

    @Override
    public List<AstralThreadIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<AstralThreadIngredient> left,
                                                          boolean simulate) {
        if (io != this.handlerIO) return left;

        for (int i = 0; i < left.size(); i++) {
            AstralThreadIngredient ingredient = left.get(i);
            int amountNeeded = ingredient.getThread();

            if (io == IO.IN) {
                if (currentThread >= amountNeeded) {
                    if (!simulate) {
                        removeThread(amountNeeded);
                    }
                    left.remove(i);
                    break;
                }
            } else {
                if (maxThread - currentThread >= amountNeeded) {
                    if (!simulate) {
                        addThread(amountNeeded);
                    }
                    left.remove(i);
                    break;
                }
            }
        }

        return left;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public @NotNull List getContents() {
        return List.of(new AstralThreadIngredient((int) currentThread));
    }

    @Override
    public RecipeCapability<AstralThreadIngredient> getCapability() {
        return AstralThreadRecipeCapability.CAP;
    }

    @Override
    public double getTotalContentAmount() {
        return currentThread;
    }

    public int getTransferRate() {
        return maxConsumption;
    }

    public int getThread() {
        return Math.toIntExact(currentThread);
    }

    public int getMaxThread() {
        return Math.toIntExact(maxThread);
    }

    public int addThread(int amount) {
        int inserted = Math.toIntExact(Math.min(amount, maxThread - currentThread));
        currentThread += inserted;
        this.getSyncDataHolder().markClientSyncFieldDirty("currentThread");
        return inserted;
    }

    public int removeThread(int amount) {
        int extracted = Math.toIntExact(Math.min(amount, currentThread));
        currentThread -= extracted;
        this.getSyncDataHolder().markClientSyncFieldDirty("currentThread");
        return extracted;
    }
}
