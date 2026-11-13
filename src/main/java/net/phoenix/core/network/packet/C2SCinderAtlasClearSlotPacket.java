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

public record C2SCinderAtlasClearSlotPacket(InteractionHand hand, int loadoutIndex, int slotIndex)
        implements CustomPacketPayload {

    public static final Type<C2SCinderAtlasClearSlotPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_atlas_clear_slot_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderAtlasClearSlotPacket> STREAM_CODEC = StreamCodec
            .composite(
                    PacketCodecs.enumOrdinal(InteractionHand.class), C2SCinderAtlasClearSlotPacket::hand,
                    ByteBufCodecs.VAR_INT, C2SCinderAtlasClearSlotPacket::loadoutIndex,
                    ByteBufCodecs.VAR_INT, C2SCinderAtlasClearSlotPacket::slotIndex,
                    C2SCinderAtlasClearSlotPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SCinderAtlasClearSlotPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand());
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;

            CinderAtlasData.clearSlot(stack, msg.loadoutIndex(), msg.slotIndex());
        });
    }
}
