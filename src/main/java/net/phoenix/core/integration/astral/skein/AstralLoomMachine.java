package net.phoenix.core.integration.astral.skein;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;

import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;
import net.phoenix.core.integration.astral.puzzle.StarChart;

public class AstralLoomMachine extends AstralCalibratedMachine {

    public AstralLoomMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    @Override
    public StarChart.Kind chartKind() {
        return StarChart.Kind.WEB;
    }
}
