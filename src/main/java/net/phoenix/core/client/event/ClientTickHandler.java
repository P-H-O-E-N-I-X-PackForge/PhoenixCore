package net.phoenix.core.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.gui.WingFlightScreen;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.common.data.item.PhoenixArmorItem;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (PhoenixKeybinds.OPEN_WING_GUI.consumeClick()) {
            if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof PhoenixArmorItem) {
                mc.setScreen(new WingFlightScreen());
            }
        }
    }
}
