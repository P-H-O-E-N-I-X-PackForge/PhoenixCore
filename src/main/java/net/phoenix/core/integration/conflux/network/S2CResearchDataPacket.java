package net.phoenix.core.integration.conflux.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.conflux.client.ClientResearchCache;

/**
 * One research tree or gated-recipe rule, as the JSON the server loaded it from. Remote clients never read the datapack,
 * so without this they have no trees to draw and no rules to describe. {@code RESET} clears the client's copy first.
 */
public class S2CResearchDataPacket implements CustomPacketPayload {

    public static final byte RESET = 0;
    public static final byte TREE = 1;
    public static final byte RULE = 2;

    public static final Type<S2CResearchDataPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2_c_research_data_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CResearchDataPacket> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> msg.encode(buf), S2CResearchDataPacket::new);

    private static final int MAX_JSON = 1 << 20;

    private final byte kind;
    private final ResourceLocation id;
    private final String json;

    public S2CResearchDataPacket(byte kind, ResourceLocation id, String json) {
        this.kind = kind;
        this.id = id;
        this.json = json;
    }

    public static S2CResearchDataPacket reset() {
        return new S2CResearchDataPacket(RESET, ResourceLocation.fromNamespaceAndPath("phoenixcore", "reset"), "");
    }

    private S2CResearchDataPacket(FriendlyByteBuf buf) {
        this.kind = buf.readByte();
        this.id = buf.readResourceLocation();
        this.json = buf.readUtf(MAX_JSON);
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeByte(kind);
        buf.writeResourceLocation(id);
        buf.writeUtf(json, MAX_JSON);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CResearchDataPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) ClientResearchCache.acceptDefinition(msg.kind, msg.id, msg.json);
        });
    }
}
