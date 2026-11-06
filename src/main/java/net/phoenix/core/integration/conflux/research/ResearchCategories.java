package net.phoenix.core.integration.conflux.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResearchCategories {

    private ResearchCategories() {}

    public static final String SEPARATOR = "/";

    public record Item(int index, String group, int order, String title) {}

    public record Entry(String path, String name, int depth, int treeIndex, boolean collapsed, List<Integer> trees) {

        public boolean isHeader() {
            return treeIndex < 0;
        }
    }

    private static final class Node {

        final String name;
        final String path;
        final Map<String, Node> children = new LinkedHashMap<>();
        final List<Item> items = new ArrayList<>();
        int minOrder = Integer.MAX_VALUE;
        int firstSeen;

        Node(String name, String path, int firstSeen) {
            this.name = name;
            this.path = path;
            this.firstSeen = firstSeen;
        }
    }

    public static List<String> segments(String group) {
        List<String> out = new ArrayList<>();
        if (group != null) {
            for (String part : group.split(SEPARATOR)) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) out.add(trimmed);
            }
        }
        if (out.isEmpty()) out.add("Research");
        return out;
    }

    public static List<Entry> layout(List<Item> items, List<String> topOrder, Set<String> collapsed) {
        Node root = new Node("", "", 0);
        int seen = 0;
        for (Item item : items) {
            Node node = root;
            StringBuilder path = new StringBuilder();
            for (String segment : segments(item.group())) {
                if (path.length() > 0) path.append(SEPARATOR);
                path.append(segment);
                Node parent = node;
                int at = ++seen;
                node = node.children.computeIfAbsent(segment, k -> new Node(segment, path.toString(), at));
                parent.minOrder = Math.min(parent.minOrder, item.order());
            }
            node.items.add(item);
            node.minOrder = Math.min(node.minOrder, item.order());
        }

        List<Entry> out = new ArrayList<>();
        List<Node> top = new ArrayList<>(root.children.values());
        top.sort(Comparator.comparingInt((Node n) -> {
            int i = topOrder.indexOf(n.name);
            return i < 0 ? Integer.MAX_VALUE : i;
        }).thenComparingInt(n -> n.firstSeen));
        for (Node node : top) emit(node, 0, collapsed, out);
        return out;
    }

    private static void emit(Node node, int depth, Set<String> collapsed, List<Entry> out) {
        List<Integer> all = new ArrayList<>();
        collect(node, all);
        boolean shut = collapsed.contains(node.path);
        out.add(new Entry(node.path, node.name, depth, -1, shut, all));
        if (shut) return;

        List<Item> items = new ArrayList<>(node.items);
        items.sort(Comparator.comparingInt(Item::order).thenComparing(Item::title).thenComparingInt(Item::index));
        for (Item item : items) out.add(new Entry(node.path, node.name, depth + 1, item.index(), false, List.of()));

        List<Node> children = new ArrayList<>(node.children.values());
        children.sort(Comparator.comparingInt((Node n) -> n.minOrder).thenComparingInt(n -> n.firstSeen));
        for (Node child : children) emit(child, depth + 1, collapsed, out);
    }

    private static void collect(Node node, List<Integer> into) {
        for (Item item : node.items) into.add(item.index());
        for (Node child : node.children.values()) collect(child, into);
    }
}
