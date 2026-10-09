package net.phoenix.core.integration.continuum.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Mission {

    public enum State {

        ACTIVE,

        SUCCESS,

        FAILED;

        public boolean finished() {
            return this != ACTIVE;
        }
    }

    public enum Type {

        SURVEY,

        EXTRACT,

        DEPLOY,

        HAUL,

        REPAIR,

        RESCUE,

        STATION;

        public String label() {
            return switch (this) {
                case SURVEY -> "Survey";
                case EXTRACT -> "Extraction run";
                case DEPLOY -> "Outpost deployment";
                case HAUL -> "Outpost haul";
                case REPAIR -> "Outpost repair run";
                case RESCUE -> "Rescue run";
                case STATION -> "Station construction";
            };
        }
    }

    public final UUID id;
    public final UUID team;
    public final UUID launcher;
    public final String launcherName;
    public final ResourceLocation destination;
    public final Type type;

    public final int probes;

    public final List<ItemStack> rewards;
    public long startMillis;
    public long durationMillis;
    public final boolean willSucceed;

    public final float wearCost;
    public final ItemStack rocket;

    public List<String> events;

    public State state;

    public @org.jetbrains.annotations.Nullable String padDimension;
    public long padPos;

    public long distressUntil;

    public boolean rescuing;

    public boolean stranded() {
        return distressUntil > 0;
    }

    public Mission(UUID id, UUID team, UUID launcher, String launcherName, ResourceLocation destination, Type type,
                   int probes, List<ItemStack> rewards, long startMillis, long durationMillis, boolean willSucceed,
                   float wearCost, ItemStack rocket, State state, List<String> events) {
        this.events = events;
        this.id = id;
        this.team = team;
        this.launcher = launcher;
        this.launcherName = launcherName;
        this.destination = destination;
        this.type = type;
        this.probes = probes;
        this.rewards = rewards;
        this.startMillis = startMillis;
        this.durationMillis = durationMillis;
        this.willSucceed = willSucceed;
        this.wearCost = wearCost;
        this.rocket = rocket;
        this.state = state;
    }

    public long endMillis() {
        return startMillis + durationMillis;
    }

    public boolean due(long now) {
        return state == State.ACTIVE && now >= endMillis();
    }

    public float progress(long now) {
        if (state.finished()) return 1.0f;
        if (durationMillis <= 0) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, (now - startMillis) / (float) durationMillis));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("Team", team);
        tag.putUUID("Launcher", launcher);
        tag.putString("LauncherName", launcherName);
        tag.putString("Destination", destination.toString());
        tag.putLong("Start", startMillis);
        tag.putLong("Duration", durationMillis);
        tag.putBoolean("WillSucceed", willSucceed);
        tag.putFloat("WearCost", wearCost);
        tag.put("Rocket", rocket.save(new CompoundTag()));
        tag.putByte("State", (byte) state.ordinal());
        tag.putByte("Type", (byte) type.ordinal());
        tag.putInt("Probes", probes);
        ListTag rewardList = new ListTag();
        for (ItemStack reward : rewards) rewardList.add(reward.save(new CompoundTag()));
        tag.put("Rewards", rewardList);
        ListTag eventList = new ListTag();
        for (String event : events) eventList.add(net.minecraft.nbt.StringTag.valueOf(event));
        tag.put("Events", eventList);
        if (padDimension != null) {
            tag.putString("PadDimension", padDimension);
            tag.putLong("PadPos", padPos);
        }
        if (distressUntil > 0) {
            tag.putLong("DistressUntil", distressUntil);
            tag.putBoolean("Rescuing", rescuing);
        }
        return tag;
    }

    public static Mission load(CompoundTag tag) {
        int ordinal = Math.max(0, Math.min(tag.getByte("State"), State.values().length - 1));
        int typeOrdinal = Math.max(0, Math.min(tag.getByte("Type"), Type.values().length - 1));
        List<ItemStack> rewards = new ArrayList<>();
        for (Tag t : tag.getList("Rewards", Tag.TAG_COMPOUND)) rewards.add(ItemStack.of((CompoundTag) t));
        List<String> events = new ArrayList<>();
        for (Tag t : tag.getList("Events", Tag.TAG_STRING)) events.add(t.getAsString());
        Mission mission = new Mission(
                tag.getUUID("Id"), tag.getUUID("Team"), tag.getUUID("Launcher"), tag.getString("LauncherName"),
                new ResourceLocation(tag.getString("Destination")), Type.values()[typeOrdinal], tag.getInt("Probes"),
                rewards, tag.getLong("Start"), tag.getLong("Duration"), tag.getBoolean("WillSucceed"),
                tag.getFloat("WearCost"), ItemStack.of(tag.getCompound("Rocket")), State.values()[ordinal], events);
        if (tag.contains("PadDimension")) {
            mission.padDimension = tag.getString("PadDimension");
            mission.padPos = tag.getLong("PadPos");
        }
        mission.distressUntil = tag.getLong("DistressUntil");
        mission.rescuing = tag.getBoolean("Rescuing");
        return mission;
    }
}
