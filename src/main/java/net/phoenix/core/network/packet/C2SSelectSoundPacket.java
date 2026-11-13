package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.vocal_resonance.ResonantJukeboxMachine;

public record C2SSelectSoundPacket(BlockPos pos, String soundLoc, String streamUrl) implements CustomPacketPayload {

    public static final Type<C2SSelectSoundPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_select_sound_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSelectSoundPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SSelectSoundPacket::pos,
            ByteBufCodecs.STRING_UTF8, C2SSelectSoundPacket::soundLoc,
            ByteBufCodecs.STRING_UTF8, C2SSelectSoundPacket::streamUrl,
            C2SSelectSoundPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SSelectSoundPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            var level = player.level();

            if (!(level.getBlockEntity(msg.pos()) instanceof ResonantJukeboxMachine jukebox)) return;

            if (msg.soundLoc().length() > 256 || msg.streamUrl().length() > 512) return;

            if (!jukebox.selectedLibrarySound.equals(msg.soundLoc()) ||
                    !jukebox.currentStreamUrl.equals(msg.streamUrl())) {
                jukebox.resetAcousticData();
            }

            if (!msg.soundLoc().isEmpty() && msg.streamUrl().isEmpty()) jukebox.onLibrarySoundChosen(msg.soundLoc());

            jukebox.selectedLibrarySound = msg.soundLoc();
            jukebox.currentStreamUrl = msg.streamUrl();

            jukebox.markAsChanged();

            var state = level.getBlockState(msg.pos());
            level.sendBlockUpdated(msg.pos(), state, state, 3);
        });
    }
}
