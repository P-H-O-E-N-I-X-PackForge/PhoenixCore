package net.phoenix.core.integration.drone.group;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DroneConfig {

    public final List<GroupDefinition> groups = new ArrayList<>();
    public final Map<BlockPos, MachineAssignment> assignments = new HashMap<>();

    public GroupDefinition addGroup(String name) {
        GroupDefinition group = new GroupDefinition(UUID.randomUUID().toString(), name, 0);
        groups.add(group);
        return group;
    }

    public void removeGroup(String groupId) {
        groups.removeIf(g -> g.id().equals(groupId));
        assignments.replaceAll((pos, a) -> a.groupId().equals(groupId) ? new MachineAssignment("", a.priority()) : a);
    }

    public void setGroupPriorityFloor(String groupId, int floor) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).id().equals(groupId)) {
                groups.set(i, groups.get(i).withPriorityFloor(floor));
                return;
            }
        }
    }

    public MachineAssignment getAssignment(BlockPos pos) {
        return assignments.getOrDefault(pos, MachineAssignment.DEFAULT);
    }

    public void setAssignment(BlockPos pos, MachineAssignment assignment) {
        assignments.put(pos, assignment);
    }

    public int getEffectivePriority(BlockPos pos) {
        MachineAssignment assignment = getAssignment(pos);
        int floor = 0;
        for (GroupDefinition group : groups) {
            if (group.id().equals(assignment.groupId())) {
                floor = group.priorityFloor();
                break;
            }
        }
        return Math.max(assignment.priority(), floor);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag groupList = new ListTag();
        for (GroupDefinition group : groups) groupList.add(group.save());
        tag.put("groups", groupList);

        ListTag assignList = new ListTag();
        for (var entry : assignments.entrySet()) {
            CompoundTag entryTag = entry.getValue().save();
            entryTag.putLong("pos", entry.getKey().asLong());
            assignList.add(entryTag);
        }
        tag.put("assignments", assignList);
        return tag;
    }

    public static DroneConfig load(CompoundTag tag) {
        DroneConfig config = new DroneConfig();
        ListTag groupList = tag.getList("groups", 10);
        for (int i = 0; i < groupList.size(); i++) config.groups.add(GroupDefinition.load(groupList.getCompound(i)));

        ListTag assignList = tag.getList("assignments", 10);
        for (int i = 0; i < assignList.size(); i++) {
            CompoundTag entryTag = assignList.getCompound(i);
            config.assignments.put(BlockPos.of(entryTag.getLong("pos")), MachineAssignment.load(entryTag));
        }
        return config;
    }
}
