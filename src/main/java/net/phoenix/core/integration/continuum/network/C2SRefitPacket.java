package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.common.RocketRefit;

import java.util.function.Supplier;

/** A button in the refit screen: raise the hull, fit a module, or repair the rocket in hand. The server decides. */
public class C2SRefitPacket {

    private final RocketRefit.Action action;
    private final String key;

    public C2SRefitPacket(RocketRefit.Action action, String key) {
        this.action = action;
        this.key = key;
    }

    public C2SRefitPacket(FriendlyByteBuf buf) {
        this.action = RocketRefit.Action.values()[Math.max(0,
                Math.min(buf.readByte(), RocketRefit.Action.values().length - 1))];
        this.key = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(action.ordinal());
        buf.writeUtf(key, 64);
    }

    public static void handle(C2SRefitPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            String error = RocketRefit.apply(player, msg.action, msg.key);
            if (error != null) player.displayClientMessage(Component.literal(error), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
