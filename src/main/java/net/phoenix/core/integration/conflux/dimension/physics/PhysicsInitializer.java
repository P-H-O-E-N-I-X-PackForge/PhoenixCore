package net.phoenix.core.integration.conflux.dimension.physics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.MOD)
public class PhysicsInitializer {

    private static boolean physicsInitialized = false;

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                
                DimensionPhysicsPresets.initializeDimensionPhysics("phoenix");
                DimensionPhysicsPresets.initializeDimensionPhysics("sculk");
                DimensionPhysicsPresets.initializeDimensionPhysics("void");
                DimensionPhysicsPresets.initializeDimensionPhysics("sealed_a");
                DimensionPhysicsPresets.initializeDimensionPhysics("sealed_b");

                physicsInitialized = true;
                System.out.println("[PhoenixCore] Physics system initialized with all presets");

                PhysicsRegistry.getInstance().logStatistics();
            } catch (Exception e) {
                System.err.println("[PhoenixCore] Failed to initialize physics: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static boolean isInitialized() {
        return physicsInitialized;
    }

    public static void reset() {
        PhysicsRegistry.getInstance().clearAll();
        physicsInitialized = false;

        DimensionPhysicsPresets.initializeDimensionPhysics("phoenix");
        DimensionPhysicsPresets.initializeDimensionPhysics("sculk");
        DimensionPhysicsPresets.initializeDimensionPhysics("void");
        DimensionPhysicsPresets.initializeDimensionPhysics("sealed_a");
        DimensionPhysicsPresets.initializeDimensionPhysics("sealed_b");

        physicsInitialized = true;
        System.out.println("[PhoenixCore] Physics system reset and reinitialized");
    }
}
