package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkProgressionProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<ChunkProgressionState> CHUNK_PROGRESSION_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    private ChunkProgressionState progression = null;
    private final LazyOptional<ChunkProgressionState> optional = LazyOptional.of(this::createProgression);

    private ChunkProgressionState createProgression() {
        if (this.progression == null) {
            this.progression = new ChunkProgressionState();
        }
        return this.progression;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CHUNK_PROGRESSION_CAP) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createProgression().save(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        ChunkProgressionState loadedState = ChunkProgressionState.load(nbt);
        createProgression().getAppliedMilestones().clear(); 
        for (String milestone : loadedState.getAppliedMilestones()) {
            createProgression().applyMilestone(milestone);
        }
    }
}