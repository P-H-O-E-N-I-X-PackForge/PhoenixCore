package net.phoenix.core.integration.conflux.dimension.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class SealedBSkyRenderer extends SkyRenderer {

    private final List<PlanetOrbit> chaosOrbitals;

    public SealedBSkyRenderer(String dimensionId, Level level) {
        super(dimensionId, level);
        this.chaosOrbitals = new ArrayList<>();

        initializeSealedBSky();
    }

    private void initializeSealedBSky() {
        
        // Scaled to fit the world's normal projection frustum (see PhoenixSkyRenderer) -
        // vanilla's own sun/moon sit around +/-100 units for this same reason.
        chaosOrbitals.add(new PlanetOrbit(
            "Reality Fragment 1",
            PlanetOrbit.PlanetType.NEBULA,
            112,
            0.002f,
            6.25f,
            new Vec3(0, 44, 0),
            0xFF00FF,
            true,
            1.4f
        ));

        chaosOrbitals.add(new PlanetOrbit(
            "Reality Fragment 2",
            PlanetOrbit.PlanetType.NEBULA,
            125,
            0.0015f,
            5.6f,
            new Vec3(0, 50, 0),
            0x00FFFF,
            true,
            1.3f
        ));

        chaosOrbitals.add(new PlanetOrbit(
            "Reality Fragment 3",
            PlanetOrbit.PlanetType.NEBULA,
            100,
            0.0025f,
            4.4f,
            new Vec3(0, 38, 0),
            0xFFFF00,
            true,
            1.2f
        ));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       float partialTick, float skyBrightness) {
        poseStack.pushPose();

        for (PlanetOrbit fragment : chaosOrbitals) {
            fragment.render(poseStack, bufferSource, worldTime);
        }

        poseStack.popPose();
    }
}
