package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class CinemaScreenClientHelper {

    private CinemaScreenClientHelper() {}

    public static void openTypingScreen(BlockPos pos, int lineIndex, String initial) {
        Minecraft.getInstance().setScreen(new CinemaTypingScreen(pos, lineIndex, initial));
    }
}
