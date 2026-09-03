package net.phoenix.core.client.renderer.cinema;

import net.minecraft.core.BlockPos;

public final class CinemaEditState {

    private CinemaEditState() {}

    private static BlockPos activePos;
    private static int activeLineIndex = -1;
    private static StringBuilder buffer = new StringBuilder();

    public static boolean isEditing(BlockPos pos) {
        return activePos != null && activePos.equals(pos);
    }

    public static int getActiveLineIndex() {
        return activeLineIndex;
    }

    public static String getBuffer() {
        return buffer.toString();
    }

    public static void begin(BlockPos pos, int lineIndex, String initial) {
        activePos = pos;
        activeLineIndex = lineIndex;
        buffer = new StringBuilder(initial);
    }

    public static void appendChar(char c) {
        if (buffer.length() < 128) {
            buffer.append(c);
        }
    }

    public static void backspace() {
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
        }
    }

    public static void clear() {
        activePos = null;
        activeLineIndex = -1;
        buffer = new StringBuilder();
    }
}
