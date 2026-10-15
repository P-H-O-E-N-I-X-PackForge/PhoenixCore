package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;
import net.phoenix.core.integration.gregvaults.client.screen.VaultDisplayMode;

public class CPacketVaultDisplayMode implements CustomPacketPayload {

    public static final Type<CPacketVaultDisplayMode> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c_packet_vault_display_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketVaultDisplayMode> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> CPacketVaultDisplayMode.encode(msg, buf), CPacketVaultDisplayMode::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final VaultDisplayMode mode;

    public CPacketVaultDisplayMode(VaultDisplayMode mode) {
        this.mode = mode;
    }

    public static void encode(CPacketVaultDisplayMode packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.mode);
    }

    public static CPacketVaultDisplayMode decode(FriendlyByteBuf buf) {
        return new CPacketVaultDisplayMode(buf.readEnum(VaultDisplayMode.class));
    }

    public static void handle(CPacketVaultDisplayMode packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            if (player.containerMenu instanceof AbstractVaultMenu menu) {
                menu.setDisplayMode(packet.mode);
            }
        });
    }
}
