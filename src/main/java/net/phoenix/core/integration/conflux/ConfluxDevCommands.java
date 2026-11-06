package net.phoenix.core.integration.conflux;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.phoenix.core.integration.conflux.network.ConfluxNetwork;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;
import net.phoenix.core.integration.conflux.terminal.ConfluxDataStore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.UUID;

@EventBusSubscriber(modid = "phoenixcore")
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
                        .then(Commands.literal("signature")
                                .then(Commands.literal("show").executes(c -> signatureShow(c.getSource())))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                net.phoenix.core.integration.conflux.signature.SignatureRegistry.INSTANCE
                                                        .all().stream().map(s -> s.id()).toList(),
                                                b))
                                        .then(Commands.literal("add").then(Commands.argument("amount",
                                                com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                                .executes(c -> signatureMove(c.getSource(),
                                                        StringArgumentType.getString(c, "id"),
                                                        com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c,
                                                                "amount"),
                                                        true))))
                                        .then(Commands.literal("set").then(Commands.argument("amount",
                                                com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                                .executes(c -> signatureMove(c.getSource(),
                                                        StringArgumentType.getString(c, "id"),
                                                        com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c,
                                                                "amount"),
                                                        false))))))
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

    private static int signatureShow(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }
        UUID team = ResearchTeamHelper.getTeamId(player);
        var data = net.phoenix.core.integration.conflux.signature.SignatureData.get(player.serverLevel());
        int shown = 0;
        for (var def : net.phoenix.core.integration.conflux.signature.SignatureRegistry.INSTANCE.all()) {
            double v = data.get(team, def);
            source.sendSuccess(() -> Component.literal(def.title() + " (" + def.discipline() + "): " +
                    String.format("%.1f", v) + " / " + (int) def.max() + " - " + def.levelName(v)), false);
            shown++;
        }
        return shown;
    }

    private static int signatureMove(CommandSourceStack source, String id, double amount, boolean relative) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }
        if (net.phoenix.core.integration.conflux.signature.SignatureRegistry.INSTANCE.get(id).isEmpty()) {
            source.sendFailure(Component.literal("Unknown signature: " + id));
            return 0;
        }
        UUID team = ResearchTeamHelper.getTeamId(player);
        if (relative)
            net.phoenix.core.integration.conflux.signature.Signatures.add(player.serverLevel(), team, id, amount);
        else net.phoenix.core.integration.conflux.signature.Signatures.set(player.serverLevel(), team, id, amount);
        net.phoenix.core.integration.conflux.signature.SignatureEvents.sendTo(player);
        refresh(player, team);
        source.sendSuccess(() -> Component.literal("Signature " + id + " is now " + String.format("%.1f",
                net.phoenix.core.integration.conflux.signature.Signatures.get(player.serverLevel(), team, id))), true);
        return 1;
    }

    private static void refresh(ServerPlayer player, UUID team) {
        ConfluxNetwork.syncResearchToPlayer(player);
        net.phoenix.core.api.event.ConfluxEvents.teamChanged(player.server, team);
    }
}
