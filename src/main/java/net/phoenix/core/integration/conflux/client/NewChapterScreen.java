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
public final class NewChapterScreen extends Screen {

    public record Result(String title, String group, String discipline, String description, int order) {}

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 262;

    private final Screen parent;
    private final Consumer<Result> onDone;
    private final boolean editing;
    private final Result initial;
    private EditBox title, group, discipline, description, order;
    private int px, py;
    private Result kept;

    public NewChapterScreen(Screen parent, boolean editing, Result initial, Consumer<Result> onDone) {
        super(Component.literal(editing ? "Edit chapter" : "New chapter"));
        this.parent = parent;
        this.editing = editing;
        this.initial = initial;
        this.kept = initial;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        if (title != null) kept = read();
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        title = box(py + 38, "chapter title", kept.title());
        group = box(py + 74, "category, such as Discipline/Tier 1", kept.group());
        discipline = box(py + 110, "discipline id (empty = every discipline)", kept.discipline());
        description = box(py + 146, "what the chapter is about", kept.description());
        order = box(py + 182, "0", kept.order() == 0 ? "" : String.valueOf(kept.order()));
        order.setFilter(s -> s.matches("-?[0-9]*"));
        order.setWidth(80);
        setInitialFocus(title);
    }

    private Result read() {
        int o = 0;
        try {
            o = Integer.parseInt(order.getValue().trim());
        } catch (NumberFormatException ignored) {}
        return new Result(title.getValue().trim(), group.getValue().trim(), discipline.getValue().trim(),
                description.getValue(), o);
    }

    private EditBox box(int y, String hint, String value) {
        EditBox b = new EditBox(font, px + 12, y, PANEL_W - 24, 16, Component.empty());
        b.setHint(Component.literal(hint));
        b.setMaxLength(400);
        b.setValue(value);
        b.moveCursorToStart(false);
        addRenderableWidget(b);
        return b;
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
        g.drawString(font, editing ? "§eEdit chapter" : "§eNew chapter", px + 12, py + 9, 0xFFFFFF, false);
        String[] labels = { "Title", "Category", "Discipline §8(optional)", "Description", "Order in its category" };
        for (int i = 0; i < labels.length; i++) {
            g.drawString(font, "§7" + labels[i], px + 14, py + 28 + i * 36, 0xFFFFFF, false);
        }
        if (!editing) {
            g.drawString(font, "§8It starts with one node; right-click the canvas to add more.", px + 14, py + 208,
                    0xFFFFFF, false);
        }

        super.render(g, mx, my, pt);

        boolean ready = !title.getValue().isBlank();
        button(g, px + PANEL_W - 190, py + PANEL_H - 28, 86, ready ? (editing ? "§aSave" : "§aCreate") :
                "§8Save", mx, my, ready);
        button(g, px + PANEL_W - 98, py + PANEL_H - 28, 86, "§fCancel", mx, my, true);
    }

    private void button(GuiGraphics g, int x, int y, int w, String label, int mx, int my, boolean enabled) {
        boolean hov = enabled && mx >= x && mx < x + w && my >= y && my < y + 16;
        g.fill(x, y, x + w, y + 16, hov ? 0xFF22223A : 0xFF14142A);
        g.renderOutline(x, y, w, 16, ConfluxTheme.BORDER);
        g.drawCenteredString(font, label, x + w / 2, y + 4, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        int y = py + PANEL_H - 28;
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
        if (title.getValue().isBlank()) return;
        Result r = read();
        onDone.accept(r.group().isEmpty() ? new Result(r.title(), initial.group(), r.discipline(), r.description(),
                r.order()) : r);
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
