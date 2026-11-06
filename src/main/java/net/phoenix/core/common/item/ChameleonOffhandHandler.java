package net.phoenix.core.common.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ChameleonOffhandHandler {

    private ChameleonOffhandHandler() {}

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Player player)) return;
        if (!(event.getLevel() instanceof Level level)) return;
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof ChameleonSprayCanItem)) return;
        ChameleonSprayCanBehaviour.paintPlaced(level, event.getPos(), offhand, player);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return;
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof Level level)) return;
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof ChameleonSprayCanItem)) return;
        if (ChameleonSprayCanBehaviour.stripBroken(level, event.getPos(), offhand, player)) {
            event.setCanceled(true);
        }
    }
}
