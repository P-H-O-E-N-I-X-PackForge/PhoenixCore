package net.phoenix.core.integration.conflux.world;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

@EventBusSubscriber(modid = "phoenixcore")
public final class ConfluxWorldCommands {

    private ConfluxWorldCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("conflux").requires(s -> s.hasPermission(2))
                .then(Commands.literal("world")
                        .then(Commands.literal("fire")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(c -> fire(c.getSource(), StringArgumentType.getString(c, "type")))))
                        .then(Commands.literal("rules").executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal(
                                    HookRules.INSTANCE.all().size() + " world hook rule(s) loaded"), false);
                            return HookRules.INSTANCE.all().size();
                        }))
                        .then(Commands.literal("familiarity")
                                .then(Commands.argument("points", IntegerArgumentType.integer(0, 100000))
                                        .executes(c -> familiarity(c.getSource(),
                                                IntegerArgumentType.getInteger(c, "points")))))));
    }

    private static int fire(CommandSourceStack source, String type) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ConfluxWorld.fire(player, type, ConfluxWorld.data("source", "command"));
        source.sendSuccess(() -> Component.literal("Fired world hook '" + type + "'"), false);
        return 1;
    }

    private static int familiarity(CommandSourceStack source, int points) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        SculkFamiliarity data = SculkFamiliarity.get(player.serverLevel());
        int before = data.level(player.chunkPosition());
        data.setPoints(player.chunkPosition(), points);
        int after = data.level(player.chunkPosition());
        if (after > before) {
            ConfluxWorld.fire(player, ConfluxWorld.FAMILIARITY_UP, ConfluxWorld.data("level", after, "chunk_x",
                    player.chunkPosition().x, "chunk_z", player.chunkPosition().z, "known_chunks",
                    data.countAtLeast(1)));
        }
        source.sendSuccess(
                () -> Component.literal("This chunk now has " + points + " familiarity (level " + after + ")"),
                false);
        return after;
    }
}
