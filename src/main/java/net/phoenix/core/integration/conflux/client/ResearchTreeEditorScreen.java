package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchTreeValidator;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class ResearchTreeEditorScreen extends Screen {

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    private static final int PANEL_W = 240;
    private static final int TOOLBAR_H = 24;

    private static final int NODE_W = 64;
    private static final int NODE_H = 48;
    private static final int NODE_R = 5;
    private static final int GRID = 80;

    private static final int C_BG = 0xFF07070F;
    private static final int C_CANVAS = 0xFF09090E;
    private static final int C_PANEL = 0xFF0F0F1C;
    private static final int C_TOOLBAR = 0xFF0C0C18;
    private static final int C_BORDER = 0xFF252538;
    private static final int C_TEXT = 0xFFCCCCEE;
    private static final int C_DIM = 0xFF666688;
    private static final int C_ACCENT = 0xFF5577FF;
    private static final int C_SELECTED = 0xFFFFCC22;
    private static final int C_LINK_SRC = 0xFF22CCFF;
    private static final int C_DELETE = 0xFFCC3333;
    private static final int C_GRID = 0x0AFFFFFF;

    private enum Mode {
        SELECT,
        PLACE,
        LINK
    }

    private Mode mode = Mode.SELECT;

    private float panX, panY, zoom = 1f;
    private boolean panDragging;
    private double lastDragX, lastDragY;

    private final List<EditorNode> nodes = new ArrayList<>();
    private final List<EditorEdge> edges = new ArrayList<>();
    private EditorNode selected = null;
    private EditorNode linkSource = null;
    private EditorNode hovering = null;
    private boolean nodeDragging = false;
    private double nodeDragOffX, nodeDragOffY;
    private int nodeIdCounter = 1;

    private String idNamespace = "phoenixcore";

    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private String lastSnapshot = null;
    private static final int HISTORY_MAX = 100;

    private EditorNode clipboard = null;

    private boolean nodePopup = false;
    private int popX, popY, popH;

    private boolean showIssues = false;
    private boolean showLoad = false;
    private boolean showTreeDialog = false;
    private int overlayScroll = 0;

    private record MenuItem(String label, int color, Runnable action) {}

    private List<MenuItem> menu = null;
    private int menuX, menuY;
    private boolean rightDown = false;
    private double rightDownX, rightDownY;
    private EditorNode rightHit = null;

    private EditBox wGroup, wOrder, wDiscipline, wRequires, wNamespace;

    private String status = "";
    private long statusUntil = 0L;

    private String treeName = "my_tree";
    private String treeTitle = "New Tree";
    private String treeDesc = "";

    private String treeGroup = null;
    private int treeOrder = 0;
    private String treeDiscipline = null;
    private String treeRequires = null;

    private EditBox wTreeName, wTreeTitle, wTreeDesc;
    private EditBox wNodeTitle, wNodeLore, wNodeIcon, wNodeExGroup;
    private final Map<ConfluxDataType, EditBox> wCosts = new EnumMap<>(ConfluxDataType.class);
    private final List<UnlockEntry> unlockEntries = new ArrayList<>();

    private EditBox wUnlockType, wUnlockValue;

    private record UnlockEntry(String type, String value) {}

    private static final int MIN_CANVAS_W = 320;
    private static final int MIN_CANVAS_H = 300;
    private float uiScale = 1f;
    private int vw, vh;

    public ResearchTreeEditorScreen() {
        super(Component.literal("Conflux Tree Editor"));
    }

    @Override
    protected void init() {
        float neededW = PANEL_W + MIN_CANVAS_W;
        float neededH = MIN_CANVAS_H + TOOLBAR_H;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        int px = canvasW() + 8;
        int py = TOOLBAR_H + 8;
        int fw = PANEL_W - 16;

        wTreeName = addBox(px, py, fw, "tree name…");
        wTreeTitle = addBox(px, py + 18, fw, "display title…");
        wTreeDesc = addBox(px, py + 36, fw, "description…");
        wTreeName.setValue(treeName);
        wTreeTitle.setValue(treeTitle);
        wTreeDesc.setValue(treeDesc);

        int ny = py + 72;
        wNodeTitle = addBox(px, ny, fw, "node title…");
        wNodeLore = addBox(px, ny + 18, fw, "lore / flavour text…");
        wNodeIcon = addBox(px, ny + 36, fw, "icon item id…");
        wNodeExGroup = addBox(px, ny + 54, fw, "exclusion group…");

        int cy = ny + 78;
        for (ConfluxDataType type : ConfluxDataType.values()) {
            EditBox b = addBox(px + 90, cy, fw - 90, "0");
            b.setFilter(s -> s.isEmpty() || s.matches("\\d*"));
            wCosts.put(type, b);
            cy += 14;
        }

        int uy = cy + 8;
        wUnlockType = addBox(px, uy, 60, "recipe_tag");
        wUnlockValue = addBox(px + 64, uy, fw - 64, "value…");

        wGroup = addBox(0, 0, 200, "category, such as Discipline/Tier 1…");
        wOrder = addBox(0, 0, 60, "0");
        wOrder.setFilter(v -> v.isEmpty() || v.matches("-?\\d*"));
        wDiscipline = addBox(0, 0, 200, "discipline id (empty = every discipline)…");
        wRequires = addBox(0, 0, 200, "requires discipline (empty = none)…");
        wNamespace = addBox(0, 0, 200, "node id namespace…");
        wGroup.setValue(treeGroup == null ? "" : treeGroup);
        wOrder.setValue(treeOrder == 0 ? "" : String.valueOf(treeOrder));
        wDiscipline.setValue(treeDiscipline == null ? "" : treeDiscipline);
        wRequires.setValue(treeRequires == null ? "" : treeRequires);
        wNamespace.setValue(idNamespace);
        positionDialog();
        setDialogVisible(showTreeDialog);

        refreshNodeWidgets();
        if (lastSnapshot == null) lastSnapshot = buildJson();
    }

    private EditBox addBox(int x, int y, int w, String hint) {
        EditBox b = new EditBox(font, x, y, w, 12, Component.empty());
        b.setHint(Component.literal(hint));
        b.setMaxLength(256);
        addRenderableWidget(b);
        return b;
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        updateTreeMeta();

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        hovering = nodeAt(screenToCanvas(mx, my));

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        g.fill(0, 0, vw, vh, C_BG);

        renderCanvas(g, mx, my);
        renderToolbar(g, mx, my);
        renderPanel(g, mx, my);
        positionPopup();
        if (nodePopup && selected != null) renderPopupBackdrop(g, mx, my);
        if (showTreeDialog) renderDialogBackdrop(g, mx, my);

        super.render(g, mx, my, pt);

        if (showIssues || showLoad) renderOverlay(g, mx, my);
        if (menu != null) renderMenu(g, mx, my);

        g.pose().popPose();
    }

    private void renderCanvas(GuiGraphics g, int mx, int my) {
        int cw = canvasW(), ch = canvasH();
        g.fill(0, TOOLBAR_H, cw, TOOLBAR_H + ch, C_CANVAS);

        g.pose().pushPose();
        g.pose().translate(panX, TOOLBAR_H + panY, 0);
        g.pose().scale(zoom, zoom, 1f);
        drawGrid(g, cw, ch);
        drawEdges(g, mx, my);
        drawNodes(g, mx, my);
        g.pose().popPose();

        g.fill(cw, TOOLBAR_H, cw + 1, vh, C_BORDER);
    }

    private void drawGrid(GuiGraphics g, int cw, int ch) {
        int step = GRID;
        int startX = (int) ((-panX / zoom) / step) * step - step;
        int startY = (int) ((-panY / zoom) / step) * step - step;
        int endX = startX + (int) (cw / zoom) + step * 2;
        int endY = startY + (int) (ch / zoom) + step * 2;
        for (int x = startX; x < endX; x += step) g.fill(x, startY, x + 1, endY, C_GRID);
        for (int y = startY; y < endY; y += step) g.fill(startX, y, endX, y + 1, C_GRID);
    }

    private void drawEdges(GuiGraphics g, int mx, int my) {
        for (EditorEdge e : edges) {
            int x1 = e.from.x * GRID, y1 = e.from.y * GRID;
            int x2 = e.to.x * GRID, y2 = e.to.y * GRID;
            int col = dominantColor(e.from);
            drawBezier(g, x1, y1, x2, y2, col);
        }

        if (mode == Mode.LINK && linkSource != null) {
            float[] c = screenToCanvas(mx, my);
            int x1 = linkSource.x * GRID, y1 = linkSource.y * GRID;
            drawBezier(g, x1, y1, (int) c[0], (int) c[1], 0xAA22CCFF);
        }
    }

    private void drawNodes(GuiGraphics g, int mx, int my) {
        for (EditorNode node : nodes) {
            boolean isSel = node == selected;
            boolean isHov = node == hovering && !isSel;
            boolean isLink = node == linkSource;
            drawNode(g, node, isSel, isHov, isLink);
        }
    }

    private void drawNode(GuiGraphics g, EditorNode node, boolean sel, boolean hov, boolean linkSrc) {
        int cx = node.x * GRID;
        int cy = node.y * GRID;
        int x = cx - NODE_W / 2;
        int y = cy - NODE_H / 2;

        int fill = nodeFill(node);
        int border = sel ? C_SELECTED : linkSrc ? C_LINK_SRC : hov ? C_ACCENT : nodeBorder(node);

        if (sel || hov || linkSrc) {
            for (int i = 4; i >= 1; i--) {
                int alpha = 0x18 * (5 - i);
                int glowC = (alpha << 24) | (border & 0x00FFFFFF);
                drawRoundRect(g, x - i, y - i, NODE_W + i * 2, NODE_H + i * 2, NODE_R, 0, glowC);
            }
        }

        drawRoundRect(g, x, y, NODE_W, NODE_H, NODE_R, fill, border);

        if (!node.icon.isEmpty()) ConfluxIcons.draw(g, node.icon, cx - 8, cy - 12);

        String label = node.title.isEmpty() ? node.id : node.title;
        if (label.length() > 9) label = label.substring(0, 8) + "…";
        g.drawCenteredString(font, label, cx, cy + 6, sel ? C_SELECTED : C_TEXT);

        if (edges.stream().noneMatch(e -> e.to == node)) {
            g.drawString(font, "★", x + 2, y + 2, C_SELECTED, false);
        }

        if (!node.exclusionGroup.isEmpty()) {
            g.drawString(font, "⊗", x + NODE_W - 9, y + 2, 0xFFFF6644, false);
        }
    }

    private void renderToolbar(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, vw, TOOLBAR_H, C_TOOLBAR);
        g.fill(0, TOOLBAR_H - 1, vw, TOOLBAR_H, C_BORDER);

        int x = 6;
        for (String label : toolbarLabels()) {
            if (label.equals("|")) {
                x += 8;
                g.fill(x, 3, x + 1, TOOLBAR_H - 3, C_BORDER);
                x += 9;
                continue;
            }
            boolean active = (label.equals("SELECT") && mode == Mode.SELECT) ||
                    (label.equals("PLACE") && mode == Mode.PLACE) || (label.equals("LINK") && mode == Mode.LINK) ||
                    (label.startsWith("CHECK") && showIssues) || (label.equals("TREE") && showTreeDialog) ||
                    (label.equals("LOAD TREE") && showLoad);
            int bg = switch (label) {
                case "PLACE" -> 0xFF334433;
                case "LINK" -> 0xFF333355;
                case "SAVE FILE" -> 0xFF223322;
                case "LOAD TREE" -> 0xFF332233;
                default -> label.startsWith("CHECK") && ResearchTreeValidator.hasErrors(currentIssues()) ? 0xFF552222 :
                        0xFF333344;
            };
            x = drawToolBtn(g, mx, my, x, label, active, bg);
        }

        String right = System.currentTimeMillis() < statusUntil ? "§a" + status :
                "§7" + nodes.size() + " nodes  " + edges.size() + " edges";
        g.drawString(font, right, vw - font.width(right) - 8, 8, 0xFFFFFF, false);
    }

    private int drawToolBtn(GuiGraphics g, int mx, int my, int x, String label, boolean active, int bg) {
        int w = font.width(label) + 10;
        boolean hov = mx >= x && mx < x + w && my >= 2 && my < TOOLBAR_H - 2;
        g.fill(x, 2, x + w, TOOLBAR_H - 2, active ? C_ACCENT : hov ? bg + 0x101010 : bg);
        if (active) g.fill(x, TOOLBAR_H - 3, x + w, TOOLBAR_H - 2, 0xFFFFFFFF);
        g.drawString(font, active || hov ? "§f" + label : "§7" + label, x + 5, 8, 0xFFFFFF, false);
        return x + w + 4;
    }

    private void renderPanel(GuiGraphics g, int mx, int my) {
        int px = canvasW();
        g.fill(px, 0, vw, vh, C_PANEL);

        int x = px + 8, y = TOOLBAR_H + 8;

        g.drawString(font, "§bTree", x, y - 2, 0xFFFFFF, false);
        y += 10;
        g.drawString(font, "§7ID", x, y + 2, 0xFFFFFF, false);
        y += 14;
        g.drawString(font, "§7Title", x, y + 2, 0xFFFFFF, false);
        y += 14;
        g.drawString(font, "§7Desc", x, y + 2, 0xFFFFFF, false);
        y += 18;

        g.fill(px + 4, y, vw - 4, y + 1, C_BORDER);
        y += 6;

        g.drawString(font, "§7Right-click a node to edit it,", x, y, 0xFFFFFF, false);
        y += 10;
        g.drawString(font, "§7or empty space to add a node.", x, y, 0xFFFFFF, false);
        y += 14;
        g.drawString(font, "§8Drag a node to move it.", x, y, 0xFFFFFF, false);
        y += 10;
        g.drawString(font, "§8Right-drag or Alt-drag to pan.", x, y, 0xFFFFFF, false);
        y += 10;
        g.drawString(font, "§8Scroll to zoom.", x, y, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int btn) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;

        if (menu != null) {
            MenuItem hit = menuItemAt(mx, my);
            menu = null;
            if (hit != null && btn == 0) hit.action().run();
            return true;
        }
        if ((showIssues || showLoad) && overlayClick(mx, my, btn)) return true;
        if (showTreeDialog && dialogClick(mx, my)) return true;
        if (nodePopup && selected != null && inPopup(mx, my)) {
            popupClick(mx, my, btn);
            return true;
        }
        if (super.mouseClicked(mx, my, btn)) return true;

        if (my < TOOLBAR_H) {
            handleToolbarClick((int) mx, (int) my, btn);
            return true;
        }

        if (mx >= canvasW()) {
            handlePanelClick((int) mx, (int) my, btn);
            return true;
        }

        float[] pos = screenToCanvas(mx, my);
        EditorNode hit = nodeAt(pos);

        if (btn == 2 || (btn == 0 && hasAltDown())) {

            panDragging = true;
            lastDragX = mx;
            lastDragY = my;
            return true;
        }
        if (btn == 1) {

            rightDown = true;
            rightDownX = mx;
            rightDownY = my;
            rightHit = hit;
            if (hit == null) {
                panDragging = true;
                lastDragX = mx;
                lastDragY = my;
            }
            return true;
        }

        if (btn == 0) {
            switch (mode) {
                case SELECT -> {
                    if (hit != null) {
                        if (selected != hit) commit();
                        selected = hit;
                        nodeDragging = true;
                        nodeDragOffX = pos[0] - hit.x * GRID;
                        nodeDragOffY = pos[1] - hit.y * GRID;
                        refreshNodeWidgets();
                    } else {
                        commit();
                        selected = null;
                        nodePopup = false;
                        refreshNodeWidgets();
                        panDragging = true;
                        lastDragX = mx;
                        lastDragY = my;
                    }
                }
                case PLACE -> {
                    addNodeAt(pos[0], pos[1]);
                    mode = Mode.SELECT;
                }
                case LINK -> {
                    if (hit != null) {
                        if (linkSource == null) {
                            linkSource = hit;
                        } else if (hit != linkSource) {

                            if (edges.stream().noneMatch(e -> e.from == linkSource && e.to == hit)) {
                                edges.add(new EditorEdge(linkSource, hit));
                                commit();
                            }
                            linkSource = null;
                            mode = Mode.SELECT;
                        }
                    } else {
                        linkSource = null;
                    }
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double rmx, double rmy, int btn) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (btn == 0) {
            if (nodeDragging) commit();
            nodeDragging = false;
            panDragging = false;
        }
        if (btn == 1 && rightDown) {
            rightDown = false;
            panDragging = false;
            if (Math.hypot(mx - rightDownX, my - rightDownY) < 4 && mx < canvasW() && my >= TOOLBAR_H) {
                if (rightHit != null) openNodePopup(rightHit);
                else openMenu(mx, my, null);
            }
        }
        if (btn == 2) panDragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double rmx, double rmy, int btn, double dragX, double dragY) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (panDragging) {
            panX += (float) (mx - lastDragX);
            panY += (float) (my - lastDragY);
            lastDragX = mx;
            lastDragY = my;
            return true;
        }
        if (nodeDragging && selected != null) {
            float[] pos = screenToCanvas(mx, my);
            selected.x = Math.round((float) (pos[0] - nodeDragOffX) / GRID);
            selected.y = Math.round((float) (pos[1] - nodeDragOffY) / GRID);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseScrolled(double rmx, double rmy, double scrollX, double delta) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (showIssues || showLoad) {
            overlayScroll = Math.max(0, overlayScroll - (int) Math.signum(delta));
            return true;
        }
        if (mx < canvasW()) {
            float old = zoom;
            zoom = Math.max(0.3f, Math.min(3f, zoom + (float) (delta * 0.1f)));
            float f = zoom / old;
            panX = (float) (mx - f * (mx - panX));
            panY = (float) ((my - TOOLBAR_H) - f * ((my - TOOLBAR_H) - panY));
            return true;
        }
        return super.mouseScrolled(mx, my, 0.0, delta);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            if (menu != null) {
                menu = null;
            } else if (nodePopup) {
                nodePopup = false;
            } else if (showIssues || showLoad || showTreeDialog) {
                showIssues = false;
                showLoad = false;
                showTreeDialog = false;
                setDialogVisible(false);
            } else if (mode != Mode.SELECT || linkSource != null) {
                mode = Mode.SELECT;
                linkSource = null;
            } else {
                onClose();
            }
            return true;
        }

        if (hasControlDown()) {
            if (kc == GLFW.GLFW_KEY_Z) {
                if (hasShiftDown()) doRedo();
                else doUndo();
                return true;
            }
            if (kc == GLFW.GLFW_KEY_Y) {
                doRedo();
                return true;
            }
            if (kc == GLFW.GLFW_KEY_D && selected != null && !(getFocused() instanceof EditBox)) {
                duplicate(selected);
                return true;
            }
            if (kc == GLFW.GLFW_KEY_C && selected != null && !(getFocused() instanceof EditBox eb && eb.isFocused())) {
                clipboard = copyOf(selected);
                setStatus("Copied " + selected.id);
                return true;
            }
            if (kc == GLFW.GLFW_KEY_V && clipboard != null && !(getFocused() instanceof EditBox eb && eb.isFocused())) {
                paste();
                return true;
            }
            if (kc == GLFW.GLFW_KEY_S) {
                saveFile();
                return true;
            }
        }

        boolean typing = getFocused() instanceof EditBox eb && eb.isFocused();
        if (!typing) {
            if (kc == GLFW.GLFW_KEY_DELETE && selected != null) {
                deleteSelected();
                return true;
            }
            if (!hasControlDown() && kc == GLFW.GLFW_KEY_S) {
                mode = Mode.SELECT;
                return true;
            }
            if (kc == GLFW.GLFW_KEY_P) {
                mode = Mode.PLACE;
                return true;
            }
            if (kc == GLFW.GLFW_KEY_L) {
                mode = Mode.LINK;
                return true;
            }
        }
        return super.keyPressed(kc, sc, mod);
    }

    private List<String> toolbarLabels() {
        List<String> out = new ArrayList<>(List.of("SELECT", "PLACE", "LINK", "|", "UNDO", "REDO", "|"));
        out.add(checkLabel());
        out.addAll(List.of("TREE", "|", "COPY JSON", "SAVE FILE", "LOAD TREE"));
        return out;
    }

    private String checkLabel() {
        List<ResearchTreeValidator.Issue> issues = currentIssues();
        return issues.isEmpty() ? "CHECK ✓" : "CHECK (" + issues.size() + ")";
    }

    private void handleToolbarClick(int mx, int my, int btn) {
        int x = 6;
        for (String label : toolbarLabels()) {
            if (label.equals("|")) {
                x += 17;
                continue;
            }
            if (hitBtn(mx, my, x, label)) {
                toolbarAction(label);
                return;
            }
            x += font.width(label) + 14;
        }
    }

    private void toolbarAction(String label) {
        if (label.startsWith("CHECK")) {
            showIssues = !showIssues;
            showLoad = false;
            overlayScroll = 0;
            return;
        }
        switch (label) {
            case "SELECT" -> {
                mode = Mode.SELECT;
                linkSource = null;
            }
            case "PLACE" -> mode = Mode.PLACE;
            case "LINK" -> {
                mode = Mode.LINK;
                linkSource = null;
            }
            case "UNDO" -> doUndo();
            case "REDO" -> doRedo();
            case "TREE" -> {
                showTreeDialog = !showTreeDialog;
                setDialogVisible(showTreeDialog);
            }
            case "COPY JSON" -> {
                copyJson();
                setStatus("Copied the tree's JSON");
            }
            case "SAVE FILE" -> saveFile();
            case "LOAD TREE" -> {
                showLoad = !showLoad;
                showIssues = false;
                overlayScroll = 0;
            }
            default -> {}
        }
    }

    private boolean hitBtn(int mx, int my, int x, String label) {
        int w = font.width(label) + 10;
        return mx >= x && mx < x + w && my >= 2 && my < TOOLBAR_H - 2;
    }

    private void handlePanelClick(int mx, int my, int btn) {}

    private static final int POP_W = 236;
    private static final String[] POP_BUTTONS = { "Duplicate", "Copy", "Link from", "Unlink all", "Start node",
            "Delete" };

    private void openNodePopup(EditorNode node) {
        if (selected != node) commit();
        selected = node;
        menu = null;
        refreshNodeWidgets();
        nodePopup = true;
    }

    private List<ConfluxDataType> availableTypes() {
        List<ConfluxDataType> out = new ArrayList<>();
        for (ConfluxDataType t : ConfluxDataType.values()) {
            if (t.isAvailable()) out.add(t);
        }
        return out;
    }

    private int popupHeight() {
        return 20 + 4 * 16 + 6 + 12 + availableTypes().size() * 14 + 6 + 12 + unlockEntries.size() * 10 + 18 + 6 +
                2 * 16 + 6;
    }

    private EditBox[] popupBoxes() {
        return new EditBox[] { wNodeTitle, wNodeLore, wNodeIcon, wNodeExGroup, wUnlockType, wUnlockValue };
    }

    private boolean inPopup(double mx, double my) {
        return mx >= popX && mx < popX + POP_W && my >= popY && my < popY + popH;
    }

    private int popFieldY(int i) {
        return popY + 20 + i * 16;
    }

    private int popCostY(int i) {
        return popFieldY(4) + 6 + 12 + i * 14;
    }

    private int popUnlockY() {
        return popCostY(availableTypes().size()) + 6 + 12;
    }

    private int popAddY() {
        return popUnlockY() + unlockEntries.size() * 10 + 2;
    }

    private int popButtonsY() {
        return popAddY() + 18 + 6;
    }

    private void positionPopup() {
        boolean show = nodePopup && selected != null;
        for (EditBox b : popupBoxes()) {
            if (b == null) continue;
            b.visible = show;
            b.active = show;
            if (!show) b.setFocused(false);
        }
        for (EditBox b : wCosts.values()) {
            b.visible = false;
            b.active = false;
        }
        if (!show) return;

        popH = popupHeight();
        float sx = panX + selected.x * GRID * zoom;
        float sy = TOOLBAR_H + panY + selected.y * GRID * zoom;
        int half = Math.round(NODE_W / 2f * zoom);
        int x = (int) (sx + half + 10);
        if (x + POP_W > canvasW() - 4) x = (int) (sx - half - 10 - POP_W);
        popX = Math.max(4, Math.min(x, canvasW() - POP_W - 4));
        popY = (int) Math.max(TOOLBAR_H + 4, Math.min(sy - 24, vh - popH - 4));

        int bx = popX + 50, bw = POP_W - 58;
        EditBox[] fields = { wNodeTitle, wNodeLore, wNodeIcon, wNodeExGroup };
        for (int i = 0; i < fields.length; i++) {
            fields[i].setX(bx);
            fields[i].setY(popFieldY(i));
            fields[i].setWidth(bw);
        }
        List<ConfluxDataType> types = availableTypes();
        for (int i = 0; i < types.size(); i++) {
            EditBox b = wCosts.get(types.get(i));
            b.visible = true;
            b.active = true;
            b.setX(popX + 90);
            b.setY(popCostY(i));
            b.setWidth(POP_W - 98);
        }
        wUnlockType.setX(popX + 8);
        wUnlockType.setY(popAddY());
        wUnlockType.setWidth(60);
        wUnlockValue.setX(popX + 72);
        wUnlockValue.setY(popAddY());
        wUnlockValue.setWidth(POP_W - 72 - 52);
    }

    private void renderPopupBackdrop(GuiGraphics g, int mx, int my) {
        g.fill(popX - 1, popY - 1, popX + POP_W + 1, popY + popH + 1, C_SELECTED);
        g.fill(popX, popY, popX + POP_W, popY + popH, 0xFF0F0F1C);
        g.drawString(font, "§eNode §f" + selected.id, popX + 8, popY + 6, 0xFFFFFF, false);
        boolean closeHov = mx >= popX + POP_W - 14 && mx < popX + POP_W - 4 && my >= popY + 4 && my < popY + 15;
        g.drawString(font, closeHov ? "§c×" : "§8×", popX + POP_W - 11, popY + 6, 0xFFFFFF, false);

        String[] labels = { "Title", "Lore", "Icon", "Group" };
        for (int i = 0; i < labels.length; i++) {
            g.drawString(font, "§7" + labels[i], popX + 8, popFieldY(i) + 2, 0xFFFFFF, false);
        }
        g.fill(popX + 4, popFieldY(4) + 1, popX + POP_W - 4, popFieldY(4) + 2, C_BORDER);
        g.drawString(font, "§7Cost", popX + 8, popFieldY(4) + 6, 0xFFFFFF, false);
        List<ConfluxDataType> types = availableTypes();
        for (int i = 0; i < types.size(); i++) {
            g.drawString(font, types.get(i).displayComponent(), popX + 8, popCostY(i) + 2, 0xFFFFFF, false);
        }
        int uy = popUnlockY();
        g.fill(popX + 4, uy - 14, popX + POP_W - 4, uy - 13, C_BORDER);
        g.drawString(font, "§7Unlocks", popX + 8, uy - 11, 0xFFFFFF, false);
        for (int i = 0; i < unlockEntries.size(); i++) {
            UnlockEntry ue = unlockEntries.get(i);
            int y = uy + i * 10;
            g.drawString(font, "§b" + ue.type() + " §f" + ue.value(), popX + 8, y, 0xFFFFFF, false);
            boolean hov = mx >= popX + POP_W - 16 && mx < popX + POP_W - 4 && my >= y && my < y + 9;
            g.drawString(font, hov ? "§c[×]" : "§8[×]", popX + POP_W - 16, y, 0xFFFFFF, false);
        }
        int ay = popAddY();
        boolean addHov = mx >= popX + POP_W - 44 && mx < popX + POP_W - 6 && my >= ay && my < ay + 12;
        g.fill(popX + POP_W - 44, ay, popX + POP_W - 6, ay + 12, addHov ? 0xFF224422 : 0xFF112211);
        g.renderOutline(popX + POP_W - 44, ay, 38, 12, 0xFF336633);
        g.drawCenteredString(font, "§aAdd", popX + POP_W - 25, ay + 2, 0xFFFFFF);

        int by = popButtonsY();
        int bw = (POP_W - 16 - 8) / 3;
        for (int i = 0; i < POP_BUTTONS.length; i++) {
            int bx = popX + 8 + (i % 3) * (bw + 4);
            int y = by + (i / 3) * 16;
            boolean hov = mx >= bx && mx < bx + bw && my >= y && my < y + 14;
            boolean delete = i == POP_BUTTONS.length - 1;
            g.fill(bx, y, bx + bw, y + 14, delete ? (hov ? 0xFF551111 : 0xFF330A0A) : (hov ? 0xFF22223A : 0xFF14142A));
            g.renderOutline(bx, y, bw, 14, delete ? C_DELETE : C_BORDER);
            g.drawCenteredString(font, (delete ? "\u00a7c" : "\u00a7f") + POP_BUTTONS[i], bx + bw / 2, y + 3,
                    0xFFFFFF);
        }
    }

    private void popupClick(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return;
        if (btn != 0) return;

        if (mx >= popX + POP_W - 14 && mx < popX + POP_W - 4 && my >= popY + 4 && my < popY + 15) {
            commit();
            nodePopup = false;
            return;
        }
        int uy = popUnlockY();
        for (int i = 0; i < unlockEntries.size(); i++) {
            int y = uy + i * 10;
            if (mx >= popX + POP_W - 16 && mx < popX + POP_W - 4 && my >= y && my < y + 9) {
                unlockEntries.remove(i);
                syncNodeUnlocks();
                commit();
                return;
            }
        }
        int ay = popAddY();
        if (mx >= popX + POP_W - 44 && mx < popX + POP_W - 6 && my >= ay && my < ay + 12) {
            addUnlock();
            return;
        }
        int by = popButtonsY();
        int bw = (POP_W - 16 - 8) / 3;
        for (int i = 0; i < POP_BUTTONS.length; i++) {
            int bx = popX + 8 + (i % 3) * (bw + 4);
            int y = by + (i / 3) * 16;
            if (mx >= bx && mx < bx + bw && my >= y && my < y + 14) {
                popupAction(i);
                return;
            }
        }
    }

    private void popupAction(int button) {
        EditorNode node = selected;
        if (node == null) return;
        switch (button) {
            case 0 -> duplicate(node);
            case 1 -> {
                clipboard = copyOf(node);
                setStatus("Copied " + node.id);
            }
            case 2 -> {
                mode = Mode.LINK;
                linkSource = node;
                nodePopup = false;
            }
            case 3 -> unlinkAll(node);
            case 4 -> makeRoot(node);
            case 5 -> deleteSelected();
            default -> {}
        }
    }

    private void deleteSelected() {
        if (selected == null) return;
        commit();
        nodes.remove(selected);
        edges.removeIf(e -> e.from == selected || e.to == selected);
        selected = null;
        nodePopup = false;
        refreshNodeWidgets();
        commit();
    }

    private void addUnlock() {
        if (wUnlockType == null || wUnlockValue == null) return;
        String type = wUnlockType.getValue().trim();
        String value = wUnlockValue.getValue().trim();
        if (type.isEmpty() || value.isEmpty()) return;
        unlockEntries.add(new UnlockEntry(type, value));
        wUnlockType.setValue("");
        wUnlockValue.setValue("");
        syncNodeUnlocks();
        commit();
    }

    private void syncNodeUnlocks() {
        if (selected == null) return;
        selected.unlocks.clear();
        unlockEntries.forEach(u -> selected.unlocks.add(u));
    }

    private void setStatus(String text) {
        status = text;
        statusUntil = System.currentTimeMillis() + 2500L;
    }

    private void commit() {
        updateTreeMeta();
        String now = buildJson();
        if (lastSnapshot == null) {
            lastSnapshot = now;
            return;
        }
        if (now.equals(lastSnapshot)) return;
        undoStack.push(lastSnapshot);
        while (undoStack.size() > HISTORY_MAX) undoStack.removeLast();
        redoStack.clear();
        lastSnapshot = now;
    }

    private void doUndo() {
        commit();
        if (undoStack.isEmpty()) {
            setStatus("Nothing to undo");
            return;
        }
        redoStack.push(lastSnapshot);
        lastSnapshot = undoStack.pop();
        restore(lastSnapshot);
    }

    private void doRedo() {
        commit();
        if (redoStack.isEmpty()) {
            setStatus("Nothing to redo");
            return;
        }
        undoStack.push(lastSnapshot);
        lastSnapshot = redoStack.pop();
        restore(lastSnapshot);
    }

    private void restore(String json) {
        ResearchTree tree = ResearchTree.fromJson(ResourceLocation.fromNamespaceAndPath(idNamespace, treeName),
                JsonParser.parseString(json).getAsJsonObject());
        String keep = treeName;
        loadTree(tree);
        treeName = keep;
        wTreeName.setValue(treeName);
    }

    private static String slug(String text) {
        String s = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    private String uniqueId(String base, EditorNode except) {
        String id = base;
        int n = 2;
        while (true) {
            final String candidate = id;
            if (nodes.stream().noneMatch(o -> o != except && o.id.equals(candidate))) return id;
            id = base + "_" + n++;
        }
    }

    private EditorNode addNodeAt(float canvasX, float canvasY) {
        commit();
        int gx = Math.round(canvasX / GRID);
        int gy = Math.round(canvasY / GRID);
        EditorNode n = new EditorNode(uniqueId("node_" + nodeIdCounter++, null), gx, gy);
        n.autoId = true;
        nodes.add(n);
        selected = n;
        refreshNodeWidgets();
        commit();
        return n;
    }

    private boolean occupied(int gx, int gy) {
        for (EditorNode n : nodes) if (n.x == gx && n.y == gy) return true;
        return false;
    }

    private EditorNode copyOf(EditorNode n) {
        EditorNode c = new EditorNode(n.id, n.x, n.y);
        c.title = n.title;
        c.lore = n.lore;
        c.icon = n.icon;
        c.exclusionGroup = n.exclusionGroup;
        c.cost.putAll(n.cost);
        c.unlocks.addAll(n.unlocks);
        c.autoId = true;
        return c;
    }

    private void placeCopy(EditorNode source, int gx, int gy) {
        commit();
        while (occupied(gx, gy)) gx++;
        EditorNode c = copyOf(source);
        c.x = gx;
        c.y = gy;
        String base = c.title.isBlank() ? "node" : slug(c.title);
        c.id = uniqueId(base.isEmpty() ? "node" : base, null);
        nodes.add(c);
        selected = c;
        refreshNodeWidgets();
        commit();
    }

    private void duplicate(EditorNode n) {
        placeCopy(n, n.x, n.y + 1);
    }

    private void paste() {
        if (clipboard == null) return;
        placeCopy(clipboard, clipboard.x + 1, clipboard.y + 1);
    }

    private void unlinkAll(EditorNode n) {
        commit();
        edges.removeIf(e -> e.from == n || e.to == n);
        commit();
    }

    private void makeRoot(EditorNode n) {
        commit();
        edges.removeIf(e -> e.to == n);
        commit();
    }

    private void centreOn(EditorNode n) {
        panX = canvasW() / 2f - n.x * GRID * zoom;
        panY = canvasH() / 2f - n.y * GRID * zoom;
    }

    private void openMenu(double x, double y, EditorNode hit) {
        List<MenuItem> items = new ArrayList<>();
        float[] pos = screenToCanvas(x, y);
        if (hit == null) {
            items.add(new MenuItem("New node here", 0xFF88EE88, () -> addNodeAt(pos[0], pos[1])));
            if (clipboard != null) {
                items.add(new MenuItem("Paste node here", 0xFFCCCCEE, () -> {
                    placeCopy(clipboard, Math.round(pos[0] / GRID), Math.round(pos[1] / GRID));
                }));
            }
            items.add(new MenuItem("Check the tree", 0xFFCCCCEE, () -> {
                showIssues = true;
                showLoad = false;
                overlayScroll = 0;
            }));
            items.add(new MenuItem("Tree settings…", 0xFFCCCCEE, () -> {
                showTreeDialog = true;
                setDialogVisible(true);
            }));
        } else {
            items.add(new MenuItem("Select", 0xFFCCCCEE, () -> {
                selected = hit;
                refreshNodeWidgets();
            }));
            items.add(new MenuItem("Duplicate  (Ctrl+D)", 0xFFCCCCEE, () -> duplicate(hit)));
            items.add(new MenuItem("Copy  (Ctrl+C)", 0xFFCCCCEE, () -> {
                clipboard = copyOf(hit);
                setStatus("Copied " + hit.id);
            }));
            items.add(new MenuItem("Link from here", 0xFF77CCFF, () -> {
                mode = Mode.LINK;
                linkSource = hit;
            }));
            if (selected != null && selected != hit) {
                boolean linked = edges.stream().anyMatch(e -> (e.from == selected && e.to == hit) ||
                        (e.from == hit && e.to == selected));
                if (linked) {
                    items.add(new MenuItem("Unlink from " + selected.id, 0xFFFFAA55, () -> {
                        commit();
                        edges.removeIf(e -> (e.from == selected && e.to == hit) || (e.from == hit && e.to == selected));
                        commit();
                    }));
                }
            }
            items.add(new MenuItem("Remove all links", 0xFFFFAA55, () -> unlinkAll(hit)));
            items.add(new MenuItem("Make a starting node", 0xFFFFAA55, () -> makeRoot(hit)));
            items.add(new MenuItem("Delete", 0xFFFF6666, () -> {
                selected = hit;
                deleteSelected();
            }));
        }
        menu = items;
        int w = menuWidth();
        menuX = (int) Math.min(x, canvasW() - w - 2);
        menuY = (int) Math.min(y, vh - items.size() * MENU_ROW - 4);
    }

    private static final int MENU_ROW = 14;

    private int menuWidth() {
        int w = 90;
        for (MenuItem i : menu) w = Math.max(w, font.width(i.label()) + 16);
        return w;
    }

    private MenuItem menuItemAt(double mx, double my) {
        if (menu == null) return null;
        int w = menuWidth();
        if (mx < menuX || mx >= menuX + w || my < menuY) return null;
        int i = (int) ((my - menuY) / MENU_ROW);
        return i >= 0 && i < menu.size() ? menu.get(i) : null;
    }

    private void renderMenu(GuiGraphics g, int mx, int my) {
        int w = menuWidth();
        int h = menu.size() * MENU_ROW;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(menuX - 1, menuY - 1, menuX + w + 1, menuY + h + 1, C_BORDER);
        g.fill(menuX, menuY, menuX + w, menuY + h, 0xFF11111F);
        for (int i = 0; i < menu.size(); i++) {
            int y = menuY + i * MENU_ROW;
            boolean hov = mx >= menuX && mx < menuX + w && my >= y && my < y + MENU_ROW;
            if (hov) g.fill(menuX, y, menuX + w, y + MENU_ROW, 0xFF22223A);
            g.drawString(font, menu.get(i).label(), menuX + 8, y + 3, menu.get(i).color(), false);
        }
        g.pose().popPose();
    }

    private List<ResearchTreeValidator.Issue> currentIssues() {
        List<ResearchTreeValidator.Node> in = new ArrayList<>();
        for (EditorNode n : nodes) {
            List<String> prereqs = new ArrayList<>();
            edges.stream().filter(e -> e.to == n).forEach(e -> prereqs.add(e.from.id));
            List<String> types = new ArrayList<>();
            n.unlocks.forEach(u -> types.add(u.type()));
            in.add(new ResearchTreeValidator.Node(n.id, n.title, n.x, n.y, prereqs, n.exclusionGroup,
                    n.cost.values().stream().anyMatch(v -> v > 0), types));
        }
        return ResearchTreeValidator.check(in);
    }

    private record Row(String label, int color, Runnable action) {}

    private static final int OVERLAY_W = 300;
    private static final int OVERLAY_ROW = 12;

    private List<Row> overlayRows() {
        List<Row> rows = new ArrayList<>();
        if (showIssues) {
            List<ResearchTreeValidator.Issue> issues = currentIssues();
            if (issues.isEmpty()) rows.add(new Row("No problems found.", 0xFF77DD77, null));
            for (ResearchTreeValidator.Issue i : issues) {
                boolean error = i.severity() == ResearchTreeValidator.Severity.ERROR;
                String who = i.nodeId() == null ? "" : i.nodeId() + ": ";
                String label = (error ? "✖ " : "! ") + who + i.message();
                EditorNode target = i.nodeId() == null ? null :
                        nodes.stream().filter(n -> n.id.equals(i.nodeId())).findFirst().orElse(null);
                rows.add(new Row(label, error ? 0xFFFF7777 : 0xFFFFCC66, target == null ? null : () -> {
                    selected = target;
                    refreshNodeWidgets();
                    centreOn(target);
                }));
            }
        } else if (showLoad) {
            for (ResearchTree tree : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
                rows.add(new Row(tree.title + "  §8" + tree.id, 0xFFCCCCEE, () -> {
                    commit();
                    loadTree(tree);
                    commit();
                    showLoad = false;
                    setStatus("Loaded " + tree.id);
                }));
            }
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("conflux_trees");
            if (Files.isDirectory(dir)) {
                try (var files = Files.list(dir)) {
                    files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().forEach(f -> {
                        String name = f.getFileName().toString().replace(".json", "");
                        rows.add(new Row("§bfile: §f" + name, 0xFFCCCCEE, () -> {
                            try {
                                commit();
                                JsonObject obj = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                                loadTree(ResearchTree.fromJson(ResourceLocation.fromNamespaceAndPath(idNamespace, name),
                                        obj));
                                commit();
                                showLoad = false;
                                setStatus("Loaded " + name + ".json");
                            } catch (Exception e) {
                                setStatus("Could not read " + name + ".json");
                            }
                        }));
                    });
                } catch (Exception ignored) {}
            }
            if (rows.isEmpty()) rows.add(new Row("No trees to load.", 0xFF888899, null));
        }
        return rows;
    }

    private int overlayHeight(int rows) {
        return Math.min(rows, (vh - TOOLBAR_H - 40) / OVERLAY_ROW) * OVERLAY_ROW + 22;
    }

    private void renderOverlay(GuiGraphics g, int mx, int my) {
        List<Row> rows = overlayRows();
        int x = 8, y = TOOLBAR_H + 8;
        int h = overlayHeight(rows.size());
        int visible = (h - 22) / OVERLAY_ROW;
        overlayScroll = Math.max(0, Math.min(overlayScroll, Math.max(0, rows.size() - visible)));
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(x - 1, y - 1, x + OVERLAY_W + 1, y + h + 1, C_BORDER);
        g.fill(x, y, x + OVERLAY_W, y + h, 0xFF0F0F1C);
        g.drawString(font, showIssues ? "§bTree check  §8(click a problem to go to it, Esc closes)" :
                "§bLoad a tree  §8(Esc closes)", x + 6, y + 5, 0xFFFFFF, false);
        for (int i = 0; i < visible && i + overlayScroll < rows.size(); i++) {
            Row row = rows.get(i + overlayScroll);
            int ry = y + 18 + i * OVERLAY_ROW;
            boolean hov = row.action() != null && mx >= x && mx < x + OVERLAY_W && my >= ry && my < ry + OVERLAY_ROW;
            if (hov) g.fill(x + 1, ry, x + OVERLAY_W - 1, ry + OVERLAY_ROW, 0xFF22223A);
            g.drawString(font, font.plainSubstrByWidth(row.label(), OVERLAY_W - 14), x + 6, ry + 2, row.color(), false);
        }
        g.pose().popPose();
    }

    private boolean overlayClick(double mx, double my, int btn) {
        List<Row> rows = overlayRows();
        int x = 8, y = TOOLBAR_H + 8;
        int h = overlayHeight(rows.size());
        if (mx < x || mx >= x + OVERLAY_W || my < y || my >= y + h) return false;
        if (btn == 0) {
            int i = (int) ((my - y - 18) / OVERLAY_ROW) + overlayScroll;
            if (my >= y + 18 && i >= 0 && i < rows.size() && rows.get(i).action() != null) rows.get(i).action().run();
        }
        return true;
    }

    private static final int DIALOG_W = 280;
    private static final int DIALOG_H = 136;

    private int dialogX() {
        return Math.max(8, (canvasW() - DIALOG_W) / 2);
    }

    private int dialogY() {
        return TOOLBAR_H + 40;
    }

    private void positionDialog() {
        int x = dialogX() + 74, y = dialogY() + 24;
        wNamespace.setX(x);
        wNamespace.setY(y);
        wGroup.setX(x);
        wGroup.setY(y + 18);
        wOrder.setX(x);
        wOrder.setY(y + 36);
        wDiscipline.setX(x);
        wDiscipline.setY(y + 54);
        wRequires.setX(x);
        wRequires.setY(y + 72);
        wGroup.setWidth(DIALOG_W - 84);
        wDiscipline.setWidth(DIALOG_W - 84);
        wRequires.setWidth(DIALOG_W - 84);
        wNamespace.setWidth(DIALOG_W - 84);
    }

    private void setDialogVisible(boolean visible) {
        for (EditBox b : new EditBox[] { wGroup, wOrder, wDiscipline, wRequires, wNamespace }) {
            if (b == null) continue;
            b.visible = visible;
            b.active = visible;
            if (!visible) b.setFocused(false);
        }
    }

    private void renderDialogBackdrop(GuiGraphics g, int mx, int my) {
        int x = dialogX(), y = dialogY();
        g.fill(x - 1, y - 1, x + DIALOG_W + 1, y + DIALOG_H + 1, C_BORDER);
        g.fill(x, y, x + DIALOG_W, y + DIALOG_H, 0xFF0F0F1C);
        g.drawString(font, "§bTree settings", x + 8, y + 6, 0xFFFFFF, false);
        String[] labels = { "Namespace", "Category", "Order", "Discipline", "Requires" };
        for (int i = 0; i < labels.length; i++)
            g.drawString(font, "§7" + labels[i], x + 8, y + 27 + i * 18, 0xFFFFFF, false);
        g.drawString(font, "§8A category path nests with / : Discipline/Tier 1", x + 8, y + 24 + 5 * 18, 0xFFFFFF,
                false);
        g.drawString(font, "§8Order sorts trees (and tiers) inside a category.", x + 8, y + 24 + 5 * 18 + 10, 0xFFFFFF,
                false);
        boolean hov = mx >= x + DIALOG_W - 50 && mx < x + DIALOG_W - 6 && my >= y + 4 && my < y + 16;
        g.fill(x + DIALOG_W - 50, y + 4, x + DIALOG_W - 6, y + 16, hov ? 0xFF331111 : 0xFF221111);
        g.drawCenteredString(font, "§cClose", x + DIALOG_W - 28, y + 6, 0xFFFFFF);
    }

    private boolean dialogClick(double mx, double my) {
        int x = dialogX(), y = dialogY();
        if (mx < x || mx >= x + DIALOG_W || my < y || my >= y + DIALOG_H) return false;
        if (mx >= x + DIALOG_W - 50 && mx < x + DIALOG_W - 6 && my >= y + 4 && my < y + 16) {
            commit();
            showTreeDialog = false;
            setDialogVisible(false);
            return true;
        }

        return super.mouseClicked(mx, my, 0) || true;
    }

    private void saveFile() {
        try {
            updateTreeMeta();
            Path game = Minecraft.getInstance().gameDirectory.toPath();
            Path dir = game.resolve("conflux_trees");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(treeName + ".json"), buildJson());

            Path parent = game.getParent();
            Path project = parent == null ? null :
                    parent.resolve("src/main/resources/data").resolve(idNamespace).resolve("conflux/research");
            if (project != null && Files.isDirectory(project)) {
                Files.writeString(project.resolve(treeName + ".json"), buildJson());
                setStatus("Saved to conflux_trees and the project");
            } else {
                setStatus("Saved conflux_trees/" + treeName + ".json");
            }
        } catch (Exception e) {
            setStatus("Could not save: " + e.getMessage());
        }
    }

    private void loadTree(ResearchTree tree) {
        treeName = tree.id.getPath();
        treeTitle = tree.title;
        treeDesc = tree.description;
        treeGroup = tree.group;
        treeOrder = tree.order;
        treeDiscipline = tree.discipline;
        treeRequires = tree.requiresDiscipline;
        idNamespace = tree.getNodes().stream().findFirst().map(n -> n.id.getNamespace()).orElse(idNamespace);
        nodes.clear();
        edges.clear();
        selected = null;
        linkSource = null;
        Map<String, EditorNode> byId = new HashMap<>();
        tree.getNodes().forEach(n -> {
            EditorNode en = new EditorNode(n.id.getPath(), n.posX, n.posY);
            en.title = n.title;
            en.lore = n.lore;
            en.icon = n.icon;
            en.exclusionGroup = n.exclusionGroup == null ? "" : n.exclusionGroup;
            en.cost.putAll(n.cost);
            n.unlocks.forEach(u -> en.unlocks.add(new UnlockEntry(u.type(), u.value())));
            nodes.add(en);
            byId.put(n.id.toString(), en);
        });
        tree.getNodes().forEach(n -> n.prerequisites.forEach(pid -> {
            EditorNode from = byId.get(pid.toString());
            EditorNode to = byId.get(n.id.toString());
            if (from != null && to != null) edges.add(new EditorEdge(from, to));
        }));
        wTreeName.setValue(treeName);
        wTreeTitle.setValue(treeTitle);
        wTreeDesc.setValue(treeDesc);
        wGroup.setValue(treeGroup == null ? "" : treeGroup);
        wOrder.setValue(treeOrder == 0 ? "" : String.valueOf(treeOrder));
        wDiscipline.setValue(treeDiscipline == null ? "" : treeDiscipline);
        wRequires.setValue(treeRequires == null ? "" : treeRequires);
        wNamespace.setValue(idNamespace);
        refreshNodeWidgets();
    }

    private void refreshNodeWidgets() {
        boolean has = selected != null;
        wNodeTitle.setValue(has ? selected.title : "");
        wNodeLore.setValue(has ? selected.lore : "");
        wNodeIcon.setValue(has ? selected.icon : "");
        wNodeExGroup.setValue(has ? selected.exclusionGroup : "");
        for (ConfluxDataType type : ConfluxDataType.values()) {
            long v = has ? selected.cost.getOrDefault(type, 0L) : 0L;
            wCosts.get(type).setValue(v == 0 ? "" : String.valueOf(v));
        }
        unlockEntries.clear();
        if (has) unlockEntries.addAll(selected.unlocks);

        wNodeTitle.setEditable(has);
        wNodeLore.setEditable(has);
        wNodeIcon.setEditable(has);
        wNodeExGroup.setEditable(has);
        wCosts.values().forEach(b -> b.setEditable(has));
    }

    private void updateTreeMeta() {
        treeName = wTreeName.getValue().trim().isEmpty() ? treeName : wTreeName.getValue().trim();
        treeTitle = wTreeTitle.getValue().trim().isEmpty() ? treeTitle : wTreeTitle.getValue().trim();
        treeDesc = wTreeDesc.getValue();
        if (wGroup != null) {
            String group = wGroup.getValue().trim();
            treeGroup = group.isEmpty() ? null : group;
            try {
                treeOrder = wOrder.getValue().isBlank() || wOrder.getValue().equals("-") ? 0 :
                        Integer.parseInt(wOrder.getValue());
            } catch (NumberFormatException ignored) {}
            String disc = wDiscipline.getValue().trim();
            treeDiscipline = disc.isEmpty() ? null : disc;
            String req = wRequires.getValue().trim();
            treeRequires = req.isEmpty() ? null : req;
            String ns = wNamespace.getValue().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "");
            if (!ns.isEmpty()) idNamespace = ns;
        }
        if (selected != null) {
            selected.title = wNodeTitle.getValue();
            if (selected.autoId && !selected.title.isBlank() && !slug(selected.title).isEmpty()) {
                selected.id = uniqueId(slug(selected.title), selected);
            }
            selected.lore = wNodeLore.getValue();
            selected.icon = wNodeIcon.getValue().trim();
            selected.exclusionGroup = wNodeExGroup.getValue().trim();
            for (ConfluxDataType type : ConfluxDataType.values()) {
                String raw = wCosts.get(type).getValue().trim();
                if (raw.isEmpty()) selected.cost.remove(type);
                else try {
                    selected.cost.put(type, Long.parseLong(raw));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private String buildJson() {
        JsonObject root = new JsonObject();
        root.addProperty("title", treeTitle);
        root.addProperty("description", treeDesc);
        if (treeGroup != null) root.addProperty("group", treeGroup);
        if (treeOrder != 0) root.addProperty("order", treeOrder);
        if (treeDiscipline != null) root.addProperty("discipline", treeDiscipline);
        if (treeRequires != null) root.addProperty("requires_discipline", treeRequires);
        JsonArray nodesArr = new JsonArray();
        for (EditorNode n : nodes) {
            JsonObject nj = new JsonObject();
            nj.addProperty("id", idNamespace + ":" + n.id);
            nj.addProperty("title", n.title.isEmpty() ? n.id : n.title);
            if (!n.lore.isEmpty()) nj.addProperty("lore", n.lore);
            if (!n.icon.isEmpty()) nj.addProperty("icon", n.icon);
            if (!n.exclusionGroup.isEmpty()) nj.addProperty("exclusion_group", n.exclusionGroup);
            JsonArray pos = new JsonArray();
            pos.add(n.x);
            pos.add(n.y);
            nj.add("position", pos);

            JsonArray prereqs = new JsonArray();
            edges.stream().filter(e -> e.to == n)
                    .forEach(e -> prereqs.add(idNamespace + ":" + e.from.id));
            if (prereqs.size() > 0) nj.add("prerequisites", prereqs);

            if (!n.cost.isEmpty()) {
                JsonObject cost = new JsonObject();
                n.cost.forEach((t, v) -> cost.addProperty(t.id(), v));
                nj.add("cost", cost);
            }

            if (!n.unlocks.isEmpty()) {
                JsonArray ul = new JsonArray();
                n.unlocks.forEach(u -> {
                    JsonObject uj = new JsonObject();
                    uj.addProperty("type", u.type());
                    uj.addProperty("value", u.value());
                    ul.add(uj);
                });
                nj.add("unlocks", ul);
            }
            nodesArr.add(nj);
        }
        root.add("nodes", nodesArr);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private void copyJson() {
        Minecraft.getInstance().keyboardHandler.setClipboard(buildJson());
    }

    private void drawRoundRect(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border) {
        if (fill != 0) {
            g.fill(x + r, y, x + w - r, y + h, fill);
            g.fill(x, y + r, x + r, y + h - r, fill);
            g.fill(x + w - r, y + r, x + w, y + h - r, fill);

            g.fill(x + r - 1, y + 1, x + r, y + r, fill);
            g.fill(x + w - r, y + 1, x + w - r + 1, y + r, fill);
            g.fill(x + r - 1, y + h - r, x + r, y + h - 1, fill);
            g.fill(x + w - r, y + h - r, x + w - r + 1, y + h - 1, fill);
        }
        if (border != 0) {
            g.fill(x + r, y, x + w - r, y + 1, border);
            g.fill(x + r, y + h - 1, x + w - r, y + h, border);
            g.fill(x, y + r, x + 1, y + h - r, border);
            g.fill(x + w - 1, y + r, x + w, y + h - r, border);
        }
    }

    private void drawBezier(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
        float cx1 = x1 + (x2 - x1) * 0.4f;
        float cx2 = x2 - (x2 - x1) * 0.4f;
        float px = x1, py = y1;
        int steps = 20;
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            float nt = 1 - t;
            float nx = nt * nt * nt * x1 + 3 * nt * nt * t * cx1 + 3 * nt * t * t * cx2 + t * t * t * x2;
            float ny = nt * nt * nt * y1 + 3 * nt * nt * t * y1 + 3 * nt * t * t * y2 + t * t * t * y2;
            g.fill((int) Math.min(px, nx), (int) Math.min(py, ny),
                    (int) Math.max(px, nx) + 1, (int) Math.max(py, ny) + 1, color);
            px = nx;
            py = ny;
        }
    }

    private float[] screenToCanvas(double sx, double sy) {
        return new float[] { (float) ((sx - panX) / zoom), (float) ((sy - TOOLBAR_H - panY) / zoom) };
    }

    private EditorNode nodeAt(float[] pos) {
        float px = pos[0], py = pos[1];
        for (EditorNode n : nodes) {
            float nx = n.x * GRID, ny = n.y * GRID;
            if (Math.abs(px - nx) <= NODE_W / 2f && Math.abs(py - ny) <= NODE_H / 2f) return n;
        }
        return null;
    }

    private int canvasW() {
        return vw - PANEL_W;
    }

    private int canvasH() {
        return vh - TOOLBAR_H;
    }

    private int dominantColor(EditorNode n) {
        return n.cost.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> typeRGB(e.getKey())).orElse(C_BORDER);
    }

    private int nodeFill(EditorNode n) {
        int base = dominantColor(n);
        return blendTowards(base, C_CANVAS, 0.75f);
    }

    private int nodeBorder(EditorNode n) {
        return dominantColor(n);
    }

    private int typeRGB(ConfluxDataType t) {
        return switch (t) {
            case MATERIAL -> 0xFFCC8800;
            case BIOLOGICAL -> 0xFF22AA44;
            case ENERGETIC -> 0xFF2299CC;
            case COMPUTATIONAL -> 0xFF9933CC;
            case ARCANE -> 0xFF880099;
        };
    }

    private int blendTowards(int color, int target, float factor) {
        int ar = (color >> 16) & 0xFF, ag = (color >> 8) & 0xFF, ab = color & 0xFF;
        int br = (target >> 16) & 0xFF, bg = (target >> 8) & 0xFF, bb = target & 0xFF;
        int r = (int) (ar + (br - ar) * factor);
        int g = (int) (ag + (bg - ag) * factor);
        int b = (int) (ab + (bb - ab) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class EditorNode {

        String id, title = "", lore = "", icon = "", exclusionGroup = "";
        int x, y;

        boolean autoId = false;
        final Map<ConfluxDataType, Long> cost = new EnumMap<>(ConfluxDataType.class);
        final List<UnlockEntry> unlocks = new ArrayList<>();

        EditorNode(String id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private record EditorEdge(EditorNode from, EditorNode to) {}
}
