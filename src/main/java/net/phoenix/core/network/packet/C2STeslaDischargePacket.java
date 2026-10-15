package net.phoenix.core.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.gregtechceu.gtceu.api.capability.IElectricItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.data.item.PhoenixArmorItem;
import net.phoenix.core.common.data.item.PhoenixTechSuite;

import java.util.function.Supplier;

public class C2STeslaDischargePacket implements CustomPacketPayload {
    public static final Type<C2STeslaDischargePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_tesla_discharge_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2STeslaDischargePacket> STREAM_CODEC = StreamCodec.ofMember(C2STeslaDischargePacket::encode, C2STeslaDischargePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public C2STeslaDischargePacket() {}

    public C2STeslaDischargePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(C2STeslaDischargePacket msg, IPayloadContext ctxGetter) {
        IPayloadContext ctx = ctxGetter;
        ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;

            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

            if (!boots.isEmpty() && boots.getItem() instanceof PhoenixArmorItem) {
                CompoundTag data = boots.getOrCreateTag();

                if (data.getInt("dischargeCooldown") == 0) {

                    PhoenixTechSuite suiteController = (PhoenixTechSuite) (Object) boots.getItem();
                    IElectricItem electricItemCapability = (IElectricItem) (Object) boots.getItem();

                    suiteController.doTeslaDischarge((ServerLevel) player.level(), player, electricItemCapability);

                    data.putInt("dischargeCooldown", 100);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
