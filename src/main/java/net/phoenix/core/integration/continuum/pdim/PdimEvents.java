package net.phoenix.core.integration.continuum.pdim;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class PdimEvents {

    private PdimEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level) || !PdimDimensions.isPersonal(level.dimension())) return;

        if (player.getY() < level.getMinBuildHeight() - 16) {
            Vec3 motion = player.getDeltaMovement();
            player.fallDistance = 0.0f;
            player.connection.teleport(player.getX(), level.getMaxBuildHeight() - 4.0, player.getZ(), player.getYRot(),
                    player.getXRot());
            player.setDeltaMovement(motion.x, Math.max(motion.y, -0.6), motion.z);
            player.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (PdimDimensions.isPersonal(event.getEntity().level().dimension())) event.setDamageMultiplier(0.0f);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PdimActions.sendState(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PdimActions.sendState(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PdimActions.sendState(player);
    }
}
