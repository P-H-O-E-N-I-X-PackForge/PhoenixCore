package net.phoenix.core.integration.conflux.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResearchTreeValidator {

    private ResearchTreeValidator() {}

    public static final Set<String> UNLOCK_TYPES = Set.of("item", "archive", "multiblock", "recipe", "recipe_tag",
            "flag", "world_stage", "recipe_modifier");

    public enum Severity {
        ERROR,
        WARNING
    }

    public record Node(String id, String title, int x, int y, List<String> prerequisites, String exclusionGroup,
                       boolean hasCost, List<String> unlockTypes) {}

    public record Issue(Severity severity, String nodeId, String message) {}

    public static List<Issue> check(List<Node> nodes) {
        List<Issue> out = new ArrayList<>();
        if (nodes.isEmpty()) {
            out.add(new Issue(Severity.WARNING, null, "The tree has no nodes."));
            return out;
        }

        Map<String, Node> byId = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (Node n : nodes) {
            if (!seen.add(n.id())) {
                out.add(new Issue(Severity.ERROR, n.id(), "Two nodes share the id '" + n.id() + "'."));
            }
            byId.putIfAbsent(n.id(), n);
        }

        boolean anyRoot = false;
        Map<String, List<Node>> byPosition = new HashMap<>();
        Map<String, List<Node>> byExclusion = new HashMap<>();
        for (Node n : nodes) {
            if (n.prerequisites().isEmpty()) anyRoot = true;
            for (String pre : n.prerequisites()) {
                if (!byId.containsKey(pre)) {
                    out.add(new Issue(Severity.ERROR, n.id(), "Needs '" + pre + "', which is not in this tree."));
                }
                if (pre.equals(n.id())) out.add(new Issue(Severity.ERROR, n.id(), "Needs itself."));
            }
            if (n.title() == null || n.title().isBlank() || n.title().equals(n.id())) {
                out.add(new Issue(Severity.WARNING, n.id(), "Has no title of its own."));
            }
            if (!n.hasCost() && n.unlockTypes().isEmpty()) {
                out.add(new Issue(Severity.WARNING, n.id(), "Costs nothing and unlocks nothing."));
            }
            for (String type : n.unlockTypes()) {
                if (!UNLOCK_TYPES.contains(type)) {
                    out.add(new Issue(Severity.WARNING, n.id(), "Unknown unlock type '" + type + "'."));
                }
            }
            byPosition.computeIfAbsent(n.x() + "," + n.y(), k -> new ArrayList<>()).add(n);
            if (n.exclusionGroup() != null && !n.exclusionGroup().isBlank()) {
                byExclusion.computeIfAbsent(n.exclusionGroup(), k -> new ArrayList<>()).add(n);
            }
        }
        if (!anyRoot)
            out.add(new Issue(Severity.ERROR, null, "Every node has a prerequisite, so nothing can be started."));

        for (List<Node> same : byPosition.values()) {
            if (same.size() > 1) {
                for (Node n : same) out.add(new Issue(Severity.WARNING, n.id(), "Sits on top of another node."));
            }
        }
        byExclusion.forEach((group, members) -> {
            if (members.size() == 1) {
                out.add(new Issue(Severity.WARNING, members.get(0).id(),
                        "Is the only node in the exclusion group '" + group + "'."));
            }
        });

        Set<String> reported = new HashSet<>();
        for (Node n : nodes) {
            if (reported.contains(n.id())) continue;
            if (reaches(n.id(), n.id(), byId, new HashSet<>())) {
                out.add(new Issue(Severity.ERROR, n.id(), "Is part of a loop of prerequisites."));
                reported.add(n.id());
            }
        }

        out.sort((a, b) -> a.severity() == b.severity() ? 0 : a.severity() == Severity.ERROR ? -1 : 1);
        return out;
    }

    private static boolean reaches(String from, String target, Map<String, Node> byId, Set<String> visited) {
        Node node = byId.get(from);
        if (node == null) return false;
        for (String pre : node.prerequisites()) {
            if (pre.equals(target) && !pre.equals(from)) return true;
            if (pre.equals(from)) continue;
            if (visited.add(pre) && reaches(pre, target, byId, visited)) return true;
        }
        return false;
    }

    public static boolean hasErrors(List<Issue> issues) {
        for (Issue i : issues) if (i.severity() == Severity.ERROR) return true;
        return false;
    }
}
