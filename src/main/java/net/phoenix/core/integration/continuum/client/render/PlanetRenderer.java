package net.phoenix.core.integration.continuum.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

public final class PlanetRenderer {

    private PlanetRenderer() {}

    public enum Quality {

        LOW(3, 3),
        MEDIUM(4, 5),
        HIGH(5, 7);

        public final int subdivisions;
        public final int octaves;

        Quality(int subdivisions, int octaves) {
            this.subdivisions = subdivisions;
            this.octaves = octaves;
        }

        public Quality next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public record View(float yawDeg, float pitchDeg, float distance, float surfaceSpinDeg, float cloudSpinDeg) {}

    private static final Vector3f SUN_VIEW = new Vector3f(-0.78f, 0.32f, 0.53f).normalize();

    private static final Map<Quality, IcosphereMesh> MESHES = new EnumMap<>(Quality.class);

    public static IcosphereMesh mesh(Quality quality) {
        return MESHES.computeIfAbsent(quality, q -> IcosphereMesh.create(q.subdivisions));
    }

    public static void releaseMeshes() {
        MESHES.values().forEach(IcosphereMesh::close);
        MESHES.clear();
    }

    public static boolean render(RenderTarget target, PlanetParams params, View view, Quality quality) {
        if (!ContinuumShaders.ready()) return false;

        SceneRenderer.begin(target, view.yawDeg());

        Matrix4f projection = SceneRenderer.projection(target.width, target.height, 32.0f);
        Matrix4f viewMatrix = new Matrix4f()
                .translate(0.0f, 0.0f, -view.distance())
                .rotateX((float) Math.toRadians(view.pitchDeg()));

        Vector3f sunWorld = new Matrix4f(viewMatrix).invert().transformPosition(new Vector3f(SUN_VIEW).mul(60.0f));

        SceneRenderer.drawPlanet(params, viewMatrix, projection, new Vector3f(), 1.0f, sunWorld,
                view.yawDeg() + view.surfaceSpinDeg(), view.cloudSpinDeg(), quality, true);

        SceneRenderer.end();
        return true;
    }
}
