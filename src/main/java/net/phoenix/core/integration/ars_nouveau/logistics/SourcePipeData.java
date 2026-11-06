package net.phoenix.core.integration.ars_nouveau.logistics;

/** What a node of the Source pipe network remembers: which tier of pipe it is. */
public final class SourcePipeData {

    private static final SourcePipeData[] DATA = new SourcePipeData[SourcePipeTier.values().length];

    static {
        for (SourcePipeTier tier : SourcePipeTier.values()) DATA[tier.ordinal()] = new SourcePipeData(tier);
    }

    public final SourcePipeTier tier;

    private SourcePipeData(SourcePipeTier tier) {
        this.tier = tier;
    }

    public static SourcePipeData of(SourcePipeTier tier) {
        return DATA[tier.ordinal()];
    }
}
