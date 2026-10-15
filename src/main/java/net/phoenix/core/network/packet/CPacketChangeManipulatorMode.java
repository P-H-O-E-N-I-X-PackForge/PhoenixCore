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

public class CPacketChangeManipulatorMode implements CustomPacketPayload {

    public static final Type<CPacketChangeManipulatorMode> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c_packet_change_manipulator_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketChangeManipulatorMode> STREAM_CODEC = StreamCodec
            .ofMember(CPacketChangeManipulatorMode::encode, CPacketChangeManipulatorMode::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int modeOrdinal;

    public CPacketChangeManipulatorMode(int modeOrdinal) {
        this.modeOrdinal = modeOrdinal;
    }

    public CPacketChangeManipulatorMode(FriendlyByteBuf buf) {
        this.modeOrdinal = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(modeOrdinal);
    }

    public static void handle(CPacketChangeManipulatorMode msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof PhoenixManipulatorItem tool) {
                tool.setMode(stack, PhoenixManipulatorMode.values()[msg.modeOrdinal], player);
            }
        });
    }
}
