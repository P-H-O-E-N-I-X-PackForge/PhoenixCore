package net.phoenix.core.integration.vocal_resonance.puzzle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.vocal_resonance.puzzle.AcousticChart;

import java.util.List;

/** Entry points the attunement packets call on the client. */
@OnlyIn(Dist.CLIENT)
public final class AttuneClient {

    private AttuneClient() {}

    public static void open(BlockPos pos, int bpm, List<Integer> laneNotes, List<AcousticChart.Beat> beats) {
        Minecraft.getInstance().setScreen(new AttuneScreen(pos, bpm, laneNotes, beats));
    }

    public static void result(BlockPos pos, boolean passed, int grade, int perfect, int good, int total) {
        if (Minecraft.getInstance().screen instanceof AttuneScreen screen && screen.isFor(pos)) {
            screen.showResult(passed, grade, perfect, good, total);
        }
    }
}
