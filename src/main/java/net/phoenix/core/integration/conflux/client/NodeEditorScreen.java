package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeEdits;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchUnlock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class NodeEditorScreen extends Screen {

    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 32;
    private static final int MARGIN = 14;
    private static final int MAX_W = 520;
    private static final int FIELD_H = 16;
    private static final int SEC_H = 18;

    private enum Section {

        BASIC("Basic Info"),
        COST("Cost"),
        UNLOCKS("Unlocks"),
        POSITION("Position & Links"),
        RAW("Raw JSON preview");

        final String label;

        Section(String label) {
            this.label = label;
        }
    }

    private final Screen parent;
    private final TerminalEditor editor;
    private final ResearchTree tree;
    private final ResearchNode node;

    private final EditBox title, lore, hint, icon, exclusion, gridX, gridY, flagInput, milestoneBox;
    private final Map<ConfluxDataType, EditBox> costs = new EnumMap<>(ConfluxDataType.class);
    private final List<ResearchUnlock> unlocks = new ArrayList<>();
    private final List<String> prerequisites = new ArrayList<>();
    private final List<String> prerequisitesAny = new ArrayList<>();
    private final List<String> requiresFlags = new ArrayList<>();
    private boolean hidden, commitment;
    private final Set<Section> collapsed = EnumSet.of(Section.RAW);
    private final List<EditBox> allBoxes = new ArrayList<>();

    private int scroll = 0;
    private int contentH = 0;
    private boolean confirmDelete = false;

    private record Rect(int x, int y, int w, int h) {

        boolean has(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Map<Section, Rect> headers = new EnumMap<>(Section.class);
    private final List<Rect> unlockRemovers = new ArrayList<>();
    private final List<Rect> prereqRemovers = new ArrayList<>();
    private final List<Rect> prereqAnyRemovers = new ArrayList<>();
    private final List<Rect> flagRemovers = new ArrayList<>();
    private final List<Rect> levelChips = new ArrayList<>();
    private final List<String> levelChipFlags = new ArrayList<>();
    private Rect addFlag;
    private Rect addUnlock, hiddenBox, commitBox, startNode, pickIcon;

    private record Pencil(Rect rect, EditBox box, String heading) {}

    private final List<Pencil> pencils = new ArrayList<>();

    public NodeEditorScreen(Screen parent, TerminalEditor editor, ResearchTree tree, ResearchNode node) {
        super(Component.literal("Edit node"));
        this.parent = parent;
        this.editor = editor;
        this.tree = tree;
        this.node = node;
        var font = Minecraft.getInstance().font;

        title = box(font, node.title, "title");
        lore = box(font, node.lore, "lore / flavour text");
        hint = box(font, node.hint, "hint (shown before it is researched)");
        icon = box(font, node.icon, "icon item id");
        exclusion = box(font, node.exclusionGroup == null ? "" : node.exclusionGroup, "exclusion group");
        flagInput = box(font, "", "a flag, such as sig:ember:2");
        milestoneBox = box(font, node.milestone == null ? "" : node.milestone, "milestone id");
        gridX = box(font, String.valueOf(node.posX), "x");
        gridY = box(font, String.valueOf(node.posY), "y");
        for (EditBox b : new EditBox[] { gridX, gridY }) b.setFilter(s -> s.matches("-?[0-9]*"));
        for (ConfluxDataType type : ConfluxDataType.values()) {
            if (!type.isAvailable()) continue;
            long v = node.cost.getOrDefault(type, 0L);
            EditBox b = box(font, v == 0 ? "" : String.valueOf(v), "0");
            b.setFilter(s -> s.matches("[0-9]*"));
            costs.put(type, b);
        }
        unlocks.addAll(node.unlocks);
        node.prerequisites.forEach(id -> prerequisites.add(id.toString()));
        node.prerequisitesAny.forEach(id -> prerequisitesAny.add(id.toString()));
        requiresFlags.addAll(node.requiresFlags);
        hidden = node.hidden;
        commitment = node.isCommitmentNode;
    }

    private EditBox box(net.minecraft.client.gui.Font font, String value, String hintText) {
        EditBox b = new EditBox(font, 0, 0, 100, FIELD_H, Component.empty());
        b.setMaxLength(512);
        b.setHint(Component.literal(hintText));
        b.setValue(value);
        b.moveCursorToStart(false);
        allBoxes.add(b);
        return b;
    }

    @Override
    protected void init() {
        for (EditBox b : allBoxes) addRenderableWidget(b);
    }

    private int accent() {
        return ConfluxTheme.accent(tree.discipline);
    }

    private int colW() {
        return Math.min(MAX_W, width - MARGIN * 2);
    }

    private int colX() {
        return (width - colW()) / 2;
    }

    private int viewTop() {
        return HEADER_H + 1;
    }

    private int viewBottom() {
        return height - FOOTER_H - 1;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        ConfluxTheme.refresh();
        g.fill(0, 0, width, height, ConfluxTheme.BG);
        int accent = accent();

        for (EditBox b : allBoxes) {
            b.visible = false;
            b.active = false;
        }
        headers.clear();
        unlockRemovers.clear();
        prereqRemovers.clear();
        prereqAnyRemovers.clear();
        flagRemovers.clear();
        levelChips.clear();
        levelChipFlags.clear();
        addFlag = null;
        addUnlock = hiddenBox = commitBox = startNode = pickIcon = null;
        pencils.clear();

        int x = colX(), w = colW();
        int y = viewTop() + 8 - scroll;
        int top = viewTop(), bottom = viewBottom();

        g.enableScissor(0, top, width, bottom);

        y = sectionHeader(g, Section.BASIC, x, y, w, mx, my, accent);
        if (!collapsed.contains(Section.BASIC)) {
            y = pencilField(g, "Title", title, "the title", x, y, w, top, bottom, mx, my);
            y = pencilField(g, "Lore", lore, "the lore", x, y, w, top, bottom, mx, my);
            y = pencilField(g, "Hint  §8(shown before it is researched)", hint, "the hint", x, y, w, top, bottom, mx,
                    my);
            g.drawString(font,
                    "§7Icon  §8(an item, a fluid or any picture: " + ConfluxIcons.label(icon.getValue()) + ")", x + 4,
                    y, 0xFFFFFF, false);
            y += 10;
            place(icon, x + 24, y, w - 24 - 70, top, bottom);
            g.fill(x, y, x + 20, y + FIELD_H, 0xFF14142A);
            g.renderOutline(x, y, 20, FIELD_H, ConfluxTheme.BORDER);
            ConfluxIcons.draw(g, icon.getValue(), x + 2, y);
            pickIcon = new Rect(x + w - 66, y, 66, FIELD_H);
            button(g, pickIcon, "§ePick icon…", mx, my, 0xFF14142A, 0xFF22223A);
            y += FIELD_H + 8;
        }

        y = sectionHeader(g, Section.COST, x, y, w, mx, my, accent);
        if (!collapsed.contains(Section.COST)) {
            for (var entry : costs.entrySet()) {
                g.drawString(font, entry.getKey().displayComponent(), x + 4, y + 4, 0xFFFFFF, false);
                place(entry.getValue(), x + 130, y, w - 130, top, bottom);
                y += FIELD_H + 6;
            }
            y += 4;
        }

        y = sectionHeader(g, Section.UNLOCKS, x, y, w, mx, my, accent);
        if (!collapsed.contains(Section.UNLOCKS)) {
            if (unlocks.isEmpty()) {
                g.drawString(font, "§8nothing yet", x + 4, y + 2, 0xFFFFFF, false);
                y += 14;
            }
            for (int i = 0; i < unlocks.size(); i++) {
                ResearchUnlock u = unlocks.get(i);
                g.fill(x, y, x + w, y + 14, ConfluxTheme.PANEL);
                g.drawString(font, "§b" + u.type(), x + 6, y + 3, 0xFFFFFF, false);
                g.drawString(font, font.plainSubstrByWidth(u.value(), w - 120), x + 90, y + 3, 0xFFFFFF, false);
                Rect remove = new Rect(x + w - 18, y, 18, 14);
                unlockRemovers.add(remove);
                g.drawString(font, remove.has(mx, my) ? "§c×" : "§8×", x + w - 12, y + 3, 0xFFFFFF, false);
                y += 16;
            }
            addUnlock = new Rect(x, y + 2, 150, 16);
            button(g, addUnlock, "§a+ Add an unlock…", mx, my, 0xFF112211, 0xFF224422);
            y += 28;
        }

        y = sectionHeader(g, Section.POSITION, x, y, w, mx, my, accent);
        if (!collapsed.contains(Section.POSITION)) {
            g.drawString(font, "§7Grid position", x + 4, y, 0xFFFFFF, false);
            place(gridX, x, y + 10, 70, top, bottom);
            place(gridY, x + 78, y + 10, 70, top, bottom);
            y += 10 + FIELD_H + 8;
            y = field(g, "Exclusion group (only one node of a group can be researched)", exclusion, x, y, w, top,
                    bottom);

            hiddenBox = new Rect(x, y, w, 14);
            toggle(g, hiddenBox, "Hidden until its prerequisites are researched", hidden, mx, my);
            y += 18;
            commitBox = new Rect(x, y, w, 14);
            toggle(g, commitBox, "Commitment node (researching it commits the team to the discipline)", commitment,
                    mx, my);
            y += 22;

            g.drawString(font, "§7Requires all of", x + 4, y, 0xFFFFFF, false);
            y += 11;
            y = chips(g, prerequisites, prereqRemovers, x, y, w, mx, my);
            g.drawString(font, "§7Requires any one of", x + 4, y, 0xFFFFFF, false);
            y += 11;
            y = chips(g, prerequisitesAny, prereqAnyRemovers, x, y, w, mx, my);
            g.drawString(font, "\u00a77Requires flags  \u00a78(a signature level, or any flag a team can hold)", x + 4,
                    y, 0xFFFFFF,
                    false);
            y += 11;
            y = chips(g, requiresFlags, flagRemovers, x, y, w, mx, my);
            place(flagInput, x, y, w - 62, top, bottom);
            addFlag = new Rect(x + w - 58, y, 58, FIELD_H);
            button(g, addFlag, "\u00a7a+ Add", mx, my, 0xFF112211, 0xFF224422);
            y += FIELD_H + 4;
            var meters = net.phoenix.core.integration.conflux.signature.ClientSignatureCache.entries();
            if (!meters.isEmpty()) {
                g.drawString(font, "\u00a78Signature levels you can add:", x + 4, y + 3, 0xFFFFFF, false);
                y += 14;
                int cx = x;
                for (var entry : meters) {
                    var def = entry.definition();
                    for (int i = 1; i < def.levels().size(); i++) {
                        String label = def.title() + ": " + def.levels().get(i).name();
                        int cw = font.width(label) + 12;
                        if (cx + cw > x + w) {
                            cx = x;
                            y += 18;
                        }
                        Rect chip = new Rect(cx, y, cw, 15);
                        levelChips.add(chip);
                        levelChipFlags.add(def.flag(i));
                        button(g, chip, label, mx, my, 0xFF14142A, 0xFF22223A);
                        cx += cw + 4;
                    }
                }
                y += 20;
            }
            y = field(g,
                    "Milestone  \u00a78(reaching it is kept for good, even if the path is abandoned; other paths can ask for it)",
                    milestoneBox, x, y, w, top, bottom);
            startNode = new Rect(x, y + 2, 130, 16);
            button(g, startNode, "Make a start node", mx, my, 0xFF14142A, 0xFF22223A);
            g.drawString(font, "§8Links to other nodes are made on the canvas (right-click, Link / unlink).",
                    x + 140, y + 6, 0xFFFFFF, false);
            y += 30;
        }

        y = sectionHeader(g, Section.RAW, x, y, w, mx, my, accent);
        if (!collapsed.contains(Section.RAW)) {
            for (String line : rawLines()) {
                g.drawString(font, "§8" + font.plainSubstrByWidth(line, w - 8), x + 4, y, 0xFFFFFF, false);
                y += 9;
            }
            y += 6;
        }

        contentH = y + scroll - viewTop() + 8;
        g.disableScissor();

        g.enableScissor(0, top, width, bottom);
        super.render(g, mx, my, pt);
        g.disableScissor();

        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH - (bottom - top))));

        g.fill(0, 0, width, HEADER_H, ConfluxTheme.HEADER);
        g.fillGradient(0, 0, width, HEADER_H, ConfluxTheme.withAlpha(accent, 70), 0x00000000);
        g.fill(0, HEADER_H, width, HEADER_H + 1, accent);
        g.fill(0, 0, 3, HEADER_H, accent);
        g.drawString(font, "§eEditing §f" + node.title, MARGIN, 8, 0xFFFFFF, false);
        g.drawString(font, "§8" + node.id + "  ·  " + tree.title, MARGIN, 19, 0xFFFFFF, false);
        Rect close = new Rect(width - 26, 8, 16, 16);
        g.drawString(font, close.has(mx, my) ? "§c×" : "§7×", width - 20, 12, 0xFFFFFF, false);

        g.fill(0, height - FOOTER_H, width, height, ConfluxTheme.HEADER);
        g.fill(0, height - FOOTER_H - 1, width, height - FOOTER_H, accent);
        Rect delete = deleteButton(), cancel = cancelButton(), save = saveButton();
        button(g, delete, confirmDelete ? "§cClick again to delete" : "§cDelete node", mx, my, 0xFF330A0A,
                0xFF551111);
        button(g, cancel, "§fCancel", mx, my, 0xFF14142A, 0xFF22223A);
        button(g, save, "§aSave", mx, my, 0xFF112211, 0xFF224422);
    }

    private int sectionHeader(GuiGraphics g, Section s, int x, int y, int w, int mx, int my, int accent) {
        Rect r = new Rect(x, y, w, SEC_H);
        headers.put(s, r);
        boolean hov = r.has(mx, my) && my >= viewTop() && my < viewBottom();
        g.fill(x, y, x + w, y + SEC_H, hov ? 0xFF20202E : 0xFF1A1A26);
        g.fill(x, y, x + 2, y + SEC_H, accent);
        g.drawString(font, (collapsed.contains(s) ? "§7▸ " : "§f▾ ") + s.label, x + 8, y + 5, 0xFFFFFF, false);
        return y + SEC_H + 6;
    }

    private int field(GuiGraphics g, String label, EditBox box, int x, int y, int w, int top, int bottom) {
        if (label != null) {
            g.drawString(font, "§7" + label, x + 4, y, 0xFFFFFF, false);
            y += 10;
        }
        place(box, x, y, w, top, bottom);
        return y + FIELD_H + 8;
    }

    private int pencilField(GuiGraphics g, String label, EditBox box, String heading, int x, int y, int w, int top,
                            int bottom, int mx, int my) {
        g.drawString(font, "§7" + label, x + 4, y, 0xFFFFFF, false);
        y += 10;
        place(box, x, y, w - 24, top, bottom);
        Rect pencil = new Rect(x + w - 20, y, 20, FIELD_H);
        boolean hov = pencil.has(mx, my) && y + FIELD_H > top && y < bottom;
        g.fill(pencil.x(), pencil.y(), pencil.x() + pencil.w(), pencil.y() + pencil.h(), hov ? 0xFF22223A : 0xFF14142A);
        g.renderOutline(pencil.x(), pencil.y(), pencil.w(), pencil.h(), hov ? accent() : ConfluxTheme.BORDER);
        g.drawCenteredString(font, hov ? "§e✎" : "§7✎", pencil.x() + 10, pencil.y() + 4, 0xFFFFFF);
        pencils.add(new Pencil(pencil, box, heading));
        return y + FIELD_H + 8;
    }

    private void place(EditBox box, int x, int y, int w, int top, int bottom) {
        boolean inView = y + FIELD_H > top && y < bottom;
        box.setX(x);
        box.setY(y);
        box.setWidth(w);
        box.visible = inView;
        box.active = inView;
    }

    private void button(GuiGraphics g, Rect r, String label, int mx, int my, int base, int hover) {
        boolean hov = r.has(mx, my);
        g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), hov ? hover : base);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), ConfluxTheme.BORDER);
        g.drawCenteredString(font, label, r.x() + r.w() / 2, r.y() + (r.h() - 8) / 2 + 1, 0xFFFFFF);
    }

    private void toggle(GuiGraphics g, Rect r, String label, boolean on, int mx, int my) {
        boolean hov = r.has(mx, my);
        g.fill(r.x(), r.y(), r.x() + 12, r.y() + 12, hov ? 0xFF22223A : 0xFF14142A);
        g.renderOutline(r.x(), r.y(), 12, 12, ConfluxTheme.BORDER);
        if (on) g.drawString(font, "§a✔", r.x() + 2, r.y() + 2, 0xFFFFFF, false);
        g.drawString(font, "§7" + label, r.x() + 18, r.y() + 2, 0xFFFFFF, false);
    }

    private int chips(GuiGraphics g, List<String> ids, List<Rect> removers, int x, int y, int w, int mx, int my) {
        if (ids.isEmpty()) {
            g.drawString(font, "§8none", x + 4, y + 2, 0xFFFFFF, false);
            return y + 14;
        }
        for (String id : ids) {
            g.fill(x, y, x + w, y + 13, ConfluxTheme.PANEL);
            g.drawString(font, "§f" + font.plainSubstrByWidth(id, w - 28), x + 6, y + 3, 0xFFFFFF, false);
            Rect remove = new Rect(x + w - 18, y, 18, 13);
            removers.add(remove);
            g.drawString(font, remove.has(mx, my) ? "§c×" : "§8×", x + w - 12, y + 3, 0xFFFFFF, false);
            y += 15;
        }
        return y + 4;
    }

    private List<String> rawLines() {
        String raw = ResearchTreeRegistry.INSTANCE.rawJson().get(tree.id);
        if (raw == null) return List.of();
        JsonObject n = ResearchTreeEdits.find(JsonParser.parseString(raw).getAsJsonObject(), node.id.toString());
        if (n == null) return List.of();
        return List.of(new GsonBuilder().setPrettyPrinting().create().toJson(n).split("\n"));
    }

    private Rect saveButton() {
        return new Rect(width - MARGIN - 90, height - FOOTER_H + 8, 90, 16);
    }

    private Rect cancelButton() {
        return new Rect(width - MARGIN - 90 - 8 - 90, height - FOOTER_H + 8, 90, 16);
    }

    private Rect deleteButton() {
        return new Rect(MARGIN, height - FOOTER_H + 8, 140, 16);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (my < HEADER_H && mx >= width - 26 && mx < width - 10) {
                onClose();
                return true;
            }
            if (saveButton().has(mx, my)) {
                save();
                return true;
            }
            if (cancelButton().has(mx, my)) {
                onClose();
                return true;
            }
            if (deleteButton().has(mx, my)) {
                if (confirmDelete) delete();
                else confirmDelete = true;
                return true;
            }
            confirmDelete = false;
        }
        if (my >= viewTop() && my < viewBottom()) {
            if (super.mouseClicked(mx, my, btn)) return true;
            if (btn != 0) return false;
            for (var entry : headers.entrySet()) {
                if (entry.getValue().has(mx, my)) {
                    if (!collapsed.remove(entry.getKey())) collapsed.add(entry.getKey());
                    return true;
                }
            }
            for (int i = 0; i < unlockRemovers.size(); i++) {
                if (unlockRemovers.get(i).has(mx, my)) {
                    unlocks.remove(i);
                    return true;
                }
            }
            for (int i = 0; i < prereqRemovers.size(); i++) {
                if (prereqRemovers.get(i).has(mx, my)) {
                    prerequisites.remove(i);
                    return true;
                }
            }
            for (int i = 0; i < flagRemovers.size(); i++) {
                if (flagRemovers.get(i).has(mx, my)) {
                    requiresFlags.remove(i);
                    return true;
                }
            }
            if (addFlag != null && addFlag.has(mx, my)) {
                commitFlagInput();
                return true;
            }
            for (int i = 0; i < levelChips.size(); i++) {
                if (levelChips.get(i).has(mx, my)) {
                    String flag = levelChipFlags.get(i);
                    if (!requiresFlags.contains(flag)) requiresFlags.add(flag);
                    return true;
                }
            }
            for (int i = 0; i < prereqAnyRemovers.size(); i++) {
                if (prereqAnyRemovers.get(i).has(mx, my)) {
                    prerequisitesAny.remove(i);
                    return true;
                }
            }
            for (Pencil p : pencils) {
                if (p.rect().has(mx, my)) {
                    Minecraft.getInstance().setScreen(new TextEditorScreen(this, p.heading(), p.box().getValue(),
                            v -> {
                                p.box().setValue(v);
                                p.box().moveCursorToStart(false);
                            }, accent()));
                    return true;
                }
            }
            if (pickIcon != null && pickIcon.has(mx, my)) {
                Minecraft.getInstance().setScreen(new IconPickerScreen(this, value -> {
                    icon.setValue(value);
                    icon.moveCursorToStart(false);
                }, accent()));
                return true;
            }
            if (addUnlock != null && addUnlock.has(mx, my)) {
                Minecraft.getInstance().setScreen(new UnlockPickerScreen(this, unlock -> {
                    if (!unlocks.contains(unlock)) unlocks.add(unlock);
                }));
                return true;
            }
            if (hiddenBox != null && hiddenBox.has(mx, my)) {
                hidden = !hidden;
                return true;
            }
            if (commitBox != null && commitBox.has(mx, my)) {
                commitment = !commitment;
                return true;
            }
            if (startNode != null && startNode.has(mx, my)) {
                prerequisites.clear();
                prerequisitesAny.clear();
                return true;
            }
            return false;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double delta) {
        scroll = (int) Math.max(0, Math.min(Math.max(0, contentH - (viewBottom() - viewTop())), scroll - delta * 24));
        return true;
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if ((kc == GLFW.GLFW_KEY_ENTER || kc == GLFW.GLFW_KEY_KP_ENTER) && flagInput.isFocused()) {
            commitFlagInput();
            return true;
        }
        if (kc == GLFW.GLFW_KEY_S && (mod & GLFW.GLFW_MOD_CONTROL) != 0) {
            save();
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    private void commitFlagInput() {
        String flag = flagInput.getValue().trim();
        if (flag.isEmpty() || requiresFlags.contains(flag)) return;
        requiresFlags.add(flag);
        flagInput.setValue("");
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save() {
        String id = node.id.toString();
        editor.editTree(tree, json -> {
            JsonObject target = ResearchTreeEdits.find(json, id);
            if (target == null) return;
            Map<ConfluxDataType, Long> cost = new EnumMap<>(ConfluxDataType.class);
            costs.forEach((type, b) -> {
                String v = b.getValue().trim();
                if (!v.isEmpty()) cost.put(type, Long.parseLong(v));
            });
            String t = title.getValue().trim();
            ResearchTreeEdits.setFields(target, t.isEmpty() ? node.id.getPath() : t, lore.getValue(),
                    icon.getValue().trim(), cost, new ArrayList<>(unlocks));
            ResearchTreeEdits.setMeta(target, hint.getValue(), hidden, commitment, exclusion.getValue().trim(),
                    prerequisites, prerequisitesAny);
            commitFlagInput();
            ResearchTreeEdits.setRequirements(target, requiresFlags, milestoneBox.getValue());
            int x = parseInt(gridX.getValue(), node.posX), y = parseInt(gridY.getValue(), node.posY);
            if ((x != node.posX || y != node.posY) && !occupied(json, id, x, y)) {
                ResearchTreeEdits.setPosition(target, x, y);
            }
        });
        onClose();
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean occupied(JsonObject json, String exceptId, int x, int y) {
        for (var e : ResearchTreeEdits.nodes(json)) {
            JsonObject n = e.getAsJsonObject();
            if (n.get("id").getAsString().equals(exceptId)) continue;
            if (ResearchTreeEdits.posX(n) == x && ResearchTreeEdits.posY(n) == y) return true;
        }
        return false;
    }

    private void delete() {
        String id = node.id.toString();
        editor.editTree(tree, json -> ResearchTreeEdits.removeNode(json, id));
        onClose();
    }
}
