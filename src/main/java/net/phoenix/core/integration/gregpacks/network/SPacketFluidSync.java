package net.phoenix.core.integration.gregpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackMenu;

public class SPacketFluidSync implements CustomPacketPayload {

    public static final Type<SPacketFluidSync> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s_packet_fluid_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketFluidSync> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> SPacketFluidSync.encode(msg, buf), SPacketFluidSync::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final String fluidKey;

    public SPacketFluidSync(String fluidKey) {
        this.fluidKey = fluidKey;
    }

    public static void encode(SPacketFluidSync msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.fluidKey);
    }

    public static SPacketFluidSync decode(FriendlyByteBuf buf) {
        return new SPacketFluidSync(buf.readUtf());
    }

    public static void handle(SPacketFluidSync msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> applySync(msg));
    }

    @OnlyIn(Dist.CLIENT)
    private static void applySync(SPacketFluidSync msg) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof OmniPackMenu menu) {
            menu.setSyncedFluidKey(msg.fluidKey);
        }
    }
}
