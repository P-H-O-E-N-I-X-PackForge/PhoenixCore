package net.phoenix.core.integration.conflux.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ConfluxSpawns extends SimpleJsonResourceReloadListener {

    public static final ConfluxSpawns INSTANCE = new ConfluxSpawns();
    private static final String PREFIX = "conflux/";

    public record Entry(ResourceLocation entity, int weight, int cap, boolean air) {}

    private Map<String, List<Entry>> byDiscipline = Map.of();

    private ConfluxSpawns() {
        super(new Gson(), "conflux/spawns");
    }

    @SubscribeEvent
    public static void addListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, List<Entry>> loaded = new HashMap<>();
        files.forEach((id, element) -> {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
                String discipline = GsonHelper.getAsString(json, "discipline");
                for (JsonElement spawn : GsonHelper.getAsJsonArray(json, "spawns")) {
                    JsonObject s = GsonHelper.convertToJsonObject(spawn, "spawn");
                    loaded.computeIfAbsent(discipline, d -> new ArrayList<>()).add(new Entry(
                            new ResourceLocation(GsonHelper.getAsString(s, "entity")),
                            Math.max(1, GsonHelper.getAsInt(s, "weight", 1)),
                            Math.max(1, GsonHelper.getAsInt(s, "cap", 2)),
                            "air".equals(GsonHelper.getAsString(s, "placement", "ground"))));
                }
            } catch (RuntimeException e) {
                PhoenixCore.LOGGER.error("[Conflux] Bad spawn list {}: {}", id, e.getMessage());
            }
        });
        byDiscipline = loaded;
        PhoenixCore.LOGGER.info("[Conflux] Loaded spawn lists for {} discipline(s)", loaded.size());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var cfg = PhoenixConfigs.INSTANCE.confluxSpawns;
        if (!cfg.enabled || event.getServer().getTickCount() % cfg.intervalTicks != 0) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) continue;
            ResourceLocation dimension = level.dimension().location();
            if (!PhoenixCore.MOD_ID.equals(dimension.getNamespace()) || !dimension.getPath().startsWith(PREFIX)) {
                continue;
            }
            List<Entry> entries = INSTANCE.byDiscipline.get(dimension.getPath().substring(PREFIX.length()));
            if (entries != null && !entries.isEmpty()) trySpawn(level, player, entries, cfg);
        }
    }

    private static void trySpawn(ServerLevel level, ServerPlayer player, List<Entry> entries,
                                 PhoenixConfigs.ConfluxSpawnConfigs cfg) {
        RandomSource random = level.getRandom();
        if (random.nextDouble() >= cfg.chance) return;

        List<Entry> open = new ArrayList<>();
        List<EntityType<?>> types = new ArrayList<>();
        AABB area = player.getBoundingBox().inflate(cfg.countRadius);
        int total = 0;
        for (Entry entry : entries) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entry.entity());
            if (type == null || !ForgeRegistries.ENTITY_TYPES.containsKey(entry.entity())) continue;
            long near = level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area,
                    e -> e.getType() == type).size();
            if (near >= entry.cap()) continue;
            open.add(entry);
            types.add(type);
            total += entry.weight();
        }
        if (open.isEmpty()) return;

        int pick = random.nextInt(total);
        int chosen = 0;
        for (int i = 0; i < open.size(); i++) {
            pick -= open.get(i).weight();
            if (pick < 0) {
                chosen = i;
                break;
            }
        }
        Entry entry = open.get(chosen);
        EntityType<?> type = types.get(chosen);

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 24.0 + random.nextDouble() * 32.0;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.isLoaded(new BlockPos(x, level.getMinBuildHeight() + 1, z))) continue;

            BlockPos at;
            if (entry.air()) {
                at = new BlockPos(x, (int) player.getY() + random.nextInt(14) - 2, z);
            } else {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                at = new BlockPos(x, y, z);
                if (!level.getBlockState(at.below()).isSolid()) continue;
            }
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;

            var entity = type.create(level);
            if (entity == null) return;
            entity.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null, null);
            }
            level.addFreshEntity(entity);
            return;
        }
    }
}
