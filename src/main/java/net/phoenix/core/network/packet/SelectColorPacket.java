package net.phoenix.core.network.packet;



import net.phoenix.core.util.StackNbt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;

import java.util.function.Supplier;

public class SelectColorPacket implements CustomPacketPayload {
    public static final Type<SelectColorPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "select_color_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectColorPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), SelectColorPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final InteractionHand hand;
    private final int selectedIndex;

    public SelectColorPacket(InteractionHand hand, int selectedIndex) {
        this.hand = hand;
        this.selectedIndex = selectedIndex;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeVarInt(selectedIndex);
    }

    public static SelectColorPacket decode(FriendlyByteBuf buf) {
        return new SelectColorPacket(buf.readEnum(InteractionHand.class), buf.readVarInt());
    }

    public static void handle(SelectColorPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            ItemStack stack = player.getItemInHand(msg.hand);
            if (stack.getItem() instanceof ChameleonSprayCanItem) {

                if (StackNbt.hasTag(stack)) {
                    StackNbt.getTag(stack).remove("chromatic_code");
                }

                DyeColor[] colors = DyeColor.values();
                DyeColor selectedColor = null;

                if (msg.selectedIndex >= 0 && msg.selectedIndex < colors.length) {
                    selectedColor = colors[msg.selectedIndex];
                }

                ChameleonSprayCanBehaviour.setColor(stack, selectedColor);
            }
        });
        }
}
