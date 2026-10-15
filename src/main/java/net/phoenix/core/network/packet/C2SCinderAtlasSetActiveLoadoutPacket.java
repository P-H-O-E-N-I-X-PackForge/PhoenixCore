package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.common.item.cinder.CinderAtlasUpgrades;

public class C2SCinderAtlasSetActiveLoadoutPacket implements CustomPacketPayload {

    public static final Type<C2SCinderAtlasSetActiveLoadoutPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_atlas_set_active_loadout_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderAtlasSetActiveLoadoutPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SCinderAtlasSetActiveLoadoutPacket::encode, C2SCinderAtlasSetActiveLoadoutPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(C2SCinderAtlasSetActiveLoadoutPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;
            if (msg.loadoutIndex < 0 || msg.loadoutIndex >= CinderAtlasUpgrades.getLoadoutCapacity(stack)) return;

            CinderAtlasData.setActiveLoadout(stack, msg.loadoutIndex);
        });
    }
}
