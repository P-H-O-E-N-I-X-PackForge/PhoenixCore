package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import brachy.modularui.drawable.DynamicDrawable;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.StringValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.textfield.TextFieldWidget;

import java.util.function.Supplier;

public final class VaultPanel {

    public static final int WIDTH = 190;
    public static final int HEIGHT = 296;

    private static final int GRID_X = 7;
    private static final int GRID_Y = 22;
    private static final int CRAFT_Y = 144;
    private static final int PLAYER_Y = 214;

    private static final ResourceLocation ORGANIZE = icon("sort_inventory");
    private static final ResourceLocation STACKED = icon("stacked_view");
    private static final ResourceLocation SLOTS = icon("slot_view");
    private static final ResourceLocation A_FIRST = icon("a_first");
    private static final ResourceLocation Z_FIRST = icon("z_first");
    private static final ResourceLocation HIGHEST = icon("highest_first");
    private static final ResourceLocation LOWEST = icon("lowest_first");
    private static final ResourceLocation ARROW = icon("crafting_table");

    private VaultPanel() {}

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath("phoenixcore", "textures/gui/overlay/" + name + ".png");
    }

    public static ModularPanel<?> build(VaultSession session, PanelSyncManager sync, UISettings settings,
                                        int lockedPlayerSlot) {
        VaultSyncHandler handler = new VaultSyncHandler(session);
        session.sync = handler;
        sync.syncValue("vault", handler);

        registerSlots(session, sync, lockedPlayerSlot);

        settings.customContainer(id -> new VaultMenu(id, session));
        settings.canInteractWith(session::stillValid);

        if (session.server) {
            session.initGrid(session.access.savedGrid());
            sync.addOpenListener(player -> {
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) session.access.opened(sp);
            });
            sync.addCloseListener(player -> {
                session.access.saveGrid(session.copyGrid());
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) session.access.closed(sp);
            });
        } else {
            sync.addOpenListener(player -> applySavedState(session, handler));
        }

        ModularPanel<?> panel = ModularPanel.defaultPanel("vault", WIDTH, HEIGHT);
        net.phoenix.core.client.gui.PanelFit.apply(panel, WIDTH, HEIGHT);
        ChosenState chosen = new ChosenState(session);
        if (!session.server) {
            sync.addCloseListener(player -> VaultScreenState.save(session.displayMode, chosen.sortMode,
                    chosen.sortReversed, chosen.query));
        }
        addHeader(panel, session, handler, chosen);
        addGrid(panel, session);
        addCrafting(panel, session);
        panel.child(label(new TextWidget(Component.literal("Inventory")), 8, PLAYER_Y - 10));
        SlotGroupWidget inventory = SlotGroupWidget.playerInventory(false);
        inventory.disableSortButtons();
        inventory.pos(GRID_X, PLAYER_Y);
        panel.child(inventory);
        return panel;
    }

    private static void registerSlots(VaultSession session, PanelSyncManager sync, int lockedPlayerSlot) {
        sync.registerSlotGroup("vault_window", VaultWindow.COLS, false);
        sync.registerSlotGroup("vault_grid", 3, false);

        for (int i = 0; i < VaultWindow.SIZE; i++) {
            VaultWindowSlot slot = new VaultWindowSlot(session, i);
            slot.slotGroup("vault_window");
            session.windowSlots.add(slot);
            sync.itemSlot("vault_window", i, slot);
        }
        for (int i = 0; i < 9; i++) {
            ModularSlot slot = new ModularSlot(session.gridHandler(), i);
            slot.slotGroup("vault_grid");
            session.gridSlots.add(slot);
            sync.itemSlot("vault_grid", i, slot);
        }
        VaultResultSlot result = new VaultResultSlot(session);
        result.singletonSlotGroup();
        session.resultSlot = result;
        sync.itemSlot("vault_result", 0, result);

        Player player = session.player;
        sync.bindPlayerInventory(player, (wrapper, index) -> {
            ModularSlot slot = ModularSlot.playerSlot(wrapper, index, player);
            if (index == lockedPlayerSlot) slot.accessibility(false, false);
            session.playerSlots.add(slot);
            return slot;
        });
    }

    private static final class ChosenState {

        String query = "";
        VaultSortMode sortMode = VaultSortMode.NAME;
        boolean sortReversed = false;
        boolean nameReversed = false;
        boolean amountReversed = false;

        ChosenState(VaultSession session) {
            if (session.server) return;
            var saved = VaultScreenState.get();
            query = saved.searchQuery;
            sortMode = saved.sortMode;
            sortReversed = saved.sortReversed;
            nameReversed = saved.sortMode == VaultSortMode.NAME && saved.sortReversed;
            amountReversed = saved.sortMode == VaultSortMode.COUNT && saved.sortReversed;
        }
    }

    private static void addHeader(ModularPanel<?> panel, VaultSession session, VaultSyncHandler handler,
                                  ChosenState chosen) {
        panel.child(new TextFieldWidget()
                .value(new StringValue.Dynamic(() -> chosen.query, text -> {
                    chosen.query = text;
                    handler.requestSearch(text);
                }))
                .hintText(Component.literal("Search..."))
                .setMaxLength(64)
                .pos(8, 6)
                .size(100, 11));

        int x = 112;
        panel.child(button(x, "Organize", () -> ORGANIZE, handler::requestOrganize));
        panel.child(button(x + 18, "Stacked / Slot view", () -> session.isStacked() ? STACKED : SLOTS,
                () -> handler.requestDisplayMode(session.displayMode.next())));
        panel.child(button(x + 36, "Sort: Name", () -> chosen.nameReversed ? Z_FIRST : A_FIRST, () -> {
            chosen.nameReversed = !chosen.nameReversed;
            chosen.sortMode = VaultSortMode.NAME;
            chosen.sortReversed = chosen.nameReversed;
            handler.requestSort(VaultSortMode.NAME, chosen.nameReversed);
        }));
        panel.child(button(x + 54, "Sort: Amount", () -> chosen.amountReversed ? LOWEST : HIGHEST, () -> {
            chosen.amountReversed = !chosen.amountReversed;
            chosen.sortMode = VaultSortMode.COUNT;
            chosen.sortReversed = chosen.amountReversed;
            handler.requestSort(VaultSortMode.COUNT, chosen.amountReversed);
        }));
    }

    private static TextWidget label(TextWidget text, int x, int y) {
        text.color(0x404040);
        text.pos(x, y);
        return text;
    }

    private static ButtonWidget<?> button(int x, String tooltip, Supplier<ResourceLocation> texture, Runnable action) {
        return new ButtonWidget<>()
                .size(16, 16)
                .pos(x, 3)
                .overlay(new DynamicDrawable(() -> UITexture.fullImage(texture.get())))
                .tooltipBuilder(t -> t.addLine(Component.literal(tooltip)))
                .onMousePressed((context, button) -> {
                    if (button != 0) return false;
                    action.run();
                    return true;
                });
    }

    private static void addGrid(ModularPanel<?> panel, VaultSession session) {
        VaultGrid grid = new VaultGrid(session);
        grid.pos(GRID_X, GRID_Y).size(VaultWindow.COLS * 18, VaultWindow.ROWS * 18);
        for (int i = 0; i < VaultWindow.SIZE; i++) {
            grid.child(new VaultSlotWidget(session, i)
                    .syncHandler("vault_window", i)
                    .pos((i % VaultWindow.COLS) * 18, (i / VaultWindow.COLS) * 18));
        }
        panel.child(grid);
        panel.child(new VaultScrollBar(session).pos(GRID_X + VaultWindow.COLS * 18 + 2, GRID_Y)
                .size(10, VaultWindow.ROWS * 18));
    }

    private static void addCrafting(ModularPanel<?> panel, VaultSession session) {
        panel.child(label(new TextWidget(Component.literal("Crafting")), 8, CRAFT_Y - 10));
        panel.child(
                label(new TextWidget(() -> Component.literal(String.format("%,d slots", session.storageSlots))), 110,
                        CRAFT_Y - 10));

        for (int i = 0; i < 9; i++) {
            panel.child(new ItemSlot().syncHandler("vault_grid", i)
                    .pos(GRID_X + (i % 3) * 18, CRAFT_Y + (i / 3) * 18));
        }
        panel.child(UITexture.fullImage(ARROW).asWidget().size(22, 15).pos(GRID_X + 54 + 4, CRAFT_Y + 18 + 1));
        panel.child(new ItemSlot().syncHandler("vault_result", 0).pos(GRID_X + 54 + 26, CRAFT_Y + 18));
    }

    private static void applySavedState(VaultSession session, VaultSyncHandler handler) {
        var state = VaultScreenState.get();
        if (state.displayMode != VaultDisplayMode.SLOTS) handler.requestDisplayMode(state.displayMode);
        if (state.sortMode != VaultSortMode.NAME || state.sortReversed) {
            handler.requestSort(state.sortMode, state.sortReversed);
        }
        if (!state.searchQuery.isEmpty()) handler.requestSearch(state.searchQuery);
    }
}
