package net.phoenix.core.integration.continuum.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One expedition. Timing is wall-clock milliseconds so a mission keeps running while the server is off or the player is
 * away; the outcome is rolled once at launch and hidden until the mission lands, so reloading cannot re-roll it.
 * The rocket rides along inside the mission and comes back (with wear) when the team collects it.
 */
public final class Mission {

    public enum State {

        /** Still travelling. */
        ACTIVE,
        /** Landed successfully; waiting to be collected. */
        SUCCESS,
        /** Failed; waiting to be collected. */
        FAILED;

        public boolean finished() {
            return this != ACTIVE;
        }
    }

    /** What the expedition is for. */
    public enum Type {

        /** Resolve the target: Detected becomes Surveyed. */
        SURVEY,
        /** Drop extraction probes on a surveyed body and bring resources home. */
        EXTRACT,
        /** Leave probes behind as a lasting outpost that produces resources over time. */
        DEPLOY,
        /** Fly out to an outpost and bring back what it has stockpiled. */
        HAUL;

        public String label() {
            return switch (this) {
                case SURVEY -> "Survey";
                case EXTRACT -> "Extraction run";
                case DEPLOY -> "Outpost deployment";
                case HAUL -> "Outpost haul";
            };
        }
    }

    public final UUID id;
    public final UUID team;
    public final UUID launcher;
    public final String launcherName;
    public final ResourceLocation destination;
    public final Type type;
    /** Extraction probes the mission carries (and loses if it fails). */
    public final int probes;
    /** Resources rolled at launch for a successful extraction; handed over on collection. */
    public final List<ItemStack> rewards;
    public long startMillis;
    public long durationMillis;
    public final boolean willSucceed;
    /** Wear the rocket picks up on this trip (a failure adds extra on top, see {@link ContinuumMissions}). */
    public final float wearCost;
    public final ItemStack rocket;

    public State state;

    public Mission(UUID id, UUID team, UUID launcher, String launcherName, ResourceLocation destination, Type type,
                   int probes, List<ItemStack> rewards, long startMillis, long durationMillis, boolean willSucceed,
                   float wearCost, ItemStack rocket, State state) {
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
        return tag;
    }

    public static Mission load(CompoundTag tag) {
        int ordinal = Math.max(0, Math.min(tag.getByte("State"), State.values().length - 1));
        int typeOrdinal = Math.max(0, Math.min(tag.getByte("Type"), Type.values().length - 1));
        List<ItemStack> rewards = new ArrayList<>();
        for (Tag t : tag.getList("Rewards", Tag.TAG_COMPOUND)) rewards.add(ItemStack.of((CompoundTag) t));
        return new Mission(
                tag.getUUID("Id"), tag.getUUID("Team"), tag.getUUID("Launcher"), tag.getString("LauncherName"),
                new ResourceLocation(tag.getString("Destination")), Type.values()[typeOrdinal], tag.getInt("Probes"),
                rewards, tag.getLong("Start"), tag.getLong("Duration"), tag.getBoolean("WillSucceed"),
                tag.getFloat("WearCost"), ItemStack.of(tag.getCompound("Rocket")), State.values()[ordinal]);
    }
}
