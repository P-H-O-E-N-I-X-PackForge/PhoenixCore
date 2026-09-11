package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import java.util.function.Supplier;

/** The Configurator's "Change Target" action - server-authoritative wipe of a Core's target and
 *  configuration, mirroring the client-side clear the screen does optimistically for its own copy. */
public class C2SCinderClearTargetPacket {

    private final InteractionHand hand;

    public C2SCinderClearTargetPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public C2SCinderClearTargetPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
    }

    public static void handle(C2SCinderClearTargetPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderCoreItem)) return;
            CinderSchemaData.clearTarget(stack);
        });
        ctx.setPacketHandled(true);
    }
}
