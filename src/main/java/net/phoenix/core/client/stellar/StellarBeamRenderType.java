package net.phoenix.core.client.stellar;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

/**
 * The render type of the forge's beams and rings: additive-looking soft light, depth tested against the world and
 * written into the main target (vanilla's lightning type draws into the weather target when fabulous graphics are on,
 * which is what let it show through blocks and under glass). Two sided, no culling.
 */
public final class StellarBeamRenderType extends RenderType {

    public static final RenderType BEAMS = create("phoenixcore_stellar_beams", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 262144, false, true,
            CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setWriteMaskState(COLOR_DEPTH_WRITE).setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setOutputState(MAIN_TARGET)
                    .createCompositeState(false));

    private StellarBeamRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                  boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}
