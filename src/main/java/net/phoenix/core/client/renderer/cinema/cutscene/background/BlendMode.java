package net.phoenix.core.client.renderer.cinema.cutscene.background;

import net.minecraft.util.StringRepresentable;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** How a {@link CutsceneBackgroundBuilder} layer is composited onto the layers below it. */
public enum BlendMode implements StringRepresentable {

    NORMAL,
    ADD,
    SCREEN,
    MULTIPLY;

    public static final Codec<BlendMode> CODEC = StringRepresentable.fromEnum(BlendMode::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int composite(int dst, int src) {
        int dstA = (dst >>> 24) & 0xFF, srcA = (src >>> 24) & 0xFF;
        if (dstA == 0) return src;
        if (srcA == 0) return dst;

        float da = dstA / 255f, sa = srcA / 255f;
        int dr = (dst >> 16) & 0xFF, dg = (dst >> 8) & 0xFF, db = dst & 0xFF;
        int sr = (src >> 16) & 0xFF, sg = (src >> 8) & 0xFF, sb = src & 0xFF;

        int rr, rg, rb;
        switch (this) {
            case ADD -> {
                rr = Math.min(255, dr + Math.round(sr * sa));
                rg = Math.min(255, dg + Math.round(sg * sa));
                rb = Math.min(255, db + Math.round(sb * sa));
            }
            case SCREEN -> {
                rr = 255 - Math.round((255 - dr) * (255 - Math.round(sr * sa)) / 255f);
                rg = 255 - Math.round((255 - dg) * (255 - Math.round(sg * sa)) / 255f);
                rb = 255 - Math.round((255 - db) * (255 - Math.round(sb * sa)) / 255f);
            }
            case MULTIPLY -> {
                rr = Math.round(dr * (sr * sa + 255 * (1 - sa)) / 255f);
                rg = Math.round(dg * (sg * sa + 255 * (1 - sa)) / 255f);
                rb = Math.round(db * (sb * sa + 255 * (1 - sa)) / 255f);
            }
            default -> {
                rr = Math.round(dr * (1f - sa) + sr * sa);
                rg = Math.round(dg * (1f - sa) + sg * sa);
                rb = Math.round(db * (1f - sa) + sb * sa);
            }
        }

        int ra = Math.round((sa + da * (1f - sa)) * 255f);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
