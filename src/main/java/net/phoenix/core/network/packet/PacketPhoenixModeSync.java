package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.matter_manipulater.api.PhoenixManipulatorMode;
import net.phoenix.core.integration.matter_manipulater.common.data.item.PhoenixManipulatorItem;

public class PacketPhoenixModeSync implements CustomPacketPayload {

    public static final Type<PacketPhoenixModeSync> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "packet_phoenix_mode_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPhoenixModeSync> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> PacketPhoenixModeSync.encode(msg, buf), PacketPhoenixModeSync::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int modeOrdinal;

    public PacketPhoenixModeSync(int modeOrdinal) {
        this.modeOrdinal = modeOrdinal;
    }

    public static void encode(PacketPhoenixModeSync msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.modeOrdinal);
    }

    public static PacketPhoenixModeSync decode(FriendlyByteBuf buf) {
        return new PacketPhoenixModeSync(buf.readInt());
    }

    public static void handle(PacketPhoenixModeSync msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player != null) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof PhoenixManipulatorItem tool) {
                    tool.setMode(stack, PhoenixManipulatorMode.values()[msg.modeOrdinal], player);
                }
            }
        });
    }
}
