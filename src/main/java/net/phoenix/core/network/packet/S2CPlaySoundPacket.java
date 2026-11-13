package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CPlaySoundPacket(BlockPos pos, ResourceLocation soundLocation, float volume, float pitch, float range)
        implements CustomPacketPayload {

    public static final Type<S2CPlaySoundPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_play_sound_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlaySoundPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, S2CPlaySoundPacket::pos,
            ResourceLocation.STREAM_CODEC, S2CPlaySoundPacket::soundLocation,
            ByteBufCodecs.FLOAT, S2CPlaySoundPacket::volume,
            ByteBufCodecs.FLOAT, S2CPlaySoundPacket::pitch,
            ByteBufCodecs.FLOAT, S2CPlaySoundPacket::range,
            S2CPlaySoundPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CPlaySoundPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;

            net.phoenix.core.network.client.ClientSoundHandler.playSound(
                    msg.pos(), msg.soundLocation(), msg.volume(), msg.pitch(), msg.range());
        });
    }
}
