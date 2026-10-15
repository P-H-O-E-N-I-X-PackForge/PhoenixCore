package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;

import java.util.function.Supplier;

public class C2SCinderAtlasRenameLoadoutPacket {

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

    public static void handle(C2SCinderAtlasRenameLoadoutPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;

            CinderAtlasData.setLoadoutName(stack, msg.loadoutIndex, msg.name);
        });
        ctx.setPacketHandled(true);
    }
}
