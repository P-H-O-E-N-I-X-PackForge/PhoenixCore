package net.phoenix.core.client.fx;

import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;

/**
 * A copy of the depth of everything solid, taken just before the translucent blocks (water, glass, ice) are drawn. The
 * volume shaders stop their rays at this, not at the live depth buffer: the live one has water and glass in it by the
 * time they are drawn, which would hide a volume behind them completely, and the volumes are drawn after the
 * translucent
 * blocks, so that water and glass lie over them and they show through. Solid blocks, entities and block entities still
 * hide them.
 */
public final class SceneDepth {

    private static TextureTarget copy;
    private static boolean valid;

    private SceneDepth() {}

    /** Takes the copy. Call it when the solid things have been drawn and the translucent ones have not. */
    public static void capture() {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (copy == null || copy.width != main.width || copy.height != main.height) {
            if (copy != null) copy.destroyBuffers();
            copy = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        }
        // errors from before are not this copy's
        while (org.lwjgl.opengl.GL11.glGetError() != org.lwjgl.opengl.GL11.GL_NO_ERROR) {}
        copy.copyDepthFrom(main);
        // copying leaves another target bound, and the rest of the frame draws into the main one
        main.bindWrite(false);
        // The blit fails when the two depth attachments are not the same format, which happens with some setups, and
        // the
        // copy is then left empty: the volumes were drawn through every block. Without a copy the live depth is used,
        // which has water and glass in it but does hide volumes behind solid blocks.
        valid = org.lwjgl.opengl.GL11.glGetError() == org.lwjgl.opengl.GL11.GL_NO_ERROR;
    }

    /** Lets go of the copy once the volumes of this frame have been drawn. */
    public static void release() {
        valid = false;
    }

    /** The depth texture the shaders should read: the copy if there is one for this frame, else the live depth. */
    public static int textureId() {
        if (valid && copy != null) return copy.getDepthTextureId();
        return Minecraft.getInstance().getMainRenderTarget().getDepthTextureId();
    }
}
