package net.phoenix.core.shop.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.integration.conflux.client.render.MotionClock;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ItemPickerScreen extends Screen {

    private enum SourceTab {
        REGISTRY,
        INVENTORY
    }

    private static final int PANEL_W = 280;
    private static final int PANEL_H = 220;
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 12;
    private static final int QTY_ROW_H = 20;

    private final Screen parent;
    private final Consumer<List<ItemStack>> onPick;

    private int cText, cDim, cBorder, cBorderDim, cPanel1, cPanel2;

    private float uiScale = 1f;
    private int vw, vh;

    private EditBox searchBox;
    private SourceTab activeTab = SourceTab.REGISTRY;
    private final List<ItemStack> allItems = new ArrayList<>();
    private List<ItemStack> displayItems = new ArrayList<>();
    private int scrollOffset = 0;

    private final List<ItemStack> multiSelection = new ArrayList<>();

    private boolean awaitingQuantity = false;
    private final List<ItemStack> confirmStacks = new ArrayList<>();
    private final List<EditBox> quantityBoxes = new ArrayList<>();

    private final MotionClock clock = new MotionClock();
    private long lastNanos = System.nanoTime();

    public ItemPickerScreen(Screen parent, Consumer<List<ItemStack>> onPick) {
        super(Component.literal("Select Product"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        float neededW = PANEL_W + 20f;
        float neededH = PANEL_H + 20f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        refreshTheme();
        rebuildPickerWidgets();
        populateAllItems();
        refresh();
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cText = t.text.getColor();
        cDim = t.textDim.getColor();
        cBorder = t.accent.getColor();
        cBorderDim = t.border.getColor();
        cPanel1 = (t.panel.getColor() & 0x00FFFFFF) | 0xF0000000;
        cPanel2 = (t.header.getColor() & 0x00FFFFFF) | 0xF0000000;
    }

    private void rebuildPickerWidgets() {
        clearWidgets();
        quantityBoxes.clear();

        int px = (vw - PANEL_W) / 2;
        int py = (vh - PANEL_H) / 2;

        if (awaitingQuantity) {
            buildQuantityWidgets(px, py);
            return;
        }

        searchBox = new EditBox(font, px + 8, py + 28, PANEL_W - 16, 16, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search items...").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setResponder(q -> refresh());
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);

        addRenderableWidget(Button.builder(Component.literal("Registry"), b -> {
            activeTab = SourceTab.REGISTRY;
            refresh();
        }).bounds(px + 8, py + 8, 70, 16).build());

        addRenderableWidget(Button.builder(Component.literal("My Inventory"), b -> {
            activeTab = SourceTab.INVENTORY;
            refresh();
        }).bounds(px + 82, py + 8, 90, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(px + PANEL_W - 60, py + 8, 52, 16).build());
    }

    private void buildQuantityWidgets(int px, int py) {
        int y = py + 40;
        for (ItemStack stack : confirmStacks) {
            EditBox box = new EditBox(font, px + PANEL_W - 70, y, 50, 16, Component.literal("Qty"));
            box.setMaxLength(5);
            box.setValue(String.valueOf(Math.max(1, stack.getCount())));
            addRenderableWidget(box);
            quantityBoxes.add(box);
            y += QTY_ROW_H;
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            awaitingQuantity = false;
            rebuildPickerWidgets();
        }).bounds(px + 8, py + PANEL_H - 24, 60, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Confirm"), b -> finalizeQuantities())
                .bounds(px + PANEL_W - 68, py + PANEL_H - 24, 60, 16).build());
    }

    private void populateAllItems() {
        allItems.clear();
        if (activeTab == SourceTab.INVENTORY) {
            if (minecraft != null && minecraft.player != null) {
                for (ItemStack stack : minecraft.player.getInventory().items) {
                    if (!stack.isEmpty()) allItems.add(stack.copy());
                }
            }
        } else {
            for (Item item : ForgeRegistries.ITEMS) {
                allItems.add(new ItemStack(item));
            }
        }
    }

    private void refresh() {
        populateAllItems();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        scrollOffset = 0;
        if (query.isEmpty()) {
            displayItems = allItems;
            return;
        }
        displayItems = new ArrayList<>();
        for (ItemStack stack : allItems) {
            String registryName = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
            String displayName = stack.getHoverName().getString().toLowerCase();
            if (registryName.toLowerCase().contains(query) || displayName.contains(query)) {
                displayItems.add(stack);
            }
        }
    }

    private int gridTop(int py) {
        return py + 50;
    }

    private int gridBottom(int py) {
        return py + PANEL_H - 30;
    }

    @Nullable
    private ItemStack slotAt(int px, int py, double mx, double my) {
        int top = gridTop(py);
        int bottom = gridBottom(py);
        if (mx < px + 8 || my < top || my >= bottom) return null;
        int col = (int) (mx - (px + 8)) / SLOT_SIZE;
        int row = (int) (my - top) / SLOT_SIZE;
        if (col < 0 || col >= SLOTS_PER_ROW) return null;
        int index = scrollOffset + row * SLOTS_PER_ROW + col;
        if (index < 0 || index >= displayItems.size()) return null;
        return displayItems.get(index);
    }

    private boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    private boolean isSelected(ItemStack stack) {
        for (ItemStack s : multiSelection) if (sameItem(s, stack)) return true;
        return false;
    }

    private void toggleSelection(ItemStack stack) {
        for (int i = 0; i < multiSelection.size(); i++) {
            if (sameItem(multiSelection.get(i), stack)) {
                multiSelection.remove(i);
                return;
            }
        }
        multiSelection.add(stack.copy());
    }

    private void goToQuantityStep(List<ItemStack> stacks) {
        if (stacks.isEmpty()) return;
        confirmStacks.clear();
        for (ItemStack s : stacks) confirmStacks.add(s.copy());
        awaitingQuantity = true;
        rebuildPickerWidgets();
    }

    private void finalizeQuantities() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < confirmStacks.size(); i++) {
            ItemStack stack = confirmStacks.get(i);
            int qty = 1;
            if (i < quantityBoxes.size()) {
                try {
                    qty = Integer.parseInt(quantityBoxes.get(i).getValue().trim());
                } catch (NumberFormatException ignored) {
                    qty = 1;
                }
            }
            qty = Math.max(1, Math.min(6400, qty));
            ItemStack out = stack.copy();
            out.setCount(qty);
            result.add(out);
        }
        onPick.accept(result);
        onClose();
    }

    private void enableScissorScaled(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.enableScissor(Math.round(x1 * uiScale), Math.round(y1 * uiScale), Math.round(x2 * uiScale),
                Math.round(y2 * uiScale));
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        clock.tick(dt);
        refreshTheme();
        float pulse = MotionClock.Signature.DEFAULT.pulse(clock.getElapsed());

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        g.fillGradient(0, 0, width, height, 0xC0000000, 0xC0000000);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        int px = (vw - PANEL_W) / 2;
        int py = (vh - PANEL_H) / 2;
        renderPanel(g, px, py, PANEL_W, PANEL_H, pulse);

        if (awaitingQuantity) {
            renderQuantityStep(g, px, py);
            super.render(g, mx, my, pt);
            g.pose().popPose();
            return;
        }

        super.render(g, mx, my, pt);

        int top = gridTop(py);
        int bottom = gridBottom(py);
        enableScissorScaled(g, px + 8, top, px + PANEL_W - 8, bottom);
        ItemStack hovered = null;
        int rows = (bottom - top) / SLOT_SIZE;
        int visible = rows * SLOTS_PER_ROW;
        for (int i = 0; i < visible && scrollOffset + i < displayItems.size(); i++) {
            ItemStack stack = displayItems.get(scrollOffset + i);
            int col = i % SLOTS_PER_ROW;
            int row = i / SLOTS_PER_ROW;
            int sx = px + 8 + col * SLOT_SIZE;
            int sy = top + row * SLOT_SIZE;

            boolean hover = mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE;
            boolean selected = isSelected(stack);
            if (selected) {
                g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, (140 << 24) | 0x55CC55);
            } else if (hover) {
                g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, (100 << 24) | (cBorder & 0xFFFFFF));
            }
            if (hover) hovered = stack;
            g.renderItem(stack, sx + 1, sy + 1);
        }
        g.disableScissor();

        if (displayItems.isEmpty()) {
            g.drawCenteredString(font, "No matching items", px + PANEL_W / 2, top + 20, cDim);
        }

        if (hovered != null) {
            g.renderTooltip(font, hovered, mx, my);
        }

        String hint = "Left-click: pick  |  Right-click: multi-select";
        g.drawString(font, hint, px + 8, py + PANEL_H - 24, cDim, false);

        if (!multiSelection.isEmpty()) {
            String confirm = multiSelection.size() + " selected - press Enter to confirm";
            g.drawString(font, Component.literal(confirm).withStyle(ChatFormatting.GREEN), px + 8,
                    py + PANEL_H - 13, cText, false);
        }

        g.pose().popPose();
    }

    private void renderQuantityStep(GuiGraphics g, int px, int py) {
        g.drawString(font, "Choose quantities", px + 8, py + 10, cText, false);

        int y = py + 40;
        for (ItemStack stack : confirmStacks) {
            g.renderItem(stack, px + 8, y + 2);
            g.drawString(font, trimName(stack.getHoverName().getString(), PANEL_W - 100), px + 30, y + 5,
                    cText, false);
            y += QTY_ROW_H;
        }
    }

    private String trimName(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 1 && font.width(text + "..") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "..";
    }

    private void renderPanel(GuiGraphics g, int x, int y, int w, int h, float pulse) {
        g.fillGradient(x, y, x + w, y + h, cPanel1, cPanel2);

        int vignette = 14;
        g.fillGradient(x, y, x + w, y + vignette, 0x99000000, 0x00000000);
        g.fillGradient(x, y + h - vignette, x + w, y + h, 0x00000000, 0x99000000);

        int borderCol = (0xFF << 24) |
                (MotionClock.lerpColor(0xFF000000 | cBorderDim, 0xFF000000 | cBorder, pulse) & 0xFFFFFF);
        g.fill(x, y, x + w, y + 1, borderCol);
        g.fill(x, y + h - 1, x + w, y + h, borderCol);
        g.fill(x, y, x + 1, y + h, borderCol);
        g.fill(x + w - 1, y, x + w, y + h, borderCol);
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (!awaitingQuantity) {
            int px = (vw - PANEL_W) / 2;
            int py = (vh - PANEL_H) / 2;
            ItemStack picked = slotAt(px, py, mx, my);
            if (picked != null) {
                if (button == 0) {
                    goToQuantityStep(List.of(picked.copy()));
                    return true;
                } else if (button == 1) {
                    toggleSelection(picked);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double rmx, double rmy, int button, double dragX, double dragY) {
        return super.mouseDragged(rmx / uiScale, rmy / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseReleased(double rmx, double rmy, int button) {
        return super.mouseReleased(rmx / uiScale, rmy / uiScale, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (awaitingQuantity) return super.mouseScrolled(mx, my, delta);
        int maxRow = Math.max(0, (displayItems.size() - 1) / SLOTS_PER_ROW);
        int visibleRows = (gridBottom(0) - gridTop(0)) / SLOT_SIZE;
        int maxScrollRow = Math.max(0, maxRow - visibleRows + 1);
        int rowDelta = delta > 0 ? -SLOTS_PER_ROW : SLOTS_PER_ROW;
        scrollOffset = Math.max(0, Math.min(maxScrollRow * SLOTS_PER_ROW, scrollOffset + rowDelta));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            if (awaitingQuantity) {
                awaitingQuantity = false;
                rebuildPickerWidgets();
            } else {
                onClose();
            }
            return true;
        }
        if (key == 257 || key == 335) {
            if (awaitingQuantity) {
                finalizeQuantities();
                return true;
            } else if (!multiSelection.isEmpty()) {
                goToQuantityStep(multiSelection);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
