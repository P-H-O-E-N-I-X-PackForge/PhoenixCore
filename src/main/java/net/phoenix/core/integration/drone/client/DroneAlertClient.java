package net.phoenix.core.integration.drone.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.drone.network.S2CDroneAlertPacket;

public final class DroneAlertClient {

    private DroneAlertClient() {}

    public static void show(S2CDroneAlertPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(SystemToast.multiline(mc, new SystemToast.SystemToastId(msg.severe() ? 10000L : 6000L),
                Component.literal("Drone: " + msg.title()), Component.literal(msg.message())));
        if (msg.severe() && mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Drone] " + msg.title() + " - " + msg.message())
                    .withStyle(ChatFormatting.GOLD), false);
        }
    }
}
