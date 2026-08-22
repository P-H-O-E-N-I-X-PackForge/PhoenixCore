package net.phoenix.core.integration.gregvaults.common.blocks;

public enum CoreTier {

    MK1(1),
    MK2(2),
    MK3(3);

    public final int level;

    CoreTier(int level) {
        this.level = level;
    }
}