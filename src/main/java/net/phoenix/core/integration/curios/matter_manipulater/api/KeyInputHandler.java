package net.phoenix.core.integration.matter_manipulater.api;



import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.integration.matter_manipulater.common.data.item.PhoenixManipulatorItem;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (PhoenixKeybinds.MANIPULATOR_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();

            if (stack.getItem() instanceof PhoenixManipulatorItem) {
                mc.setScreen(new PhoenixRadialMenu());
            }
        }
        while (PhoenixKeybinds.MANIPULATOR_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();

            if (stack.getItem() instanceof PhoenixManipulatorItem) {

                if (mc.player.getOffhandItem().isEmpty()) {
                    PhoenixInventoryService.findMatchingPipe(mc.player, ItemStack.EMPTY)
                            .ifPresent(foundStack -> {

                            });
                }

                mc.setScreen(new PhoenixRadialMenu());
            }
        }
    }
}
