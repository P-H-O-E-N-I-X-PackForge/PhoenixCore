package net.phoenix.core.integration.conflux.dimension.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class SealedASkyRenderer extends SkyRenderer {

    private final List<PlanetOrbit> orbitingStructures;

    public SealedASkyRenderer(String dimensionId, Level level) {
        super(dimensionId, level);
        this.orbitingStructures = new ArrayList<>();

        initializeSealedASky();
    }

    private void initializeSealedASky() {
        
        // Scaled to fit the world's normal projection frustum (see PhoenixSkyRenderer) -
        // vanilla's own sun/moon sit around +/-100 units for this same reason.
        orbitingStructures.add(new PlanetOrbit(
            "Industrial Satellite 1",
            PlanetOrbit.PlanetType.NEBULA,
            87,
            0.0007f,
            5,
            new Vec3(0, 50, 0),
            0xFF00FF,
            true,
            1.1f
        ));

        orbitingStructures.add(new PlanetOrbit(
            "Industrial Satellite 2",
            PlanetOrbit.PlanetType.NEBULA,
            106,
            0.0005f,
            4.4f,
            new Vec3(0, 47, 0),
            0x00FFFF,
            true,
            1.0f
        ));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       float partialTick, float skyBrightness) {
        poseStack.pushPose();

        for (PlanetOrbit structure : orbitingStructures) {
            structure.render(poseStack, bufferSource, worldTime);
        }

        poseStack.popPose();
    }
}
