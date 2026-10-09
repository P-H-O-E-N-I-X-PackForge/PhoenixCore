package net.phoenix.core.integration.continuum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.continuum.pdim.PdimActions;

import java.util.function.Supplier;

public class C2SPdimPacket {

    public enum Action {
        ENTER,
        LEAVE
    }

    private final Action action;
    private final ResourceLocation body;
    private final float gravity;

    public C2SPdimPacket(Action action, ResourceLocation body, float gravity) {
        this.action = action;
        this.body = body;
        this.gravity = gravity;
    }

    public C2SPdimPacket(FriendlyByteBuf buf) {
        this.action = Action.values()[Math.max(0, Math.min(buf.readByte(), Action.values().length - 1))];
        this.body = buf.readResourceLocation();
        this.gravity = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(action.ordinal());
        buf.writeResourceLocation(body);
        buf.writeFloat(gravity);
    }

    public static void handle(C2SPdimPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            String error = msg.action == Action.LEAVE ? PdimActions.leave(player) :
                    PdimActions.createAndEnter(player, msg.body, msg.gravity);
            if (error != null) player.displayClientMessage(Component.literal(error), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
