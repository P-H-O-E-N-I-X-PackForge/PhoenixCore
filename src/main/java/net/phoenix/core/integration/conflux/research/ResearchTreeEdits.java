package net.phoenix.core.integration.conflux.research;

import net.phoenix.core.integration.conflux.ConfluxDataType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class ResearchTreeEdits {

    private ResearchTreeEdits() {}

    public static JsonArray nodes(JsonObject tree) {
        if (!tree.has("nodes")) tree.add("nodes", new JsonArray());
        return tree.getAsJsonArray("nodes");
    }

    @Nullable
    public static JsonObject find(JsonObject tree, String nodeId) {
        for (JsonElement e : nodes(tree)) {
            JsonObject node = e.getAsJsonObject();
            if (node.has("id") && node.get("id").getAsString().equals(nodeId)) return node;
        }
        return null;
    }

    public static void setFields(JsonObject node, String title, String lore, String icon,
                                 Map<ConfluxDataType, Long> cost, List<ResearchUnlock> unlocks) {
        node.addProperty("title", title);
        if (lore.isBlank()) node.remove("lore");
        else node.addProperty("lore", lore);
        node.addProperty("icon", icon.isBlank() ? "minecraft:book" : icon);

        JsonObject costObject = new JsonObject();
        cost.forEach((type, amount) -> {
            if (amount != null && amount > 0) costObject.addProperty(type.id(), amount);
        });
        if (costObject.size() == 0) node.remove("cost");
        else node.add("cost", costObject);

        if (unlocks.isEmpty()) {
            node.remove("unlocks");
        } else {
            JsonArray array = new JsonArray();
            unlocks.forEach(u -> array.add(u.toJson()));
            node.add("unlocks", array);
        }
    }

    public static JsonObject addNode(JsonObject tree, String id, String title, int x, int y, @Nullable String parent) {
        JsonObject node = new JsonObject();
        node.addProperty("id", id);
        node.addProperty("title", title);
        node.addProperty("icon", "minecraft:book");
        JsonArray position = new JsonArray();
        position.add(x);
        position.add(y);
        node.add("position", position);
        if (parent != null) {
            JsonArray prerequisites = new JsonArray();
            prerequisites.add(parent);
            node.add("prerequisites", prerequisites);
        }
        nodes(tree).add(node);
        return node;
    }

    public static boolean removeNode(JsonObject tree, String nodeId) {
        JsonArray all = nodes(tree);
        boolean removed = false;
        for (int i = all.size() - 1; i >= 0; i--) {
            JsonObject node = all.get(i).getAsJsonObject();
            if (node.has("id") && node.get("id").getAsString().equals(nodeId)) {
                all.remove(i);
                removed = true;
            }
        }
        for (JsonElement e : all) {
            JsonObject node = e.getAsJsonObject();
            for (String key : new String[] { "prerequisites", "prerequisites_any" }) {
                if (!node.has(key)) continue;
                JsonArray list = node.getAsJsonArray(key);
                for (int i = list.size() - 1; i >= 0; i--) {
                    if (list.get(i).getAsString().equals(nodeId)) list.remove(i);
                }
                if (list.isEmpty()) node.remove(key);
            }
        }
        return removed;
    }

    public static String freshId(JsonObject tree, String namespace, String prefix) {
        int n = nodes(tree).size() + 1;
        while (true) {
            String id = namespace + ":" + prefix + "_" + n;
            if (find(tree, id) == null) return id;
            n++;
        }
    }

    public static void setPosition(JsonObject node, int x, int y) {
        JsonArray position = new JsonArray();
        position.add(x);
        position.add(y);
        node.add("position", position);
    }

    public static int posX(JsonObject node) {
        return node.has("position") ? node.getAsJsonArray("position").get(0).getAsInt() : 0;
    }

    public static int posY(JsonObject node) {
        return node.has("position") ? node.getAsJsonArray("position").get(1).getAsInt() : 0;
    }

    private static boolean has(JsonObject node, String key, String id) {
        if (!node.has(key)) return false;
        for (JsonElement e : node.getAsJsonArray(key)) {
            if (e.getAsString().equals(id)) return true;
        }
        return false;
    }

    private static void drop(JsonObject node, String key, String id) {
        if (!node.has(key)) return;
        JsonArray list = node.getAsJsonArray(key);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getAsString().equals(id)) list.remove(i);
        }
        if (list.isEmpty()) node.remove(key);
    }

    public static boolean toggleLink(JsonObject tree, String from, String to) {
        JsonObject a = find(tree, from), b = find(tree, to);
        if (a == null || b == null || from.equals(to)) return false;
        boolean linked = has(b, "prerequisites", from) || has(b, "prerequisites_any", from) ||
                has(a, "prerequisites", to) || has(a, "prerequisites_any", to);
        if (linked) {
            for (String key : new String[] { "prerequisites", "prerequisites_any" }) {
                drop(a, key, to);
                drop(b, key, from);
            }
            return false;
        }
        if (!b.has("prerequisites")) b.add("prerequisites", new JsonArray());
        b.getAsJsonArray("prerequisites").add(from);
        return true;
    }

    public static void makeStart(JsonObject node) {
        node.remove("prerequisites");
        node.remove("prerequisites_any");
    }

    public static JsonObject copyNode(JsonObject node, String newId, int x, int y) {
        JsonObject copy = node.deepCopy();
        copy.addProperty("id", newId);
        copy.remove("prerequisites");
        copy.remove("prerequisites_any");
        copy.remove("commitment");
        setPosition(copy, x, y);
        return copy;
    }

    public static void autoArrange(JsonObject tree) {
        java.util.Map<String, JsonObject> byId = new java.util.LinkedHashMap<>();
        for (JsonElement e : nodes(tree)) {
            JsonObject n = e.getAsJsonObject();
            byId.put(n.get("id").getAsString(), n);
        }
        java.util.Map<String, Integer> depth = new java.util.HashMap<>();
        for (String id : byId.keySet()) depthOf(id, byId, depth, new java.util.HashSet<>());

        java.util.Map<Integer, java.util.List<JsonObject>> columns = new java.util.TreeMap<>();
        for (var entry : byId.entrySet()) {
            columns.computeIfAbsent(depth.get(entry.getKey()), k -> new java.util.ArrayList<>()).add(entry.getValue());
        }
        for (var column : columns.entrySet()) {
            var list = column.getValue();
            list.sort(java.util.Comparator.comparingInt(ResearchTreeEdits::posY)
                    .thenComparing(n -> n.get("id").getAsString()));
            for (int i = 0; i < list.size(); i++) setPosition(list.get(i), column.getKey(), i);
        }
    }

    private static int depthOf(String id, java.util.Map<String, JsonObject> byId, java.util.Map<String, Integer> depth,
                               java.util.Set<String> path) {
        Integer known = depth.get(id);
        if (known != null) return known;
        if (!path.add(id)) return 0;
        JsonObject node = byId.get(id);
        int best = 0;
        if (node != null) {
            for (String key : new String[] { "prerequisites", "prerequisites_any" }) {
                if (!node.has(key)) continue;
                for (JsonElement e : node.getAsJsonArray(key)) {
                    if (byId.containsKey(e.getAsString())) {
                        best = Math.max(best, depthOf(e.getAsString(), byId, depth, path) + 1);
                    }
                }
            }
        }
        path.remove(id);
        depth.put(id, best);
        return best;
    }

    public static void setRequirements(JsonObject node, java.util.List<String> flags, String milestone) {
        if (flags.isEmpty()) {
            node.remove("requires_flags");
        } else {
            JsonArray array = new JsonArray();
            flags.forEach(array::add);
            node.add("requires_flags", array);
        }
        if (milestone.isBlank()) node.remove("milestone");
        else node.addProperty("milestone", milestone.trim());
    }

    public static void setMeta(JsonObject node, String hint, boolean hidden, boolean commitment, String exclusionGroup,
                               List<String> prerequisites, List<String> prerequisitesAny) {
        if (hint.isBlank()) node.remove("hint");
        else node.addProperty("hint", hint);
        if (hidden) node.addProperty("hidden", true);
        else node.remove("hidden");
        if (commitment) node.addProperty("commitment", true);
        else node.remove("commitment");
        if (exclusionGroup.isBlank()) node.remove("exclusion_group");
        else node.addProperty("exclusion_group", exclusionGroup);
        for (var entry : java.util.Map.of("prerequisites", prerequisites, "prerequisites_any", prerequisitesAny)
                .entrySet()) {
            if (entry.getValue().isEmpty()) {
                node.remove(entry.getKey());
            } else {
                JsonArray array = new JsonArray();
                entry.getValue().forEach(array::add);
                node.add(entry.getKey(), array);
            }
        }
    }
}
