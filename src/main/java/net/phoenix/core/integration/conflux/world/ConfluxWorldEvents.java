package net.phoenix.core.integration.conflux.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.conflux.dimension.DisciplineChunkGenerator;
import net.phoenix.core.network.packet.S2CFamiliarityPacket;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ConfluxWorldEvents {

    private static final String SCULK = "conflux/sculk";
    private static final int WATCH_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 40;
    private static final int SYNC_RADIUS = 12;
    private static final int GROWTHS_PER_TICK = 2;
    private static final double EDGE_MARGIN = 0.12;

    private static final Map<UUID, String> LAST_REGION = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SYNC = new HashMap<>();
    private static final ArrayDeque<long[]> GROWTH = new ArrayDeque<>();

    private ConfluxWorldEvents() {}

    private static String disciplineOf(ServerLevel level) {
        if (!"phoenixcore".equals(level.dimension().location().getNamespace())) return null;
        String path = level.dimension().location().getPath();
        return path.startsWith("conflux/") ? path.substring("conflux/".length()) : null;
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String from = event.getFrom().location().getPath();
        String to = event.getTo().location().getPath();
        LAST_REGION.remove(player.getUUID());
        LAST_SYNC.remove(player.getUUID());
        if (from.startsWith("conflux/") && "phoenixcore".equals(event.getFrom().location().getNamespace())) {
            ConfluxWorld.fire(player, ConfluxWorld.DISCIPLINE_LEFT,
                    ConfluxWorld.data("discipline", from.substring("conflux/".length())));
        }
        if (to.startsWith("conflux/") && "phoenixcore".equals(event.getTo().location().getNamespace())) {
            ConfluxWorld.fire(player, ConfluxWorld.DISCIPLINE_ENTERED,
                    ConfluxWorld.data("discipline", to.substring("conflux/".length())));
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_REGION.remove(event.getEntity().getUUID());
        LAST_SYNC.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % WATCH_INTERVAL != 0) return;
        ServerLevel level = player.serverLevel();
        String discipline = disciplineOf(level);
        if (discipline == null) return;

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (generator instanceof DisciplineChunkGenerator gen) {
            watchRegion(player, level, gen, discipline);
        }
        if (discipline.equals("sculk") && PhoenixConfigs.INSTANCE.confluxSolaris.familiarityGrowth) {
            learn(player, level);
            if (player.tickCount % SYNC_INTERVAL == 0) sync(player, level);
        }
    }

    private static void watchRegion(ServerPlayer player, ServerLevel level, DisciplineChunkGenerator gen,
                                    String discipline) {
        var randomState = level.getChunkSource().randomState();
        int x = player.getBlockX();
        int z = player.getBlockZ();

        String region = gen.regionAt(randomState, x, z);
        if (region != null) {
            String last = LAST_REGION.put(player.getUUID(), region);
            if (!region.equals(last)) {
                ConfluxWorld.fire(player, ConfluxWorld.REGION_ENTERED,
                        ConfluxWorld.data("region", region, "discipline", discipline));
            }
        }

        double margin = gen.chasmMargin(randomState, x, z);
        if (!Double.isNaN(margin) && margin >= 0.0 && margin < EDGE_MARGIN) {
            ConfluxWorld.fire(player, ConfluxWorld.VOID_EDGE, ConfluxWorld.data("margin", margin));
        }
    }

    private static void learn(ServerPlayer player, ServerLevel level) {
        SculkFamiliarity data = SculkFamiliarity.get(level);
        ChunkPos here = player.chunkPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos pos = new ChunkPos(here.x + dx, here.z + dz);
                int[] levels = data.add(pos, dx == 0 && dz == 0 ? 2 : 1);
                if (levels[1] > levels[0]) {
                    ConfluxWorld.fire(player, ConfluxWorld.FAMILIARITY_UP, ConfluxWorld.data("level", levels[1],
                            "chunk_x", pos.x, "chunk_z", pos.z, "known_chunks", data.countAtLeast(1)));
                }
                if (data.level(pos) > data.grown(pos)) GROWTH.add(new long[] { pos.toLong() });
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SCULK.equals(pathOf(level))) return;
        if (GROWTH.isEmpty() || !PhoenixConfigs.INSTANCE.confluxSolaris.familiarityGrowth) return;
        SculkFamiliarity data = SculkFamiliarity.get(level);
        for (int i = 0; i < GROWTHS_PER_TICK && !GROWTH.isEmpty(); i++) {
            ChunkPos pos = new ChunkPos(GROWTH.poll()[0]);
            int target = data.level(pos);
            int grown = data.grown(pos);
            if (target <= grown) continue;
            if (level.getChunkSource().getChunkNow(pos.x, pos.z) == null) continue;
            SculkGrowth.grow(level, pos, grown, target);
            data.setGrown(pos, target);
        }
    }

    private static String pathOf(ServerLevel level) {
        return "phoenixcore".equals(level.dimension().location().getNamespace()) ?
                level.dimension().location().getPath() : "";
    }

    private static void sync(ServerPlayer player, ServerLevel level) {
        SculkFamiliarity data = SculkFamiliarity.get(level);
        ChunkPos here = player.chunkPosition();
        int size = SYNC_RADIUS * 2 + 1;
        byte[] levels = new byte[size * size];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = (byte) data
                    .level(new ChunkPos(here.x - SYNC_RADIUS + i % size, here.z - SYNC_RADIUS + i / size));
        }
        int hash = Arrays.hashCode(levels) * 31 + here.hashCode();
        Integer last = LAST_SYNC.put(player.getUUID(), hash);
        if (last != null && last == hash) return;
        PacketDistributor.sendToPlayer(player, new S2CFamiliarityPacket(level.dimension().location().toString(),
                here.x, here.z, SYNC_RADIUS, levels));
    }
}
