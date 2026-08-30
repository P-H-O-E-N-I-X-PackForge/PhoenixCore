package net.phoenix.core.integration.conflux.dimension.shaders;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.phoenix.core.PhoenixCore;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetupHandler {

    private static boolean shadersInitialized = false;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> tryInitializeShaders("PhoenixCore shaders initialized successfully"));
    }

    static void tryInitializeShaders(String successMessage) {
        if (shadersInitialized) return;

        try {
            Minecraft mc = Minecraft.getInstance();
            ShaderManager manager = ShaderManager.getInstance();

            if (mc.getResourceManager() != null) {
                manager.init(mc.getResourceManager());
                shadersInitialized = true;
                System.out.println(successMessage);
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize PhoenixCore shaders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean areShadersInitialized() {
        return shadersInitialized;
    }

    public static void resetShaderInitialization() {
        shadersInitialized = false;
        ShaderManager.getInstance().cleanup();
    }
}
