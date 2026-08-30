package net.phoenix.core.client.emi;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmiFavoritePagesScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PADDING = 14;
    private static final int ROW_HEIGHT = 22;
    private static final int TITLE_HEIGHT = 26;
    private static final int ARROW_W = 16;
    private static final int ICON_W = 20;

    private final @Nullable Screen parent;
    private EditBox newPageBox;

    private @Nullable String renaming;
    private EditBox renameBox;

    // Refreshed from the shared Phoenix theme at the top of every render() call.
    private int cPanel, cBorderLight, cBorderDark, cTitleBar, cText;

    // The panel's height grows with the favorite-page count (unbounded) with no clamp at all;
    // below the room it needs we shrink the whole screen via a pose scale (same idea used across
    // the rest of the Phoenix Suite) instead of letting it run off-screen.
    private float uiScale = 1f;
    private int vw, vh;

    public EmiFavoritePagesScreen(@Nullable Screen parent) {
        super(Component.literal("EMI Favorite Pages"));
        this.parent = parent;
    }

    private int contentHeight(int pageCount) {
        return TITLE_HEIGHT + pageCount * ROW_HEIGHT + 10 + ROW_HEIGHT + 12 + ROW_HEIGHT;
    }

    @Override
    protected void init() {
        int panelHeight = (renaming != null ? renamePanelHeight() :
                contentHeight(PhoenixFavoriteSets.getSetNames().size()) + PADDING * 2);
        float neededW = PANEL_WIDTH + 20f;
        float neededH = panelHeight + 20f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        refreshTheme();

        if (renaming != null) {
            initRenameView();
        } else {
            initListView();
        }
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cPanel = (t.panel.getColor() & 0x00FFFFFF) | 0xF0000000;
        cBorderLight = t.accent.getColor();
        cBorderDark = t.border.getColor();
        cTitleBar = (t.header.getColor() & 0x00FFFFFF) | 0x30000000;
        cText = t.text.getColor();
    }

    private void initListView() {
        List<String> pageNames = PhoenixFavoriteSets.getSetNames();
        String active = PhoenixFavoriteSets.getActiveSet();

        int panelHeight = contentHeight(pageNames.size()) + PADDING * 2;
        int left = vw / 2 - PANEL_WIDTH / 2;
        int top = (vh - panelHeight) / 2;
        int contentWidth = PANEL_WIDTH - PADDING * 2;

        int y = top + PADDING + TITLE_HEIGHT;

        for (int i = 0; i < pageNames.size(); i++) {
            String page = pageNames.get(i);
            boolean isActive = page.equals(active);
            boolean canDelete = pageNames.size() > 1;

            int x = left + PADDING;

            int rowY = y;
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
                PhoenixFavoriteSets.moveUp(page);
                rebuild();
            }).bounds(x, rowY, ARROW_W, 20).build()).active = i > 0;
            x += ARROW_W + 2;

            addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
                PhoenixFavoriteSets.moveDown(page);
                rebuild();
            }).bounds(x, rowY, ARROW_W, 20).build()).active = i < pageNames.size() - 1;
            x += ARROW_W + 4;

            int nameWidth = contentWidth - (x - (left + PADDING)) - ICON_W * 2 - 4;
            String label = isActive ? "★ " + page : page;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                PhoenixFavoriteSets.switchTo(page);
                onClose();
            }).bounds(x, rowY, nameWidth, 20).build());
            x += nameWidth + 2;

            addRenderableWidget(Button.builder(Component.literal("✎"), b -> {
                renaming = page;
                renameBox = null;
                rebuild();
            }).bounds(x, rowY, ICON_W, 20).build());
            x += ICON_W + 2;

            Button deleteBtn = addRenderableWidget(Button.builder(Component.literal("🗑"), b -> {
                if (PhoenixFavoriteSets.deleteSet(page)) rebuild();
            }).bounds(x, rowY, ICON_W, 20).build());
            deleteBtn.active = canDelete;

            y += ROW_HEIGHT;
        }

        y += 10;
        int boxWidth = contentWidth - 40;
        newPageBox = new EditBox(this.font, left + PADDING, y, boxWidth, 20, Component.literal("New page name"));
        newPageBox.setMaxLength(32);
        addRenderableWidget(newPageBox);

        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            String name = newPageBox.getValue().trim();
            if (!name.isEmpty()) {
                PhoenixFavoriteSets.createSet(name);
                onClose();
            }
        }).bounds(left + PADDING + boxWidth + 4, y, 36, 20).build());

        y += ROW_HEIGHT + 12;

        PhoenixFavoriteSets.Scope scope = PhoenixFavoriteSets.getScope();
        String scopeLabel = "Pages: " +
                (scope == PhoenixFavoriteSets.Scope.GLOBAL ? "Shared across worlds" : "Private per world");
        addRenderableWidget(Button.builder(Component.literal(scopeLabel), b -> {
            PhoenixFavoriteSets
                    .setScope(scope == PhoenixFavoriteSets.Scope.GLOBAL ? PhoenixFavoriteSets.Scope.PER_WORLD :
                            PhoenixFavoriteSets.Scope.GLOBAL);
            rebuild();
        }).bounds(left + PADDING, y, contentWidth, 20).build());
    }

    private void initRenameView() {
        int panelHeight = renamePanelHeight();
        int left = vw / 2 - PANEL_WIDTH / 2;
        int top = (vh - panelHeight) / 2;
        int contentWidth = PANEL_WIDTH - PADDING * 2;
        int y = top + PADDING + TITLE_HEIGHT;

        renameBox = new EditBox(this.font, left + PADDING, y, contentWidth, 20, Component.literal("New name"));
        renameBox.setMaxLength(32);
        renameBox.setValue(renaming);
        addRenderableWidget(renameBox);
        y += ROW_HEIGHT + 8;

        addRenderableWidget(Button.builder(Component.literal("Rename"), b -> {
            String newName = renameBox.getValue().trim();
            if (!newName.isEmpty()) {
                PhoenixFavoriteSets.renameSet(renaming, newName);
            }
            renaming = null;
            rebuild();
        }).bounds(left + PADDING, y, contentWidth / 2 - 4, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
            renaming = null;
            rebuild();
        }).bounds(left + PADDING + contentWidth / 2 + 4, y, contentWidth / 2 - 4, 20).build());
    }

    private int renamePanelHeight() {
        return TITLE_HEIGHT + ROW_HEIGHT + 8 + ROW_HEIGHT + PADDING * 2;
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int rmx, int rmy, float partialTick) {
        renderBackground(graphics);
        refreshTheme();

        int mouseX = Math.round(rmx / uiScale);
        int mouseY = Math.round(rmy / uiScale);

        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1f);

        boolean isRename = renaming != null;
        int panelHeight = isRename ? renamePanelHeight() :
                contentHeight(PhoenixFavoriteSets.getSetNames().size()) + PADDING * 2;
        int left = vw / 2 - PANEL_WIDTH / 2;
        int top = (vh - panelHeight) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, cPanel);
        graphics.fill(left, top, right, top + 1, cBorderLight);
        graphics.fill(left, top, left + 1, bottom, cBorderLight);
        graphics.fill(left, bottom - 1, right, bottom, cBorderDark);
        graphics.fill(right - 1, top, right, bottom, cBorderDark);

        graphics.fill(left + 1, top + 1, right - 1, top + TITLE_HEIGHT, cTitleBar);

        String title = isRename ? "Rename \"" + renaming + "\"" : "EMI Favorite Pages";
        graphics.drawCenteredString(this.font, title, vw / 2, top + (TITLE_HEIGHT - 8) / 2, cText);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        return super.mouseClicked(rmx / uiScale, rmy / uiScale, button);
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
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
