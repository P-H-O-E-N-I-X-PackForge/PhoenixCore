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

    /** Adds probes. Cycles already banked are kept; the running cycle carries on. */
    public void addProbes(int count) {
        probes += count;
    }

    /**
     * Banks every cycle that finished since the last call, charging the upkeep for each.
     *
     * @param maxReady cycles beyond this are not produced: a full outpost stops until it is hauled
     * @param payer    pays for the finished cycles; cycles it cannot pay for are lost
     */
    public void settle(long now, long cycleMillis, int maxReady, CyclePayer payer) {
        if (cycleMillis <= 0) return;

        if (readyCycles >= maxReady) {
            // full: the clock does not run up a backlog, production resumes from the moment space frees up
            cycleStartMillis = now;
            return;
        }

        long finished = Math.max(0L, (now - cycleStartMillis) / cycleMillis);
        if (finished <= 0) return;

        int banked = (int) Math.min(finished, maxReady - readyCycles);
        int paid = Math.max(0, Math.min(banked, payer.pay(banked, probes)));

        readyCycles += paid;
        lostCycles += banked - paid;
        powered = paid >= banked;
        cycleStartMillis = readyCycles >= maxReady ? now : cycleStartMillis + finished * cycleMillis;
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
        return tag;
    }

    public static Outpost load(CompoundTag tag) {
        Outpost outpost = new Outpost(tag.getInt("Probes"), tag.getInt("Ready"), tag.getLong("CycleStart"));
        outpost.lostCycles = tag.getInt("Lost");
        outpost.powered = !tag.contains("Powered") || tag.getBoolean("Powered");
        return outpost;
    }
}
