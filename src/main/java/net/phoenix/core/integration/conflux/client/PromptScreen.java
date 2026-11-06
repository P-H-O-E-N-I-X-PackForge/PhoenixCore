package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class PromptScreen extends Screen {

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 96;

    private final Screen parent;
    private final String heading;
    private final String label;
    private final String initial;
    private final Consumer<String> onDone;
    private EditBox box;
    private int px, py;
    private String kept;

    public PromptScreen(Screen parent, String heading, String label, String initial, Consumer<String> onDone) {
        super(Component.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.label = label;
        this.initial = initial;
        this.kept = initial;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        if (box != null) kept = box.getValue();
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        box = new EditBox(font, px + 12, py + 40, PANEL_W - 24, 16, Component.empty());
        box.setMaxLength(200);
        box.setValue(kept);
        addRenderableWidget(box);
        setInitialFocus(box);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        ConfluxTheme.refresh();
        int accent = ConfluxTheme.accent();
        g.fill(0, 0, width, height, 0xC0000000);
        g.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, accent);
        g.fill(px, py, px + PANEL_W, py + PANEL_H, ConfluxTheme.PANEL);
        g.fillGradient(px, py, px + PANEL_W, py + 24, ConfluxTheme.withAlpha(accent, 50), 0x00000000);
        g.drawString(font, "§e" + heading, px + 12, py + 9, 0xFFFFFF, false);
        g.drawString(font, "§7" + label, px + 14, py + 29, 0xFFFFFF, false);
        super.render(g, mx, my, pt);
        button(g, px + PANEL_W - 190, py + PANEL_H - 24, 86, "§aOK", mx, my);
        button(g, px + PANEL_W - 98, py + PANEL_H - 24, 86, "§fCancel", mx, my);
    }

    private void button(GuiGraphics g, int x, int y, int w, String text, int mx, int my) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 16;
        g.fill(x, y, x + w, y + 16, hov ? 0xFF22223A : 0xFF14142A);
        g.renderOutline(x, y, w, 16, ConfluxTheme.BORDER);
        g.drawCenteredString(font, text, x + w / 2, y + 4, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        int y = py + PANEL_H - 24;
        if (btn == 0 && my >= y && my < y + 16) {
            if (mx >= px + PANEL_W - 190 && mx < px + PANEL_W - 104) {
                finish();
                return true;
            }
            if (mx >= px + PANEL_W - 98 && mx < px + PANEL_W - 12) {
                onClose();
                return true;
            }
        }
        return false;
    }

    private void finish() {
        String v = box.getValue().trim();
        if (!v.isEmpty()) onDone.accept(v);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (kc == GLFW.GLFW_KEY_ENTER || kc == GLFW.GLFW_KEY_KP_ENTER) {
            finish();
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
