package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.phoenix.core.integration.gregvaults.network.CPacketVaultAction;
import net.phoenix.core.integration.gregvaults.network.CPacketVaultDisplayMode;
import net.phoenix.core.integration.gregvaults.network.VaultNetwork;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public abstract class AbstractVaultScreen<T extends AbstractVaultMenu>
                                         extends AbstractContainerScreen<T> {

    protected static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/gui/container/generic_54.png");
    protected static final ResourceLocation ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/gui/overlay/crafting_table.png");
    protected static final ResourceLocation SORT_TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/gui/overlay/sort_inventory.png");
    protected static final ResourceLocation STACKED_VIEW_TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/gui/overlay/stacked_view.png");
    protected static final ResourceLocation SLOT_VIEW_TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/gui/overlay/slot_view.png");
    protected static final ResourceLocation A_FIRST_TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/gui/overlay/a_first.png");
    protected static final ResourceLocation Z_FIRST_TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/gui/overlay/z_first.png");
    protected static final ResourceLocation HIGHEST_TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/gui/overlay/highest_first.png");
    protected static final ResourceLocation LOWEST_TEXTURE = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "textures/gui/overlay/lowest_first.png");

    protected static final int TEX_W = 176;
    protected static final int TEX_TOP_H = 17;
    protected static final int TEX_ROW_H = 18;
    protected static final int TEX_PLAYER_V = 125;
    protected static final int TEX_PLAYER_H = 96;

    protected static final int SB_X = TEX_W + 2;
    protected static final int SB_W = 12;
    protected static final int SB_BTN = 12;
    private static final int C_INACTIVE = 0x99111111;

    private int cSbTrack, cSbThumb, cSbBtn, cAccent;

    protected static final int BTN_X_OFFSET = -20;
    protected static final int BTN_SIZE = 18;
    protected static final int BTN_GAP = 2;

    private float uiScale = 1f;
    private int vw, vh;

    private record IconBtn(int relX, int relY, int size, Component tooltip, Runnable action) {

        boolean isHovered(int screenX, int screenY, int mx, int my) {
            int ax = screenX + relX, ay = screenY + relY;
            return mx >= ax && mx < ax + size && my >= ay && my < ay + size;
        }
    }

    protected final int visibleRows;
    protected final int sbH;
    protected final int sbTrackH;
    private int btnScreenX, btnScreenY;

    protected EditBox searchBox;
    private List<IconBtn> iconButtons;
    protected int scrollOffset = 0;
    protected int sbScreenX, sbScreenTopY, sbScreenBotY;

    private VaultSortMode currentSortMode = VaultSortMode.NAME;
    private boolean currentSortReversed = false;
    private boolean nameSortReversed = false;
    private boolean amountSortReversed = false;

    protected AbstractVaultScreen(T menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.visibleRows = menu.visibleRows;
        this.sbH = visibleRows * TEX_ROW_H;
        this.sbTrackH = sbH - 2 * SB_BTN;
        this.imageWidth = TEX_W + 2 + SB_W;
        this.imageHeight = menu.hotbarY + AbstractVaultMenu.SLOT_SIZE + 4;
    }

    @Override
    protected void init() {
        super.init();

        float neededW = imageWidth + 20f;
        float neededH = imageHeight + 20f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);
        leftPos = (vw - imageWidth) / 2;
        topPos = (vh - imageHeight) / 2;

        sbScreenX = leftPos + SB_X;
        sbScreenTopY = topPos + TEX_TOP_H;
        sbScreenBotY = sbScreenTopY + sbH - SB_BTN;
        btnScreenX = leftPos;
        btnScreenY = topPos;

        searchBox = new EditBox(font,
                leftPos + AbstractVaultMenu.SLOTS_X, topPos + 4,
                TEX_W - AbstractVaultMenu.SLOTS_X * 2 - 40, 10,
                Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setBordered(false);
        searchBox.setResponder(query -> {
            scrollOffset = 0;
            menu.updateSearch(query);
            menu.updateScroll(0);
            VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.search(query));
            onSearch(query);
        });
        addRenderableWidget(searchBox);

        iconButtons = new ArrayList<>();
        iconButtons.add(new IconBtn(BTN_X_OFFSET, 3 + (BTN_SIZE + BTN_GAP) * 0, BTN_SIZE,
                Component.literal("Organize"),
                () -> VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.organize())));
        iconButtons.add(new IconBtn(BTN_X_OFFSET, 3 + (BTN_SIZE + BTN_GAP) * 1, BTN_SIZE,
                Component.literal("Stacked / Slot view"),
                () -> {
                    VaultDisplayMode next = menu.getDisplayMode().next();
                    menu.setDisplayMode(next);
                    VaultNetwork.CHANNEL.sendToServer(new CPacketVaultDisplayMode(next));
                }));
        iconButtons.add(new IconBtn(BTN_X_OFFSET, 3 + (BTN_SIZE + BTN_GAP) * 2, BTN_SIZE,
                Component.literal("Sort: Name"),
                () -> {
                    nameSortReversed = !nameSortReversed;

                    currentSortMode = VaultSortMode.NAME;
                    currentSortReversed = nameSortReversed;

                    menu.setSort(VaultSortMode.NAME, nameSortReversed);
                    VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.sort(VaultSortMode.NAME, nameSortReversed));
                }));
        iconButtons.add(new IconBtn(BTN_X_OFFSET, 3 + (BTN_SIZE + BTN_GAP) * 3, BTN_SIZE,
                Component.literal("Sort: Amount"),
                () -> {
                    amountSortReversed = !amountSortReversed;

                    currentSortMode = VaultSortMode.COUNT;
                    currentSortReversed = amountSortReversed;

                    menu.setSort(VaultSortMode.COUNT, amountSortReversed);
                    VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.sort(VaultSortMode.COUNT, amountSortReversed));
                }));

        VaultScreenState.State state = VaultScreenState.get();
        if (state.displayMode != VaultDisplayMode.SLOTS) {
            menu.setDisplayMode(state.displayMode);
            VaultNetwork.CHANNEL.sendToServer(new CPacketVaultDisplayMode(state.displayMode));
        }
        currentSortMode = state.sortMode;
        currentSortReversed = state.sortReversed;
        if (state.sortMode != VaultSortMode.NAME || state.sortReversed) {
            menu.setSort(state.sortMode, state.sortReversed);
            VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.sort(state.sortMode, state.sortReversed));
        }
        if (state.searchQuery != null && !state.searchQuery.isEmpty()) {
            searchBox.setValue(state.searchQuery);
        }

        currentSortMode = state.sortMode;
        currentSortReversed = state.sortReversed;

        if (state.sortMode == VaultSortMode.NAME) {
            nameSortReversed = state.sortReversed;
        } else if (state.sortMode == VaultSortMode.COUNT) {
            amountSortReversed = state.sortReversed;
        }

        onInit();
    }

    protected void onInit() {}

    protected void onSearch(String query) {}

    protected int maxScroll() {
        return Math.max(0, menu.getTotalFilteredRows() - visibleRows);
    }

    protected void applyScroll(int newScroll) {
        newScroll = Math.max(0, Math.min(maxScroll(), newScroll));
        if (newScroll == scrollOffset) return;
        scrollOffset = newScroll;
        menu.updateScroll(scrollOffset);
        VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.scroll(scrollOffset));
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        renderBackground(g);

        PhoenixTheme t = PhoenixTheme.current();
        cSbTrack = t.panel.getColor();
        cSbThumb = t.textDim.getColor();
        cSbBtn = t.header.getColor();
        cAccent = t.accent.getColor();

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        super.render(g, mx, my, pt);
        renderButtonIcons(g, mx, my);
        renderExtras(g, mx, my, pt);
        for (IconBtn btn : iconButtons) {
            if (btn.isHovered(btnScreenX, btnScreenY, mx, my)) {
                g.renderTooltip(font, btn.tooltip(), mx, my);
                break;
            }
        }
        renderTooltip(g, mx, my);

        g.pose().popPose();
    }

    public boolean isStackedMode() {
        return menu.getDisplayMode() == VaultDisplayMode.STACKED;
    }

    public int getSlotIndex(Slot slot) {
        return menu.slots.indexOf(slot);
    }

    public void applyOptimisticPickup(AggregatedStack agg, boolean half) {
        if (agg == null || menu.clientCache == null) return;
        int backingSlot = agg.backingSlots.isEmpty() ? -1 : agg.backingSlots.get(0);
        if (backingSlot < 0 || backingSlot >= menu.clientCache.length) return;
        net.minecraft.world.item.ItemStack inSlot = menu.clientCache[backingSlot];
        if (inSlot == null || inSlot.isEmpty()) return;
        int amount = half ? Math.max(1, inSlot.getCount() / 2) : inSlot.getCount();
        int remaining = inSlot.getCount() - amount;
        menu.clientCache[backingSlot] = remaining > 0 ? inSlot.copyWithCount(remaining) :
                net.minecraft.world.item.ItemStack.EMPTY;
        menu.applyDeltaUpdate();
    }

    public void renderStackedCountLabel(GuiGraphics g, Slot slot, long total) {
        String text = formatStackedCount(total);
        final float scale = 0.5f;
        g.pose().pushPose();
        g.pose().translate(slot.x, slot.y, 200);
        g.pose().scale(scale, scale, scale);
        int tx = (int) ((16f - font.width(text) * scale) / scale);
        int ty = (int) ((16f - 7f * scale) / scale);
        g.drawString(font, text, tx, ty, 0xFFFFFF, true);
        g.pose().popPose();
    }

    public boolean renderStackedTooltip(GuiGraphics g, AggregatedStack agg, int x, int y) {
        net.minecraft.world.item.ItemStack display = agg.displayStack.copy();
        display.setCount(1);
        List<Component> lines = display.getTooltipLines(
                minecraft.player,
                minecraft.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED :
                        net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        lines.add(1, Component.literal(
                formatStackedCount(agg.totalCount())).withStyle(net.minecraft.ChatFormatting.GRAY));
        g.renderComponentTooltip(font, lines, x, y);
        return true;
    }

    private static String formatStackedCount(long count) {
        if (count < 10_000L) return Long.toString(count);
        if (count < 1_000_000L) return (count / 1_000L) + "k";
        if (count < 1_000_000_000L) return (count / 1_000_000L) + "m";
        return (count / 1_000_000_000L) + "b";
    }

    private void renderButtonIcons(GuiGraphics g, int mx, int my) {
        ResourceLocation[] icons = {
                SORT_TEXTURE,
                menu.getDisplayMode() == VaultDisplayMode.STACKED ? STACKED_VIEW_TEXTURE : SLOT_VIEW_TEXTURE,
                nameSortReversed ? Z_FIRST_TEXTURE : A_FIRST_TEXTURE,
                amountSortReversed ? LOWEST_TEXTURE : HIGHEST_TEXTURE
        };

        for (int i = 0; i < iconButtons.size(); i++) {
            IconBtn btn = iconButtons.get(i);
            int bx = btnScreenX + btn.relX(), by = btnScreenY + btn.relY();
            if (btn.isHovered(btnScreenX, btnScreenY, mx, my)) {
                g.fill(bx - 1, by - 1, bx + btn.size() + 1, by + btn.size() + 1, (cAccent & 0x00FFFFFF) | 0x55000000);
            }
            g.blit(icons[i], bx, by, 0, 0, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);
        }
    }

    protected void renderExtras(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        int S = AbstractVaultMenu.SLOT_SIZE;

        g.blit(TEXTURE, x, y, 0, 0, TEX_W, TEX_TOP_H);
        for (int row = 0; row < visibleRows; row++) {
            g.blit(TEXTURE, x, y + TEX_TOP_H + row * TEX_ROW_H, 0, 17, TEX_W, TEX_ROW_H);
        }

        int craftSecY = y + menu.craftSectionY;
        int craftSecH = 4 * TEX_ROW_H;
        g.fill(x, craftSecY, x + TEX_W, craftSecY + craftSecH, 0xFFC6C6C6);
        g.fill(x, craftSecY, x + 1, craftSecY + craftSecH, 0xFF000000);
        g.fill(x + 1, craftSecY, x + 3, craftSecY + craftSecH, 0xFFFFFFFE);
        g.fill(x + TEX_W - 1, craftSecY, x + TEX_W, craftSecY + craftSecH, 0xFF000000);
        g.fill(x + TEX_W - 3, craftSecY, x + TEX_W - 1, craftSecY + craftSecH, 0xFF4F4F4F);

        int craftGY = y + menu.craftGridY;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                g.blit(TEXTURE,
                        x + menu.craftGridX + col * S - 1,
                        craftGY + row * S - 1,
                        7, 17, S, S);
            }
        }
        g.blit(ARROW_TEXTURE,
                x + menu.craftGridX + 3 * S + 1,
                craftGY + S + (S - 15) / 2,
                0, 0, 22, 15, 22, 15);
        g.blit(TEXTURE, x + menu.craftOutX - 1, y + menu.craftOutY - 1, 7, 17, S, S);
        g.blit(TEXTURE, x, y + menu.playerY - 15, 0, TEX_PLAYER_V, TEX_W, TEX_PLAYER_H);

        renderScrollbar(g);
        renderInactiveSlotOverlays(g);
    }

    private void renderScrollbar(GuiGraphics g) {
        int sbX = sbScreenX, sbY = sbScreenTopY;
        g.fill(sbX, sbY, sbX + SB_W, sbY + sbH, cSbTrack);
        g.fill(sbX, sbY, sbX + SB_W, sbY + SB_BTN, cSbBtn);
        g.drawString(font, "\u25b2", sbX + 2, sbY + 2, 0x333333, false);
        g.fill(sbX, sbScreenBotY, sbX + SB_W, sbScreenBotY + SB_BTN, cSbBtn);
        g.drawString(font, "\u25bc", sbX + 2, sbScreenBotY + 2, 0x333333, false);

        int maxRows = maxScroll();
        if (maxRows > 0) {
            int thumbH = Math.max(10, sbTrackH * visibleRows / (maxRows + visibleRows));
            int thumbY = (int) ((float) scrollOffset / maxRows * (sbTrackH - thumbH));
            g.fill(sbX + 1, sbY + SB_BTN + thumbY, sbX + SB_W - 1, sbY + SB_BTN + thumbY + thumbH, cSbThumb);
        } else {
            g.fill(sbX + 1, sbY + SB_BTN, sbX + SB_W - 1, sbScreenBotY, cSbThumb);
        }
    }

    private void renderInactiveSlotOverlays(GuiGraphics g) {
        int count = menu.getVisibleSlotCount();
        for (int i = 0; i < count; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive()) {
                g.fill(leftPos + slot.x, topPos + slot.y,
                        leftPos + slot.x + 16, topPos + slot.y + 16, C_INACTIVE);
            }
        }
    }

    @Override
    public boolean mouseDragged(double rmx, double rmy, int button, double dragX, double dragY) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (button == 0) {
            int ix = (int) mx;
            if (ix >= sbScreenX && ix < sbScreenX + SB_W) {
                int trackTop = sbScreenTopY + SB_BTN;
                int trackH = sbScreenBotY - trackTop;
                if (trackH > 0) {
                    float ratio = (float) ((int) my - trackTop) / trackH;
                    applyScroll(Math.round(ratio * maxScroll()));
                    return true;
                }
            }
        }
        return super.mouseDragged(mx, my, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        applyScroll(scrollOffset + (delta < 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (button == 0) {
            int ix = (int) mx, iy = (int) my;
            if (ix >= sbScreenX && ix < sbScreenX + SB_W) {
                if (iy >= sbScreenTopY && iy < sbScreenTopY + SB_BTN) {
                    applyScroll(scrollOffset - 1);
                    return true;
                }
                if (iy >= sbScreenBotY && iy < sbScreenBotY + SB_BTN) {
                    applyScroll(scrollOffset + 1);
                    return true;
                }
            }
            for (IconBtn btn : iconButtons) {
                if (btn.isHovered(btnScreenX, btnScreenY, ix, iy)) {
                    btn.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double rmx, double rmy, int button) {
        return super.mouseReleased(rmx / uiScale, rmy / uiScale, button);
    }

    @Override
    public void onClose() {
        VaultScreenState.save(
                menu.getDisplayMode(),
                currentSortMode,
                currentSortReversed,
                searchBox != null ? searchBox.getValue() : "");
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        if (searchBox.isFocused()) return searchBox.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (searchBox.isFocused()) return searchBox.charTyped(c, modifiers);
        return super.charTyped(c, modifiers);
    }
}
