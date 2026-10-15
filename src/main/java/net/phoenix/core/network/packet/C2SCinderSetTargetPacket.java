package net.phoenix.core.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import java.util.function.Supplier;

public class C2SCinderSetTargetPacket implements CustomPacketPayload {
    public static final Type<C2SCinderSetTargetPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_set_target_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderSetTargetPacket> STREAM_CODEC = StreamCodec.ofMember(C2SCinderSetTargetPacket::encode, C2SCinderSetTargetPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final InteractionHand hand;
    private final ResourceLocation targetId;

    public C2SCinderSetTargetPacket(InteractionHand hand, ResourceLocation targetId) {
        this.hand = hand;
        this.targetId = targetId;
    }

    public C2SCinderSetTargetPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.targetId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeResourceLocation(targetId);
    }

    public static void handle(C2SCinderSetTargetPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderCoreItem)) return;

            MachineDefinition definition = GTRegistries.MACHINES.get(msg.targetId);
            if (definition instanceof MultiblockMachineDefinition multiblock) {
                CinderSchemaData.setTarget(stack, multiblock);
            }
        });
        ctx.setPacketHandled(true);
    }
}
