package net.phoenix.core.client.stellar;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.phoenix.core.PhoenixCore;

/**
 * What the player themselves feels when a Stellar Forge collapses: the camera shakes as the star tears itself in and the
 * screen whites out in the flash, both fading with distance. The renderer reports each frame what the nearest collapse is
 * doing; this applies it (a frame late, which nobody sees).
 */
@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class StellarCinematic {

    private static float shakeAcc;
    private static float whiteAcc;
    private static float shakeShown;
    private static float whiteShown;

    private StellarCinematic() {}

    /** Start of a frame: what was reported last frame is what is shown, and the tally starts again. */
    static void frame() {
        shakeShown = shakeAcc;
        whiteShown = whiteAcc;
        shakeAcc = 0.0f;
        whiteAcc = 0.0f;
    }

    /** A collapse is shaking the camera by {@code shake} and whiting the screen by {@code white} (both 0 to 1). */
    static void report(float shake, float white) {
        shakeAcc = Math.max(shakeAcc, shake);
        whiteAcc = Math.max(whiteAcc, white);
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shakeShown <= 0.002f) return;
        float t = (System.nanoTime() % 1_000_000_000_000L) / 1.0e9f;
        float k = shakeShown * shakeShown;
        event.setRoll(event.getRoll() + (float) Math.sin(t * 47.0) * 2.4f * k);
        event.setPitch(event.getPitch() + (float) Math.sin(t * 61.0 + 1.3) * 1.6f * k);
        event.setYaw(event.getYaw() + (float) Math.sin(t * 53.0 + 2.1) * 1.6f * k);
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        if (whiteShown <= 0.01f) return;
        var window = Minecraft.getInstance().getWindow();
        int alpha = (int) (255.0f * Mth.clamp(whiteShown, 0.0f, 1.0f));
        event.getGuiGraphics().fill(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight(),
                (alpha << 24) | 0xF4F8FF);
    }
}
