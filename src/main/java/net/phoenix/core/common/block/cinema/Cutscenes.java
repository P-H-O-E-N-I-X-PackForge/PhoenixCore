package net.phoenix.core.common.block.cinema;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.S2CPlayCutscenePacket;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Server-side entry point for cutscenes. Cutscene definitions live client-side in
 * {@code assets/<namespace>/cutscenes/}; the server only sends the id to play.
 *
 * <p>
 * Trigger from code with {@link #play(ServerPlayer, ResourceLocation)}, or in-game with
 * {@code /cutscene open <id>} (anyone, self only) or {@code /cutscene play <targets> <id>} (ops).
 * {@code /cutscene reset <targets>} (ops) forgets which once-only choices a player has made.
 */
@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class Cutscenes {

    private Cutscenes() {}

    /** Cutscene ids for autocomplete. The client points this at its loaded cutscenes; empty on a dedicated server. */
    public static Supplier<Collection<ResourceLocation>> knownCutscenes = List::of;

    // Like vanilla's sound suggestions, this is resolved on the player's own client, where the cutscene files live.
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CUTSCENES = SuggestionProviders.register(
            PhoenixCore.id("cutscenes"),
            (context, builder) -> SharedSuggestionProvider.suggestResource(knownCutscenes.get(), builder));

    // The cutscene each player was last sent. Cutscene actions are only accepted for this one.
    private static final Map<UUID, ResourceLocation> SESSIONS = new ConcurrentHashMap<>();

    public static void play(ServerPlayer player, ResourceLocation id) {
        SESSIONS.put(player.getUUID(), id);
        PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPlayCutscenePacket(id));
    }

    public static @Nullable ResourceLocation activeCutscene(ServerPlayer player) {
        return SESSIONS.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cutscene")
                // No permission needed: only ever opens on the player running it, so quest command rewards
                // and non-op players can use it.
                .then(Commands.literal("open")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests(SUGGEST_CUTSCENES)
                                .executes(context -> {
                                    play(context.getSource().getPlayerOrException(),
                                            ResourceLocationArgument.getId(context, "id"));
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    int cleared = targets.stream().mapToInt(CutsceneActions::reset).sum();
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Cleared " + cleared + " used cutscene action(s) for " + targets.size() +
                                                    " player(s)"),
                                            true);
                                    return targets.size();
                                })))
                .then(Commands.literal("play")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(SUGGEST_CUTSCENES)
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context,
                                                    "targets");
                                            ResourceLocation id = ResourceLocationArgument.getId(context, "id");
                                            targets.forEach(player -> play(player, id));
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    "Playing cutscene " + id + " for " + targets.size() +
                                                            " player(s)"),
                                                    true);
                                            return targets.size();
                                        })))));
    }
}
