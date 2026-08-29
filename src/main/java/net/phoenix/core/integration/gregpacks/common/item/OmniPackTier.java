package net.phoenix.core.integration.gregpacks.common.item;

import net.phoenix.core.configs.PhoenixConfigs;

public enum OmniPackTier {

    BASIC("basic",
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.basicPack.basicPackItemSlots,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.basicPack.basicPackEUStorage,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.basicPack.basicPackUpgradeSlots,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.basicPack.basicPackFluidStorage),
    ADVANCED("advanced",
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.advancedPack.advancedPackItemSlots,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.advancedPack.advancedPackEUStorage,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.advancedPack.advancedPackUpgradeSlots,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.advancedPack.advancedPackFluidStorage),
    ELITE("elite",
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.elitePack.elitePackItemSlots,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.elitePack.elitePackEUStorage,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.elitePack.elitePackUpgradeSlots,
            PhoenixConfigs.INSTANCE.OmniPackBaseValues.elitePack.elitePackFluidStorage);

    private final String id;
    public final int defaultSlots;
    public final int defaultEnergyStorage;
    public final int defaultFluidStorage;
    public final int defaultMaxUpgrades;

    OmniPackTier(String id, int defaultSlots, int defaultEnergyStorage, int defaultMaxUpgrades,
                 int defaultFluidStorage) {
        this.id = id;
        this.defaultSlots = defaultSlots;
        this.defaultEnergyStorage = defaultEnergyStorage;
        this.defaultMaxUpgrades = defaultMaxUpgrades;
        this.defaultFluidStorage = defaultFluidStorage;
    }

    public String itemId() {
        return id + "_omnipack";
    }
}
