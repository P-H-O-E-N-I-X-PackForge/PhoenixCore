package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;

import java.util.List;
import java.util.UUID;

public record C2SPanopticDevDisciplinePacket(BlockPos controller) implements CustomPacketPayload {

    public static final Type<C2SPanopticDevDisciplinePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_panoptic_dev_discipline"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SPanopticDevDisciplinePacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> buf.writeBlockPos(msg.controller),
                    buf -> new C2SPanopticDevDisciplinePacket(buf.readBlockPos()));

    private static final List<String> ORDER = java.util.Arrays.asList(null, "phoenix", "sculk", "void", "sealed_a",
            "sealed_b");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPanopticDevDisciplinePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
            PanopticEngineMachine engine = PanopticEngineMachine.at(player.serverLevel(), msg.controller);
            if (engine == null || !engine.isFormed()) return;

            UUID team = ResearchTeamHelper.getTeamId(player);
            PanopticData data = PanopticData.get(player.serverLevel());
            String current = data.team(team).devDiscipline;
            int next = (ORDER.indexOf(current) + 1) % ORDER.size();
            data.setDevDiscipline(team, ORDER.get(next));
            S2CPanopticStatePacket.send(player, S2CPanopticStatePacket.withMessage(player, msg.controller,
                    ORDER.get(next) == null ? "Playing as your real discipline." :
                            "Playing as " + ORDER.get(next).replace('_', ' ') + " (dev)."));
        });
    }
}
