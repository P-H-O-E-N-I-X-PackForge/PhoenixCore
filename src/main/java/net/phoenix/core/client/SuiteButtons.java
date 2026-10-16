package net.phoenix.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.client.ResearchTerminalScreen;
import net.phoenix.core.integration.continuum.client.screen.ContinuumMapScreen;
import net.phoenixvine.wiki.client.suite.SuiteHudBar;

import static net.phoenixvine.wiki.client.suite.SuiteHudBar.PRIORITY_CONFLUX;
import static net.phoenixvine.wiki.client.suite.SuiteHudBar.PRIORITY_CONTINUUM;

public final class SuiteButtons {

    private SuiteButtons() {}

    public static void register() {
        Minecraft mc = Minecraft.getInstance();

        SuiteHudBar.register(
                PhoenixCore.MOD_ID + "_conflux",
                PRIORITY_CONFLUX,
                ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "textures/gui/suite_conflux.png"),
                () -> Component.literal("\u00a7bConflux Research \u00a77(view)"),
                () -> 1,
                () -> mc.setScreen(new ResearchTerminalScreen()),
                16, 16, false);

        SuiteHudBar.register(
                PhoenixCore.MOD_ID + "_continuum",
                PRIORITY_CONTINUUM,
                ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "textures/gui/suite_continuum.png"),
                () -> Component.literal("\u00a7dContinuum Map \u00a77(view)"),
                () -> 1,
                () -> mc.setScreen(new ContinuumMapScreen()),
                16, 16, false);
    }
}
