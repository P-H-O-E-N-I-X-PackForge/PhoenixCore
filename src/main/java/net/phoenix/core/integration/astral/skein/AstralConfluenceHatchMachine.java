package net.phoenix.core.integration.astral.skein;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;

import net.phoenix.core.integration.ars_nouveau.api.machine.trait.NotifiableSourceContainer;
import net.phoenix.core.integration.astral.api.machine.trait.NotifiableAstralThreadContainer;

import lombok.Getter;

@Getter
public class AstralConfluenceHatchMachine extends SimpleTieredMachine {

    private final NotifiableAstralThreadContainer threadContainer;
    private final NotifiableSourceContainer sourceContainer;

    public AstralConfluenceHatchMachine(BlockEntityCreationInfo info, int tier) {
        super(info, tier);
        this.threadContainer = attachTrait(
                new NotifiableAstralThreadContainer(IO.OUT, getThreadCapacity(tier), getThreadTransferRate(tier)));
        this.sourceContainer = attachTrait(
                new NotifiableSourceContainer(IO.IN, getSourceCapacity(tier), getSourceTransferRate(tier)));
    }

    public static int getThreadCapacity(int tier) {
        return 4000 * tier;
    }

    public static int getThreadTransferRate(int tier) {
        return 1000 * tier;
    }

    public static int getSourceCapacity(int tier) {
        return 4000 * tier;
    }

    public static int getSourceTransferRate(int tier) {
        return 1000 * tier;
    }
}
