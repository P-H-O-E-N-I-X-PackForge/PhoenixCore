package net.phoenix.core.integration.continuum.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.continuum.common.RocketStats;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The rocket a mission flies. It is one persistent item: its wear and upgrades live in its NBT and it comes back from
 * every mission (see {@link RocketStats} for what the numbers do).
 */
public class ContinuumRocketItem extends Item {

    public ContinuumRocketItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return RocketStats.wear(stack) > 0.0f;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (1.0f - RocketStats.wear(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0f, 1.0f - RocketStats.wear(stack)) / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        float wear = RocketStats.wear(stack);
        ChatFormatting wearColor = wear < 0.35f ? ChatFormatting.GREEN : wear < 0.7f ? ChatFormatting.YELLOW :
                ChatFormatting.RED;
        tooltip.add(Component.literal("Wear: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(Math.round(wear * 100.0f) + "%").withStyle(wearColor)));

        int overdrive = RocketStats.level(stack, RocketStats.OVERDRIVE);
        int hull = RocketStats.level(stack, RocketStats.REINFORCED_HULL);
        boolean interlock = RocketStats.level(stack, RocketStats.HULL_INTERLOCK) > 0;

        if (overdrive > 0) {
            tooltip.add(Component.literal("Overdrive " + overdrive + ": -" + overdrive * 10 + "% trip time")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (hull > 0) {
            tooltip.add(Component.literal("Reinforced Hull " + hull + ": -" + Math.min(75, hull * 25) + "% wear")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (interlock) {
            tooltip.add(Component.literal("Hull Interlock: refuses to launch when too worn")
                    .withStyle(ChatFormatting.AQUA));
            if (RocketStats.interlockRefuses(stack)) {
                tooltip.add(Component.literal("Interlock engaged - repair needed").withStyle(ChatFormatting.RED));
            }
        }
        boolean stellar = RocketStats.level(stack, RocketStats.STELLAR_SHIELD) > 0;
        boolean singularity = RocketStats.level(stack, RocketStats.SINGULARITY_SHIELD) > 0;
        if (stellar) {
            tooltip.add(Component.literal("Stellar Shielding: can work at stars").withStyle(ChatFormatting.AQUA));
        }
        if (singularity) {
            tooltip.add(Component.literal("Singularity Shielding: can work at black holes")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (overdrive == 0 && hull == 0 && !interlock && !stellar && !singularity) {
            tooltip.add(Component.literal("No upgrades").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
