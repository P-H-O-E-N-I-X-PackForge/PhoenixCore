package net.phoenix.core.integration.continuum.common;

import net.minecraft.nbt.CompoundTag;

public final class Outpost {

    @FunctionalInterface
    public interface CyclePayer {

        int pay(int cycles, int probes);
    }

    public static final CyclePayer FREE = (cycles, probes) -> cycles;

    private int probes;

    private int readyCycles;

    private long cycleStartMillis;

    private int lostCycles;

    private boolean powered = true;

    private boolean damaged;

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

    public void damage() {
        damaged = true;
        brokenCycles = 0;
    }

    public void repair(long now) {
        damaged = false;
        brokenCycles = 0;
        cycleStartMillis = now;
    }

    public void addProbes(int count) {
        probes += count;
    }

    public boolean settle(long now, long cycleMillis, int maxReady, CyclePayer payer) {
        if (cycleMillis <= 0) return false;

        if (damaged) {

            long passed = Math.max(0L, (now - cycleStartMillis) / cycleMillis);
            if (passed > 0) {
                brokenCycles = (int) Math.min(Integer.MAX_VALUE, brokenCycles + passed);
                cycleStartMillis += passed * cycleMillis;
            }
            return false;
        }

        if (readyCycles >= maxReady) {

            cycleStartMillis = now;
            return false;
        }

        long finished = Math.max(0L, (now - cycleStartMillis) / cycleMillis);
        if (finished <= 0) return false;

        int banked = (int) Math.min(finished, maxReady - readyCycles);

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
