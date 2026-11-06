package net.phoenix.core.integration.conflux.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.research.DisciplineInfo;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ClientResearchCache {

    public static Set<ResourceLocation> unlocked = Collections.emptySet();
    public static Set<ResourceLocation> lockedOut = Collections.emptySet();
    public static Set<String> flags = Collections.emptySet();

    private static @Nullable String disciplineId = null;
    private static @Nullable String disciplineTitle = null;
    private static boolean disciplineCommitted = false;
    private static Map<ConfluxDataType, Long> switchCost = Map.of();
    private static Map<ConfluxDataType, Long> data = Map.of();

    private ClientResearchCache() {}

    public static void update(Set<ResourceLocation> u, Set<ResourceLocation> lo, Set<String> f,
                              @Nullable String discId, @Nullable String discTitle,
                              boolean committed, Map<ConfluxDataType, Long> cost, Map<ConfluxDataType, Long> stored) {
        data = Map.copyOf(stored);
        unlocked = Set.copyOf(u);
        lockedOut = Set.copyOf(lo);
        flags = Set.copyOf(f);
        disciplineId = discId;
        disciplineTitle = discTitle;
        disciplineCommitted = committed;
        switchCost = Map.copyOf(cost);
        ConfluxRecipeVisibility.onDisciplineMaybeChanged();
    }

    public static boolean hasMilestone(String milestone) {
        if (flags.contains(
                net.phoenix.core.integration.conflux.research.WorldResearchData.MILESTONE_PREFIX + milestone)) {
            return true;
        }
        for (ResourceLocation nodeId : unlocked) {
            var node = net.phoenix.core.integration.conflux.research.ResearchTreeRegistry.INSTANCE.getNode(nodeId)
                    .orElse(null);
            if (node != null && milestone.equals(node.milestone)) return true;
        }
        return false;
    }

    public static boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public static String flagLabel(String flag) {
        if (flag.startsWith("sig:")) {
            String[] parts = flag.split(":");
            if (parts.length == 3) {
                for (var entry : net.phoenix.core.integration.conflux.signature.ClientSignatureCache.entries()) {
                    var def = entry.definition();
                    if (!def.id().equals(parts[1])) continue;
                    try {
                        int level = Math.max(0, Math.min(def.levels().size() - 1, Integer.parseInt(parts[2])));
                        String name = def.levels().get(level).name();
                        return def.title() + ": " + (name.isEmpty() ? "level " + level : name);
                    } catch (NumberFormatException ignored) {}
                }
                return parts[1] + " level " + parts[2];
            }
        }
        return flag;
    }

    public static void clear() {
        unlocked = Collections.emptySet();
        lockedOut = Collections.emptySet();
        flags = Collections.emptySet();
        disciplineId = null;
        disciplineTitle = null;
        disciplineCommitted = false;
        switchCost = Map.of();
        data = Map.of();
    }

    public static long dataStored(ConfluxDataType type) {
        return data.getOrDefault(type, 0L);
    }

    public static boolean isUnlocked(ResourceLocation id) {
        return unlocked.contains(id);
    }

    public static boolean isLockedOut(ResourceLocation id) {
        return lockedOut.contains(id);
    }

    public static void acceptDefinition(byte kind, ResourceLocation id, String json) {
        if (net.minecraft.client.Minecraft.getInstance().isLocalServer()) return;
        var trees = net.phoenix.core.integration.conflux.research.ResearchTreeRegistry.INSTANCE;
        var rules = net.phoenix.core.integration.conflux.research.GatedRecipeRegistry.INSTANCE;
        switch (kind) {
            case net.phoenix.core.integration.conflux.network.S2CResearchDataPacket.RESET -> {
                trees.clientReset();
                rules.clientReset();
            }
            case net.phoenix.core.integration.conflux.network.S2CResearchDataPacket.TREE -> trees.clientAdd(id, json);
            case net.phoenix.core.integration.conflux.network.S2CResearchDataPacket.RULE -> rules.clientAdd(id, json);
            case net.phoenix.core.integration.conflux.network.S2CResearchDataPacket.REMOVE -> trees.clientRemove(id);
            default -> {}
        }
    }

    public static DisciplineInfo getDisciplineInfo() {
        if (disciplineId == null) return DisciplineInfo.NONE;
        return new DisciplineInfo(disciplineId, disciplineTitle, disciplineCommitted, switchCost);
    }
}
