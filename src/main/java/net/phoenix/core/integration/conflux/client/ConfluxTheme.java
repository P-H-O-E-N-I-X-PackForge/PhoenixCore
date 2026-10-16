package net.phoenix.core.integration.conflux.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenixvine.wiki.client.screen.WikiTheme;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import org.jetbrains.annotations.Nullable;

/**
 * Colours for the terminal, the node viewer and the wiki. The surface colours follow the shared Phoenix theme (so the
 * theme editor restyles Conflux along with the rest of the suite); the accent follows the player's discipline while the
 * default theme is active, and the theme's own accent otherwise. Call {@link #refresh()} once per frame.
 */
@OnlyIn(Dist.CLIENT)
public final class ConfluxTheme {

    private ConfluxTheme() {}

    public static final String MOD_ID = "phoenixcore";

    public static int BG = 0xFF07070F;
    public static int CANVAS = 0xFF09090E;
    public static int PANEL = 0xFF0F0F1C;
    public static int PANEL_DEEP = 0xFF0A0A15;
    public static int HEADER = 0xFF0B0B18;
    public static int BORDER = 0xFF252540;
    public static int BASE_ACCENT = 0xFF5577FF;
    public static int TEXT = 0xFFCCCCEE;
    public static int DIM = 0xFF555577;
    public static int FAINT = 0xFF3A3A55;

    private static boolean registered;
    private static boolean themed;

    public static void refresh() {
        if (!registered) {
            registered = true;
            PhoenixTheme.registerMod("net.phoenix.core", MOD_ID);
        }
        PhoenixTheme t = PhoenixTheme.current(MOD_ID);
        themed = !"DARK".equalsIgnoreCase(PhoenixTheme.getActiveName(MOD_ID));
        if (t == null) return;
        BG = t.bg.getColor();
        PANEL = t.panel.getColor();
        HEADER = t.header.getColor();
        BORDER = t.border.getColor();
        BASE_ACCENT = t.accent.getColor();
        TEXT = t.text.getColor();
        DIM = t.textDim.getColor();
        FAINT = t.textFaint.getColor();
        CANVAS = darken(BG, 0.85f);
        PANEL_DEEP = darken(PANEL, 0.8f);
    }

    /** The shared theme in the shape the Phoenix Wiki screens take. */
    public static WikiTheme wikiTheme() {
        refresh();
        PhoenixTheme t = PhoenixTheme.current(MOD_ID);
        return new WikiTheme(BG, PANEL, HEADER, BORDER, accent(), TEXT, DIM, FAINT, t.done.getColor(),
                t.activeColor.getColor());
    }

    private static int darken(int argb, float f) {
        int a = argb >>> 24;
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int g = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int accent(@Nullable String discipline) {
        if (themed || discipline == null) return BASE_ACCENT;
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
