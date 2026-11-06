package net.phoenix.core.integration.gregvaults.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.gregvaults.common.items.WirelessTerminalItem;
import net.phoenix.core.integration.gregvaults.network.CPacketOpenTerminal;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class VaultKeyBindings {

    public static final KeyMapping OPEN_TERMINAL = new KeyMapping(
            "key.gregtechvaults.open_terminal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_EQUAL,
            "key.categories.gregtechvaults");

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TERMINAL);
    }

    public static class TickHandler {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            while (OPEN_TERMINAL.consumeClick()) {
                findAndOpenTerminal(mc.player);
            }
        }

        private static void findAndOpenTerminal(Player player) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof WirelessTerminalItem) {
                PacketDistributor.sendToServer(new CPacketOpenTerminal(player.getInventory().selected, false));
                return;
            }

            ItemStack offHand = player.getOffhandItem();
            if (offHand.getItem() instanceof WirelessTerminalItem) {
                PacketDistributor.sendToServer(new CPacketOpenTerminal(-1, true));
                return;
            }

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof WirelessTerminalItem) {
                    boolean isOffhand = (i == 40);
                    int slot = isOffhand ? -1 : i;
                    PacketDistributor.sendToServer(new CPacketOpenTerminal(slot, isOffhand));
                    return;
                }
            }
        }
    }
}
