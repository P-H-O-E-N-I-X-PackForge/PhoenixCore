package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "phoenixcore")
public class PlayerSpawnHandler {

    private record Spawn(ResourceKey<Level> dimension, BlockPos pos, boolean forced) {}

    private static final Map<UUID, Spawn> AT_DEATH = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CompoundTag persistentData = serverPlayer.getPersistentData();
        boolean chosen = net.phoenix.core.integration.conflux.research.WorldResearchData
                .get(serverPlayer.serverLevel())
                .getDiscipline(
                        net.phoenix.core.integration.conflux.research.ResearchTeamHelper.getTeamId(serverPlayer)) !=
                null;
        if (!chosen && !persistentData.getBoolean("phoenixcore_visited_ethereal")) {

            EtherealSpawnManager.teleportToEtherealSpawn(serverPlayer);
            persistentData.putBoolean("phoenixcore_visited_ethereal", true);
        }
    }

    private static boolean isConflux(ResourceKey<Level> dimension) {
        return dimension.location().getNamespace().equals("phoenixcore") &&
                dimension.location().getPath().startsWith("conflux/");
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos pos = player.getRespawnPosition();
        if (pos != null && isConflux(player.getRespawnDimension())) {
            AT_DEATH.put(player.getUUID(), new Spawn(player.getRespawnDimension(), pos, player.isRespawnForced()));
        } else {
            AT_DEATH.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) return;
        Spawn spawn = AT_DEATH.remove(player.getUUID());
        if (spawn == null || player.level().dimension().equals(spawn.dimension())) return;

        ServerLevel level = player.getServer().getLevel(spawn.dimension());
        if (level == null) return;

        BlockState state = level.getBlockState(spawn.pos());
        BlockPos stand = null;
        if (state.getBlock() instanceof BedBlock) {
            stand = findFreeSpot(level, spawn.pos());
        } else if (spawn.forced()) {
            stand = findFreeSpot(level, spawn.pos());
        }
        if (stand == null) return;

        player.teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(),
                player.getXRot());
        player.setRespawnPosition(spawn.dimension(), spawn.pos(), 0f, spawn.forced(), false);
    }

    private static BlockPos findFreeSpot(ServerLevel level, BlockPos from) {
        for (int up = 0; up <= 4; up++) {
            BlockPos feet = from.above(up);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() &&
                    level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty() &&
                    !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) {
                return feet;
            }
        }
        return null;
    }
}
