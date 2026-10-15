package net.phoenix.core.integration.conflux.dimension.physics;


import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = "phoenixcore", bus = EventBusSubscriber.Bus.MOD)
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
