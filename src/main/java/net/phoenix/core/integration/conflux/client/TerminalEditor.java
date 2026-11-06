package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.conflux.network.C2SEditResearchTreePacket;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeEdits;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

final class TerminalEditor {

    private static final int MENU_ROW = 14;
    private static final int C_BG = 0xF00F0F1C;
    private static final int HISTORY_MAX = 60;

    @Nullable
    private ResearchTree tree;

    private record MenuEntry(String label, boolean enabled, Runnable action) {}

    private List<MenuEntry> menu = null;
    private int menuX, menuY, menuW;

    @Nullable
    private ResearchNode linkSource;

    @Nullable
    private JsonObject clipboard;

    private record Change(ResourceLocation id, @Nullable String before, @Nullable String after) {}

    private record Action(List<Change> changes) {}

    private final Deque<Action> undo = new ArrayDeque<>();
    private final Deque<Action> redo = new ArrayDeque<>();

    private String feedback = "";
    private long feedbackUntil = 0L;

    static boolean allowed() {
        var player = Minecraft.getInstance().player;
        return player != null && player.hasPermissions(2);
    }

    boolean menuOpen() {
        return menu != null;
    }

    boolean linking() {
        return linkSource != null;
    }

    void close() {
        menu = null;
        linkSource = null;
    }

    private void say(String text) {
        feedback = text;
        feedbackUntil = System.currentTimeMillis() + 2500L;
    }

    void setTree(@Nullable ResearchTree tree) {
        this.tree = tree;
    }

    private void showMenu(List<MenuEntry> entries, int x, int y, int boundsRight, int boundsBottom) {
        close();
        menu = entries;
        Font font = Minecraft.getInstance().font;
        int w = 120;
        for (MenuEntry e : entries) w = Math.max(w, font.width(e.label()) + 18);
        menuW = w;
        menuX = Math.max(2, Math.min(x, boundsRight - w - 2));
        menuY = Math.max(2, Math.min(y, boundsBottom - entries.size() * MENU_ROW - 2));
    }

    record Item(String label, boolean enabled, Runnable action) {}

    void openCustomMenu(List<Item> items, int x, int y, int boundsRight, int boundsBottom) {
        List<MenuEntry> entries = new ArrayList<>();
        for (Item i : items) entries.add(new MenuEntry(i.label(), i.enabled(), i.action()));
        showMenu(entries, x, y, boundsRight, boundsBottom);
    }

    void openNodeMenu(ResearchTree tree, ResearchNode node, int x, int y, int boundsRight, int boundsBottom) {
        this.tree = tree;
        List<MenuEntry> items = new ArrayList<>();
        items.add(new MenuEntry("§eEdit…", true, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) mc.setScreen(new NodeEditorScreen(mc.screen, this, tree, node));
        }));
        items.add(new MenuEntry("Duplicate §8(Ctrl+D)", true, () -> duplicate(node)));
        items.add(new MenuEntry("Copy §8(Ctrl+C)", true, () -> copy(node)));
        items.add(new MenuEntry("§bLink / unlink to…", true, () -> {
            linkSource = node;
            say("Click another node to link or unlink it  (Esc cancels)");
        }));
        items.add(new MenuEntry("Make a start node", !node.isRoot(), () -> edit(json -> {
            var target = ResearchTreeEdits.find(json, node.id.toString());
            if (target != null) ResearchTreeEdits.makeStart(target);
        })));
        items.add(new MenuEntry("§cDelete", true, () -> edit(json -> ResearchTreeEdits.removeNode(json,
                node.id.toString()))));
        showMenu(items, x, y, boundsRight, boundsBottom);
    }

    void openCanvasMenu(ResearchTree tree, int x, int y, int gridX, int gridY, int boundsRight, int boundsBottom) {
        this.tree = tree;
        List<MenuEntry> items = new ArrayList<>();
        items.add(new MenuEntry("§a+ New node here", true, () -> addNode(gridX, gridY)));
        items.add(new MenuEntry("Paste node here §8(Ctrl+V)", clipboard != null, () -> paste(gridX, gridY)));
        items.add(new MenuEntry("⊞ Auto-arrange tree", true, () -> edit(ResearchTreeEdits::autoArrange)));
        items.add(new MenuEntry("↶ Undo §8(Ctrl+Z)", canUndo(), this::undoEdit));
        items.add(new MenuEntry("↷ Redo §8(Ctrl+Y)", canRedo(), this::redoEdit));
        showMenu(items, x, y, boundsRight, boundsBottom);
    }

    void renderBackdrop(GuiGraphics g, Font font, int mx, int my, int accent) {
        if (menu != null) {
            int h = menu.size() * MENU_ROW;
            g.fill(menuX - 1, menuY - 1, menuX + menuW + 1, menuY + h + 1, accent);
            g.fill(menuX, menuY, menuX + menuW, menuY + h, C_BG);
            for (int i = 0; i < menu.size(); i++) {
                int y = menuY + i * MENU_ROW;
                MenuEntry entry = menu.get(i);
                boolean hov = entry.enabled() && mx >= menuX && mx < menuX + menuW && my >= y && my < y + MENU_ROW;
                if (hov) g.fill(menuX, y, menuX + menuW, y + MENU_ROW, 0xFF22223A);
                g.drawString(font, entry.enabled() ? entry.label() : "§8" + entry.label().replaceAll("§.", ""),
                        menuX + 8, y + 3, 0xFFFFFF, false);
            }
        }
        long now = System.currentTimeMillis();
        if (now < feedbackUntil || linkSource != null) {
            String text = linkSource != null && now >= feedbackUntil ?
                    "Linking from " + linkSource.title + " — click another node  (Esc cancels)" : feedback;
            int w = font.width(text) + 12;
            int x = (g.guiWidth() - w) / 2;
            g.fill(x, 40, x + w, 54, C_BG);
            g.drawCenteredString(font, "§e" + text, x + w / 2, 43, 0xFFFFFF);
        }
    }

    boolean clickMenu(double mx, double my, int btn) {
        if (menu == null) return false;
        List<MenuEntry> entries = menu;
        menu = null;
        if (btn == 0 && mx >= menuX && mx < menuX + menuW && my >= menuY) {
            int i = (int) ((my - menuY) / MENU_ROW);
            if (i >= 0 && i < entries.size() && entries.get(i).enabled()) entries.get(i).action().run();
        }
        return true;
    }

    boolean finishLink(ResearchNode target) {
        if (linkSource == null) return false;
        ResearchNode from = linkSource;
        linkSource = null;
        if (target == from) return true;
        edit(json -> {
            boolean linked = ResearchTreeEdits.toggleLink(json, from.id.toString(), target.id.toString());
            say(linked ? "Linked" : "Unlinked");
        });
        return true;
    }

    boolean keyPressed(int key, boolean ctrl, boolean shift, @Nullable ResearchNode hovered, int gridX, int gridY,
                       boolean gridKnown) {
        if (tree == null && hovered == null) return false;
        if (ctrl && key == GLFW.GLFW_KEY_Z) {
            if (shift) redoEdit();
            else undoEdit();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_Y) {
            redoEdit();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_D && hovered != null) {
            duplicate(hovered);
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_C && hovered != null) {
            copy(hovered);
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_V && clipboard != null && gridKnown) {
            paste(gridX, gridY);
            return true;
        }
        return false;
    }

    @Nullable
    private JsonObject treeJson() {
        if (tree == null) return null;
        String raw = ResearchTreeRegistry.INSTANCE.rawJson().get(tree.id);
        return raw == null ? null : JsonParser.parseString(raw).getAsJsonObject();
    }

    @Nullable
    private String currentRaw() {
        return tree == null ? null : ResearchTreeRegistry.INSTANCE.rawJson().get(tree.id);
    }

    private void edit(Consumer<JsonObject> change) {
        JsonObject json = treeJson();
        String before = currentRaw();
        if (json == null || before == null || tree == null) return;
        change.accept(json);
        String after = json.toString();
        if (after.equals(before)) return;
        record(new Action(List.of(new Change(tree.id, before, after))));
        send(tree.id, after);
    }

    void editTree(ResearchTree tree, Consumer<JsonObject> change) {
        this.tree = tree;
        edit(change);
    }

    void editTrees(List<ResearchTree> targets, java.util.function.BiConsumer<ResearchTree, JsonObject> change) {
        List<Change> changes = new ArrayList<>();
        for (ResearchTree t : targets) {
            String before = ResearchTreeRegistry.INSTANCE.rawJson().get(t.id);
            if (before == null) continue;
            JsonObject json = JsonParser.parseString(before).getAsJsonObject();
            change.accept(t, json);
            String after = json.toString();
            if (!after.equals(before)) changes.add(new Change(t.id, before, after));
        }
        if (changes.isEmpty()) return;
        record(new Action(changes));
        changes.forEach(c -> send(c.id(), c.after()));
    }

    void createTree(ResourceLocation id, JsonObject json) {
        record(new Action(List.of(new Change(id, null, json.toString()))));
        send(id, json.toString());
    }

    void deleteTree(ResearchTree tree) {
        String before = ResearchTreeRegistry.INSTANCE.rawJson().get(tree.id);
        if (before == null) return;
        record(new Action(List.of(new Change(tree.id, before, null))));
        send(tree.id, null);
    }

    void deleteTrees(List<ResearchTree> targets) {
        List<Change> changes = new ArrayList<>();
        for (ResearchTree t : targets) {
            String before = ResearchTreeRegistry.INSTANCE.rawJson().get(t.id);
            if (before != null) changes.add(new Change(t.id, before, null));
        }
        if (changes.isEmpty()) return;
        record(new Action(changes));
        changes.forEach(c -> send(c.id(), null));
    }

    private void record(Action action) {
        undo.push(action);
        while (undo.size() > HISTORY_MAX) undo.removeLast();
        redo.clear();
    }

    private static void send(ResourceLocation id, @Nullable String json) {
        PacketDistributor.sendToServer(new C2SEditResearchTreePacket(id, json == null ? "" : json));
    }

    private boolean canUndo() {
        return !undo.isEmpty();
    }

    private boolean canRedo() {
        return !redo.isEmpty();
    }

    private void undoEdit() {
        if (!canUndo()) return;
        Action action = undo.pop();
        redo.push(action);
        action.changes().forEach(c -> send(c.id(), c.before()));
        say("Undone");
    }

    private void redoEdit() {
        if (!canRedo()) return;
        Action action = redo.pop();
        undo.push(action);
        action.changes().forEach(c -> send(c.id(), c.after()));
        say("Redone");
    }

    private static boolean occupied(JsonObject json, int x, int y) {
        for (var e : ResearchTreeEdits.nodes(json)) {
            JsonObject n = e.getAsJsonObject();
            if (ResearchTreeEdits.posX(n) == x && ResearchTreeEdits.posY(n) == y) return true;
        }
        return false;
    }

    private String freshId(JsonObject json) {
        return ResearchTreeEdits.freshId(json, tree.id.getNamespace(), tree.id.getPath().replace('/', '_'));
    }

    private void addNode(int gridX, int gridY) {
        if (tree == null) return;
        edit(json -> {
            int y = gridY;
            while (occupied(json, gridX, y)) y++;
            ResearchTreeEdits.addNode(json, freshId(json), "New node", gridX, y, null);
        });
    }

    private void duplicate(ResearchNode source) {
        edit(json -> {
            JsonObject original = ResearchTreeEdits.find(json, source.id.toString());
            if (original == null) return;
            int x = ResearchTreeEdits.posX(original) + 1, y = ResearchTreeEdits.posY(original);
            while (occupied(json, x, y)) y++;
            ResearchTreeEdits.nodes(json).add(ResearchTreeEdits.copyNode(original, freshId(json), x, y));
        });
    }

    private void copy(ResearchNode source) {
        JsonObject json = treeJson();
        if (json == null) return;
        JsonObject original = ResearchTreeEdits.find(json, source.id.toString());
        if (original == null) return;
        clipboard = original.deepCopy();
        say("Copied " + source.title);
    }

    private void paste(int gridX, int gridY) {
        if (clipboard == null) return;
        JsonObject toPaste = clipboard;
        edit(json -> {
            int y = gridY;
            while (occupied(json, gridX, y)) y++;
            ResearchTreeEdits.nodes(json).add(ResearchTreeEdits.copyNode(toPaste, freshId(json), gridX, y));
        });
    }

    void moveNode(ResearchTree tree, ResearchNode node, int gridX, int gridY) {
        this.tree = tree;
        if (gridX == node.posX && gridY == node.posY) return;
        edit(json -> {
            if (occupied(json, gridX, gridY)) {
                say("That cell is taken");
                return;
            }
            JsonObject target = ResearchTreeEdits.find(json, node.id.toString());
            if (target != null) ResearchTreeEdits.setPosition(target, gridX, gridY);
        });
    }
}
