package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.integration.ae2.CinderAtlasWirelessLink;

import java.util.function.Supplier;

/**
 * Manual "prepare ahead" trigger (design doc feature #4) - opens AE2's real crafting terminal against
 * the held Atlas's linked network on demand, independent of any deploy attempt, so a player can queue
 * crafts for a loadout's materials before they're actually needed.
 */
public class C2SCinderAtlasOpenCraftingTerminalPacket {

    private final InteractionHand hand;

    public C2SCinderAtlasOpenCraftingTerminalPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public C2SCinderAtlasOpenCraftingTerminalPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
    }

    public static void handle(C2SCinderAtlasOpenCraftingTerminalPacket msg,
                              Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            var accessPoint = CinderAtlasWirelessLink.getLinkedAccessPoint(stack, level);
            if (accessPoint == null) {
                player.displayClientMessage(
                        Component.literal("This Atlas isn't linked to an AE2 network - bind it with a " +
                                "Security Terminal first."),
                        true);
                return;
            }
            if (!CinderAtlasWirelessLink.isInRange(stack, accessPoint, player)) {
                player.displayClientMessage(
                        Component.literal("Too far from the linked access point - move closer or install a " +
                                "Range Extender upgrade."),
                        true);
                return;
            }

            CinderAtlasItem.openCraftingTerminal(player, msg.hand);
        });
        ctx.setPacketHandled(true);
    }
}
