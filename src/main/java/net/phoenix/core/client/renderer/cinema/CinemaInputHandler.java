package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;
import net.phoenix.core.network.packet.C2SCinemaActionPacket;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public class CinemaInputHandler {

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        // this also fires on the integrated server's thread: opening a screen from there is not allowed
        if (!event.getLevel().isClientSide()) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CinemaScreenBlockEntity screen)) return;

        if (!screen.getAction().isEmpty() && !Screen.hasAltDown() && !Screen.hasControlDown() &&
                !event.getEntity().isShiftKeyDown()) {
            consume(event);
            CinemaClientActions.run(screen.getAction());
            return;
        }
        if (Screen.hasAltDown()) {
            consume(event);
            PacketDistributor.sendToServer(
                    new C2SCinemaActionPacket(event.getPos(), C2SCinemaActionPacket.Action.CYCLE_COLOR));
        } else if (Screen.hasControlDown()) {
            consume(event);
            CinemaScreenClientHelper.openTypingScreen(event.getPos(), screen.getLines().size(), "");
        }
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CinemaScreenBlockEntity)) return;

        consume(event);
        PacketDistributor.sendToServer(
                new C2SCinemaActionPacket(event.getPos(), C2SCinemaActionPacket.Action.REMOVE_CURRENT_LINE));
    }

    private static void consume(PlayerInteractEvent event) {
        if (event instanceof net.neoforged.bus.api.ICancellableEvent cancellable) cancellable.setCanceled(true);
        if (event instanceof PlayerInteractEvent.RightClickBlock rightClick) {
            rightClick.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        } else if (event instanceof PlayerInteractEvent.LeftClickBlock leftClick) {
            leftClick.setCanceled(true);
        }
    }
}
