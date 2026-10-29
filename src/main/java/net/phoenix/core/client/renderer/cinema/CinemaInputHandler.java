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

        if (!Screen.hasAltDown() && !Screen.hasControlDown() && !event.getEntity().isShiftKeyDown()) {
            var target = CinemaClientState.target(event.getLevel(), event.getPos());
            if (target != null) {
                // a button or an arrow under the cursor does its action
                float[] at = CinemaClientState.local(target, event.getHitVec().getLocation());
                var hit = CinemaClientState.layout(target).hitAt(at[0], at[1]);
                if (hit != null) {
                    consume(event);
                    CinemaClientActions.run(hit.action(), target);
                    return;
                }
                // the screen's own action, if it has one
                if (!screen.getAction().isEmpty()) {
                    consume(event);
                    CinemaClientActions.run(screen.getAction(), target);
                    return;
                }
                // anywhere else turns the page, for a screen whose pages are each player's own
                if (target.centre().isLocalPages()) {
                    consume(event);
                    CinemaClientActions.run("next", target);
                    return;
                }
            }
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
