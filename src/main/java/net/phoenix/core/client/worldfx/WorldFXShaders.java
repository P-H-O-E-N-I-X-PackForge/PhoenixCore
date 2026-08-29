package net.phoenix.core.client.worldfx;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class WorldFXShaders {

    private WorldFXShaders() {}

    private static final Logger LOGGER = LogManager.getLogger();

    public static ShaderInstance BLACK_HOLE;

    public static ShaderInstance NEBULA;

    public static ShaderInstance ATMOSPHERE_GRADE;

    public static ShaderInstance VOID_GALAXY;

    public static ShaderInstance PHOENIX_SUNFLARE;

    public static ShaderInstance SCULK_ABYSS;

    public static ShaderInstance SEALED_A_INDUSTRIAL;

    public static ShaderInstance SEALED_B_CHAOS;

    public static ShaderInstance VOID_BLACK_HOLE;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        register(event, "phoenixcore:phoenix_black_hole", DefaultVertexFormat.POSITION, s -> BLACK_HOLE = s);
        register(event, "phoenixcore:phoenix_nebula", DefaultVertexFormat.POSITION, s -> NEBULA = s);
        register(event, "phoenixcore:phoenix_atmosphere_grade", DefaultVertexFormat.POSITION,
                s -> ATMOSPHERE_GRADE = s);
        register(event, "phoenixcore:void_galaxy", DefaultVertexFormat.POSITION, s -> VOID_GALAXY = s);
        register(event, "phoenixcore:phoenix_sunflare", DefaultVertexFormat.POSITION, s -> PHOENIX_SUNFLARE = s);
        register(event, "phoenixcore:sculk_abyss", DefaultVertexFormat.POSITION, s -> SCULK_ABYSS = s);
        register(event, "phoenixcore:sealed_a_industrial", DefaultVertexFormat.POSITION, s -> SEALED_A_INDUSTRIAL = s);
        register(event, "phoenixcore:sealed_b_chaos", DefaultVertexFormat.POSITION, s -> SEALED_B_CHAOS = s);
        register(event, "phoenixcore:void_black_hole", DefaultVertexFormat.POSITION, s -> VOID_BLACK_HOLE = s);
    }

    private static void register(RegisterShadersEvent event,
                                 String name,
                                 com.mojang.blaze3d.vertex.VertexFormat format,
                                 java.util.function.Consumer<ShaderInstance> onLoad) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), name, format),
                    onLoad);
        } catch (IOException e) {
            LOGGER.error("[PhoenixCore/WorldFX] Failed to register shader '{}': {}", name, e.getMessage());
        }
    }
}
