package net.phoenix.core.integration.conflux.dimension.physics;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientPhysicsHook {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // See PhysicsHook.onServerTick - must run at END, after vanilla's own gravity has
        // already been applied this tick, or our modifier gets immediately overwritten.
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null || mc.player == null) {
            return;
        }

        PhysicsHook.applyPhysicsToEntity(mc.player, level);

        level.getEntitiesOfClass(Entity.class, mc.player.getBoundingBox().inflate(32))
                .forEach(entity -> PhysicsHook.applyPhysicsToEntity(entity, level));
    }
}
