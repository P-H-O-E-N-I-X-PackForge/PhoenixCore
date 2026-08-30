package net.phoenix.core.integration.conflux.dimension.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class SculkSkyRenderer extends SkyRenderer {

    private final List<PlanetOrbit> orbitingBodies;

    public SculkSkyRenderer(String dimensionId, Level level) {
        super(dimensionId, level);
        this.orbitingBodies = new ArrayList<>();

        initializeSculkSky();
    }

    private void initializeSculkSky() {
        
        // Scaled to fit the world's normal projection frustum (see PhoenixSkyRenderer) -
        // vanilla's own sun/moon sit around +/-100 units for this same reason.
        orbitingBodies.add(new PlanetOrbit(
            "Sculk Moon 1",
            PlanetOrbit.PlanetType.MOON,
            75,
            0.0008f,
            6.25f,
            new Vec3(0, 44, 0),
            0x00FF88,
            true,
            1.5f
        ));

        orbitingBodies.add(new PlanetOrbit(
            "Sculk Moon 2",
            PlanetOrbit.PlanetType.MOON,
            94,
            0.0006f,
            5,
            new Vec3(0, 38, 0),
            0x00FFFF,
            true,
            1.3f
        ));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       float partialTick, float skyBrightness) {
        poseStack.pushPose();

        for (PlanetOrbit body : orbitingBodies) {
            body.render(poseStack, bufferSource, worldTime);
        }

        poseStack.popPose();
    }
}
