package net.phoenix.core.integration.gregvaults.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("all")
public class VaultScreen extends AbstractVaultScreen<VaultContainerMenu> {

    public VaultScreen(VaultContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        String slotInfo = menu.totalSlots + " slots";
        g.drawString(font, slotInfo,
                TEX_W - font.width(slotInfo) - AbstractVaultMenu.SLOTS_X, 6, 0x404040, false);
        g.drawString(font, "Crafting",
                AbstractVaultMenu.SLOTS_X, menu.craftSectionY + 4, 0x404040, false);
        g.drawString(font, Component.translatable("container.inventory"),
                AbstractVaultMenu.SLOTS_X, menu.playerY - 11, 0x404040, false);
    }
}
