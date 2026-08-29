package net.phoenix.core.integration.gregpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class CPacketKeyState {

    public static final Map<UUID, boolean[]> KEYS = new ConcurrentHashMap<>();

    private final boolean[] keys;

    public CPacketKeyState(boolean[] keys) {
        this.keys = keys;
    }

    public static void encode(CPacketKeyState msg, FriendlyByteBuf buf) {
        for (boolean k : msg.keys) buf.writeBoolean(k);
    }

    public static CPacketKeyState decode(FriendlyByteBuf buf) {
        boolean[] keys = new boolean[6];
        for (int i = 0; i < 6; i++) keys[i] = buf.readBoolean();
        return new CPacketKeyState(keys);
    }

    public static void handle(CPacketKeyState msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) KEYS.put(player.getUUID(), msg.keys);
        });
        ctx.get().setPacketHandled(true);
    }

    public static boolean isJumping(Player player) {
        return get(player, 0);
    }

    public static boolean isForward(Player player) {
        return get(player, 1);
    }

    public static boolean isBackward(Player player) {
        return get(player, 2);
    }

    public static boolean isLeft(Player player) {
        return get(player, 3);
    }

    public static boolean isRight(Player player) {
        return get(player, 4);
    }

    public static boolean isSneaking(Player player) {
        return get(player, 5);
    }

    private static boolean get(Player player, int i) {
        boolean[] keys = KEYS.get(player.getUUID());
        return keys != null && keys[i];
    }
}
