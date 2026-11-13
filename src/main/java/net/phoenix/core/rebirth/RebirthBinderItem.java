package net.phoenix.core.rebirth;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class RebirthBinderItem extends Item {

    public RebirthBinderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) ||
                !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        MetaMachine machine = MetaMachine.getMachine(level, context.getClickedPos());
        if (machine == null) return InteractionResult.PASS;

        RebirthNetwork.Result result;
        if (RebirthNetwork.isForge(machine)) {
            result = RebirthNetwork.attuneForge(player, level, context.getClickedPos(), machine);
        } else if (Capstone.of(machine.getDefinition().getId()) != null) {
            result = net.phoenix.core.rebirth.rite.RebirthRites.begin(player, level, context.getClickedPos(), machine);
        } else {
            return InteractionResult.PASS;
        }
        player.displayClientMessage(Component.literal((result.ok() ? "§6" : "§c") + result.message()), true);
        return InteractionResult.CONSUME;
    }
}
