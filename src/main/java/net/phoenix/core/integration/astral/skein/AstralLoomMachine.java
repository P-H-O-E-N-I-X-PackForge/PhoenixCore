package net.phoenix.core.integration.astral.skein;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;

import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;
import net.phoenix.core.integration.astral.puzzle.StarChart;

/** Weaves Skein into Ensorcelled Weave. Its chart is a web: a loop with cross-links, in any order. */
public class AstralLoomMachine extends AstralCalibratedMachine {

    public AstralLoomMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    @Override
    public StarChart.Kind chartKind() {
        return StarChart.Kind.WEB;
    }
}
