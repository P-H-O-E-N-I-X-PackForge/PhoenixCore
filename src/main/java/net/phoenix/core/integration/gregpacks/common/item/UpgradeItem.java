package net.phoenix.core.integration.gregpacks.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UpgradeItem extends Item {

    private final UpgradeType upgradeType;
    private final String descriptionKey;

    public UpgradeItem(UpgradeType upgradeType, String descriptionKey) {
        super(new Properties().stacksTo(1));
        this.upgradeType = upgradeType;
        this.descriptionKey = descriptionKey;
    }

    public UpgradeType getUpgradeType() {
        return upgradeType;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable(descriptionKey));
    }
}
