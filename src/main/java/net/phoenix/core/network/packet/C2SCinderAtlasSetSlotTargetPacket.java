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

import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.common.item.cinder.CinderSchemaData;

import java.util.function.Supplier;

public class C2SCinderAtlasSetSlotTargetPacket implements CustomPacketPayload {
    public static final Type<C2SCinderAtlasSetSlotTargetPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_cinder_atlas_set_slot_target_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCinderAtlasSetSlotTargetPacket> STREAM_CODEC = StreamCodec.ofMember(C2SCinderAtlasSetSlotTargetPacket::encode, C2SCinderAtlasSetSlotTargetPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private final InteractionHand hand;
    private final int loadoutIndex;
    private final int slotIndex;
    private final ResourceLocation targetId;

    public C2SCinderAtlasSetSlotTargetPacket(InteractionHand hand, int loadoutIndex, int slotIndex,
                                             ResourceLocation targetId) {
        this.hand = hand;
        this.loadoutIndex = loadoutIndex;
        this.slotIndex = slotIndex;
        this.targetId = targetId;
    }

    public C2SCinderAtlasSetSlotTargetPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.loadoutIndex = buf.readVarInt();
        this.slotIndex = buf.readVarInt();
        this.targetId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeVarInt(loadoutIndex);
        buf.writeVarInt(slotIndex);
        buf.writeResourceLocation(targetId);
    }

    public static void handle(C2SCinderAtlasSetSlotTargetPacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof CinderAtlasItem)) return;

            MachineDefinition definition = GTRegistries.MACHINES.get(msg.targetId);
            if (definition instanceof MultiblockMachineDefinition multiblock) {
                CinderSchemaData.setTarget(
                        CinderAtlasData.getOrCreateSlotTag(stack, msg.loadoutIndex, msg.slotIndex), multiblock);
            }
        });
    }
}
