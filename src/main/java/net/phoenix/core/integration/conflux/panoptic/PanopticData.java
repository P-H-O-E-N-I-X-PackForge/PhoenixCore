package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PanopticData extends SavedData {

    private static final String ID = "conflux_panoptic";

    public static final String COMPLETE_FLAG = "panoptic_complete";

    public static String stageFlag(int stage) {
        return "panoptic_stage_" + stage;
    }

    public static final class Team {

        public int stage;
        public float progress;
        public long seed;
        public boolean complete;
        @Nullable
        public PanopticPuzzle.Round round;

        @Nullable
        public String devDiscipline;
    }

    private final Map<UUID, Team> teams = new HashMap<>();

    public static PanopticData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PanopticData::new, PanopticData::load, null), ID);
    }

    public Team team(UUID id) {
        return teams.computeIfAbsent(id, k -> {
            Team t = new Team();
            t.seed = id.getLeastSignificantBits() ^ (id.getMostSignificantBits() * 0x9E3779B97F4A7C15L);
            return t;
        });
    }

    public float growth(UUID id) {
        Team t = teams.get(id);
        if (t == null) return 0f;
        return t.complete ? 1f : PanopticTreeModel.growth(t.stage, t.progress);
    }

    public @Nullable PanopticPuzzle.Round currentRound(UUID id, Testimony testimony) {
        Team t = team(id);
        if (t.complete) return null;
        PanopticPuzzle puzzle = PanopticPuzzles.forDiscipline(testimony.discipline());
        if (puzzle == null) return null;
        if (t.round == null) {
            t.round = puzzle.generate(testimony, t.stage, t.seed);
            setDirty();
        }
        return t.round;
    }

    public boolean solveRound(UUID id) {
        Team t = team(id);
        if (t.complete) return true;
        t.stage++;
        t.progress = 0f;
        t.round = null;
        if (t.stage >= PanopticTreeModel.STAGES) {
            t.stage = PanopticTreeModel.STAGES;
            t.complete = true;
        }
        setDirty();
        return t.complete;
    }

    public void setDevDiscipline(UUID id, @Nullable String discipline) {
        Team t = team(id);
        t.devDiscipline = discipline;
        t.round = null;
        setDirty();
    }

    public void devSet(UUID id, int stage, float progress) {
        Team t = team(id);
        t.stage = Math.max(0, Math.min(PanopticTreeModel.STAGES, stage));
        t.progress = Math.max(0f, Math.min(1f, progress));
        t.complete = t.stage >= PanopticTreeModel.STAGES;
        t.round = null;
        setDirty();
    }

    public void nudge(UUID id, float closeness) {
        Team t = team(id);
        if (t.complete) return;
        float target = Math.min(0.9f, Math.max(0f, closeness) * 0.9f);
        if (target > t.progress) {
            t.progress = target;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        teams.forEach((id, t) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", id);
            e.putInt("stage", t.stage);
            e.putFloat("progress", t.progress);
            e.putLong("seed", t.seed);
            e.putBoolean("complete", t.complete);
            if (t.round != null) e.put("round", writeRound(t.round));
            if (t.devDiscipline != null) e.putString("devDiscipline", t.devDiscipline);
            list.add(e);
        });
        tag.put("teams", list);
        return tag;
    }

    public static PanopticData load(CompoundTag tag, HolderLookup.Provider registries) {
        PanopticData d = new PanopticData();
        for (Tag raw : tag.getList("teams", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) raw;
            Team t = new Team();
            t.stage = e.getInt("stage");
            t.progress = e.getFloat("progress");
            t.seed = e.getLong("seed");
            t.complete = e.getBoolean("complete");
            if (e.contains("round", Tag.TAG_COMPOUND)) t.round = readRound(e.getCompound("round"));
            if (e.contains("devDiscipline")) t.devDiscipline = e.getString("devDiscipline");
            d.teams.put(e.getUUID("id"), t);
        }
        return d;
    }

    static CompoundTag writeRound(PanopticPuzzle.Round r) {
        CompoundTag t = new CompoundTag();
        t.putInt("index", r.index());
        t.putString("title", r.title());
        t.put("clues", strings(r.clues()));
        t.put("choices", strings(r.choices()));
        t.putInt("slots", r.slots());
        t.putInt("max", r.maxValue());
        t.put("truth", new IntArrayTag(r.truth()));
        return t;
    }

    static PanopticPuzzle.Round readRound(CompoundTag t) {
        return new PanopticPuzzle.Round(t.getInt("index"), t.getString("title"), strings(t.getList("clues", 8)),
                strings(t.getList("choices", 8)), t.getInt("slots"), t.getInt("max"), t.getIntArray("truth"));
    }

    private static ListTag strings(List<String> values) {
        ListTag list = new ListTag();
        for (String s : values) list.add(StringTag.valueOf(s));
        return list;
    }

    private static List<String> strings(ListTag list) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }
}
