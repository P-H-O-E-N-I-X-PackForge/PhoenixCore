package net.phoenix.core.integration.conflux.dimension.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class PhoenixSkyRenderer extends SkyRenderer {

    private final List<PlanetOrbit> orbitingBodies;

    public PhoenixSkyRenderer(String dimensionId, Level level) {
        super(dimensionId, level);
        this.orbitingBodies = new ArrayList<>();

        initializePhoenixSky();
    }

    // Volcano cone meshes used to live here too, but they're superseded by the phoenix_sunflare
    // shader (see DisciplineSkyEffects) - a per-pixel fire effect reads far better than a few
    // flat-shaded cones, so the meshes were removed rather than left drawing underneath it.
    private void initializePhoenixSky() {
        orbitingBodies.add(new PlanetOrbit(
            "Lava Moon",
            PlanetOrbit.PlanetType.MOON,
            100,
            0.001f,
            7.5f,
            new Vec3(0, 50, 0),
            0xFF6600,
            true,
            1.2f
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
