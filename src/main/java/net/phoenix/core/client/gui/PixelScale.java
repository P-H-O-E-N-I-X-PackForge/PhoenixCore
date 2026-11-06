package net.phoenix.core.client.gui;

import net.minecraft.client.Minecraft;

public final class PixelScale {

    private static final float MIN_SHARE = 0.85f;

    private PixelScale() {}

    public static float bottomReserve() {
        return net.neoforged.fml.ModList.get().isLoaded("emi") ? 24f : 0f;
    }

    public static float snap(float fit) {
        if (fit >= 1f) return 1f;
        double gui = Minecraft.getInstance().getWindow().getGuiScale();
        double pixels = Math.floor(fit * gui + 1e-4);
        if (pixels < 1) return fit;
        float whole = (float) (pixels / gui);
        return whole >= fit * MIN_SHARE ? whole : fit;
    }
}
