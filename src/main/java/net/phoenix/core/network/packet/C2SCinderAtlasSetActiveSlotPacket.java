package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;

import java.util.function.Supplier;

public class C2SCinderAtlasSetActiveSlotPacket {

    private final InteractionHand hand;
    private final int slotIndex;

    public C2SCinderAtlasSetActiveSlotPacket(InteractionHand hand, int slotIndex) {
        this.hand = hand;
        this.slotIndex = slotIndex;
    }

    public C2SCinderAtlasSetActiveSlotPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.slotIndex = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeVarInt(slotIndex);
    }

    public static void handle(C2SCinderAtlasSetActiveSlotPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;

            CinderAtlasData.setActiveSlot(stack, msg.slotIndex);
        });
        ctx.setPacketHandled(true);
    }
}
