package net.phoenix.core.integration.conflux.tools.capture;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.conflux.client.render.IntensityController;
import net.phoenix.core.integration.conflux.client.render.MotionClock;
import net.phoenix.core.integration.conflux.client.render.RenderContext;
import net.phoenix.core.integration.conflux.research.ResearchTree;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BakeRenderContext {

    private BakeRenderContext() {}

    private static final ResearchTree EMPTY_TREE = new ResearchTree(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "bake_stub"),
            "Bake Stub", "", null, Map.of(), List.of());

    public static RenderContext of(int w, int h, float elapsed) {
        MotionClock clock = new MotionClock();
        clock.tick(elapsed);

        IntensityController intensity = new IntensityController();
        intensity.onEvent();

        return new RenderContext(
                EMPTY_TREE,
                Set.of(),
                Set.of(),
                null,
                0f, 0f,
                w, h,
                clock,
                intensity);
    }
}
