package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class FitScreen extends Screen {

    private final int minW, minH;

    protected float uiScale = 1f;

    protected int vw, vh;
    private Component pendingTip;

    protected FitScreen(Component title, int minW, int minH) {
        super(title);
        this.minW = minW;
        this.minH = minH;
    }

    protected final void fit() {
        uiScale = Math.max(0.1f, Math.min(1f, Math.min(width / (float) minW, height / (float) minH)));
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);
    }

    protected abstract void renderScaled(GuiGraphics g, int mx, int my, float pt);

    protected final void renderWidgets(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
    }

    protected final void tip(Component c) {
        pendingTip = c;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public final void render(GuiGraphics g, int rmx, int rmy, float pt) {
        pendingTip = null;
        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);
        renderScaled(g, Math.round(rmx / uiScale), Math.round(rmy / uiScale), pt);
        g.pose().popPose();
        if (pendingTip != null) g.renderTooltip(font, pendingTip, rmx, rmy);
    }

    protected boolean click(double mx, double my, int btn) {
        return super.mouseClicked(mx, my, btn);
    }

    protected boolean scrolled(double mx, double my, double sx, double sy) {
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public final boolean mouseClicked(double mx, double my, int btn) {
        return click(mx / uiScale, my / uiScale, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        return super.mouseReleased(mx / uiScale, my / uiScale, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        return super.mouseDragged(mx / uiScale, my / uiScale, btn, dx / uiScale, dy / uiScale);
    }

    @Override
    public final boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return scrolled(mx / uiScale, my / uiScale, sx, sy);
    }
}
