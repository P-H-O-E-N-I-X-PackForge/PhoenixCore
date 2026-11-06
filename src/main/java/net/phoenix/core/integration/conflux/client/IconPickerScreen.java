package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class IconPickerScreen extends Screen {

    private static final String[] TABS = { "Items", "Fluids", "Pictures" };
    private static final int CELL = 22;
    private static final int PANEL_W = 520;
    private static final int PANEL_H = 320;
    private static final int MAX_SHOWN = 1500;
    private static final int C_BG = 0xF00F0F1C;

    private final Screen parent;
    private final Consumer<String> onPick;
    private final int accent;
    private int tab = 0;
    private EditBox search;
    private List<String> all = List.of();
    private List<String> shown = List.of();
    private int scrollRows = 0;
    private int px, py;
    private String hoverName = "";

    public IconPickerScreen(Screen parent, Consumer<String> onPick, int accent) {
        super(Component.literal("Pick an icon"));
        this.parent = parent;
        this.onPick = onPick;
        this.accent = accent;
        load();
    }

    @Override
    protected void init() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        String keep = search == null ? "" : search.getValue();
        search = new EditBox(font, px + 10, py + 50, PANEL_W - 20, 14, Component.empty());
        search.setHint(Component.literal("search"));
        search.setMaxLength(120);
        search.setValue(keep);
        search.setResponder(v -> filter());
        addRenderableWidget(search);
        setInitialFocus(search);
        filter();
    }

    private void load() {
        List<String> out = new ArrayList<>();
        switch (tab) {
            case 0 -> BuiltInRegistries.ITEM.keySet().forEach(id -> out.add(id.toString()));
            case 1 -> BuiltInRegistries.FLUID.keySet().forEach(id -> {
                Fluid f = BuiltInRegistries.FLUID.get(id);
                if (f != Fluids.EMPTY && f.isSource(f.defaultFluidState())) out.add(ConfluxIcons.FLUID + id);
            });
            default -> Minecraft.getInstance().getResourceManager()
                    .listResources("textures", rl -> rl.getPath().endsWith(".png")).keySet()
                    .forEach(rl -> out.add(ConfluxIcons.TEXTURE + rl));
        }
        java.util.Collections.sort(out);
        all = out;
    }

    private void filter() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : all) {
            if (q.isEmpty() || s.toLowerCase().contains(q)) {
                out.add(s);
                if (out.size() >= MAX_SHOWN) break;
            }
        }
        shown = out;
        scrollRows = 0;
    }

    private int cols() {
        return (PANEL_W - 20) / CELL;
    }

    private int gridTop() {
        return py + 72;
    }

    private int gridRows() {
        return (PANEL_H - 72 - 26) / CELL;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        ConfluxTheme.refresh();
        g.fill(0, 0, width, height, 0xC0000000);
        g.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, accent);
        g.fill(px, py, px + PANEL_W, py + PANEL_H, C_BG);
        g.fillGradient(px, py, px + PANEL_W, py + 24, ConfluxTheme.withAlpha(accent, 50), 0x00000000);
        g.drawString(font, "§ePick an icon", px + 10, py + 9, 0xFFFFFF, false);

        for (int i = 0; i < TABS.length; i++) {
            int w = PANEL_W / TABS.length;
            int x = px + i * w;
            boolean active = i == tab;
            boolean hov = mx >= x && mx < x + w && my >= py + 24 && my < py + 44;
            g.fill(x, py + 24, x + w, py + 44, active ? 0xFF22223A : hov ? 0xFF1A1A2C : 0xFF121222);
            g.renderOutline(x, py + 24, w, 20, active ? accent : ConfluxTheme.BORDER);
            g.drawCenteredString(font, (active ? "§f" : "§7") + TABS[i], x + w / 2, py + 30, 0xFFFFFF);
        }

        int cols = cols(), rows = gridRows(), top = gridTop();
        int maxScroll = Math.max(0, (shown.size() + cols - 1) / cols - rows);
        scrollRows = Math.min(scrollRows, maxScroll);
        g.fill(px + 8, top - 2, px + PANEL_W - 8, top + rows * CELL + 2, 0xFF0A0A14);
        hoverName = "";
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = (scrollRows + r) * cols + c;
                if (i >= shown.size()) break;
                int x = px + 10 + c * CELL, y = top + r * CELL;
                boolean hov = mx >= x && mx < x + CELL && my >= y && my < y + CELL;
                if (hov) {
                    g.fill(x, y, x + CELL, y + CELL, 0xFF22223A);
                    g.renderOutline(x, y, CELL, CELL, accent);
                    hoverName = shown.get(i);
                }
                ConfluxIcons.draw(g, shown.get(i), x + 3, y + 3);
            }
        }
        if (shown.isEmpty()) {
            g.drawString(font, "§8nothing matches", px + 14, top + 4, 0xFFFFFF, false);
        }
        String footer = hoverName.isEmpty() ? shown.size() + (shown.size() >= MAX_SHOWN ? "+" : "") +
                " icons · click one · Esc cancels" : ConfluxIcons.label(hoverName);
        g.drawString(font, "§8" + font.plainSubstrByWidth(footer, PANEL_W - 20), px + 10, py + PANEL_H - 16, 0xFFFFFF,
                false);
        if (tab == 2) {
            g.drawString(font, "§8Pictures: any .png in a mod or resource pack (assets/<namespace>/textures/…)",
                    px + 10, py + 38 + 28, 0xFFFFFF, false);
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        if (btn != 0) return false;
        for (int i = 0; i < TABS.length; i++) {
            int w = PANEL_W / TABS.length;
            if (mx >= px + i * w && mx < px + (i + 1) * w && my >= py + 24 && my < py + 44) {
                tab = i;
                search.setValue("");
                load();
                filter();
                return true;
            }
        }
        int cols = cols();
        int c = (int) ((mx - px - 10) / CELL);
        int r = (int) ((my - gridTop()) / CELL);
        if (c >= 0 && c < cols && r >= 0 && r < gridRows() && mx >= px + 10 && my >= gridTop()) {
            int i = (scrollRows + r) * cols + c;
            if (i < shown.size()) {
                onPick.accept(shown.get(i));
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double delta) {
        scrollRows = Math.max(0, scrollRows - (int) Math.signum(delta) * 2);
        return true;
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
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
