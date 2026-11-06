package net.phoenix.core.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.phoenix.core.PhoenixCore;

import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class PhoenixWindowIcon {

    private static final String ICON = "/assets/phoenixcore/textures/gui/window_icon.png";

    private PhoenixWindowIcon() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(PhoenixWindowIcon::apply);
    }

    private static void apply() {
        try (InputStream in = PhoenixWindowIcon.class.getResourceAsStream(ICON)) {
            if (in == null) return;
            try (NativeImage image = NativeImage.read(in); MemoryStack stack = MemoryStack.stackPush()) {
                int w = image.getWidth();
                int h = image.getHeight();
                int[] pixels = image.getPixelsRGBA();
                ByteBuffer data = MemoryUtil.memAlloc(w * h * 4);
                try {
                    data.asIntBuffer().put(pixels);
                    GLFWImage.Buffer icons = GLFWImage.malloc(1, stack);
                    icons.position(0).width(w).height(h).pixels(data);
                    GLFW.glfwSetWindowIcon(Minecraft.getInstance().getWindow().getWindow(), icons);
                } finally {
                    MemoryUtil.memFree(data);
                }
            }
        } catch (Exception e) {
            PhoenixCore.LOGGER.warn("[PFT] could not set the window icon", e);
        }
    }
}
