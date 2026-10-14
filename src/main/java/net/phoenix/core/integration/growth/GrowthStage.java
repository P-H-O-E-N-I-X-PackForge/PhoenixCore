package net.phoenix.core.integration.growth;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

/**
 * One stage of a growing machine.
 *
 * @param bounds      how far the shell reaches in each direction at this stage
 * @param cost        what advancing to this stage takes from the machine's input buses
 * @param speedBonus  share of the recipe time this stage saves (0.1 = 10% faster)
 * @param outputBonus extra share of every output this stage adds (0.1 = 10% more)
 * @param batch       how many recipes' worth this stage runs at once, inputs, outputs and power all multiplied (1 =
 *                    one at a time)
 */
public record GrowthStage(IntList bounds, List<ItemStack> cost, float speedBonus, float outputBonus, int batch) {

    /** A stage with no bonuses: it only changes the shell. */
    public GrowthStage(IntList bounds, List<ItemStack> cost) {
        this(bounds, cost, 0.0f, 0.0f, 1);
    }

    public boolean hasBonus() {
        return speedBonus > 0.0f || outputBonus > 0.0f || batch > 1;
    }
}
