package net.phoenix.core.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;

import java.util.function.Supplier;

public class C2SCinderAtlasClearSlotPacket implements CustomPacketPayload {
    public static final Type<C2SCinderAtlasClearSlotPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_atlas_clear_slot_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderAtlasClearSlotPacket> STREAM_CODEC = StreamCodec.ofMember(C2SCinderAtlasClearSlotPacket::encode, C2SCinderAtlasClearSlotPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


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

    public static void handle(C2SCinderAtlasClearSlotPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;

            CinderAtlasData.clearSlot(stack, msg.loadoutIndex, msg.slotIndex);
        });
    }
}
