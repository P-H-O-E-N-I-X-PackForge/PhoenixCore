package net.phoenix.core.integration.astral.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;

import org.jetbrains.annotations.NotNull;

public class AstralCodexItem extends Item {

    public AstralCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                net.phoenix.core.integration.astral.client.AstralCodexScreen.open();
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
