package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class GuiScissor {

    private GuiScissor() {}

    public static void enable(GuiGraphics g, int x0, int y0, int x1, int y1) {
        Matrix4f m = g.pose().last().pose();
        Vector3f a = m.transformPosition(x0, y0, 0f, new Vector3f());
        Vector3f b = m.transformPosition(x1, y1, 0f, new Vector3f());
        g.enableScissor((int) Math.floor(Math.min(a.x, b.x)), (int) Math.floor(Math.min(a.y, b.y)),
                (int) Math.ceil(Math.max(a.x, b.x)), (int) Math.ceil(Math.max(a.y, b.y)));
    }
}
