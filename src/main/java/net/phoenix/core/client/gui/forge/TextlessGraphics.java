package net.phoenix.core.client.gui.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class TextlessGraphics extends GuiGraphics {

    public TextlessGraphics(GuiGraphics parent) {
        super(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        this.pose().mulPose(parent.pose().last().pose());
    }

    @Override
    public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        return x;
    }

    @Override
    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        return x;
    }

    @Override
    public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        return x;
    }
}
