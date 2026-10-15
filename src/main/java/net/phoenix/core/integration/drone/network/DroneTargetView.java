package net.phoenix.core.integration.drone.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record DroneTargetView(BlockPos pos, String name, ItemStack icon, boolean controllable,
                              boolean workingEnabled, String status, int progress, int duration, String recipeTypeName,
                              boolean multiRecipe, boolean electric, long inputVoltage, long outputVoltage,
                              String groupName,
                              int priority) {

    public static void encodeList(List<DroneTargetView> views, FriendlyByteBuf buf) {
        buf.writeVarInt(views.size());
        for (DroneTargetView view : views) {
            buf.writeBlockPos(view.pos());
            buf.writeUtf(view.name());
            buf.writeItem(view.icon());
            buf.writeBoolean(view.controllable());
            buf.writeBoolean(view.workingEnabled());
            buf.writeUtf(view.status());
            buf.writeVarInt(view.progress());
            buf.writeVarInt(view.duration());
            buf.writeUtf(view.recipeTypeName());
            buf.writeBoolean(view.multiRecipe());
            buf.writeBoolean(view.electric());
            buf.writeVarLong(view.inputVoltage());
            buf.writeVarLong(view.outputVoltage());
            buf.writeUtf(view.groupName());
            buf.writeVarInt(view.priority());
        }
    }

    public static List<DroneTargetView> decodeList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<DroneTargetView> views = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            views.add(new DroneTargetView(
                    buf.readBlockPos(),
                    buf.readUtf(),
                    buf.readItem(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarLong(),
                    buf.readVarLong(),
                    buf.readUtf(),
                    buf.readVarInt()));
        }
        return views;
    }
}
