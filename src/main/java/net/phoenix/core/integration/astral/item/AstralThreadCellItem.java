package net.phoenix.core.integration.astral.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AstralThreadCellItem extends Item {

    public static final int CAPACITY = 10_000;
    private static final String TAG_THREAD = "AstralThread";

    public AstralThreadCellItem(Properties properties) {
        super(properties);
    }

    public static int getThread(ItemStack stack) {
        return stack.getOrCreateTag().getInt(TAG_THREAD);
    }

    public static int addThread(ItemStack stack, int amount) {
        int current = getThread(stack);
        int inserted = Math.min(amount, CAPACITY - current);
        if (inserted > 0) stack.getOrCreateTag().putInt(TAG_THREAD, current + inserted);
        return inserted;
    }

    public static int removeThread(ItemStack stack, int amount) {
        int current = getThread(stack);
        int extracted = Math.min(amount, current);
        if (extracted > 0) stack.getOrCreateTag().putInt(TAG_THREAD, current - extracted);
        return extracted;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("Thread: " + getThread(stack) + " / " + CAPACITY)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> net.phoenix.core.integration.astral.client.AstralThreadCellScreen.open(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
