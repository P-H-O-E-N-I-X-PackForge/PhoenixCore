package net.phoenix.core.integration.conflux;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.conflux.network.ConfluxNetwork;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.integration.conflux.terminal.ConfluxDataStore;
import net.phoenix.core.integration.continuum.common.ContinuumServerEvents;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public final class ConfluxDevCommands {

    private ConfluxDevCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("conflux").requires(s -> s.hasPermission(2))
                .then(Commands.literal("dev")
                        .then(Commands.literal("data")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            java.util.List<String> names = new java.util.ArrayList<>();
                                            names.add("all");
                                            for (ConfluxDataType t : ConfluxDataType.values()) names.add(t.id());
                                            return SharedSuggestionProvider.suggest(names, b);
                                        })
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(c -> data(c.getSource(),
                                                        StringArgumentType.getString(c, "type"),
                                                        LongArgumentType.getLong(c, "amount"))))))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("node", StringArgumentType.greedyString())
                                        .suggests((c, b) -> {
                                            java.util.List<String> ids = new java.util.ArrayList<>();
                                            ids.add("all");
                                            ResearchTreeRegistry.INSTANCE.getAllNodes()
                                                    .forEach(n -> ids.add(n.id.toString()));
                                            return SharedSuggestionProvider.suggest(ids, b);
                                        })
                                        .executes(c -> unlock(c.getSource(),
                                                StringArgumentType.getString(c, "node")))))
                        .then(Commands.literal("reset").executes(c -> reset(c.getSource())))));
    }

    private static int data(CommandSourceStack source, String type, long amount) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }
        boolean known = type.equals("all");
        for (ConfluxDataType t : ConfluxDataType.values()) known |= type.equalsIgnoreCase(t.id());
        if (!known) {
            source.sendFailure(Component.literal("Unknown data type: " + type));
            return 0;
        }

        UUID team = ResearchTeamHelper.getTeamId(player);
        ConfluxDataStore store = ConfluxDataStore.get(player.serverLevel());
        long total = 0;
        for (ConfluxDataType t : ConfluxDataType.values()) {
            if (type.equals("all") || type.equalsIgnoreCase(t.id())) total += store.insert(team, t, amount);
        }
        refresh(player, team);
        long added = total;
        source.sendSuccess(() -> Component.literal("Added " + added + " Conflux data to your team's pool."), true);
        return 1;
    }

    private static int unlock(CommandSourceStack source, String nodeArg) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }
        var level = player.serverLevel();
        UUID team = ResearchTeamHelper.getTeamId(player);
        WorldResearchData research = WorldResearchData.get(level);

        int count = 0;
        if (nodeArg.equals("all")) {
            for (ResearchNode n : ResearchTreeRegistry.INSTANCE.getAllNodes()) {
                if (!research.isUnlocked(team, n.id)) {
                    research.devUnlock(team, n, ResearchTreeRegistry.INSTANCE, level);
                    count++;
                }
            }
        } else {
            ResourceLocation id = ResourceLocation.tryParse(nodeArg.trim());
            ResearchNode node = id == null ? null : ResearchTreeRegistry.INSTANCE.getNode(id).orElse(null);
            if (node == null) {
                source.sendFailure(Component.literal("Unknown research node: " + nodeArg));
                return 0;
            }
            if (!research.isUnlocked(team, node.id)) {
                research.devUnlock(team, node, ResearchTreeRegistry.INSTANCE, level);
                count = 1;
            }
        }
        refresh(player, team);
        int unlocked = count;
        source.sendSuccess(() -> Component.literal("Unlocked " + unlocked + " research node(s)."), true);
        return unlocked;
    }

    private static int reset(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }
        UUID team = ResearchTeamHelper.getTeamId(player);
        WorldResearchData.get(player.serverLevel()).devReset(team);
        refresh(player, team);
        source.sendSuccess(() -> Component.literal("Research reset for your team."), true);
        return 1;
    }

    private static void refresh(ServerPlayer player, UUID team) {
        ConfluxNetwork.syncResearchToPlayer(player);
        ContinuumServerEvents.sendStateToTeam(player.server, team);
    }
}
