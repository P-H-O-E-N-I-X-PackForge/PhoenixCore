package net.phoenix.core.integration.growth;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

/**
 * The growth control hatch. A growing machine with one fitted has its growth under the player's control: it can be
 * paused, capped at a stage, and its tendrils switched off or limited to chosen sides, and it stops overriding the
 * blocks around it (growth that would run into something waits instead of breaking it). Without one a machine grows
 * wild, always, and overrides whatever is in its way. The hatch holds nothing itself: the machine checks that it is
 * part of the structure.
 */
public class GrowthControlHatch extends MultiblockPartMachine {

    public GrowthControlHatch(BlockEntityCreationInfo info) {
        super(info);
    }
}
