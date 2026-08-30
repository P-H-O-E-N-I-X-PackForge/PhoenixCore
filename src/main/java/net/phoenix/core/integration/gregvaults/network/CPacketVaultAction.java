package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;
import net.phoenix.core.integration.gregvaults.client.screen.VaultSortMode;

import java.util.function.Supplier;

public class CPacketVaultAction {

    public enum Type { SCROLL, SEARCH, SORT, ORGANIZE }

    private final Type type;

    private final int scrollRow;
    private final String query;
    private final VaultSortMode sortMode;
    private final boolean sortReversed;

    private CPacketVaultAction(Type type, int scrollRow, String query,
                               VaultSortMode sortMode, boolean sortReversed) {
        this.type         = type;
        this.scrollRow    = scrollRow;
        this.query        = query;
        this.sortMode     = sortMode;
        this.sortReversed = sortReversed;
    }

    public static CPacketVaultAction scroll(int row) {
        return new CPacketVaultAction(Type.SCROLL, row, "", VaultSortMode.NAME, false);
    }

    public static CPacketVaultAction search(String query) {
        return new CPacketVaultAction(Type.SEARCH, 0, query == null ? "" : query, VaultSortMode.NAME, false);
    }

    public static CPacketVaultAction sort(VaultSortMode mode, boolean reversed) {
        return new CPacketVaultAction(Type.SORT, 0, "", mode, reversed);
    }

    public static CPacketVaultAction organize() {
        return new CPacketVaultAction(Type.ORGANIZE, 0, "", VaultSortMode.NAME, false);
    }

    public static void encode(CPacketVaultAction packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type);
        switch (packet.type) {
            case SCROLL   -> buf.writeVarInt(packet.scrollRow);
            case SEARCH   -> buf.writeUtf(packet.query, 64);
            case SORT     -> { buf.writeEnum(packet.sortMode); buf.writeBoolean(packet.sortReversed); }
            case ORGANIZE -> {}
        }
    }

    public static CPacketVaultAction decode(FriendlyByteBuf buf) {
        Type type = buf.readEnum(Type.class);
        return switch (type) {
            case SCROLL   -> scroll(buf.readVarInt());
            case SEARCH   -> search(buf.readUtf(64));
            case SORT     -> sort(buf.readEnum(VaultSortMode.class), buf.readBoolean());
            case ORGANIZE -> organize();
        };
    }

    public static void handle(CPacketVaultAction packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof AbstractVaultMenu menu)) return;

            switch (packet.type) {
                case SCROLL   -> menu.updateScroll(packet.scrollRow);
                case SEARCH   -> { menu.updateSearch(packet.query); menu.updateScroll(0); }
                case SORT     -> menu.setSort(packet.sortMode, packet.sortReversed);
                case ORGANIZE -> menu.organize();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}