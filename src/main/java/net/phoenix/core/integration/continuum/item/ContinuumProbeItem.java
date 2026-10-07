package net.phoenix.core.integration.continuum.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Payload for an extraction mission. A mission consumes the probes it carries and loses them if it fails. */
public class ContinuumProbeItem extends Item {

    public ContinuumProbeItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Carried by extraction missions to a surveyed body.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Each probe brings back a share of the body's resources.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Lost if the mission fails.").withStyle(ChatFormatting.RED));
    }
}
