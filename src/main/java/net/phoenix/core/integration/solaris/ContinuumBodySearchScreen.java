package net.phoenix.core.integration.solaris;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.continuum.client.ContinuumClientState;
import net.phoenix.continuum.data.ContinuumBody;
import net.phoenix.continuum.data.ContinuumData;
import net.phoenix.continuum.data.ContinuumSystem;
import net.phoenix.continuum.data.DiscoveryStage;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Search every Continuum body you have found: type to filter by name, system, type or stage, narrow by kind with the
 * chips, click (or Enter for the first match) to open it on the page. Opened from the Continuum page with F.
 */
@OnlyIn(Dist.CLIENT)
final class ContinuumBodySearchScreen extends Screen {

    private enum Kind {

        ALL("All"),
        PLANET("Planets"),
        MOON("Moons"),
        CENTRAL("Stars / holes"),
        OUTPOST("Outposts");

        final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    private static final int W = 360;
    private static final int ROW = 16;

    private final @Nullable Screen parent;
    private final List<ContinuumBody> all;
    private final Consumer<ContinuumBody> onPick;
    private final List<ContinuumBody> shown = new ArrayList<>();
    private EditBox box;
    private Kind kind = Kind.ALL;
    private int scroll;

    ContinuumBodySearchScreen(@Nullable Screen parent, List<ContinuumBody> bodies, Consumer<ContinuumBody> onPick) {
        super(Component.literal("Search bodies"));
        this.parent = parent;
        this.all = bodies;
        this.onPick = onPick;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return 28;
    }

    private int visibleRows() {
        return Math.max(4, (height - top() - 40 - 40) / ROW);
    }

    @Override
    protected void init() {
        box = new EditBox(font, left() + 8, top() + 22, W - 16, 16, Component.literal("Search"));
        box.setHint(Component.literal("Name, system, type or stage"));
        box.setResponder(text -> {
            scroll = 0;
            refilter();
        });
        addRenderableWidget(box);
        setInitialFocus(box);
        refilter();
    }

    private String system(ContinuumBody body) {
        ContinuumSystem system = ContinuumData.system(body.system());
        return system != null ? system.name() : "";
    }

    private void refilter() {
        String query = box == null ? "" : box.getValue().trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (ContinuumBody body : all) {
            if (!matchesKind(body)) continue;
            if (!query.isEmpty()) {
                String hay = (body.name() + " " + system(body) + " " + body.type().label() + " " +
                        ContinuumClientState.stage(body.id()).label()).toLowerCase(Locale.ROOT);
                boolean all = true;
                for (String part : query.split("\\s+")) all &= hay.contains(part);
                if (!all) continue;
            }
            shown.add(body);
        }
    }

    private boolean matchesKind(ContinuumBody body) {
        return switch (kind) {
            case ALL -> true;
            case PLANET -> body.type() == ContinuumBody.Type.PLANET;
            case MOON -> body.type() == ContinuumBody.Type.MOON;
            case CENTRAL -> body.type() == ContinuumBody.Type.STAR || body.type() == ContinuumBody.Type.BLACK_HOLE;
            case OUTPOST -> ContinuumClientState.outpost(body.id()) != null;
        };
    }

    /** No 1.21 background blur: this screen paints its own backdrop. */
    @Override
    protected void renderBlurredBackground(float partialTick) {}

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0xE00A0E14);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g, mx, my, partialTick);
        int x = left();
        int y = top();
        int panelH = Math.min(height - 24 - y, 62 + visibleRows() * ROW + 8);
        g.fill(x, y, x + W, y + panelH, 0xF0141A22);
        g.renderOutline(x, y, W, panelH, 0xFF3A4452);
        g.drawString(font, "Search bodies", x + 8, y + 7, 0xFFFFFFFF, false);
        String count = shown.size() + " / " + all.size();
        g.drawString(font, count, x + W - 8 - font.width(count), y + 7, 0xFF99A6B8, false);

        // kind chips
        int cx = x + 8;
        int cy = y + 42;
        for (Kind k : Kind.values()) {
            int w = font.width(k.label) + 10;
            boolean hover = mx >= cx && mx < cx + w && my >= cy && my < cy + 13;
            g.fill(cx, cy, cx + w, cy + 13, k == kind ? 0xFF2F6FB0 : hover ? 0xFF2A3340 : 0xFF1E2630);
            g.drawString(font, k.label, cx + 5, cy + 3, 0xFFDDE4EE, false);
            cx += w + 4;
        }

        int listY = y + 60;
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, shown.size() - rows)));
        for (int i = 0; i < rows && i + scroll < shown.size(); i++) {
            ContinuumBody body = shown.get(i + scroll);
            int ry = listY + i * ROW;
            boolean hover = mx >= x + 4 && mx < x + W - 4 && my >= ry && my < ry + ROW;
            if (hover) g.fill(x + 4, ry, x + W - 4, ry + ROW, 0x30FFFFFF);
            DiscoveryStage stage = ContinuumClientState.stage(body.id());
            var outpost = ContinuumClientState.outpost(body.id());
            g.fill(x + 10, ry + 5, x + 16, ry + 11, outpost != null ? 0xFFFFB040 :
                    stage == DiscoveryStage.SURVEYED ? 0xFF66DDFF : 0xFF7F8A99);
            g.drawString(font, font.plainSubstrByWidth(body.name(), 130), x + 22, ry + 4, 0xFFFFFFFF, false);
            g.drawString(font, font.plainSubstrByWidth(system(body), 90), x + 156, ry + 4, 0xFF99A6B8, false);
            String right = body.type().label() + "  " + stage.label();
            g.drawString(font, right, x + W - 10 - font.width(right), ry + 4, 0xFF7F8A99, false);
        }
        if (shown.isEmpty()) {
            g.drawCenteredString(font, "Nothing matches", x + W / 2, listY + 10, 0xFF99A6B8);
        }

        g.drawCenteredString(font, "Enter: open first match   click: open   Esc: back", width / 2, y + panelH + 8,
                0xFF99A6B8);
        super.render(g, mx, my, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;

        int x = left();
        int y = top();
        int cx = x + 8;
        int cy = y + 42;
        for (Kind k : Kind.values()) {
            int w = font.width(k.label) + 10;
            if (mx >= cx && mx < cx + w && my >= cy && my < cy + 13) {
                kind = k;
                scroll = 0;
                refilter();
                return true;
            }
            cx += w + 4;
        }

        int row = (int) ((my - (y + 60)) / ROW);
        if (my >= y + 60 && row >= 0 && row < visibleRows() && row + scroll < shown.size() && mx >= x + 4 &&
                mx < x + W - 4) {
            pick(shown.get(row + scroll));
            return true;
        }
        return false;
    }

    private void pick(ContinuumBody body) {
        onPick.accept(body);
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY) * 2;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (!shown.isEmpty()) pick(shown.get(0));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
