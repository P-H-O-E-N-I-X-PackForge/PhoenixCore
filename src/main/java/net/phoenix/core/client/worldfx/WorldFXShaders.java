package net.phoenix.core.client.worldfx;

import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class WorldFXShaders {

    private WorldFXShaders() {}

    private static final Logger LOGGER = LogManager.getLogger();

    public static ShaderInstance BLACK_HOLE;

    public static ShaderInstance ATMOSPHERE_GRADE;

    public static ShaderInstance SOUL_VISION_COMPOSITE;

    public static ShaderInstance REBIRTH_GALAXY;

    /** A rift in the world, bending the screen round it: see {@code shaders/core/fx_rift.fsh}. */
    public static ShaderInstance FX_RIFT;

    /** The Null Well seen straight on, drawn flat: see {@code shaders/core/fx_disc.fsh}. */
    public static ShaderInstance FX_DISC;

    public static ShaderInstance FX_PHOENIX;
    public static ShaderInstance FX_FLAME;
    public static ShaderInstance FX_CORTEX;
    public static ShaderInstance FX_COLLIDER;
    public static ShaderInstance FX_CRUCIBLE;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        register(event, "phoenixcore:fx_phoenix", DefaultVertexFormat.POSITION, s -> FX_PHOENIX = s);
        register(event, "phoenixcore:fx_flame", DefaultVertexFormat.POSITION, s -> FX_FLAME = s);
        register(event, "phoenixcore:fx_cortex", DefaultVertexFormat.POSITION, s -> FX_CORTEX = s);
        register(event, "phoenixcore:fx_collider", DefaultVertexFormat.POSITION, s -> FX_COLLIDER = s);
        register(event, "phoenixcore:fx_crucible", DefaultVertexFormat.POSITION, s -> FX_CRUCIBLE = s);
        register(event, "phoenixcore:fx_rift", DefaultVertexFormat.POSITION, s -> FX_RIFT = s);
        register(event, "phoenixcore:fx_disc", DefaultVertexFormat.POSITION_TEX, s -> FX_DISC = s);
        register(event, "phoenixcore:phoenix_black_hole", DefaultVertexFormat.POSITION, s -> BLACK_HOLE = s);
        register(event, "phoenixcore:phoenix_atmosphere_grade", DefaultVertexFormat.POSITION,
                s -> ATMOSPHERE_GRADE = s);
        register(event, "phoenixcore:soul_vision_composite", DefaultVertexFormat.POSITION,
                s -> SOUL_VISION_COMPOSITE = s);
        register(event, "phoenixcore:rebirth_galaxy", DefaultVertexFormat.POSITION, s -> REBIRTH_GALAXY = s);
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
