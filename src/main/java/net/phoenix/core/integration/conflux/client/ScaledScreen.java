package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ScaledScreen extends Screen {

    private static final float MARGIN = 8f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2f;

    private final int panelW, panelH;

    protected float s = 1f;

    protected int px, py;

    protected ScaledScreen(Component title, int panelW, int panelH) {
        super(title);
        this.panelW = panelW;
        this.panelH = panelH;
    }

    protected final void layout() {
        float fit = Math.min((width - 2 * MARGIN) / panelW, (height - 2 * MARGIN) / panelH);
        s = Mth.clamp((float) Math.floor(fit * 4f) / 4f, MIN_SCALE, MAX_SCALE);
        px = Math.round((width / s - panelW) / 2f);
        py = Math.round((height / s - panelH) / 2f);
    }

    protected abstract void renderPanel(GuiGraphics g, int mx, int my, float pt);

    protected final void renderWidgets(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public final void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xC0000000);
        g.pose().pushPose();
        g.pose().scale(s, s, 1f);
        renderPanel(g, (int) (mx / s), (int) (my / s), pt);
        g.pose().popPose();
    }

    protected boolean click(double mx, double my, int btn) {
        return super.mouseClicked(mx, my, btn);
    }

    protected boolean scrolled(double mx, double my, double sx, double sy) {
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public final boolean mouseClicked(double mx, double my, int btn) {
        return click(mx / s, my / s, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        return super.mouseReleased(mx / s, my / s, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        return super.mouseDragged(mx / s, my / s, btn, dx / s, dy / s);
    }

    @Override
    public final boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return scrolled(mx / s, my / s, sx, sy);
    }
}
