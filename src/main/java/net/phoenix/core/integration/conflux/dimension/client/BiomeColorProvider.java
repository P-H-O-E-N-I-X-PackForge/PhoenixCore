package net.phoenix.core.integration.conflux.dimension.client;

import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.conflux.dimension.DisciplineTheme;
import net.phoenix.core.integration.conflux.dimension.DisciplineThemeRegistry;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BiomeColorProvider {

    private static String currentDiscipline;
    private static String currentStage = "initial";
    private static DisciplineTheme.ColorProgression colorOverride;

    public static void setDisciplineProgression(String discipline, String stage) {
        currentDiscipline = discipline;
        currentStage = stage;

        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(discipline);
        if (theme != null) {
            colorOverride = findColorProgression(theme, stage);
        }
    }

    public static int getGrassColor(int originalColor) {
        if (colorOverride != null) {
            return lerpColors(originalColor, colorOverride.grassColor, 0.5f);
        }
        return originalColor;
    }

    public static int getWaterColor(int originalColor) {
        if (colorOverride != null) {
            return lerpColors(originalColor, colorOverride.waterColor, 0.5f);
        }
        return originalColor;
    }

    public static int getFoliageColor(int originalColor) {
        if (colorOverride != null) {
            return lerpColors(originalColor, colorOverride.foliageColor, 0.5f);
        }
        return originalColor;
    }

    private static DisciplineTheme.ColorProgression findColorProgression(DisciplineTheme theme, String milestone) {
        for (DisciplineTheme.ColorProgression progression : theme.colorProgression) {
            if (milestone.equals(progression.milestone)) {
                return progression;
            }
        }
        return null;
    }

    private static int lerpColors(int color1, int color2, float factor) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return (r << 16) | (g << 8) | b;
    }

    public static void reset() {
        currentDiscipline = null;
        currentStage = "initial";
        colorOverride = null;
    }
}
