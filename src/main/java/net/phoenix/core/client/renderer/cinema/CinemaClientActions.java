package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.client.renderer.cinema.cutscene.CutsceneManager;
import net.phoenix.core.integration.conflux.client.ClientResearchCache;
import net.phoenix.core.integration.conflux.client.DisciplinePickerScreen;
import net.phoenix.core.network.packet.C2SCinemaActionPacket;

/**
 * What a click on a cinema screen can do, on the client. A button's action (and a screen's own action) is one of:
 * <ul>
 * <li>{@code next}, {@code prev}: turn the page; {@code goto:N}: go to page N (screens whose pages are local);</li>
 * <li>{@code open_discipline_picker}: open the discipline picker (or say which path the player follows);</li>
 * <li>{@code cutscene:<id>}: play a cutscene, e.g. {@code cutscene:phoenixcore:dream}.</li>
 * </ul>
 */
public final class CinemaClientActions {

    /** Opens the discipline picker (or says which path the player already follows). */
    public static final String OPEN_DISCIPLINE_PICKER = "phoenixcore:open_discipline_picker";

    private CinemaClientActions() {}

    /** An action with no screen behind it (the screen's own action). */
    public static void run(String action) {
        run(action, null);
    }

    public static void run(String action, CinemaClientState.Target target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || action == null) return;
        String a = action.trim();
        switch (a) {
            case "next" -> turn(target, 1);
            case "prev" -> turn(target, -1);
            case "open_discipline_picker", OPEN_DISCIPLINE_PICKER -> openPicker(mc);
            default -> {
                if (a.startsWith("goto:") && target != null && target.centre().isLocalPages()) {
                    try {
                        CinemaClientState.setPage(target, Integer.parseInt(a.substring(5).trim()) - 1);
                    } catch (NumberFormatException ignored) {}
                } else if (a.startsWith("cutscene:")) {
                    ResourceLocation id = ResourceLocation.tryParse(a.substring(9).trim());
                    if (id != null) CutsceneManager.play(id);
                } else {
                    mc.player.displayClientMessage(Component.literal("§c[Cinema] Unknown button action: " + a),
                            true);
                }
            }
        }
    }

    private static void turn(CinemaClientState.Target target, int by) {
        if (target == null) return;
        if (target.centre().isLocalPages()) {
            CinemaClientState.setPage(target, CinemaClientState.pageIndex(target) + by);
        } else {
            PacketDistributor.sendToServer(new C2SCinemaActionPacket(target.pos(),
                    by > 0 ? C2SCinemaActionPacket.Action.NEXT_LINE : C2SCinemaActionPacket.Action.PREVIOUS_LINE));
        }
    }

    private static void openPicker(Minecraft mc) {
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
