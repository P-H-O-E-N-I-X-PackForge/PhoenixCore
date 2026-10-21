package net.phoenix.core.integration.solaris;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.client.ClientResearchCache;
import net.phoenix.core.integration.conflux.client.ConfluxTheme;
import net.phoenix.core.integration.conflux.research.DisciplineInfo;
import net.phoenixvine.solaris.api.SolarisAPI;
import net.phoenixvine.solaris.client.SolarisThemeUtils;
import net.phoenixvine.solaris.client.render.UnexploredStyle;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Makes the Solaris map look like it belongs to the player's discipline: the accent colour follows the
 * Conflux discipline (the dimension's own inside a Conflux dimension, otherwise the one the player
 * chose), and each Conflux dimension gets its own fog of war. Only reached when Solaris is loaded.
 */
@OnlyIn(Dist.CLIENT)
final class SolarisSkinBridge {

    private static final Map<String, UnexploredStyle> FOG = Map.of(
            "phoenix", UnexploredStyle.PHOENIX,
            "void", UnexploredStyle.STARFIELD,
            "sculk", UnexploredStyle.CLOUD,
            "sealed_a", UnexploredStyle.FOG,
            "sealed_b", UnexploredStyle.FOG);

    private static boolean fogApplied = false;

    private SolarisSkinBridge() {}

    static void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            SolarisThemeUtils.setAccentOverride(null);
            return;
        }

        if (!fogApplied) {
            fogApplied = true;
            for (Map.Entry<String, UnexploredStyle> fog : FOG.entrySet()) {
                SolarisAPI.setUnexploredStyle(ResourceLocation.fromNamespaceAndPath("phoenixcore",
                        "conflux/" + fog.getKey()), fog.getValue());
            }
        }

        ConfluxTheme.refresh();
        String discipline = disciplineFor(mc.level);
        Integer accent = null;
        if (discipline != null) {
            int color = ConfluxTheme.accent(discipline);
            if (color != ConfluxTheme.BASE_ACCENT) accent = color;
        }
        SolarisThemeUtils.setAccentOverride(accent);
    }

    static void clear() {
        SolarisThemeUtils.setAccentOverride(null);
    }

    private static @Nullable String disciplineFor(Level level) {
        ResourceLocation dimension = level.dimension().location();
        if (dimension.getNamespace().equals("phoenixcore") && dimension.getPath().startsWith("conflux/")) {
            return dimension.getPath().substring("conflux/".length());
        }
        DisciplineInfo info = ClientResearchCache.getDisciplineInfo();
        return info.hasDiscipline() ? info.disciplineId() : null;
    }
}
