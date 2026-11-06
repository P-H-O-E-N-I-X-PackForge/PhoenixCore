package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.IRoutePath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

public class ConfluxRoutePath implements IRoutePath<ConfluxEndpoint> {

    private final BlockPos targetPipePos;
    private final Direction targetFacing;
    private final ConfluxPipeBlockEntity[] path;
    private final int distance;

    public ConfluxRoutePath(BlockPos targetPipePos, Direction targetFacing, ConfluxPipeBlockEntity[] path,
                            int distance) {
        this.targetPipePos = targetPipePos;
        this.targetFacing = targetFacing;
        this.path = path;
        this.distance = distance;
    }

    @Override
    public BlockPos getTargetPipePos() {
        return targetPipePos;
    }

    @Override
    public Direction getTargetFacing() {
        return targetFacing;
    }

    @Override
    public int getDistance() {
        return distance;
    }

    public ConfluxPipeBlockEntity[] getPath() {
        return path;
    }

    @Nullable
    @Override
    public ConfluxEndpoint getHandler(Level level) {
        return ConfluxEndpoint.find(level, targetPipePos.relative(targetFacing), targetFacing.getOpposite());
    }
}
