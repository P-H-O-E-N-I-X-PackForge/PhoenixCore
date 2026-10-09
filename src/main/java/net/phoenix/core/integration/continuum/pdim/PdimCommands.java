package net.phoenix.core.integration.continuum.pdim;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class PdimCommands {

    private PdimCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("pdim")
                .then(literal("create").then(argument("body", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                ContinuumData.allBodies().stream().map(ContinuumBody::id), builder))
                        .executes(ctx -> enter(ctx.getSource(), ResourceLocationArgument.getId(ctx, "body"), null))
                        .then(argument("gravity", FloatArgumentType.floatArg(PdimDimensions.MIN_GRAVITY,
                                PdimDimensions.MAX_GRAVITY))
                                .executes(ctx -> enter(ctx.getSource(), ResourceLocationArgument.getId(ctx, "body"),
                                        FloatArgumentType.getFloat(ctx, "gravity"))))))
                .then(literal("enter").then(argument("body", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                ContinuumData.allBodies().stream().map(ContinuumBody::id), builder))
                        .executes(ctx -> enter(ctx.getSource(), ResourceLocationArgument.getId(ctx, "body"), null))))
                .then(literal("leave").executes(ctx -> leave(ctx.getSource())))
                .then(literal("list").executes(ctx -> list(ctx.getSource())))
                .then(literal("delete").then(argument("body", ResourceLocationArgument.id())
                        .executes(ctx -> delete(ctx.getSource(), ResourceLocationArgument.getId(ctx, "body"))))));
    }

    private static int enter(CommandSourceStack source, ResourceLocation id, Float gravity)
                                                                                            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ContinuumBody body = ContinuumData.body(id);
        float g = gravity != null ? gravity : body != null ? PdimDimensions.naturalGravity(body) : 1.0f;
        String error = PdimActions.createAndEnter(player, id, g);
        if (error != null) {
            source.sendFailure(Component.literal(error));
            return 0;
        }
        return 1;
    }

    private static int leave(CommandSourceStack source) throws CommandSyntaxException {
        String error = PdimActions.leave(source.getPlayerOrException());
        if (error != null) {
            source.sendFailure(Component.literal(error));
            return 0;
        }
        return 1;
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var entries = PdimData.get(player.server).entries(player.getUUID());
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("You have no personal dimensions."), false);
            return 0;
        }
        entries.forEach((body, entry) -> source.sendSuccess(() -> Component.literal(
                body + "  gravity x" + String.format(Locale.ROOT, "%.2f", entry.gravity())), false));
        return entries.size();
    }

    private static int delete(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        boolean removed = PdimActions.delete(source.getPlayerOrException(), id);
        if (!removed) {
            source.sendFailure(Component.literal("You have no personal dimension on " + id + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Forgot your dimension on " + id + "."), false);
        return 1;
    }
}
