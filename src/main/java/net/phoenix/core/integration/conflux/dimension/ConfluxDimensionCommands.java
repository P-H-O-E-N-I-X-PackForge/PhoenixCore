package net.phoenix.core.integration.conflux.dimension;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public class ConfluxDimensionCommands {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        registerDimensionCommands(event.getDispatcher());
    }

    private static void registerDimensionCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("conflux")
                .then(Commands.literal("dimension")
                    .then(Commands.literal("return")
                        .executes(ctx -> returnToEtherealSpawn(ctx.getSource())))
                    .then(Commands.literal("info")
                        .executes(ctx -> dimensionInfo(ctx.getSource())))
                    .then(Commands.literal("benchmarkworldgen")
                        .then(Commands.argument("discipline", StringArgumentType.word())
                            .executes(ctx -> benchmarkWorldgen(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "discipline"), 8))
                            .then(Commands.argument("gridSize", IntegerArgumentType.integer(1, 16))
                                .executes(ctx -> benchmarkWorldgen(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "discipline"),
                                        IntegerArgumentType.getInteger(ctx, "gridSize"))))))
                )
        );
    }

    private static int benchmarkWorldgen(CommandSourceStack source, String discipline, int gridSize) {
        MinecraftServer server = source.getServer();
        ServerLevel target = server.getLevel(ConfluxDimensionFactory.getDimensionKey(discipline));
        if (target == null) {
            source.sendFailure(Component.literal("Unknown discipline dimension: " + discipline));
            return 0;
        }

        int totalChunks = gridSize * gridSize;
        int baseChunkX = 100_000 + (int) ((System.nanoTime() / 1_000L) % 50_000L);
        int baseChunkZ = 100_000 + (int) ((System.nanoTime() / 7_919L) % 50_000L);

        source.sendSuccess(() -> Component.literal("§6[PhoenixCore] Benchmarking " + totalChunks
                + " chunks in '" + discipline + "' - this will freeze the server briefly..."), false);

        DisciplineChunkGenerator.resetProfiling();

        long startNanos = System.nanoTime();
        for (int dx = 0; dx < gridSize; dx++) {
            for (int dz = 0; dz < gridSize; dz++) {
                target.getChunkSource().getChunk(baseChunkX + dx, baseChunkZ + dz, ChunkStatus.FULL, true);
            }
        }
        long elapsedNanos = System.nanoTime() - startNanos;

        double totalMs = elapsedNanos / 1_000_000.0;
        double avgMs = totalMs / totalChunks;
        double chunksPerSec = 1000.0 / avgMs;
        String profileSummary = DisciplineChunkGenerator.getProfilingSummary();

        source.sendSuccess(() -> Component.literal(String.format(
                "§6[PhoenixCore] '%s' worldgen: §r%d chunks in %.1fms total, %.2fms/chunk avg, %.1f chunks/sec",
                discipline, totalChunks, totalMs, avgMs, chunksPerSec)), false);
        source.sendSuccess(() -> Component.literal("§6[PhoenixCore] Breakdown (totals across all chunks): §r"
                + profileSummary), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int returnToEtherealSpawn(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        EtherealSpawnManager.teleportToEtherealSpawn(player);

        source.sendSuccess(
                () -> Component.literal("§6[Axiom] §7Returning to the ethereal spawn..."),
                false);

        return Command.SINGLE_SUCCESS;
    }

    private static int dimensionInfo(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        ServerLevel overworld = player.getServer().overworld();
        WorldResearchData researchData = WorldResearchData.get(overworld);

        UUID teamId = ResearchTeamHelper.getTeamId(player);
        if (teamId == null) {
            source.sendFailure(Component.literal("You are not part of a team"));
            return 0;
        }

        String discipline = researchData.getDiscipline(teamId);
        boolean committed = researchData.isCommitted(teamId);

        source.sendSuccess(
                () ->   Component.literal("§6=== Discipline Info ==="),
            false);
        source.sendSuccess(
                () ->  Component.literal("§7Discipline: §r" + (discipline != null ? discipline : "None")),
            false);
        source.sendSuccess(
                () -> Component.literal("§7Status: §r" + (committed ? "§aCommitted" : "§cUncommitted")),
            false);

        return Command.SINGLE_SUCCESS;
    }
}
