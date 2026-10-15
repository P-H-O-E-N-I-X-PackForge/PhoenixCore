package net.phoenix.core.integration.drone.group;

import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class DroneControlTrait extends MachineTrait {

    private DroneConfig config;

    @Override
    public void onMachineLoad() {
        super.onMachineLoad();
        if (!getMachine().isRemote() && getMachine().getLevel() instanceof ServerLevel level) {
            this.config = DroneWorldData.get(level).getOrCreate(getMachine().getBlockPos());
        }
    }

    public DroneConfig config() {
        if (config == null && getMachine().getLevel() instanceof ServerLevel level) {
            config = DroneWorldData.get(level).getOrCreate(getMachine().getBlockPos());
        }
        return config != null ? config : new DroneConfig();
    }

    private void markDirty() {
        if (getMachine().getLevel() instanceof ServerLevel level) {
            DroneWorldData.get(level).markDirty();
        }
    }

    public GroupDefinition addGroup(String name) {
        GroupDefinition group = config().addGroup(name);
        markDirty();
        return group;
    }

    public void removeGroup(String groupId) {
        config().removeGroup(groupId);
        markDirty();
    }

    public void adjustGroupPriorityFloor(String groupId, int delta) {
        for (GroupDefinition group : config().groups) {
            if (group.id().equals(groupId)) {
                config().setGroupPriorityFloor(groupId, Math.max(0, Math.min(10, group.priorityFloor() + delta)));
                markDirty();
                return;
            }
        }
    }

    public void assign(BlockPos target, String groupId, int priority) {
        config().setAssignment(target, new MachineAssignment(groupId, priority));
        markDirty();
    }

    public String cycleGroup(BlockPos target) {
        MachineAssignment current = config().getAssignment(target);
        java.util.List<GroupDefinition> groups = config().groups;
        if (groups.isEmpty()) {
            assign(target, "", current.priority());
            return "None (no groups defined)";
        }
        int idx = -1;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).id().equals(current.groupId())) {
                idx = i;
                break;
            }
        }

        if (idx + 1 >= groups.size()) {
            assign(target, "", current.priority());
            return "Ungrouped";
        }
        GroupDefinition next = groups.get(idx + 1);
        assign(target, next.id(), current.priority());
        return next.name();
    }

    public int cyclePriority(BlockPos target) {
        MachineAssignment current = config().getAssignment(target);
        int next = (current.priority() + 1) % 11;
        assign(target, current.groupId(), next);
        return next;
    }

    public int getEffectivePriority(BlockPos pos) {
        return config().getEffectivePriority(pos);
    }
}
