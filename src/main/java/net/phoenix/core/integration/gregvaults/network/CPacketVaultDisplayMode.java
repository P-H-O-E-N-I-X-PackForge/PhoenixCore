package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;
import net.phoenix.core.integration.gregvaults.client.screen.VaultDisplayMode;

import java.util.function.Supplier;

public class CPacketVaultDisplayMode {

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

    public static void handle(CPacketVaultDisplayMode packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.containerMenu instanceof AbstractVaultMenu menu) {
                menu.setDisplayMode(packet.mode);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
