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

public class C2SCinderAtlasRenameLoadoutPacket implements CustomPacketPayload {

    public static final Type<C2SCinderAtlasRenameLoadoutPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_atlas_rename_loadout_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderAtlasRenameLoadoutPacket> STREAM_CODEC = StreamCodec
            .ofMember(C2SCinderAtlasRenameLoadoutPacket::encode, C2SCinderAtlasRenameLoadoutPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final int MAX_NAME_LENGTH = 32;

    private final InteractionHand hand;
    private final int loadoutIndex;
    private final String name;

    public C2SCinderAtlasRenameLoadoutPacket(InteractionHand hand, int loadoutIndex, String name) {
        this.hand = hand;
        this.loadoutIndex = loadoutIndex;
        this.name = name;
    }

    public C2SCinderAtlasRenameLoadoutPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.loadoutIndex = buf.readVarInt();
        this.name = buf.readUtf(MAX_NAME_LENGTH);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeVarInt(loadoutIndex);
        buf.writeUtf(name, MAX_NAME_LENGTH);
    }

    public static void handle(C2SCinderAtlasRenameLoadoutPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;

            CinderAtlasData.setLoadoutName(stack, msg.loadoutIndex, msg.name);
        });
    }
}
