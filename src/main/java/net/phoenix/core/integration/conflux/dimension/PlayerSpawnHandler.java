package net.phoenix.core.integration.conflux.dimension;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public class PlayerSpawnHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CompoundTag persistentData = serverPlayer.getPersistentData();
        if (!persistentData.getBoolean("phoenixcore_visited_ethereal")) {
            
            EtherealSpawnManager.teleportToEtherealSpawn(serverPlayer);
            persistentData.putBoolean("phoenixcore_visited_ethereal", true);
        }
    }
}
