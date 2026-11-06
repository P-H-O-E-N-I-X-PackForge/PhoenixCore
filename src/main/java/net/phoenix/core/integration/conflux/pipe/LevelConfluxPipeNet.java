package net.phoenix.core.integration.conflux.pipe;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class LevelConfluxPipeNet extends LevelPipeNet<ConfluxPipeData, ConfluxPipeNet> {

    private static final String DATA_ID = "phoenix_conflux_pipe_net";

    public static LevelConfluxPipeNet getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> new LevelConfluxPipeNet(level),
                        (tag, provider) -> new LevelConfluxPipeNet(level, tag, provider), null),
                DATA_ID);
    }

    public LevelConfluxPipeNet(ServerLevel level) {
        super(level);
    }

    public LevelConfluxPipeNet(ServerLevel level, CompoundTag tag, HolderLookup.Provider provider) {
        super(level, tag, provider);
    }

    @Override
    protected ConfluxPipeNet createNetInstance() {
        return new ConfluxPipeNet(this);
    }
}
