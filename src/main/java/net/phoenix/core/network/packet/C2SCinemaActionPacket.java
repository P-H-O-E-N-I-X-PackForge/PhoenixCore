package net.phoenix.core.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.function.Supplier;

public class C2SCinemaActionPacket implements CustomPacketPayload {
    public static final Type<C2SCinemaActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinema_action_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinemaActionPacket> STREAM_CODEC = StreamCodec.ofMember(C2SCinemaActionPacket::encode, C2SCinemaActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public enum Action {
        CYCLE_COLOR,
        REMOVE_CURRENT_LINE
    }

    private static final double MAX_DISTANCE_SQ = 64.0 * 64.0;

    private final BlockPos pos;
    private final Action action;

    public C2SCinemaActionPacket(BlockPos pos, Action action) {
        this.pos = pos;
        this.action = action;
    }

    public C2SCinemaActionPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.action = buf.readEnum(Action.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(action);
    }

    public static void handle(C2SCinemaActionPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) >
                    MAX_DISTANCE_SQ) {
                return;
            }
            if (!player.level().isLoaded(msg.pos)) return;
            if (!(player.level().getBlockEntity(msg.pos) instanceof CinemaScreenBlockEntity screen)) return;

            switch (msg.action) {
                case CYCLE_COLOR -> screen.cycleColor();
                case REMOVE_CURRENT_LINE -> screen.removeCurrentLine();
            }
        });
        ctx.setPacketHandled(true);
    }
}
