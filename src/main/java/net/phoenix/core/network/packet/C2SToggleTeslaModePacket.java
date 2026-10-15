package net.phoenix.core.network.packet;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.data.item.PhoenixArmorItem;

import java.util.function.Supplier;

public class C2SToggleTeslaModePacket implements CustomPacketPayload {
    public static final Type<C2SToggleTeslaModePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "c2_s_toggle_tesla_mode_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SToggleTeslaModePacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> C2SToggleTeslaModePacket.encode(msg, buf), C2SToggleTeslaModePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public C2SToggleTeslaModePacket() {}

    public C2SToggleTeslaModePacket(FriendlyByteBuf buf) {}

    public static void encode(C2SToggleTeslaModePacket packet, FriendlyByteBuf buf) {}

    public static void handle(C2SToggleTeslaModePacket packet, IPayloadContext contextSupplier) {
        IPayloadContext context = contextSupplier;
        context.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) context.player());
            if (player == null) return;

            ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);

            if (!chestplate.isEmpty() && chestplate.getItem() instanceof PhoenixArmorItem) {
                CompoundTag data = chestplate.getOrCreateTag();

                long currentTime = player.level().getGameTime();
                long lastToggle = data.getLong("lastToggleTime");
                if (currentTime - lastToggle < 10) return;

                boolean nextMode = !data.getBoolean("teslaMode");
                data.putBoolean("teslaMode", nextMode);
                data.putLong("lastToggleTime", currentTime);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        nextMode ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 1.0F, 2.0F);
            }
        });
    }
}
