package net.phoenix.core.integration.drone;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.drone.group.GroupDefinition;

public class DroneWandItem extends Item {

    private static final String TAG_DRONE_POS = "dronePos";

    public DroneWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.CONSUME;

        var machine = MetaMachine.getMachine(level, pos);

        if (machine instanceof DroneControllerMachine drone) {
            if (player.isShiftKeyDown()) {
                int nextIndex = drone.getControlTrait().config().groups.size() + 1;
                GroupDefinition group = drone.getControlTrait().addGroup("Group " + nextIndex);
                drone.forceRescan();
                player.displayClientMessage(Component.literal("Added group: " + group.name())
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                CompoundTag tag = stack.getOrCreateTag();
                tag.put(TAG_DRONE_POS, NbtUtils.writeBlockPos(pos));
                player.displayClientMessage(
                        Component.literal("Drone Wand bound to " + pos.toShortString()).withStyle(ChatFormatting.AQUA),
                        true);
            }
            return InteractionResult.CONSUME;
        }

        if (!(machine instanceof MultiblockControllerMachine)) return InteractionResult.PASS;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_DRONE_POS)) {
            player.displayClientMessage(
                    Component.literal("Bind the wand to a drone first (right-click it).").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.CONSUME;
        }

        BlockPos dronePos = NbtUtils.readBlockPos(tag.getCompound(TAG_DRONE_POS));
        if (!(MetaMachine.getMachine(level, dronePos) instanceof DroneControllerMachine drone) || !drone.isFormed()) {
            player.displayClientMessage(
                    Component.literal("Bound drone is missing or unformed.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (pos.distSqr(dronePos) > (double) DroneControllerMachine.RADIUS * DroneControllerMachine.RADIUS) {
            player.displayClientMessage(
                    Component.literal("Target is outside the drone's range.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            int priority = drone.getControlTrait().cyclePriority(pos);
            player.displayClientMessage(Component.literal("Priority set to " + priority).withStyle(ChatFormatting.AQUA),
                    true);
        } else {
            String groupLabel = drone.getControlTrait().cycleGroup(pos);
            player.displayClientMessage(
                    Component.literal("Assigned to: " + groupLabel).withStyle(ChatFormatting.AQUA), true);
        }
        drone.forceRescan();
        return InteractionResult.CONSUME;
    }
}
