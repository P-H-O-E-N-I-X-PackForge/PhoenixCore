package net.phoenix.core.integration.vocal_resonance.puzzle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.vocal_resonance.puzzle.AcousticChart;
import net.phoenix.core.integration.vocal_resonance.puzzle.AttuneDifficulty;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class AttuneClient {

    private AttuneClient() {}

    public static void openDifficulty(BlockPos pos, int tuneLevel) {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> mc.tell(() -> mc.setScreen(new AttuneDifficultyScreen(pos, tuneLevel))));
    }

    public static void open(BlockPos pos, int difficultyLevel, int bpm, List<Integer> laneNotes,
                            List<AcousticChart.Beat> beats) {
        Minecraft.getInstance().setScreen(
                new AttuneScreen(pos, AttuneDifficulty.fromLevel(difficultyLevel), bpm, laneNotes, beats));
    }

    public static void result(BlockPos pos, boolean passed, int grade, int perfect, int good, int total) {
        if (Minecraft.getInstance().screen instanceof AttuneScreen screen && screen.isFor(pos)) {
            screen.showResult(passed, grade, perfect, good, total);
        }
    }
}
