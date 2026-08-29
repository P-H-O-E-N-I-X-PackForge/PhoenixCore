package net.phoenix.core.integration.drone.group;

import net.minecraft.nbt.CompoundTag;

public record GroupDefinition(String id, String name, int priorityFloor) {

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putInt("priorityFloor", priorityFloor);
        return tag;
    }

    public static GroupDefinition load(CompoundTag tag) {
        return new GroupDefinition(tag.getString("id"), tag.getString("name"), tag.getInt("priorityFloor"));
    }

    public GroupDefinition withPriorityFloor(int newFloor) {
        return new GroupDefinition(id, name, newFloor);
    }
}
