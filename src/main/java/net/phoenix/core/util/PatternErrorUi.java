package net.phoenix.core.util;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanel;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.mui.GTMultiblockTextUtil;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;

/**
 * Gives a multiblock's own UI the "invalid structure" report the standard multiblock UIs have: what is wrong with the
 * structure, a button to highlight it in the world, and the errors. It shows under the panel's main contents, and only
 * while the structure has errors.
 */
public final class PatternErrorUi {

    private PatternErrorUi() {}

    /**
     * @param machine the machine the panel is for; nothing is added unless it is a multiblock controller
     * @return the panel, for returning from {@code buildUI}
     */
    public static <P extends ModularPanel<?>> P with(MetaMachine machine, PanelSyncManager syncManager, P panel) {
        if (!(machine instanceof MultiblockControllerMachine controller) ||
                !(panel instanceof MachineUIPanel machinePanel) ||
                machinePanel.getContentsColumn() == null || machinePanel.getContentsColumn().getChildren().isEmpty()) {
            return panel;
        }
        var warning = GTMultiblockTextUtil.addUnformedWarning(controller, syncManager);
        if (machinePanel.getContentsColumn().getChildren().get(0) instanceof ParentWidget<?> main) {
            // the HUD's first page is blank while the structure is not formed, so the errors go onto it, under its
            // "not formed" line, and not below it; not over the classic page, which has its own contents
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
