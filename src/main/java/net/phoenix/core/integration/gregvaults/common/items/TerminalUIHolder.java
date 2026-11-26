package net.phoenix.core.integration.gregvaults.common.items;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import brachy.modularui.api.IUIHolder;
import brachy.modularui.factory.PlayerInventoryGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;

public interface TerminalUIHolder extends IUIHolder<PlayerInventoryGuiData<?>> {

    @Override
    @OnlyIn(Dist.CLIENT)
    default ModularScreen createScreen(PlayerInventoryGuiData<?> data, ModularPanel<?> mainPanel) {
        return new com.gregtechceu.gtceu.api.mui.GTGuiScreen(mainPanel);
    }
}
