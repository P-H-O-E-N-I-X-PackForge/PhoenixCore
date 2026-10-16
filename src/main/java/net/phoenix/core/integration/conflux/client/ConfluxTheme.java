package net.phoenix.core.integration.conflux.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.ConfluxDataType;

import org.jetbrains.annotations.Nullable;

/** Colours and small formatting helpers shared by the terminal and the node viewer. */
@OnlyIn(Dist.CLIENT)
public final class ConfluxTheme {

    private ConfluxTheme() {}

    public static final int BG = 0xFF07070F;
    public static final int PANEL = 0xFF0F0F1C;
    public static final int PANEL_DEEP = 0xFF0A0A15;
    public static final int HEADER = 0xFF0B0B18;
    public static final int BORDER = 0xFF252540;
    public static final int BASE_ACCENT = 0xFF5577FF;
    public static final int TEXT = 0xFFCCCCEE;
    public static final int DIM = 0xFF555577;
    public static final int FAINT = 0xFF3A3A55;

    public static int accent(@Nullable String discipline) {
        if (discipline == null) return BASE_ACCENT;
        return switch (discipline) {
            case "phoenix" -> 0xFFFF8A3D;
            case "sculk" -> 0xFF2FC4B0;
            case "void" -> 0xFF9B6BFF;
            case "sealed_a", "sealed_b" -> 0xFFC8B070;
            default -> BASE_ACCENT;
        };
    }

    public static int accent() {
        return accent(ClientResearchCache.getDisciplineInfo().disciplineId());
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int typeColor(ConfluxDataType type) {
        return switch (type) {
            case MATERIAL -> 0xFFCC8800;
            case BIOLOGICAL -> 0xFF22AA44;
            case ENERGETIC -> 0xFF2299CC;
            case COMPUTATIONAL -> 0xFF9933CC;
            case ARCANE -> 0xFF8844CC;
        };
    }

    public static String amount(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    public static ItemStack icon(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        return new ItemStack(item == null ? net.minecraft.world.item.Items.BOOK : item);
    }
}
