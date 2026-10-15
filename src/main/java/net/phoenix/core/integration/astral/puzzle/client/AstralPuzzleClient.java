package net.phoenix.core.integration.astral.puzzle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.astral.puzzle.StarChart;

@OnlyIn(Dist.CLIENT)
public final class AstralPuzzleClient {

    private AstralPuzzleClient() {}

    public static void openCalibration(BlockPos pos, StarChart.Kind kind, int[] stars, boolean calibrated) {
        Minecraft.getInstance().setScreen(new CalibrationScreen(pos, kind, stars, calibrated));
    }

    public static void calibrationResult(BlockPos pos, boolean solved, boolean aligned, int right, int total,
                                         int drawn, String lost) {
        if (Minecraft.getInstance().screen instanceof CalibrationScreen screen && screen.isFor(pos)) {
            screen.showResult(solved, aligned, right, total, drawn, lost);
        }
    }

    public static void openSurvey(ItemStack stack) {
        Minecraft.getInstance().setScreen(new SurveyScreen(stack));
    }
}
