package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record S2CPanopticStatePacket(BlockPos controller, boolean open, @Nullable String discipline, int stage,
                                     float growth, boolean complete, long treeSeed, String title,
                                     List<String> clues, List<String> choices, int slots, int maxValue,
                                     String message, float closeness, boolean solved, String costHint, int known,
                                     List<String> memories)
        implements CustomPacketPayload {

    public static final Type<S2CPanopticStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_panoptic_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPanopticStatePacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> encode(msg, buf), S2CPanopticStatePacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static S2CPanopticStatePacket of(ServerPlayer player, BlockPos controller, boolean open,
                                            @Nullable PanopticService.Outcome outcome) {
        PanopticData data = PanopticData.get(player.serverLevel());
        var team = net.phoenix.core.integration.conflux.research.ResearchTeamHelper.getTeamId(player);
        PanopticData.Team t = data.team(team);
        String discipline = PanopticService.discipline(player.serverLevel(), team);
        PanopticPuzzle.Round round = PanopticService.round(player);
        return new S2CPanopticStatePacket(controller, open, discipline, t.stage, data.growth(team), t.complete,
                t.seed, round == null ? "" : round.title(), round == null ? List.of() : round.clues(),
                round == null ? List.of() : round.choices(), round == null ? 0 : round.slots(),
                round == null ? 0 : round.maxValue(),
                outcome == null ? "" : outcome.verdict().message(),
                outcome == null ? 0f : outcome.verdict().closeness(), outcome != null && outcome.solved(),
                PanopticService.costHint(player), PanopticService.knownCount(player),
                PanopticService.memories(player, 18));
    }

    public static S2CPanopticStatePacket withMessage(ServerPlayer player, BlockPos controller, String message) {
        S2CPanopticStatePacket base = of(player, controller, false, null);
        return new S2CPanopticStatePacket(base.controller, false, base.discipline, base.stage, base.growth,
                base.complete, base.treeSeed, base.title, base.clues, base.choices, base.slots, base.maxValue, message,
                0f, false, base.costHint, base.known, base.memories);
    }

    public static void send(ServerPlayer player, S2CPanopticStatePacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    private static void encode(S2CPanopticStatePacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.controller);
        buf.writeBoolean(m.open);
        buf.writeUtf(m.discipline == null ? "" : m.discipline);
        buf.writeVarInt(m.stage);
        buf.writeFloat(m.growth);
        buf.writeBoolean(m.complete);
        buf.writeLong(m.treeSeed);
        buf.writeUtf(m.title);
        writeStrings(buf, m.clues);
        writeStrings(buf, m.choices);
        buf.writeVarInt(m.slots);
        buf.writeVarInt(m.maxValue);
        buf.writeUtf(m.message);
        buf.writeFloat(m.closeness);
        buf.writeBoolean(m.solved);
        buf.writeUtf(m.costHint, 1024);
        buf.writeVarInt(m.known);
        writeStrings(buf, m.memories);
    }

    private static S2CPanopticStatePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean open = buf.readBoolean();
        String discipline = buf.readUtf();
        int stage = buf.readVarInt();
        float growth = buf.readFloat();
        boolean complete = buf.readBoolean();
        long seed = buf.readLong();
        String title = buf.readUtf();
        List<String> clues = readStrings(buf);
        List<String> choices = readStrings(buf);
        int slots = buf.readVarInt();
        int max = buf.readVarInt();
        String message = buf.readUtf();
        float closeness = buf.readFloat();
        boolean solved = buf.readBoolean();
        String costHint = buf.readUtf(1024);
        int known = buf.readVarInt();
        List<String> memories = readStrings(buf);
        return new S2CPanopticStatePacket(pos, open, discipline.isEmpty() ? null : discipline, stage, growth,
                complete, seed, title, clues, choices, slots, max, message, closeness, solved, costHint, known,
                memories);
    }

    private static void writeStrings(FriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s, 1024);
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(buf.readUtf(1024));
        return out;
    }

    public static void handle(S2CPanopticStatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> net.phoenix.core.integration.conflux.client.PanopticClientHandler.accept(msg));
    }
}
