package net.phoenix.core.integration.solaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.common.NeoForge;
import net.phoenix.core.api.OreSenseApi;
import net.phoenix.core.api.OreSenseEvent;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.conflux.dimension.DisciplineThemeRegistry;
import net.phoenix.core.integration.conflux.dimension.worldgen.DisciplineWorldgenConfig;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class OreSense {

    record Reading(int chunkX, int chunkZ, Block block, int count, int x, int y, int z, int minY, int maxY) {}

    private record ScanKey(ResourceKey<Level> dimension, int chunkX, int chunkZ) {}

    private record Scan(long gameTime, List<Reading> readings) {}

    private static final int RESCAN_TICKS = 3000;
    private static final int SCANS_PER_STEP = 2;
    private static final int MAX_CACHE = 4096;
    private static final int MAX_MARKERS = 128;

    private static final Map<ScanKey, Scan> CACHE = new HashMap<>();
    private static final Map<String, Set<Block>> ORE_BLOCKS = new HashMap<>();
    private static final Map<String, Object> ORE_SOURCE = new HashMap<>();

    private OreSense() {}

    static int tier(ServerPlayer player) {
        String discipline = net.phoenix.core.api.ConfluxApi.discipline(player);
        if (discipline == null) return 0;

        ResearchTree tree = null;
        for (ResearchTree candidate : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
            if (discipline.equals(candidate.discipline)) {
                tree = candidate;
                break;
            }
        }
        if (tree == null || tree.getNodes().isEmpty()) return 0;

        UUID team = ResearchTeamHelper.getTeamId(player);
        var unlocked = WorldResearchData.get(player.serverLevel()).getUnlocked(team);
        int done = 0;
        for (ResearchNode node : tree.getNodes()) {
            if (unlocked.contains(node.id)) done++;
        }
        if (done == 0) return 0;

        int percent = done * 100 / tree.getNodes().size();
        var config = PhoenixConfigs.INSTANCE.confluxSolaris;
        if (percent >= config.oreTier3Percent) return 3;
        if (percent >= config.oreTier2Percent) return 2;
        return 1;
    }

    static @Nullable String disciplineOf(ServerLevel level) {
        var location = level.dimension().location();
        if (!location.getNamespace().equals("phoenixcore") || !location.getPath().startsWith("conflux/")) return null;
        return location.getPath().substring("conflux/".length());
    }

    static void scanStep(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        String discipline = disciplineOf(level);
        if (discipline == null) return;
        Set<Block> ores = oresFor(discipline);
        if (ores.isEmpty()) return;

        if (CACHE.size() > MAX_CACHE) CACHE.clear();

        int radius = PhoenixConfigs.INSTANCE.confluxSolaris.oreSenseRadiusChunks;
        ChunkPos center = player.chunkPosition();
        long now = level.getGameTime();
        int scanned = 0;

        for (int ring = 0; ring <= radius && scanned < SCANS_PER_STEP; ring++) {
            for (int dx = -ring; dx <= ring && scanned < SCANS_PER_STEP; dx++) {
                for (int dz = -ring; dz <= ring && scanned < SCANS_PER_STEP; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int cx = center.x + dx;
                    int cz = center.z + dz;
                    ScanKey key = new ScanKey(level.dimension(), cx, cz);
                    Scan known = CACHE.get(key);
                    if (known != null && now - known.gameTime() < RESCAN_TICKS) continue;

                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    List<Reading> found = scan(chunk, ores);
                    CACHE.put(key, new Scan(now, found));
                    remember(player, level, discipline, cx, cz, found);
                    scanned++;
                }
            }
        }
    }

    private static boolean persistent(String discipline) {
        return OreSenseApi.getPersistence(discipline) == OreSenseApi.Persistence.PERSISTENT;
    }

    private static void remember(ServerPlayer player, ServerLevel level, String discipline, int cx, int cz,
                                 List<Reading> found) {
        if (!persistent(discipline) || tier(player) == 0) return;

        var config = PhoenixConfigs.INSTANCE.confluxSolaris;
        UUID team = ResearchTeamHelper.getTeamId(player);
        String dimension = level.dimension().location().toString();

        List<OreMemoryData.Memory> memories = new ArrayList<>();
        for (Reading reading : found) {
            if (reading.count() < config.oreSenseMinBlocks) continue;
            var event = new OreSenseEvent.Remember(level.getServer(), team, discipline, reading.block(), cx, cz,
                    reading.count());
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) continue;
            memories.add(new OreMemoryData.Memory(dimension, cx, cz,
                    BuiltInRegistries.BLOCK.getKey(reading.block()).toString(), reading.count(), reading.x(),
                    reading.y(), reading.z(), reading.minY(), reading.maxY()));
        }
        OreMemoryData.get(level).replaceChunk(team, dimension, cx, cz, memories);
    }

    static List<Reading> readings(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        var config = PhoenixConfigs.INSTANCE.confluxSolaris;
        String discipline = disciplineOf(level);
        if (discipline != null && persistent(discipline)) return remembered(player, level);
        int radius = config.oreSenseRadiusChunks;
        ChunkPos center = player.chunkPosition();

        List<Reading> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Scan scan = CACHE.get(new ScanKey(level.dimension(), center.x + dx, center.z + dz));
                if (scan == null) continue;
                for (Reading reading : scan.readings()) {
                    if (reading.count() >= config.oreSenseMinBlocks) out.add(reading);
                }
            }
        }
        out.sort(Comparator.comparingDouble(r -> player.distanceToSqr(r.x(), r.y(), r.z())));
        return out.size() > MAX_MARKERS ? out.subList(0, MAX_MARKERS) : out;
    }

    private static List<Reading> remembered(ServerPlayer player, ServerLevel level) {
        UUID team = ResearchTeamHelper.getTeamId(player);
        List<Reading> out = new ArrayList<>();
        for (OreMemoryData.Memory memory : OreMemoryData.get(level).forTeam(team,
                level.dimension().location().toString())) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(memory.block());
            if (id == null) continue;
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block == net.minecraft.world.level.block.Blocks.AIR) continue;
            out.add(new Reading(memory.chunkX(), memory.chunkZ(), block, memory.count(), memory.x(), memory.y(),
                    memory.z(), memory.minY(), memory.maxY()));
        }
        out.sort(Comparator.comparingDouble(r -> player.distanceToSqr(r.x(), r.y(), r.z())));
        return out.size() > MAX_MARKERS ? out.subList(0, MAX_MARKERS) : out;
    }

    private static Set<Block> oresFor(String discipline) {
        var theme = DisciplineThemeRegistry.getTheme(discipline);
        if (theme == null || theme.worldgenConfig == null) return Set.of();

        if (ORE_SOURCE.get(discipline) != theme) {
            Set<Block> blocks = new HashSet<>();
            for (DisciplineWorldgenConfig.OreConfig ore : theme.worldgenConfig.ores) {
                Block block = BuiltInRegistries.BLOCK.get(ore.oreBlock);
                if (block != net.minecraft.world.level.block.Blocks.AIR) blocks.add(block);
            }
            ORE_BLOCKS.put(discipline, blocks);
            ORE_SOURCE.put(discipline, theme);
        }
        return ORE_BLOCKS.getOrDefault(discipline, Set.of());
    }

    private static List<Reading> scan(LevelChunk chunk, Set<Block> ores) {
        class Tally {

            int count;
            long sumX;
            long sumY;
            long sumZ;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
        }

        Map<Block, Tally> tallies = new HashMap<>();
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(state -> ores.contains(state.getBlock()))) continue;

            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        Block block = section.getBlockState(x, y, z).getBlock();
                        if (!ores.contains(block)) continue;
                        Tally t = tallies.computeIfAbsent(block, b -> new Tally());
                        t.count++;
                        t.sumX += baseX + x;
                        t.sumY += baseY + y;
                        t.sumZ += baseZ + z;
                        t.minY = Math.min(t.minY, baseY + y);
                        t.maxY = Math.max(t.maxY, baseY + y);
                    }
                }
            }
        }

        List<Reading> readings = new ArrayList<>();
        for (Map.Entry<Block, Tally> entry : tallies.entrySet()) {
            Tally t = entry.getValue();
            readings.add(new Reading(chunk.getPos().x, chunk.getPos().z, entry.getKey(), t.count,
                    (int) (t.sumX / t.count), (int) (t.sumY / t.count), (int) (t.sumZ / t.count), t.minY, t.maxY));
        }
        return readings;
    }

    static BlockPos chunkCenter(Reading reading) {
        return new BlockPos(reading.chunkX() * 16 + 8, (reading.minY() + reading.maxY()) / 2,
                reading.chunkZ() * 16 + 8);
    }
}
