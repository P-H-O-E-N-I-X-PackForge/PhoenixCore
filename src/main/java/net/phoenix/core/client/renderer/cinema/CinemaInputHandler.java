package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCinemaActionPacket;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CinemaInputHandler {

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CinemaScreenBlockEntity screen)) return;

        if (Screen.hasAltDown()) {
            consume(event);
            PhoenixNetwork.CHANNEL.sendToServer(
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
        PhoenixNetwork.CHANNEL.sendToServer(
                new C2SCinemaActionPacket(event.getPos(), C2SCinemaActionPacket.Action.REMOVE_CURRENT_LINE));
    }

    private static void consume(PlayerInteractEvent event) {
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
}
