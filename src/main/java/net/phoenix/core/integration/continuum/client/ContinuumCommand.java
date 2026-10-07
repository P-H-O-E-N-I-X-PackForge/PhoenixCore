package net.phoenix.core.integration.continuum.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.phoenix.core.integration.continuum.client.screen.ContinuumMapScreen;
import net.phoenix.core.integration.continuum.client.screen.ContinuumTestScreen;

import java.util.function.Supplier;

import static net.minecraft.commands.Commands.literal;

/**
 * {@code /continuum test} opens the milestone 1 renderer test bench; {@code /continuum map} opens the milestone 2
 * galaxy / system / body map.
 */
@OnlyIn(Dist.CLIENT)
public final class ContinuumCommand {

    private ContinuumCommand() {}

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                literal("continuum")
                        .then(literal("test").executes(ctx -> open(ContinuumTestScreen::new)))
                        .then(literal("map").executes(ctx -> open(ContinuumMapScreen::new))));
    }

    /**
     * The command runs mid-chat; open the screen on the next client tick so the chat screen closing does not replace
     * it.
     */
    private static int open(Supplier<Screen> screen) {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> mc.setScreen(screen.get()));
        return 1;
    }
}
