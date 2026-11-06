package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import brachy.modularui.value.sync.SyncHandler;
import brachy.modularui.widgets.slot.ModularSlot;

public final class VaultSyncHandler extends SyncHandler<VaultSyncHandler> {

    private static final int SCROLL = 0;
    private static final int SEARCH = 1;
    private static final int SORT = 2;
    private static final int ORGANIZE = 3;
    private static final int MODE = 4;
    private static final int PICKUP = 5;

    private static final int STATE = 0;

    private final VaultSession session;

    private int sentRows = -1;
    private int sentActive = -1;
    private int sentStorage = -1;
    private int sentScroll = -1;
    private VaultDisplayMode sentMode = null;

    public VaultSyncHandler(VaultSession session) {
        this.session = session;
        allowC2S();
    }

    private int clientMaxScroll() {
        return Math.max(0, session.totalRows - VaultWindow.ROWS);
    }

    public void requestScroll(int row) {
        int clamped = Math.max(0, Math.min(clientMaxScroll(), row));
        if (clamped == session.scrollRow) return;
        session.scrollRow = clamped;
        syncToServer(SCROLL, buf -> buf.writeVarInt(clamped));
    }

    public void requestSearch(String text) {
        String query = text == null ? "" : text;
        session.scrollRow = 0;
        syncToServer(SEARCH, buf -> buf.writeUtf(query, 64));
    }

    public void requestSort(VaultSortMode mode, boolean reversed) {
        syncToServer(SORT, buf -> {
            buf.writeEnum(mode);
            buf.writeBoolean(reversed);
        });
    }

    public void requestOrganize() {
        syncToServer(ORGANIZE);
    }

    public void requestDisplayMode(VaultDisplayMode mode) {
        session.displayMode = mode;
        session.scrollRow = 0;
        syncToServer(MODE, buf -> buf.writeEnum(mode));
    }

    public void requestPickup(int windowIndex, boolean half) {
        syncToServer(PICKUP, buf -> {
            buf.writeVarInt(windowIndex);
            buf.writeBoolean(half);
        });
    }

    @Override
    public void readOnServer(int id, RegistryFriendlyByteBuf buf) {
        VaultView view = session.view;
        if (view == null) return;
        switch (id) {
            case SCROLL -> view.setScroll(buf.readVarInt());
            case SEARCH -> view.setQuery(buf.readUtf(64));
            case SORT -> view.setSort(buf.readEnum(VaultSortMode.class), buf.readBoolean());
            case ORGANIZE -> view.organize();
            case MODE -> view.setDisplayMode(buf.readEnum(VaultDisplayMode.class));
            case PICKUP -> pickup(buf.readVarInt(), buf.readBoolean());
            default -> {}
        }
        session.refreshWindowSlots();
    }

    private void pickup(int windowIndex, boolean half) {
        VaultView view = session.view;
        if (view == null || !view.window.isAggregated()) return;
        if (windowIndex < 0 || windowIndex >= session.windowSlots.size()) return;

        ModularSlot slot = session.windowSlots.get(windowIndex);
        ItemStack visible = slot.getItem();
        if (visible.isEmpty()) return;

        var menu = session.player.containerMenu;
        int max = visible.getMaxStackSize();
        int amount = half ? Math.max(1, max / 2) : max;

        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(carried, visible)) return;
            amount = Math.min(amount, carried.getMaxStackSize() - carried.getCount());
        }
        if (amount <= 0) return;

        ItemStack extracted = slot.remove(amount);
        if (extracted.isEmpty()) return;
        if (carried.isEmpty()) {
            menu.setCarried(extracted);
        } else if (ItemStack.isSameItemSameComponents(carried, extracted)) {
            carried.grow(extracted.getCount());
        } else {
            session.insertIntoVault(extracted);
        }
        view.rebuild();
    }

    @Override
    public void detectAndSendChanges(boolean init) {
        if (!session.server || session.view == null) return;
        session.tick();

        VaultView view = session.view;
        int rows = view.totalRows();
        int active = view.activeSlots();
        int scroll = view.scrollRow();
        int storage = session.access.storage().getSlots();
        VaultDisplayMode mode = view.displayMode();
        if (!init && rows == sentRows && active == sentActive && scroll == sentScroll && mode == sentMode &&
                storage == sentStorage) {
            return;
        }

        sentRows = rows;
        sentActive = active;
        sentScroll = scroll;
        sentStorage = storage;
        sentMode = mode;
        syncToClient(STATE, buf -> {
            buf.writeVarInt(rows);
            buf.writeVarInt(active);
            buf.writeVarInt(scroll);
            buf.writeVarInt(storage);
            buf.writeEnum(mode);
        });
    }

    @Override
    public void readOnClient(int id, RegistryFriendlyByteBuf buf) {
        if (id != STATE) return;
        session.totalRows = buf.readVarInt();
        session.activeSlots = buf.readVarInt();
        session.scrollRow = buf.readVarInt();
        session.storageSlots = buf.readVarInt();
        session.displayMode = buf.readEnum(VaultDisplayMode.class);
    }
}
