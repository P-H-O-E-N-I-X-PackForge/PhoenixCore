package net.phoenix.core.integration.gregpacks.common.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.phoenix.core.integration.gregpacks.network.CPacketFluidInteract;
import net.phoenix.core.integration.gregpacks.network.GregPacksNetwork;
import org.jetbrains.annotations.NotNull;

public class OmniPackScreen extends AbstractContainerScreen<OmniPackMenu> {

    private static final int C_BG    = 0xFFC6C6C6;
    private static final int C_DARK  = 0xFF555555;
    private static final int C_LIGHT = 0xFFFFFFFF;
    private static final int C_SLOT  = 0xFF8B8B8B;
    private static final int C_TEXT  = 0xFF404040;
    private static final int C_BAR   = 0xFF3B3B3B;
    private static final int C_FLUID = 0xFF0099FF;
    private static final int C_EU    = 0xFFFFD700;

    private static final int PAD  = 7;
    private static final int COLS = 9;
    private static final int SS   = 18;

    private static final int BAR_W   = 10;
    private static final int BAR_SEP = 4;
    private static final int BAR_GAP = 3;

    private static final int TAB_W   = 70;
    private static final int TAB_H   = 14;
    private static final int TAB_OFF = 0;

    private static final int UP_COLS = 2;
    private static final int UP_PAD  = 6;

    private boolean upOpen = false;
    private int popX, popY, popW, popH;

    private final int packRows;

    public OmniPackScreen(OmniPackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        packRows = (int) Math.ceil(menu.getPackSlots() / (double) COLS);
        imageWidth  = PAD + COLS * SS + PAD;
        imageHeight = PAD + 10 + packRows * SS + PAD + 4 + 3 * SS + PAD + 4 + SS + PAD;
    }

    @Override
    protected void init() {
        super.init();
        applyMainLayout();
        hideUpSlots();
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTab(g, mx, my);
        if (upOpen) renderPopup(g);
        renderBars(g, mx, my);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, C_BG);
        bevel(g, x, y, imageWidth, imageHeight);

        int divY = y + PAD + 10 + packRows * SS + 4;
        g.fill(x + PAD, divY, x + imageWidth - PAD, divY + 1, C_DARK);

        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();
        for (int i = 0; i < menu.slots.size(); i++) {
            if (i >= uStart && i < uEnd) continue;
            HideableSlot s = menu.getHideableSlot(i);
            if (s.isVisible()) slotInset(g, x + s.x - 1, y + s.y - 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, PAD, PAD, C_TEXT, false);
        int labelY = PAD + 10 + packRows * SS + 6;
        g.drawString(font, Component.translatable("container.inventory"), PAD, labelY, C_TEXT, false);
    }

    private void renderTab(GuiGraphics g, int mx, int my) {
        int tx = leftPos + TAB_OFF;
        int ty = topPos - TAB_H;
        int bg = upOpen ? C_BG : 0xFFBBBBBB;

        g.fill(tx, ty, tx + TAB_W, ty + TAB_H, bg);
        g.fill(tx, ty, tx + TAB_W, ty + 1, C_DARK);
        g.fill(tx, ty, tx + 1, ty + TAB_H, C_DARK);
        g.fill(tx + TAB_W - 1, ty, tx + TAB_W, ty + TAB_H, C_LIGHT);
        g.fill(tx + 1, ty + TAB_H - 1, tx + TAB_W - 1, ty + TAB_H, C_BG);

        String lbl = "Upgrades " + (upOpen ? "▼" : "▶");
        g.drawString(font, lbl,
                tx + (TAB_W - font.width(lbl)) / 2,
                ty + (TAB_H - 7) / 2,
                C_TEXT, false);
    }

    private void renderPopup(GuiGraphics g) {
        g.fill(popX, popY, popX + popW, popY + popH, C_BG);
        bevel(g, popX, popY, popW, popH);

        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();
        for (int i = uStart; i < uEnd; i++) {
            HideableSlot s = menu.getHideableSlot(i);
            if (s.isVisible()) slotInset(g, leftPos + s.x - 1, topPos + s.y - 1);
        }
    }

    private void renderBars(GuiGraphics g, int mx, int my) {
        int bx1 = leftPos + imageWidth + BAR_GAP;
        int bx2 = bx1 + BAR_W + BAR_SEP;
        int by  = topPos;
        int bh  = imageHeight;

        bar(g, bx1, by, BAR_W, bh);
        int amt = menu.getSyncedFluidAmount(), cap = menu.getSyncedFluidCapacity();
        if (amt > 0 && cap > 0) {
            int fh = (int) ((float) amt / cap * (bh - 2));
            Fluid fl = menu.getSyncedFluid();
            int fc = fl != null
                    ? IClientFluidTypeExtensions.of(fl.getFluidType()).getTintColor() | 0xFF000000
                    : C_FLUID;
            g.fill(bx1 + 1, by + bh - 1 - fh, bx1 + BAR_W - 1, by + bh - 1, fc);
        }
        if (mx >= bx1 && mx < bx1 + BAR_W && my >= by && my < by + bh) {
            Fluid fl = menu.getSyncedFluid();
            String nm = fl != null ? fl.getFluidType().getDescription().getString() : "Empty";
            g.renderTooltip(font, java.util.List.of(
                            Component.literal("§bFluid: " + nm),
                            Component.literal(amt + " / " + cap + " mB"),
                            Component.literal("§7Left-click to fill, Right-click to drain")),
                    java.util.Optional.empty(), mx, my);
        }

        bar(g, bx2, by, BAR_W, bh);
        long eu = menu.getSyncedEU(), euMax = menu.getSyncedMaxEU();
        if (eu > 0 && euMax > 0) {
            int fh = (int) ((float) eu / euMax * (bh - 2));
            g.fill(bx2 + 1, by + bh - 1 - fh, bx2 + BAR_W - 1, by + bh - 1, C_EU);
        }
        if (mx >= bx2 && mx < bx2 + BAR_W && my >= by && my < by + bh) {
            g.renderTooltip(font, java.util.List.of(
                            Component.literal("§6Energy (EU)"),
                            Component.literal(eu + " / " + euMax + " EU")),
                    java.util.Optional.empty(), mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        
        if (btn == 0) {
            int tx = leftPos + TAB_OFF, ty = topPos - TAB_H;
            if (mx >= tx && mx < tx + TAB_W && my >= ty && my < ty + TAB_H) {
                upOpen = !upOpen;
                if (upOpen) openPopup();
                else hideUpSlots();
                return true;
            }
        }

        int bx1 = leftPos + imageWidth + BAR_GAP;
        if (mx >= bx1 && mx < bx1 + BAR_W && my >= topPos && my < topPos + imageHeight) {
            if (btn == 0 || btn == 1) {
                GregPacksNetwork.CHANNEL.sendToServer(new CPacketFluidInteract(btn == 0));
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int guiLeft, int guiTop, int mouseButton) {
        int bx1 = guiLeft + imageWidth + BAR_GAP;
        int bx2 = bx1 + BAR_W + BAR_SEP + BAR_W;
        if (mx >= bx1 && mx <= bx2 && my >= guiTop && my < guiTop + imageHeight) {
            return false;
        }
        return super.hasClickedOutside(mx, my, guiLeft, guiTop, mouseButton);
    }

    private void applyMainLayout() {
        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();

        for (int i = 0; i < uStart; i++)
            show(i, PAD + (i % COLS) * SS, PAD + 10 + (i / COLS) * SS);

        int invY = PAD + 10 + packRows * SS + 8;
        for (int i = 0; i < 27; i++)
            show(uEnd + i, PAD + (i % 9) * SS, invY + (i / 9) * SS);

        int hotY = invY + 3 * SS + 4;
        for (int i = 0; i < 9; i++)
            show(uEnd + 27 + i, PAD + i * SS, hotY);
    }

    private void openPopup() {
        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();
        int rows = (int) Math.ceil(menu.getMaxUpgrades() / (double) UP_COLS);
        popW = UP_PAD + UP_COLS * SS + UP_PAD;
        popH = UP_PAD + rows * SS + UP_PAD;
        popY = topPos;

        int leftCandidate = leftPos - popW - 4;
        popX = leftCandidate >= 2 ? leftCandidate : leftPos + imageWidth + 4;

        for (int i = uStart; i < uEnd; i++) {
            int idx = i - uStart;
            menu.getHideableSlot(i).moveTo(
                    popX - leftPos + UP_PAD + (idx % UP_COLS) * SS,
                    popY - topPos + UP_PAD + (idx / UP_COLS) * SS);
            menu.getHideableSlot(i).show();
        }
    }

    private void hideUpSlots() {
        int uStart = menu.getPackSlots(), uEnd = uStart + menu.getMaxUpgrades();
        for (int i = uStart; i < uEnd; i++) menu.getHideableSlot(i).hide();
    }

    private void show(int i, int x, int y) {
        menu.getHideableSlot(i).moveTo(x, y);
        menu.getHideableSlot(i).show();
    }

    private void bevel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x,         y,         x + w,     y + 1,     C_DARK);
        g.fill(x,         y,         x + 1,     y + h,     C_DARK);
        g.fill(x,         y + h - 1, x + w,     y + h,     C_LIGHT);
        g.fill(x + w - 1, y,         x + w,     y + h,     C_LIGHT);
    }

    private void slotInset(GuiGraphics g, int x, int y) {
        g.fill(x,          y,          x + SS,     y + 1,      C_DARK);
        g.fill(x,          y,          x + 1,      y + SS,     C_DARK);
        g.fill(x,          y + SS - 1, x + SS,     y + SS,     C_LIGHT);
        g.fill(x + SS - 1, y,          x + SS,     y + SS,     C_LIGHT);
        g.fill(x + 1,      y + 1,      x + SS - 1, y + SS - 1, C_SLOT);
    }

    private void bar(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, C_BAR);
        bevel(g, x, y, w, h);
    }
}