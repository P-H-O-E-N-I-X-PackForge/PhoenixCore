package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.gregvaults.common.items.WirelessTerminalItem;

public class CPacketOpenTerminal implements CustomPacketPayload {

    public static final Type<CPacketOpenTerminal> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c_packet_open_terminal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketOpenTerminal> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> CPacketOpenTerminal.encode(msg, buf), CPacketOpenTerminal::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int slot;
    private final boolean offhand;

    public CPacketOpenTerminal(int slot, boolean offhand) {
        this.slot = slot;
        this.offhand = offhand;
    }

    public static void encode(CPacketOpenTerminal packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.slot);
        buf.writeBoolean(packet.offhand);
    }

    public static CPacketOpenTerminal decode(FriendlyByteBuf buf) {
        return new CPacketOpenTerminal(buf.readInt(), buf.readBoolean());
    }

    public static void handle(CPacketOpenTerminal packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            ItemStack stack;
            if (packet.offhand) {
                stack = player.getOffhandItem();
            } else if (packet.slot >= 0 && packet.slot < player.getInventory().getContainerSize()) {
                stack = player.getInventory().getItem(packet.slot);
            } else {
                return;
            }

            if (!(stack.getItem() instanceof WirelessTerminalItem)) return;

            InteractionHand hand = packet.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            stack.getItem().use(player.level(), player, hand);
        });
    }
}
