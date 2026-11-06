package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.Node;
import com.gregtechceu.gtceu.api.pipenet.PipeNet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfluxPipeNet extends PipeNet<ConfluxPipeData> {

    private final Map<BlockPos, List<ConfluxRoutePath>> netData = new HashMap<>();

    public ConfluxPipeNet(LevelConfluxPipeNet world) {
        super(world);
    }

    public List<ConfluxRoutePath> getNetData(BlockPos pipePos) {
        List<ConfluxRoutePath> data = netData.get(pipePos);
        if (data == null) {
            data = ConfluxNetWalker.createNetData(this, pipePos);
            if (data == null) {

                return Collections.emptyList();
            }
            data.sort(Comparator.comparingInt(ConfluxRoutePath::getDistance));
            netData.put(pipePos, data);
        }
        return data;
    }

    @Override
    public void onNeighbourUpdate(BlockPos fromPos) {
        netData.clear();
    }

    @Override
    public void onPipeConnectionsUpdate() {
        netData.clear();
    }

    @Override
    protected void transferNodeData(Map<BlockPos, Node<ConfluxPipeData>> transferredNodes,
                                    PipeNet<ConfluxPipeData> parentNet) {
        super.transferNodeData(transferredNodes, parentNet);
        netData.clear();
        ((ConfluxPipeNet) parentNet).netData.clear();
    }

    @Override
    protected void writeNodeData(ConfluxPipeData data, CompoundTag tag) {}

    @Override
    protected ConfluxPipeData readNodeData(CompoundTag tag) {
        return ConfluxPipeData.INSTANCE;
    }
}
