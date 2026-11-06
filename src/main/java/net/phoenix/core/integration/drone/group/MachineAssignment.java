package net.phoenix.core.integration.drone.group;

import net.minecraft.nbt.CompoundTag;

public record MachineAssignment(String groupId, int priority) {

    public static final MachineAssignment DEFAULT = new MachineAssignment("", 5);

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("groupId", groupId);
        tag.putInt("priority", priority);
        return tag;
    }

    public static MachineAssignment load(CompoundTag tag) {
        return new MachineAssignment(tag.getString("groupId"), tag.getInt("priority"));
    }
}
