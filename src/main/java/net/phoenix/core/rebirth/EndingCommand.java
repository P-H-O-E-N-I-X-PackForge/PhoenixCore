package net.phoenix.core.rebirth;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.phoenix.conflux.common.ending.Ending;
import net.phoenix.conflux.common.ending.EpilogueService;
import net.phoenix.core.PhoenixCore;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.List;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class EndingCommand {

    private static final int HALL_DELAY_TICKS = 30 * 20;

    private EndingCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var ending = Commands.argument("ending", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Ending.ALL.stream().map(Ending::id).toList(), builder));
        event.getDispatcher().register(Commands.literal("phoenixending").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("play").then(ending.executes(ctx -> run(ctx, true, true))))
                .then(Commands.literal("animatic").then(Commands.argument("ending", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                Ending.ALL.stream().map(Ending::id).toList(), builder))
                        .executes(ctx -> run(ctx, true, false))))
                .then(Commands.literal("hall").then(Commands.argument("ending", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                Ending.ALL.stream().map(Ending::id).toList(), builder))
                        .executes(ctx -> run(ctx, false, true))))
                .then(Commands.literal("leave").executes(ctx -> {
                    EpilogueService.leave(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static int list(CommandSourceStack source) {
        List<String> ids = Ending.ALL.stream().map(Ending::id).toList();
        source.sendSuccess(() -> Component.literal("Endings (" + ids.size() + "): " + String.join(", ", ids)), false);
        return ids.size();
    }

    private static int run(CommandContext<CommandSourceStack> ctx, boolean animatic, boolean hall)
                                                                                                   throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "ending");
        Ending chosen = Ending.ALL.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
        if (chosen == null) {
            ctx.getSource()
                    .sendFailure(Component.literal("There is no ending called " + id + ". Try /phoenixending list."));
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (animatic) RebirthReplay.play(player, chosen);
        if (hall) EpilogueService.scheduleArrival(player, animatic ? HALL_DELAY_TICKS : 1, chosen);
        ctx.getSource().sendSuccess(() -> Component.literal("Ending " + id + ": " + (animatic ? "animatic" : "") +
                (animatic && hall ? " then " : "") + (hall ? "hall" : "")), false);
        return 1;
    }
}
