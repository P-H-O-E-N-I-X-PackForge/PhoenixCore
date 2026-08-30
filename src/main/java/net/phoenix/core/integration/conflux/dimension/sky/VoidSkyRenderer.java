package net.phoenix.core.integration.conflux.dimension.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class VoidSkyRenderer extends SkyRenderer {

    private final List<PlanetOrbit> planets;
    private final List<PlanetOrbit> stars;

    public VoidSkyRenderer(String dimensionId, Level level) {
        super(dimensionId, level);
        this.planets = new ArrayList<>();
        this.stars = new ArrayList<>();

        initializeVoidSky();
    }

    private void initializeVoidSky() {

        // The two orbiting planet meshes that used to render here are replaced by the
        // void_black_hole shader (see DisciplineSkyEffects#renderVoidBlackHole) - a real
        // lensing/accretion-disk effect anchored to the galaxy's core reads far richer than a
        // couple of flat-colored spheres, and doubling up on both would just clutter the sky.

        stars.add(new PlanetOrbit(
            "Void Star 1",
            PlanetOrbit.PlanetType.STAR,
            187,
            0.0002f,
            2.5f,
            new Vec3(0, 75, 0),
            0xFFFFFF,
            true,
            1.5f
        ));

        stars.add(new PlanetOrbit(
            "Void Star 2",
            PlanetOrbit.PlanetType.STAR,
            200,
            0.0001f,
            1.9f,
            new Vec3(0, 69, 0),
            0xFFFFCC,
            true,
            1.2f
        ));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       float partialTick, float skyBrightness) {
        poseStack.pushPose();

        for (PlanetOrbit planet : planets) {
            planet.render(poseStack, bufferSource, worldTime);
        }

        for (PlanetOrbit star : stars) {
            star.render(poseStack, bufferSource, worldTime);
        }

        // The mesh-based nebula blobs used to render here too, but they're superseded by the
        // void_galaxy shader (see DisciplineSkyEffects) - a per-pixel domain-warped cloud reads
        // far richer than a handful of overlapping translucent spheres ever could.

        poseStack.popPose();
    }
}
