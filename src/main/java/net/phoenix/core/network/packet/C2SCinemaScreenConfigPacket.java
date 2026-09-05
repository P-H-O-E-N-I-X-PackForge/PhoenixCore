package net.phoenix.core.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class C2SCinemaScreenConfigPacket {

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

    public static void handle(C2SCinemaScreenConfigPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5)
                    > MAX_EDIT_DISTANCE_SQ) {
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
        ctx.setPacketHandled(true);
    }
}
