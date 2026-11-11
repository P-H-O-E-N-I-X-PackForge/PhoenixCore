package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record Testimony(String discipline, long seed, List<Fact> facts, int[] affinity) {

    public record Fact(ResourceLocation id, String title, int depth) {}

    public int unlocked() {
        return facts.size();
    }

    public List<ConfluxDataType> ranking() {
        List<ConfluxDataType> types = new ArrayList<>(List.of(ConfluxDataType.values()));
        types.sort(Comparator.<ConfluxDataType>comparingInt(t -> -affinity[t.ordinal()])
                .thenComparingInt(Enum::ordinal));
        return types;
    }

    public static Testimony of(UUID team, String discipline, Set<ResourceLocation> unlocked) {
        Map<ResourceLocation, Integer> depths = new HashMap<>();
        List<Fact> facts = new ArrayList<>();
        int[] affinity = new int[ConfluxDataType.values().length];
        for (ResourceLocation id : unlocked) {
            Optional<ResearchNode> node = ResearchTreeRegistry.INSTANCE.getNode(id);
            if (node.isEmpty()) continue;
            ResearchNode n = node.get();
            if (n.isCommitmentNode) continue;
            facts.add(new Fact(id, n.title, depth(id, depths, 0)));
            for (var cost : n.cost.entrySet()) {
                long v = Math.min(cost.getValue(), 1_000_000L);
                affinity[cost.getKey().ordinal()] += (int) Math.max(1, Math.round(Math.log10(v + 10)));
            }
        }
        facts.sort(Comparator.comparingInt(Fact::depth).thenComparing(f -> f.id().toString()));
        long seed = team.getMostSignificantBits() ^ team.getLeastSignificantBits() ^ (long) facts.size() * 31L;
        return new Testimony(discipline, seed, List.copyOf(facts), affinity);
    }

    private static int depth(ResourceLocation id, Map<ResourceLocation, Integer> memo, int guard) {
        Integer known = memo.get(id);
        if (known != null) return known;
        if (guard > 64) return 0;
        Optional<ResearchNode> node = ResearchTreeRegistry.INSTANCE.getNode(id);
        int d = 0;
        if (node.isPresent()) {
            for (ResourceLocation pre : node.get().prerequisites) d = Math.max(d, 1 + depth(pre, memo, guard + 1));
        }
        memo.put(id, d);
        return d;
    }
}
