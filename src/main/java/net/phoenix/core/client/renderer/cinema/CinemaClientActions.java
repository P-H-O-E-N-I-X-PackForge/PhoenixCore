package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.conflux.client.ClientResearchCache;
import net.phoenix.core.integration.conflux.client.DisciplinePickerScreen;

/** What a click on a cinema screen with an action does, on the client. */
public final class CinemaClientActions {

    /** Opens the discipline picker (or says which path the player already follows). */
    public static final String OPEN_DISCIPLINE_PICKER = "phoenixcore:open_discipline_picker";

    private CinemaClientActions() {}

    public static void run(String action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (OPEN_DISCIPLINE_PICKER.equals(action)) {
            var info = ClientResearchCache.getDisciplineInfo();
            if (info.hasDiscipline()) {
                mc.player.displayClientMessage(Component.literal("§6[Axiom] §7You already follow §r" +
                        (info.disciplineTitle() != null ? info.disciplineTitle() : info.disciplineId()) +
                        "§7. Abandon it at a research terminal to choose another."), true);
                return;
            }
            mc.setScreen(new DisciplinePickerScreen());
        }
    }
}
