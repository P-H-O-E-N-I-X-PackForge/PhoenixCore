package net.phoenix.core.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;

public class SprayCanHudOverlay {

    public static final LayeredDraw.Layer HUD_SPRAY_CAN = (guiGraphics, deltaTracker) -> {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();

        if (!(stack.getItem() instanceof ChameleonSprayCanItem)) {
            stack = mc.player.getOffhandItem();
        }

        if (stack.getItem() instanceof ChameleonSprayCanItem) {
            Component text;

            String chromCode = ChameleonSprayCanBehaviour.getChromaticCode(stack);
            if (chromCode != null && !chromCode.isEmpty()) {

                text = Component.literal("Mode: §" + chromCode + "Chromatic (" + chromCode + ")");
            } else {

                DyeColor color = ChameleonSprayCanBehaviour.getColor(stack);
                if (color != null) {
                    Component colorName = Component.translatable("color.minecraft." + color.getSerializedName());
                    text = Component.translatable("behaviour.paintspray.chameleon.status.color", colorName);
                } else {
                    text = Component.translatable("behaviour.paintspray.chameleon.status.solvent");
                }
            }

            int x = width / 2;
            int y = height - 53;

            int textWidth = mc.font.width(text);
            guiGraphics.drawString(mc.font, text, x - (textWidth / 2), y, 0xFFFFFF);
        }
    };
}
