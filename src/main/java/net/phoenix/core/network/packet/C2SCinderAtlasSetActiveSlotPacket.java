package net.phoenix.core.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.network.PacketCodecs;

public record C2SCinderAtlasSetActiveSlotPacket(InteractionHand hand, int slotIndex) implements CustomPacketPayload {

    public static final Type<C2SCinderAtlasSetActiveSlotPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_atlas_set_active_slot_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderAtlasSetActiveSlotPacket> STREAM_CODEC = StreamCodec
            .composite(
                    PacketCodecs.enumOrdinal(InteractionHand.class), C2SCinderAtlasSetActiveSlotPacket::hand,
                    ByteBufCodecs.VAR_INT, C2SCinderAtlasSetActiveSlotPacket::slotIndex,
                    C2SCinderAtlasSetActiveSlotPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SCinderAtlasSetActiveSlotPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand());
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;

            CinderAtlasData.setActiveSlot(stack, msg.slotIndex());
        });
    }
}
