package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCinemaScreenConfigPacket;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CinemaTypingScreen extends Screen {

    private final BlockPos pos;
    private final int lineIndex;

    public CinemaTypingScreen(BlockPos pos, int lineIndex, String initial) {
        super(Component.empty());
        this.pos = pos;
        this.lineIndex = lineIndex;
        CinemaEditState.begin(pos, lineIndex, initial);
    }

    @Override
    protected void init() {
        
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!Character.isISOControl(c)) {
            CinemaEditState.appendChar(c);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            commit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            CinemaEditState.backspace();
            return true;
        }
        return false;
    }

    private void commit() {
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof CinemaScreenBlockEntity screen) {
            List<String> lines = new ArrayList<>();
            for (Component line : screen.getLines()) {
                lines.add(line.getString());
            }
            String newText = CinemaEditState.getBuffer();
            if (lineIndex < lines.size()) {
                lines.set(lineIndex, newText);
            } else {
                lines.add(newText);
            }

            PhoenixNetwork.CHANNEL.sendToServer(new C2SCinemaScreenConfigPacket(
                    pos, lines, screen.getTextColor(), screen.getTextScale(), screen.getTextAlign().ordinal()));
        }

        CinemaEditState.clear();
        onClose();
    }

    private void cancel() {
        CinemaEditState.clear();
        onClose();
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }
}
