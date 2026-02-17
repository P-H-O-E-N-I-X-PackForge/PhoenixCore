package net.phoenix.core.mixin.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void phoenix$addFullCountToTooltip(Player player, TooltipFlag isAdvanced,
                                               CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        int count = stack.getCount();

        if (count >= 1000) {
            String formattedCount = NumberFormat.getNumberInstance(Locale.US).format(count);

            cir.getReturnValue().add(Component.literal("Amount: " + formattedCount)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
