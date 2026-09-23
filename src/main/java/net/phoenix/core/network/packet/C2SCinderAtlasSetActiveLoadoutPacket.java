package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.common.item.cinder.CinderAtlasUpgrades;

import java.util.function.Supplier;

public class C2SCinderAtlasSetActiveLoadoutPacket {

    private final InteractionHand hand;
    private final int loadoutIndex;

    public C2SCinderAtlasSetActiveLoadoutPacket(InteractionHand hand, int loadoutIndex) {
        this.hand = hand;
        this.loadoutIndex = loadoutIndex;
    }

    public C2SCinderAtlasSetActiveLoadoutPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.loadoutIndex = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeVarInt(loadoutIndex);
    }

    public static void handle(C2SCinderAtlasSetActiveLoadoutPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;
            if (msg.loadoutIndex < 0 || msg.loadoutIndex >= CinderAtlasUpgrades.getLoadoutCapacity(stack)) return;

            CinderAtlasData.setActiveLoadout(stack, msg.loadoutIndex);
        });
        ctx.setPacketHandled(true);
    }
}
