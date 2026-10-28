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

    /** The mouse in this widget's own pixels: the absolute mouse less the widget's absolute corner. */
    private int localX(ModularGuiContext ctx) {
        return ctx.getAbsMouseX() - getArea().x;
    }

    private int localY(ModularGuiContext ctx) {
        return ctx.getAbsMouseY() - getArea().y;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        var g = context.getGraphics();
        GuiDraw.enableScissor(g, 0, 0, getArea().width, getArea().height);
        painter.render(g, getArea().width, getArea().height, localX(context), localY(context));
        g.disableScissor();
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        ModularGuiContext ctx = getContext();
        return painter.click(localX(ctx), localY(ctx), mouseButton) ? Result.SUCCESS : Result.IGNORE;
    }

    @Override
    public void onMouseDrag(int mouseButton, double dragX, double dragY) {
        ModularGuiContext ctx = getContext();
        painter.drag(localX(ctx), localY(ctx), mouseButton);
    }

    @Override
    public boolean onMouseReleased(int mouseButton) {
        painter.release(mouseButton);
        return false;
    }

    @Override
    public boolean onMouseScrolled(double scrollX, double scrollY) {
        ModularGuiContext ctx = getContext();
        return painter.scroll(localX(ctx), localY(ctx), scrollY);
    }
}
