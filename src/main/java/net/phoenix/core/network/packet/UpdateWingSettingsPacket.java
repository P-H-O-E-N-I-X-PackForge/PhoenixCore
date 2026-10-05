package net.phoenix.core.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.data.item.PhoenixArmorItem;

import java.util.Set;
import java.util.function.Supplier;

public class UpdateWingSettingsPacket {

    private static final Set<String> VALID_MODES = Set.of(
            "basic",
            "powered",
            "creative",
            "creative+wings");

    private final String flightMode;
    private final int flightSpeed;
    private final int flightDrift;
    private final int flightVertical;
    private final int sprintSpeed;
    private final int jumpHeight;
    private final boolean keepFlyingOnLand;

    public UpdateWingSettingsPacket(String flightMode, int flightSpeed, int flightDrift, int flightVertical,
                                    int sprintSpeed, int jumpHeight, boolean keepFlyingOnLand) {
        this.flightMode = flightMode;
        this.flightSpeed = flightSpeed;
        this.flightDrift = flightDrift;
        this.flightVertical = flightVertical;
        this.sprintSpeed = sprintSpeed;
        this.jumpHeight = jumpHeight;
        this.keepFlyingOnLand = keepFlyingOnLand;
    }

    public UpdateWingSettingsPacket(FriendlyByteBuf buf) {
        this.flightMode = buf.readUtf();
        this.flightSpeed = buf.readInt();
        this.flightDrift = buf.readInt();
        this.flightVertical = buf.readInt();
        this.sprintSpeed = buf.readInt();
        this.jumpHeight = buf.readInt();
        this.keepFlyingOnLand = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(flightMode);
        buf.writeInt(flightSpeed);
        buf.writeInt(flightDrift);
        buf.writeInt(flightVertical);
        buf.writeInt(sprintSpeed);
        buf.writeInt(jumpHeight);
        buf.writeBoolean(keepFlyingOnLand);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!(chest.getItem() instanceof PhoenixArmorItem)) return;

            String mode = VALID_MODES.contains(flightMode) ? flightMode : "basic";
            int speed = Math.max(0, Math.min(20, flightSpeed));
            int drift = Math.max(0, Math.min(10, flightDrift));
            int vertical = Math.max(0, Math.min(20, flightVertical));
            int sprint = Math.max(0, Math.min(20, sprintSpeed));
            int jump = Math.max(0, Math.min(20, jumpHeight));

            CompoundTag tag = chest.getOrCreateTag();
            tag.putString("FlightMode", mode);
            tag.putInt("FlightSpeed", speed);
            tag.putInt("FlightDrift", drift);
            tag.putInt("FlightVertical", vertical);
            tag.putInt("SprintSpeed", sprint);
            tag.putInt("JumpHeight", jump);
            tag.putBoolean("KeepFlyingOnLand", keepFlyingOnLand);

            player.inventoryMenu.sendAllDataToRemote();
        });
        ctx.get().setPacketHandled(true);
    }
}
