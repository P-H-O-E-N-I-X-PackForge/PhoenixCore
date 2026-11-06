package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class C2SCinemaScreenConfigPacket implements CustomPacketPayload {

    public static final Type<C2SCinemaScreenConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinema_screen_config_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinemaScreenConfigPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SCinemaScreenConfigPacket::encode, C2SCinemaScreenConfigPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final int MAX_LINE_LENGTH = 128;
    private static final int MAX_LINES = 8;

    private static final double MAX_EDIT_DISTANCE_SQ = 64.0 * 64.0;

    private final BlockPos pos;
    private final List<String> lines;
    private final int color;
    private final float scale;
    private final int alignOrdinal;
    private final int backgroundOrdinal;

    public C2SCinemaScreenConfigPacket(BlockPos pos, List<String> lines, int color, float scale, int alignOrdinal,
                                       int backgroundOrdinal) {
        this.pos = pos;
        this.lines = lines;
        this.color = color;
        this.scale = scale;
        this.alignOrdinal = alignOrdinal;
        this.backgroundOrdinal = backgroundOrdinal;
    }

    public C2SCinemaScreenConfigPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        int count = Math.min(buf.readVarInt(), MAX_LINES);
        this.lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buf.readUtf(MAX_LINE_LENGTH));
        }
        this.color = buf.readInt();
        this.scale = buf.readFloat();
        this.alignOrdinal = buf.readVarInt();
        this.backgroundOrdinal = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(Math.min(lines.size(), MAX_LINES));
        for (int i = 0; i < Math.min(lines.size(), MAX_LINES); i++) {
            buf.writeUtf(lines.get(i), MAX_LINE_LENGTH);
        }
        buf.writeInt(color);
        buf.writeFloat(scale);
        buf.writeVarInt(alignOrdinal);
        buf.writeVarInt(backgroundOrdinal);
    }

    public static void handle(C2SCinemaScreenConfigPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) >
                    MAX_EDIT_DISTANCE_SQ) {
                return;
            }
            if (!player.level().isLoaded(msg.pos)) return;

            if (player.level().getBlockEntity(msg.pos) instanceof CinemaScreenBlockEntity screen) {
                List<Component> components = new ArrayList<>(msg.lines.size());
                for (String line : msg.lines) {
                    if (!line.isBlank()) components.add(Component.literal(line));
                }
                float clampedScale = Math.max(0.005f, Math.min(0.05f, msg.scale));
                screen.applyConfig(components, msg.color, clampedScale, msg.alignOrdinal, msg.backgroundOrdinal);
            }
        });
    }
}
