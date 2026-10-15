package net.phoenix.core.integration.growth.tendril;

import net.phoenix.core.integration.growth.GrowthMultiblockMachine;

@FunctionalInterface
public interface TendrilWeightCondition {

    int weight(GrowthMultiblockMachine machine, TendrilShape shape);

    static TendrilWeightCondition constant(int weight) {
        return (machine, shape) -> weight;
    }
}
