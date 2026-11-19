package net.phoenix.core.client.fx;

import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;

public final class SceneDepth {

    private static TextureTarget copy;
    private static boolean valid;

    private SceneDepth() {}

    public static void capture() {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (copy == null || copy.width != main.width || copy.height != main.height) {
            if (copy != null) copy.destroyBuffers();
            copy = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        }

        while (org.lwjgl.opengl.GL11.glGetError() != org.lwjgl.opengl.GL11.GL_NO_ERROR) {}
        copy.copyDepthFrom(main);

        main.bindWrite(false);

        valid = org.lwjgl.opengl.GL11.glGetError() == org.lwjgl.opengl.GL11.GL_NO_ERROR;
    }

    public static void release() {
        valid = false;
    }

    public static int textureId() {
        if (valid && copy != null) return copy.getDepthTextureId();
        return Minecraft.getInstance().getMainRenderTarget().getDepthTextureId();
    }
}
