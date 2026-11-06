package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.PipeNetWalker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.phoenix.core.PhoenixCore;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfluxNetWalker extends PipeNetWalker<ConfluxPipeBlockEntity, ConfluxPipeData, ConfluxPipeNet> {

    @Nullable
    public static List<ConfluxRoutePath> createNetData(ConfluxPipeNet pipeNet, BlockPos sourcePipe) {
        try {
            ConfluxNetWalker walker = new ConfluxNetWalker(pipeNet, sourcePipe, 1, new ArrayList<>());
            walker.traversePipeNet();
            return walker.routes;
        } catch (Exception e) {
            PhoenixCore.LOGGER.error("Could not walk a Conflux pipe network", e);
        }
        return null;
    }

    private final List<ConfluxRoutePath> routes;
    private ConfluxPipeBlockEntity[] pipes = {};

    public ConfluxNetWalker(ConfluxPipeNet pipeNet, BlockPos sourcePipe, int walkedBlocks,
                            List<ConfluxRoutePath> routes) {
        super(pipeNet, sourcePipe, walkedBlocks);
        this.routes = routes;
    }

    @Override
    protected PipeNetWalker<ConfluxPipeBlockEntity, ConfluxPipeData, ConfluxPipeNet> createSubWalker(
                                                                                                     ConfluxPipeNet pipeNet,
                                                                                                     Direction facingToNextPos,
                                                                                                     BlockPos nextPos,
                                                                                                     int walkedBlocks) {
        ConfluxNetWalker walker = new ConfluxNetWalker(pipeNet, nextPos, walkedBlocks, routes);
        walker.pipes = pipes;
        return walker;
    }

    @Override
    protected void checkPipe(ConfluxPipeBlockEntity pipeTile, BlockPos pos) {
        pipes = Arrays.copyOf(pipes, pipes.length + 1);
        pipes[pipes.length - 1] = pipeTile;
    }

    @Override
    protected void checkNeighbour(ConfluxPipeBlockEntity pipeTile, BlockPos pipePos, Direction faceToNeighbour,
                                  @Nullable BlockEntity neighbourTile) {
        if (pipeTile != pipes[pipes.length - 1]) {
            throw new IllegalStateException("The current pipe is not the last pipe walked");
        }
        if (neighbourTile == null || neighbourTile instanceof ConfluxPipeBlockEntity) return;
        ConfluxEndpoint endpoint = ConfluxEndpoint.find(neighbourTile.getLevel(), neighbourTile.getBlockPos(),
                faceToNeighbour.getOpposite());
        if (endpoint == null || !endpoint.accepts(pipeTile.getPipeType().dataType())) return;
        routes.add(new ConfluxRoutePath(pipePos.immutable(), faceToNeighbour, pipes, getWalkedBlocks()));
    }

    @Override
    protected Class<ConfluxPipeBlockEntity> getBasePipeClass() {
        return ConfluxPipeBlockEntity.class;
    }
}
