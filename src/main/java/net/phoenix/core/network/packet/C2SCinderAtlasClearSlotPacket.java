package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;

import java.util.function.Supplier;

public class C2SCinderAtlasClearSlotPacket {

    private final InteractionHand hand;
    private final int loadoutIndex;
    private final int slotIndex;

    public C2SCinderAtlasClearSlotPacket(InteractionHand hand, int loadoutIndex, int slotIndex) {
        this.hand = hand;
        this.loadoutIndex = loadoutIndex;
        this.slotIndex = slotIndex;
    }

    public C2SCinderAtlasClearSlotPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.loadoutIndex = buf.readVarInt();
        this.slotIndex = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeVarInt(loadoutIndex);
        buf.writeVarInt(slotIndex);
    }

    public static void handle(C2SCinderAtlasClearSlotPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;

            CinderAtlasData.clearSlot(stack, msg.loadoutIndex, msg.slotIndex);
        });
        ctx.setPacketHandled(true);
    }
}
