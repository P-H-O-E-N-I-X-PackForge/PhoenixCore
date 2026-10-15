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

public class CinderAtlasMenuHost extends ItemMenuHost<net.phoenix.core.common.item.cinder.CinderAtlasItem>
        implements ITerminalHost, IActionHost {

    private final IWirelessAccessPoint accessPoint;

    public CinderAtlasMenuHost(net.phoenix.core.common.item.cinder.CinderAtlasItem item, Player player,
                               appeng.menu.locator.ItemMenuHostLocator locator, IWirelessAccessPoint accessPoint) {
        super(item, player, locator);
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
    public appeng.api.storage.ILinkStatus getLinkStatus() {
        return appeng.api.storage.ILinkStatus.ofConnected();
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
