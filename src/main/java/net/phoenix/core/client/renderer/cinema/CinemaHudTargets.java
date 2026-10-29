package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.phoenix.continuum.client.screen.ContinuumMapScreen;
import net.phoenix.core.integration.conflux.client.ResearchTerminalScreen;

/**
 * What the buttons of the HUD bar open, for a cinema screen that explains them ({@code hud:solaris} and so on). The
 * screens of the mods that are not required are named only inside their own holder classes, so they are loaded only when
 * that mod is.
 */
final class CinemaHudTargets {

    private CinemaHudTargets() {}

    static void open(String name) {
        Minecraft mc = Minecraft.getInstance();
        switch (name) {
            case "conflux" -> mc.setScreen(new ResearchTerminalScreen());
            case "continuum" -> mc.setScreen(new ContinuumMapScreen());
            case "solaris" -> {
                if (loaded("solaris")) Solaris.open(mc);
                else missing(mc, "Solaris");
            }
            case "chronicles" -> {
                if (loaded("phoenix_chronicles")) Chronicles.open(mc);
                else missing(mc, "Phoenix Chronicles");
            }
            default -> mc.player.displayClientMessage(Component.literal("§c[Cinema] Unknown HUD button: " + name), true);
        }
    }

    private static boolean loaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }

    private static void missing(Minecraft mc, String mod) {
        mc.player.displayClientMessage(Component.literal("§7" + mod + " is not installed."), true);
    }

    private static final class Solaris {

        static void open(Minecraft mc) {
            net.minecraft.client.gui.screens.Screen current = mc.screen;
            mc.setScreen(null);
            net.phoenixvine.solaris.api.SolarisAPI.openMap(current);
        }
    }

    private static final class Chronicles {

        static void open(Minecraft mc) {
            mc.setScreen(new net.phoenixvine.chronicles.client.screen.ChronicleOverviewScreen(mc.screen));
        }
    }
}
