package net.phoenix.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.phoenix.core.integration.conflux.dimension.ChunkProgressionState;
import net.phoenix.core.integration.conflux.research.PlayerResearchData;

import java.util.function.Supplier;

public final class PhoenixAttachments {

    private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister
            .create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PhoenixCore.MOD_ID);

    public static final Supplier<AttachmentType<PlayerResearchData>> RESEARCH = TYPES.register("axiom_research",
            () -> AttachmentType.builder(PlayerResearchData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, PlayerResearchData>() {

                        @Override
                        public PlayerResearchData read(IAttachmentHolder holder, CompoundTag tag,
                                                       HolderLookup.Provider provider) {
                            PlayerResearchData data = new PlayerResearchData();
                            data.deserializeNBT(tag);
                            return data;
                        }

                        @Override
                        public CompoundTag write(PlayerResearchData data, HolderLookup.Provider provider) {
                            return data.serializeNBT();
                        }
                    })
                    .copyHandler((data, holder, provider) -> {
                        PlayerResearchData copy = new PlayerResearchData();
                        copy.copyFrom(data);
                        return copy;
                    })
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<ChunkProgressionState>> CHUNK_PROGRESSION = TYPES.register(
            "chunk_progression",
            () -> AttachmentType.builder(ChunkProgressionState::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, ChunkProgressionState>() {

                        @Override
                        public ChunkProgressionState read(IAttachmentHolder holder, CompoundTag tag,
                                                          HolderLookup.Provider provider) {
                            return ChunkProgressionState.load(tag);
                        }

                        @Override
                        public CompoundTag write(ChunkProgressionState state, HolderLookup.Provider provider) {
                            CompoundTag tag = new CompoundTag();
                            state.save(tag);
                            return tag;
                        }
                    })
                    .build());

    private PhoenixAttachments() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
