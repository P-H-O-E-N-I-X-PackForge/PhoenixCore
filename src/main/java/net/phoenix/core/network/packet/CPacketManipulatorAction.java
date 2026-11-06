package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.matter_manipulater.api.PhoenixPlacementEngine;
import net.phoenix.core.integration.matter_manipulater.common.data.item.PhoenixManipulatorItem;

public class CPacketManipulatorAction implements CustomPacketPayload {

    public static final Type<CPacketManipulatorAction> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c_packet_manipulator_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketManipulatorAction> STREAM_CODEC = StreamCodec
            .ofMember(CPacketManipulatorAction::encode, CPacketManipulatorAction::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos start;
    private final BlockPos end;

    public CPacketManipulatorAction(BlockPos start, BlockPos end) {
        this.start = start;
        this.end = end;
    }

    public CPacketManipulatorAction(FriendlyByteBuf buf) {
        this.start = buf.readBlockPos();
        this.end = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(start);
        buf.writeBlockPos(end);
    }

    public static void handle(CPacketManipulatorAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof PhoenixManipulatorItem tool) {

                PhoenixPlacementEngine.fillPipeArea(
                        player.level(),
                        player,
                        msg.start,
                        msg.end,
                        stack,
                        tool.getMode(stack));
            }
        });
    }
}
