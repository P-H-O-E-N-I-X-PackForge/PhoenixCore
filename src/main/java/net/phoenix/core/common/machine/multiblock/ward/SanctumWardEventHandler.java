package net.phoenix.core.common.machine.multiblock.ward;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.phoenix.core.PhoenixCore;


@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SanctumWardEventHandler {

    private SanctumWardEventHandler() {}

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof Monster)) return;

        Level level = event.getLevel().getLevel();
        if (MobWardRegistry.isWarded(level, event.getX(), event.getY(), event.getZ())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
