package net.phoenix.core.rebirth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.phoenix.conflux.common.ending.EpilogueService;
import net.phoenix.conflux.common.research.ResearchTeamHelper;

import java.util.List;

public class AshenReelItem extends Item {

    public AshenReelItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.isShiftKeyDown() && level.dimension().equals(EpilogueService.DIMENSION)) {
            EpilogueService.leave(sp);
            return InteractionResultHolder.consume(stack);
        }
        var team = RebirthData.get(serverLevel).team(ResearchTeamHelper.getTeamId(sp));
        if (!team.reborn) {
            sp.displayClientMessage(Component.literal("§cThe reel is blank. You have not been reborn."), true);
            return InteractionResultHolder.consume(stack);
        }
        RebirthReplay.play(sp, EpilogueService.endingOf(sp));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Watch your rebirth again.§r"));
        tooltip.add(Component.literal("§8In the epilogue, shift + use to go home.§r"));
    }
}
