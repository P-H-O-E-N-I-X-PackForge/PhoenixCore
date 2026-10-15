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

import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;

import java.util.function.Supplier;

public class SelectChromaticCodePacket implements CustomPacketPayload {
    public static final Type<SelectChromaticCodePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "select_chromatic_code_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectChromaticCodePacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> SelectChromaticCodePacket.encode(msg, buf), SelectChromaticCodePacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final InteractionHand hand;
    private final char code;

    public SelectChromaticCodePacket(InteractionHand hand, char code) {
        this.hand = hand;
        this.code = code;
    }

    public static void encode(SelectChromaticCodePacket msg, FriendlyByteBuf buffer) {
        buffer.writeEnum(msg.hand);
        buffer.writeChar(msg.code);
    }

    public static SelectChromaticCodePacket decode(FriendlyByteBuf buffer) {
        return new SelectChromaticCodePacket(buffer.readEnum(InteractionHand.class), buffer.readChar());
    }

    public static void handle(SelectChromaticCodePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player != null) {
                ItemStack stack = player.getItemInHand(msg.hand);
                if (stack.getItem() instanceof ChameleonSprayCanItem) {
                    ChameleonSprayCanBehaviour.setChromaticCode(stack, msg.code);
                }
            }
        });
        }
}
