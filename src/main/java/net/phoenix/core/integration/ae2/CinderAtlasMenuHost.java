package net.phoenix.core.integration.ae2;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.MEStorage;
import appeng.api.util.IConfigManager;
import appeng.menu.ISubMenu;
import appeng.util.NullConfigManager;

import org.jetbrains.annotations.Nullable;

/**
 * Minimal {@link ITerminalHost} wrapping a Cinder Atlas's linked AE2 grid storage, so AE2's own
 * multi-item {@code CraftingTermMenu} - the same menu AE2's own Wireless Crafting Terminal opens - can
 * be opened directly against it. Built as a standalone host instead of making {@link
 * net.phoenix.core.common.item.cinder.CinderAtlasItem} extend {@code WirelessTerminalItem}, since that
 * base class owns its own right-click/battery/menu-type wiring that would collide with the Atlas's
 * existing shift-click-for-editor / plain-click-to-deploy interaction scheme.
 * <p>
 * {@link #getConfigManager()} returns {@link NullConfigManager#INSTANCE} - verified against AE2's own
 * {@code MEStorageMenu} bytecode that this is safe: the menu keeps its own internal client-side settings
 * mirror (sort order, view mode, type filter) seeded with defaults on construction regardless of the
 * host, and only consults the host's config manager to persist those settings across openings. A null
 * one just skips that persistence - sort/view prefs reset to default each time the terminal is reopened,
 * a cosmetic gap, not a correctness issue.
 * <p>
 * Also implements {@link IActionHost}, delegating {@link #getActionableNode()} to the linked
 * {@link IWirelessAccessPoint}'s own real grid node - mirroring AE2's own
 * {@code WirelessTerminalMenuHost#getActionableNode}, which borrows its bound access point's node the
 * same way rather than the terminal item somehow being a grid node itself. This is what makes AE2's
 * in-terminal "request craft" flow (which hard-casts its host to {@code IActionHost} - verified via
 * {@code CraftConfirmMenu#getGrid}/{@code #getActionSrc} bytecode) actually work when opened against an
 * Atlas, not just the read-only stock browsing {@link #getInventory()} alone would provide.
 * <p>
 * Real distance-from-access-point range checking now exists ({@code CinderAtlasWirelessLink#isInRange})
 * but lives at the call site ({@code CinderAtlasItem#getMenuHost}), not here - this class is only ever
 * constructed after that check has already passed, so it has no range logic of its own to duplicate.
 */
public class CinderAtlasMenuHost extends ItemMenuHost implements ITerminalHost, IActionHost {

    private final IWirelessAccessPoint accessPoint;

    public CinderAtlasMenuHost(Player player, int slot, ItemStack stack, IWirelessAccessPoint accessPoint) {
        super(player, slot, stack);
        this.accessPoint = accessPoint;
    }

    @Override
    public MEStorage getInventory() {
        return accessPoint.getGrid().getStorageService().getInventory();
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        return accessPoint.getActionableNode();
    }

    @Override
    public IConfigManager getConfigManager() {
        return NullConfigManager.INSTANCE;
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        player.closeContainer();
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getItemStack();
    }
}
