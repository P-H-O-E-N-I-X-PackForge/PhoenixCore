package net.phoenix.core.integration.conflux.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.research.ResearchTreeEditStore;

public class C2SEditResearchTreePacket implements CustomPacketPayload {

    private static final int MAX_JSON = 1 << 20;

    public static final Type<C2SEditResearchTreePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2s_edit_research_tree"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SEditResearchTreePacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeResourceLocation(pkt.treeId);
                buf.writeUtf(pkt.json, MAX_JSON);
            },
            buf -> new C2SEditResearchTreePacket(buf.readResourceLocation(), buf.readUtf(MAX_JSON)));

    private final ResourceLocation treeId;
    private final String json;

    public C2SEditResearchTreePacket(ResourceLocation treeId, String json) {
        this.treeId = treeId;
        this.json = json;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SEditResearchTreePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
            boolean delete = pkt.json.isEmpty();
            String error = delete ? ResearchTreeEditStore.delete(player.server, pkt.treeId) :
                    ResearchTreeEditStore.apply(player.server, pkt.treeId, pkt.json);
            player.displayClientMessage(error == null ?
                    Component.literal((delete ? "Deleted research tree " : "Saved research tree ") + pkt.treeId) :
                    Component.literal(error), true);
        });
    }
}
