package net.phoenix.core.common.machine.multiblock.ward;


import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class SanctumWardEventHandler {

    private SanctumWardEventHandler() {}

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        Mob mob = event.getEntity();

        Level level = event.getLevel().getLevel();
        if (MobWardRegistry.isWarded(mob, level, event.getX(), event.getY(), event.getZ())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) return;

        Level level = event.getLevel();
        if (MobWardRegistry.isWarded(mob, level, entity.getX(), entity.getY(), entity.getZ())) {
            event.setCanceled(true);
        }
    }
}
