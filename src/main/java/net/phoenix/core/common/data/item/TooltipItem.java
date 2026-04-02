package net.phoenix.core.common.data.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.annotation.Nullable;

public class TooltipItem extends Item {

    private final String[] tooltipKeys;

    public TooltipItem(Properties properties, String... tooltipKeys) {
        super(properties);
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        for (String line : tooltipKeys) {
            tooltipComponents.add(Component.literal(line));
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
