package net.phoenix.core.integration.gregvaults.mui;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widget.Widget;

public final class VaultScrollBar extends Widget<VaultScrollBar> implements Interactable {

    private static final int TRACK = 0xFF2B2B2B;
    private static final int THUMB = 0xFF8B8B8B;
    private static final int THUMB_ACTIVE = 0xFFC6C6C6;

    private final VaultSession session;
    private boolean dragging = false;

    public VaultScrollBar(VaultSession session) {
        this.session = session;
    }

    private int maxScroll() {
        return Math.max(0, session.totalRows - VaultWindow.ROWS);
    }

    private int thumbHeight() {
        int h = getArea().height;
        int rows = Math.max(session.totalRows, VaultWindow.ROWS);
        return Math.max(10, h * VaultWindow.ROWS / rows);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        var g = context.getGraphics();
        int w = getArea().width;
        int h = getArea().height;
        g.fill(0, 0, w, h, TRACK);

        int max = maxScroll();
        int thumbH = thumbHeight();
        int thumbY = max > 0 ? (h - thumbH) * session.scrollRow / max : 0;
        g.fill(1, thumbY, w - 1, thumbY + thumbH, dragging || isHovering() ? THUMB_ACTIVE : THUMB);
    }

    private void scrollToMouse() {
        int max = maxScroll();
        if (max <= 0 || session.sync == null) return;
        int h = getArea().height;
        int thumbH = thumbHeight();
        int local = net.phoenix.core.client.gui.PanelFit.localMouse(this, getContext().getAbsMouseX(),
                getContext().getAbsMouseY(), VaultPanel.WIDTH, VaultPanel.HEIGHT)[1] - thumbH / 2;
        float ratio = Math.max(0f, Math.min(1f, local / (float) Math.max(1, h - thumbH)));
        session.sync.requestScroll(Math.round(ratio * max));
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if (mouseButton != 0) return Result.IGNORE;
        dragging = true;
        scrollToMouse();
        return Result.SUCCESS;
    }

    @Override
    public void onMouseDrag(int mouseButton, double dragX, double dragY) {
        if (dragging) scrollToMouse();
    }

    @Override
    public boolean onMouseReleased(int mouseButton) {
        dragging = false;
        return false;
    }

    @Override
    public boolean onMouseScrolled(double scrollX, double scrollY) {
        if (session.sync == null || scrollY == 0) return false;
        session.sync.requestScroll(session.scrollRow + (scrollY < 0 ? 1 : -1));
        return true;
    }
}
