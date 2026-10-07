package net.phoenix.core.integration.continuum.client.render;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Consumer;

/** Core shaders used by Continuum's planet views. Hooked up from {@code PhoenixClient.init}. */
public final class ContinuumShaders {

    private ContinuumShaders() {}

    private static final Logger LOGGER = LogManager.getLogger();

    public static ShaderInstance PLANET;
    public static ShaderInstance ATMOSPHERE;
    public static ShaderInstance GLOW;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        register(event, "phoenixcore:continuum_planet", DefaultVertexFormat.POSITION, s -> PLANET = s);
        register(event, "phoenixcore:continuum_atmosphere", DefaultVertexFormat.POSITION, s -> ATMOSPHERE = s);
        register(event, "phoenixcore:continuum_glow", DefaultVertexFormat.POSITION_TEX_COLOR, s -> GLOW = s);
    }

    private static void register(RegisterShadersEvent event, String name, VertexFormat format,
                                 Consumer<ShaderInstance> onLoad) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), name, format), onLoad);
        } catch (IOException e) {
            LOGGER.error("[PhoenixCore/Continuum] Failed to register shader '{}': {}", name, e.getMessage());
        }
    }

    public static boolean ready() {
        return PLANET != null && ATMOSPHERE != null && GLOW != null;
    }
}
