package net.phoenix.core.client.gui.hud;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widget.Widget;

/** A ModularUI widget that hands its whole area to a {@link CanvasPainter}. */
@OnlyIn(Dist.CLIENT)
public class CanvasWidget extends Widget<CanvasWidget> implements Interactable {

    private final CanvasPainter painter;

    public CanvasWidget(CanvasPainter painter) {
        this.painter = painter;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        var g = context.getGraphics();
        GuiDraw.enableScissor(g, 0, 0, getArea().width, getArea().height);
        painter.render(g, getArea().width, getArea().height, context.getMouseX(), context.getMouseY());
        g.disableScissor();
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        ModularGuiContext ctx = getContext();
        return painter.click(ctx.getMouseX(), ctx.getMouseY(), mouseButton) ? Result.SUCCESS : Result.IGNORE;
    }

    @Override
    public void onMouseDrag(int mouseButton, double dragX, double dragY) {
        ModularGuiContext ctx = getContext();
        painter.drag(ctx.getMouseX(), ctx.getMouseY(), mouseButton);
    }

    @Override
    public boolean onMouseReleased(int mouseButton) {
        painter.release(mouseButton);
        return false;
    }

    @Override
    public boolean onMouseScrolled(double scrollX, double scrollY) {
        ModularGuiContext ctx = getContext();
        return painter.scroll(ctx.getMouseX(), ctx.getMouseY(), scrollY);
    }
}
