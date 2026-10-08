package net.phoenix.core.integration.continuum.common;

import net.minecraft.nbt.CompoundTag;

/**
 * A team's installation of extraction probes on one body. It produces in real time: every cycle each probe makes one
 * roll against the body's yield table, and finished cycles pile up (to a cap) until a Haul mission claims them.
 *
 * <p>
 * Running costs power. Each finished cycle has to be paid for with energy from the team's Tesla Network (see
 * {@link OutpostPower}); a cycle that cannot be paid for is simply lost, so an outpost whose network is empty stalls
 * instead of building up a free backlog.
 *
 * <p>
 * Nothing ticks. {@link #settle} works out how many cycles have finished since it last ran, so an outpost keeps
 * producing while the server is off or nobody is online.
 */
public final class Outpost {

    /** Pays for finished cycles. */
    @FunctionalInterface
    public interface CyclePayer {

        /** Pays upkeep for up to {@code cycles} cycles of an outpost with {@code probes} probes; returns how many. */
        int pay(int cycles, int probes);
    }

    /** A payer that never charges anything: for outposts with upkeep switched off, and for test tools. */
    public static final CyclePayer FREE = (cycles, probes) -> cycles;

    private int probes;
    /** Cycles finished and waiting to be hauled. */
    private int readyCycles;
    /** The wall-clock time the next unfinished cycle started. */
    private long cycleStartMillis;
    /** Cycles that finished but could not be paid for, since the last haul. */
    private int lostCycles;
    /** Whether the most recent finished cycles were paid for. */
    private boolean powered = true;
    /** Broken by an incident: produces nothing, and costs nothing, until a repair run fixes it. */
    private boolean damaged;
    /** Cycles that passed while it was broken. */
    private int brokenCycles;

    private static final net.minecraft.util.RandomSource RANDOM = net.minecraft.util.RandomSource.create();

    public Outpost(int probes, int readyCycles, long cycleStartMillis) {
        this.probes = probes;
        this.readyCycles = readyCycles;
        this.cycleStartMillis = cycleStartMillis;
    }

    public int probes() {
        return probes;
    }

    public int readyCycles() {
        return readyCycles;
    }

    public long cycleStartMillis() {
        return cycleStartMillis;
    }

    public int lostCycles() {
        return lostCycles;
    }

    public boolean powered() {
        return powered;
    }

    public boolean damaged() {
        return damaged;
    }

    public int brokenCycles() {
        return brokenCycles;
    }

    /** Breaks the outpost (an incident, or the admin command). */
    public void damage() {
        damaged = true;
        brokenCycles = 0;
    }

    /** Fixes the outpost; its clock restarts from {@code now}, and what it had stockpiled is kept. */
    public void repair(long now) {
        damaged = false;
        brokenCycles = 0;
        cycleStartMillis = now;
    }

    /** Adds probes. Cycles already banked are kept; the running cycle carries on. */
    public void addProbes(int count) {
        probes += count;
    }

    /**
     * Banks every cycle that finished since the last call, charging the upkeep for each.
     *
     * @param maxReady cycles beyond this are not produced: a full outpost stops until it is hauled
     * @param payer    pays for the finished cycles; cycles it cannot pay for are lost
     * @return true if an incident broke the outpost during this call
     */
    public boolean settle(long now, long cycleMillis, int maxReady, CyclePayer payer) {
        if (cycleMillis <= 0) return false;

        if (damaged) {
            // a broken outpost makes nothing and costs nothing; the time that passes is simply lost
            long passed = Math.max(0L, (now - cycleStartMillis) / cycleMillis);
            if (passed > 0) {
                brokenCycles = (int) Math.min(Integer.MAX_VALUE, brokenCycles + passed);
                cycleStartMillis += passed * cycleMillis;
            }
            return false;
        }

        if (readyCycles >= maxReady) {
            // full: the clock does not run up a backlog, production resumes from the moment space frees up
            cycleStartMillis = now;
            return false;
        }

        long finished = Math.max(0L, (now - cycleStartMillis) / cycleMillis);
        if (finished <= 0) return false;

        int banked = (int) Math.min(finished, maxReady - readyCycles);

        // each finished cycle may bring an incident; cycles after the break are not produced
        int survived = banked;
        boolean broke = false;
        double chance = net.phoenix.core.configs.PhoenixConfigs.INSTANCE.continuum.outpostIncidentChance;
        if (chance > 0.0) {
            for (int i = 0; i < banked; i++) {
                if (RANDOM.nextDouble() < chance) {
                    survived = i;
                    broke = true;
                    break;
                }
            }
        }

        int paid = Math.max(0, Math.min(survived, payer.pay(survived, probes)));
        readyCycles += paid;
        lostCycles += survived - paid;
        powered = paid >= survived;

        if (broke) {
            damaged = true;
            brokenCycles = (int) Math.max(0L, finished - survived - 1);
            cycleStartMillis += finished * cycleMillis;
        } else {
            cycleStartMillis = readyCycles >= maxReady ? now : cycleStartMillis + finished * cycleMillis;
        }
        return broke;
    }

    /** Takes everything banked, for a Haul mission to carry. */
    public int claim(long now) {
        int claimed = readyCycles;
        readyCycles = 0;
        lostCycles = 0;
        cycleStartMillis = now;
        return claimed;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Probes", probes);
        tag.putInt("Ready", readyCycles);
        tag.putLong("CycleStart", cycleStartMillis);
        tag.putInt("Lost", lostCycles);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Damaged", damaged);
        tag.putInt("Broken", brokenCycles);
        return tag;
    }

    public static Outpost load(CompoundTag tag) {
        Outpost outpost = new Outpost(tag.getInt("Probes"), tag.getInt("Ready"), tag.getLong("CycleStart"));
        outpost.lostCycles = tag.getInt("Lost");
        outpost.powered = !tag.contains("Powered") || tag.getBoolean("Powered");
        outpost.damaged = tag.getBoolean("Damaged");
        outpost.brokenCycles = tag.getInt("Broken");
        return outpost;
    }
}
