package net.phoenix.core.integration.continuum.client;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.continuum.client.screen.ContinuumAscentScreen;
import net.phoenix.core.integration.continuum.client.screen.ContinuumArrivalScreen;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.integration.continuum.data.ContinuumData;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /continuumpreview}: client-side commands (no pad, no operator, nothing sent to the server) for watching the
 * launch and landing scenes for any body, for testing and for showing them off.
 *
 * <ul>
 * <li>{@code /continuumpreview ascent <body>}</li>
 * <li>{@code /continuumpreview arrival <body> [success|fail]}</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class ContinuumPreviewCommands {

    private ContinuumPreviewCommands() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(literal("continuumpreview")
                .then(literal("ascent")
                        .then(argument("body", ResourceLocationArgument.id()).suggests((ctx, builder) -> {
                            ContinuumData.allBodies().forEach(b -> builder.suggest(b.id().toString()));
                            return builder.buildFuture();
                        }).executes(ctx -> {
                            ResourceLocation body = ResourceLocationArgument.getId(ctx, "body");
                            Minecraft.getInstance().tell(() -> Minecraft.getInstance()
                                    .setScreen(new ContinuumAscentScreen(body, null)));
                            return 1;
                        })))
                .then(literal("arrival")
                        .then(argument("body", ResourceLocationArgument.id()).suggests((ctx, builder) -> {
                            ContinuumData.allBodies().forEach(b -> builder.suggest(b.id().toString()));
                            return builder.buildFuture();
                        }).executes(ctx -> arrival(ResourceLocationArgument.getId(ctx, "body"), false))
                                .then(argument("outcome", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider
                                                .suggest(List.of("success", "fail"), builder))
                                        .executes(ctx -> arrival(ResourceLocationArgument.getId(ctx, "body"),
                                                "fail".equals(StringArgumentType.getString(ctx, "outcome"))))))));
    }

    private static int arrival(ResourceLocation body, boolean fail) {
        UUID id = UUID.randomUUID();
        long now = ContinuumClientState.now();
        ContinuumClientState.addPreview(new ContinuumStateSnapshot.MissionView(id, body, Mission.Type.EXTRACT, 3,
                now - 1000L, 1000L, fail ? Mission.State.FAILED : Mission.State.SUCCESS, 0.35f, "Preview", List.of(), 0L,
                false));
        Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new ContinuumArrivalScreen(id, null)));
        return 1;
    }
}
