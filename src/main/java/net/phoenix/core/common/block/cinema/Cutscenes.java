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

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class Cutscenes {

    private Cutscenes() {}

    public static Supplier<Collection<ResourceLocation>> knownCutscenes = List::of;

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CUTSCENES = SuggestionProviders.register(
            PhoenixCore.id("cutscenes"),
            (context, builder) -> SharedSuggestionProvider.suggestResource(knownCutscenes.get(), builder));

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
