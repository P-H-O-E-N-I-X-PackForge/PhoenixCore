package net.phoenix.core.integration.conflux.dimension.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.Nullable;

import java.util.*;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientDisciplineProgressionCache {

    private static final Map<UUID, CachedProgression> progressionCache = new HashMap<>();
    private static String currentDiscipline;
    private static String currentStage = "initial";

    public static class CachedProgression {

        public String disciplineId;
        public String currentStage;
        public Set<String> unlockedStages;
        public long lastUpdate;

        public CachedProgression(String disciplineId) {
            this.disciplineId = disciplineId;
            this.currentStage = "initial";
            this.unlockedStages = new HashSet<>();
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public static void updateProgression(UUID teamId, String disciplineId, String currentStage,
                                         Set<String> unlockedStages) {
        CachedProgression prog = progressionCache.computeIfAbsent(teamId, k -> new CachedProgression(disciplineId));
        prog.currentStage = currentStage;
        prog.unlockedStages.addAll(unlockedStages);
        prog.lastUpdate = System.currentTimeMillis();

        ClientDisciplineProgressionCache.currentDiscipline = disciplineId;
        ClientDisciplineProgressionCache.currentStage = currentStage;
        BiomeColorProvider.setDisciplineProgression(disciplineId, currentStage);
        DisciplineSkyRenderer.setSkyboxProfile(disciplineId);
    }

    @Nullable
    public static CachedProgression getProgression(UUID teamId) {
        return progressionCache.get(teamId);
    }

    public static String getCurrentDiscipline() {
        return currentDiscipline;
    }

    public static String getCurrentStage() {
        return currentStage;
    }

    public static boolean hasStageUnlocked(UUID teamId, String stageName) {
        CachedProgression prog = progressionCache.get(teamId);
        return prog != null && prog.unlockedStages.contains(stageName);
    }

    public static void clearCache() {
        progressionCache.clear();
        currentDiscipline = null;
        currentStage = "initial";
        BiomeColorProvider.reset();
        DisciplineSkyRenderer.reset();
    }

    public static CompoundTag serializeToNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag progressionsTag = new ListTag();

        for (Map.Entry<UUID, CachedProgression> entry : progressionCache.entrySet()) {
            CompoundTag progTag = new CompoundTag();
            progTag.putUUID("teamId", entry.getKey());
            progTag.putString("disciplineId", entry.getValue().disciplineId);
            progTag.putString("currentStage", entry.getValue().currentStage);

            ListTag stagesTag = new ListTag();
            for (String stage : entry.getValue().unlockedStages) {
                stagesTag.add(net.minecraft.nbt.StringTag.valueOf(stage));
            }
            progTag.put("unlockedStages", stagesTag);

            progressionsTag.add(progTag);
        }

        tag.put("progressions", progressionsTag);
        tag.putString("currentDiscipline", currentDiscipline != null ? currentDiscipline : "");
        tag.putString("currentStage", currentStage);

        return tag;
    }

    public static void deserializeFromNBT(CompoundTag tag) {
        progressionCache.clear();

        if (tag.contains("progressions", Tag.TAG_LIST)) {
            ListTag progressionsTag = tag.getList("progressions", Tag.TAG_COMPOUND);
            for (int i = 0; i < progressionsTag.size(); i++) {
                CompoundTag progTag = progressionsTag.getCompound(i);
                UUID teamId = progTag.getUUID("teamId");
                CachedProgression prog = new CachedProgression(progTag.getString("disciplineId"));
                prog.currentStage = progTag.getString("currentStage");

                if (progTag.contains("unlockedStages", Tag.TAG_LIST)) {
                    ListTag stagesTag = progTag.getList("unlockedStages", Tag.TAG_STRING);
                    for (int j = 0; j < stagesTag.size(); j++) {
                        prog.unlockedStages.add(stagesTag.getString(j));
                    }
                }

                progressionCache.put(teamId, prog);
            }
        }

        if (tag.contains("currentDiscipline", Tag.TAG_STRING)) {
            String disc = tag.getString("currentDiscipline");
            currentDiscipline = disc.isEmpty() ? null : disc;
        }

        if (tag.contains("currentStage", Tag.TAG_STRING)) {
            currentStage = tag.getString("currentStage");
        }

        if (currentDiscipline != null) {
            BiomeColorProvider.setDisciplineProgression(currentDiscipline, currentStage);
            DisciplineSkyRenderer.setSkyboxProfile(currentDiscipline);
        }
    }
}
