package net.phoenix.core.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;


import java.util.function.Supplier;

public class S2CPlayCutscenePacket implements CustomPacketPayload {
    public static final Type<S2CPlayCutscenePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_play_cutscene_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayCutscenePacket> STREAM_CODEC = StreamCodec.ofMember(S2CPlayCutscenePacket::encode, S2CPlayCutscenePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final ResourceLocation id;

    public S2CPlayCutscenePacket(ResourceLocation id) {
        this.id = id;
    }

    public S2CPlayCutscenePacket(FriendlyByteBuf buf) {
        this.id = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
    }

    public static void handle(S2CPlayCutscenePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;

            net.phoenix.core.client.renderer.cinema.cutscene.CutsceneManager.play(msg.id);
        });
        }
}
