package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record S2CPlayStreamPacket(String url, BlockPos pos, int range, float volume) implements CustomPacketPayload {

    public static final Type<S2CPlayStreamPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_play_stream_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayStreamPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, S2CPlayStreamPacket::url,
            BlockPos.STREAM_CODEC, S2CPlayStreamPacket::pos,
            ByteBufCodecs.INT, S2CPlayStreamPacket::range,
            ByteBufCodecs.FLOAT, S2CPlayStreamPacket::volume,
            S2CPlayStreamPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final Logger LOGGER = LogManager.getLogger("VocalResonance");

    public static void handle(S2CPlayStreamPacket msg, IPayloadContext ctx) {
        LOGGER.info("VR S2CPlayStreamPacket received: url='{}' pos={} range={} volume={} dist={}",
                msg.url, msg.pos, msg.range, msg.volume, FMLEnvironment.dist);
        ctx.enqueueWork(() -> {
            LOGGER.info("VR S2CPlayStreamPacket enqueueWork executing, dist={}", FMLEnvironment.dist);
            try {
                if (!FMLEnvironment.dist.isClient()) {
                    LOGGER.warn("VR S2CPlayStreamPacket: dist is not CLIENT, skipping");
                    return;
                }
                net.phoenix.core.network.client.ClientSoundHandler.playStream(
                        msg.url, msg.pos, (float) msg.range, msg.volume);
            } catch (Throwable t) {
                LOGGER.error("VR S2CPlayStreamPacket: exception in enqueueWork", t);
            }
        });
    }
}
