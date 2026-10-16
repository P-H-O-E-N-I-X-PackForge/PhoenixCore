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

    /**
     * A definition from the server. On an integrated server the client already shares the server's registries, so there
     * is nothing to replace and replacing them would only churn data both sides are using.
     */
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
            default -> {}
        }
    }

    public static DisciplineInfo getDisciplineInfo() {
        if (disciplineId == null) return DisciplineInfo.NONE;
        return new DisciplineInfo(disciplineId, disciplineTitle, disciplineCommitted, switchCost);
    }
}
