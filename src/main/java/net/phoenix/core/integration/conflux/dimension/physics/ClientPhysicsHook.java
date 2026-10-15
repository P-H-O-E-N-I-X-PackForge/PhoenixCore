package net.phoenix.core.integration.conflux.dimension.physics;


import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = "phoenixcore", value = Dist.CLIENT)
public class ClientPhysicsHook {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null || mc.player == null) {
            return;
        }

        if (PhysicsRegistry.getInstance().getTotalGravityZones() == 0 &&
                PhysicsRegistry.getInstance().getTotalPlatforms() == 0) {
            return;
        }

        PhysicsHook.applyPhysicsToEntity(mc.player, level);

        level.getEntitiesOfClass(Entity.class, mc.player.getBoundingBox().inflate(32))
                .forEach(entity -> PhysicsHook.applyPhysicsToEntity(entity, level));
    }
}
