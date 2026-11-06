package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2CPlaySoundPacket implements CustomPacketPayload {

    public static final Type<S2CPlaySoundPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_play_sound_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlaySoundPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CPlaySoundPacket::encode, S2CPlaySoundPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final ResourceLocation soundLocation;
    private final float volume;
    private final float pitch;
    private final float range;

    public S2CPlaySoundPacket(BlockPos pos, ResourceLocation soundLocation, float volume, float pitch, float range) {
        this.pos = pos;
        this.soundLocation = soundLocation;
        this.volume = volume;
        this.pitch = pitch;
        this.range = range;
    }

    public S2CPlaySoundPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.soundLocation = buf.readResourceLocation();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.range = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeResourceLocation(soundLocation);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeFloat(range);
    }

    public static void handle(S2CPlaySoundPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;

            net.phoenix.core.network.client.ClientSoundHandler.playSound(
                    msg.pos, msg.soundLocation, msg.volume, msg.pitch, msg.range);
        });
    }
}
