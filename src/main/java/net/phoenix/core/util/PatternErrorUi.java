package net.phoenix.core.util;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanel;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.mui.GTMultiblockTextUtil;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;

public final class PatternErrorUi {

    private PatternErrorUi() {}

    public static <P extends ModularPanel<?>> P with(MetaMachine machine, PanelSyncManager syncManager, P panel) {
        if (!(machine instanceof MultiblockControllerMachine controller) ||
                !(panel instanceof MachineUIPanel machinePanel) ||
                machinePanel.getContentsColumn() == null || machinePanel.getContentsColumn().getChildren().isEmpty()) {
            return panel;
        }
        var warning = GTMultiblockTextUtil.addUnformedWarning(controller, syncManager);
        if (machinePanel.getContentsColumn().getChildren().get(0) instanceof ParentWidget<?> main) {

            IWidget classic = main.getChildren().size() > 1 ? main.getChildren().get(1) : null;
            warning.left(8).top(38);
            if (classic != null) warning.setEnabledIf(w -> !classic.isEnabled());
            main.child(warning);
        } else {
            machinePanel.getContentsColumn().child(1, warning);
        }
        return panel;
    }
}
