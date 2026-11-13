package net.phoenix.core.network.packet;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.item.cinder.CinderCoreItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.network.PacketCodecs;

public record C2SCinderSetTargetPacket(InteractionHand hand, ResourceLocation targetId) implements CustomPacketPayload {

    public static final Type<C2SCinderSetTargetPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_set_target_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderSetTargetPacket> STREAM_CODEC = StreamCodec
            .composite(
                    PacketCodecs.enumOrdinal(InteractionHand.class), C2SCinderSetTargetPacket::hand,
                    ResourceLocation.STREAM_CODEC, C2SCinderSetTargetPacket::targetId,
                    C2SCinderSetTargetPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SCinderSetTargetPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand());
            if (!(stack.getItem() instanceof CinderCoreItem)) return;

            MachineDefinition definition = GTRegistries.MACHINES.get(msg.targetId());
            if (definition instanceof MultiblockMachineDefinition multiblock) {
                CinderSchemaData.setTarget(stack, multiblock);
            }
        });
    }
}
