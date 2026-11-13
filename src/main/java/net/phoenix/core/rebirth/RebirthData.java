package net.phoenix.core.rebirth;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RebirthData extends SavedData {

    private static final String ID = "phoenix_rebirth";
    public static final long NONE = Long.MIN_VALUE;

    public static final class Team {

        public long seed;
        @Nullable
        public String forgeDim;
        public long forgePos = NONE;
        public final boolean[] bound = new boolean[Capstone.COUNT];
        public final long[] pos = new long[Capstone.COUNT];
        public final String[] dim = new String[Capstone.COUNT];

        public int[] ring = new int[0];
        public int lastAligned;
        public int attempts;
        public boolean closed;
        public boolean reborn;

        Team() {
            Arrays.fill(pos, NONE);
        }

        public boolean allBound() {
            for (boolean b : bound) if (!b) return false;
            return true;
        }
    }

    private final Map<UUID, Team> teams = new HashMap<>();

    public static RebirthData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(RebirthData::new, RebirthData::load, null), ID);
    }

    public Team team(UUID id) {
        return teams.computeIfAbsent(id, k -> {
            Team t = new Team();
            t.seed = id.getLeastSignificantBits() ^ (id.getMostSignificantBits() * 0xC2B2AE3D27D4EB4FL);
            return t;
        });
    }

    public void changed() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        teams.forEach((id, t) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", id);
            e.putLong("seed", t.seed);
            if (t.forgeDim != null) e.putString("forgeDim", t.forgeDim);
            e.putLong("forgePos", t.forgePos);
            for (int i = 0; i < Capstone.COUNT; i++) {
                e.putBoolean("bound" + i, t.bound[i]);
                e.putLong("pos" + i, t.pos[i]);
                if (t.dim[i] != null) e.putString("dim" + i, t.dim[i]);
            }
            e.putIntArray("ring", t.ring);
            e.putInt("aligned", t.lastAligned);
            e.putInt("attempts", t.attempts);
            e.putBoolean("closed", t.closed);
            e.putBoolean("reborn", t.reborn);
            list.add(e);
        });
        tag.put("teams", list);
        return tag;
    }

    public static RebirthData load(CompoundTag tag, HolderLookup.Provider registries) {
        RebirthData d = new RebirthData();
        for (Tag raw : tag.getList("teams", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) raw;
            Team t = new Team();
            t.seed = e.getLong("seed");
            t.forgeDim = e.contains("forgeDim") ? e.getString("forgeDim") : null;
            t.forgePos = e.contains("forgePos") ? e.getLong("forgePos") : NONE;
            for (int i = 0; i < Capstone.COUNT; i++) {
                t.bound[i] = e.getBoolean("bound" + i);
                t.pos[i] = e.contains("pos" + i) ? e.getLong("pos" + i) : NONE;
                t.dim[i] = e.contains("dim" + i) ? e.getString("dim" + i) : null;
            }
            t.ring = e.getIntArray("ring");
            t.lastAligned = e.getInt("aligned");
            t.attempts = e.getInt("attempts");
            t.closed = e.getBoolean("closed");
            t.reborn = e.getBoolean("reborn");

            if (t.ring.length != Capstone.COUNT) t.ring = new int[0];
            if (!t.allBound()) {
                t.closed = false;
                t.reborn = false;
            }
            d.teams.put(e.getUUID("id"), t);
        }
        return d;
    }
}
