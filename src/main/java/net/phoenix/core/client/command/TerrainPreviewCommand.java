package net.phoenix.core.client.command;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.phoenix.core.client.gui.screen.TerrainPreviewScreen;

import static net.minecraft.commands.Commands.literal;

@OnlyIn(Dist.CLIENT)
public final class TerrainPreviewCommand {

    private TerrainPreviewCommand() {}

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                literal("phoenixterrain")
                        .then(literal("preview")
                                .executes(ctx -> {
                                    Minecraft.getInstance().setScreen(new TerrainPreviewScreen());
                                    return 1;
                                })));
    }
}
