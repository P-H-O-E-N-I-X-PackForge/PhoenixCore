package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketRecipeBuilderGenerate implements CustomPacketPayload {

    public static final Type<PacketRecipeBuilderGenerate> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "packet_recipe_builder_generate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRecipeBuilderGenerate> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> PacketRecipeBuilderGenerate.encode(msg, buf), PacketRecipeBuilderGenerate::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final String code;

    public PacketRecipeBuilderGenerate(String code) {
        this.code = code;
    }

    public static void encode(PacketRecipeBuilderGenerate msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.code, 32_768);
    }

    public static PacketRecipeBuilderGenerate decode(FriendlyByteBuf buf) {
        return new PacketRecipeBuilderGenerate(buf.readUtf(32_768));
    }

    public static void handle(PacketRecipeBuilderGenerate msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            player.sendSystemMessage(Component.literal("§6=== Generated Recipe Code ==="));
            for (String line : msg.code.split("\n")) {
                player.sendSystemMessage(Component.literal("§f" + line));
            }
            player.sendSystemMessage(Component.literal("§6=============================="));
        });
    }
}
