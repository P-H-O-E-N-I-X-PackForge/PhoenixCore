package net.phoenix.core.client.gui.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.phoenix.core.client.render.structure.PhoenixTrackedDummyWorld;
import net.phoenix.core.client.render.structure.StructureRenderer;
import net.phoenix.core.client.render.structure.camera.CameraView;
import net.phoenix.core.client.render.structure.camera.StructureCamera;

import org.joml.Vector3f;

import java.util.Map;

public class CinderStructurePreview {

    private static final float AUTO_SPIN_DEG_PER_TICK = 0.35f;
    private static final float DRAG_SENSITIVITY = 0.5f;
    private static final float FIT_MARGIN = 1.3f;
    private static final float MIN_ZOOM = 2f;
    private static final float MAX_ZOOM = 400f;

    private final PhoenixTrackedDummyWorld world = new PhoenixTrackedDummyWorld();
    private final StructureRenderer renderer = new StructureRenderer(world);
    private final StructureCamera camera = new StructureCamera(35f, 25f, 20f, 0f, 0f, 0f);

    private boolean manuallyRotated = false;
    private Map<BlockPos, BlockInfo> lastBlocks = null;

    public void mouseDragged(double dragX, double dragY) {
        manuallyRotated = true;
        camera.orbit((float) dragX * DRAG_SENSITIVITY, (float) -dragY * DRAG_SENSITIVITY);
    }

    public void mouseScrolled(double delta) {
        camera.zoom(delta > 0 ? 0.9f : 1.1f, MIN_ZOOM, MAX_ZOOM);
    }

    public void render(GuiGraphics g, int x, int y, int w, int h, Map<BlockPos, BlockInfo> localBlocks,
                       float partialTick) {
        if (localBlocks == null || localBlocks.isEmpty()) return;
        if (localBlocks != lastBlocks) {
            rebuild(localBlocks);
            lastBlocks = localBlocks;
        }

        if (!manuallyRotated) camera.orbit(-AUTO_SPIN_DEG_PER_TICK, 0f);
        camera.tick();

        g.flush();
        CameraView view = camera.getView(partialTick);
        renderer.render(view, x, y, w, h);
    }

    private void rebuild(Map<BlockPos, BlockInfo> localBlocks) {
        world.clear();
        world.addBlocks(localBlocks);

        Vector3f min = world.minPos;
        Vector3f max = world.maxPos;
        float centerX = (min.x() + max.x() + 1) / 2f;
        float centerY = (min.y() + max.y() + 1) / 2f;
        float centerZ = (min.z() + max.z() + 1) / 2f;
        Vector3f size = world.getSize();
        float diagonal = (float) Math.sqrt(size.x() * size.x() + size.y() * size.y() + size.z() * size.z());

        float halfFovTan = (float) Math.tan(Math.toRadians(StructureCamera.FOV / 2.0));
        float fitDistance = (diagonal / 2f) / halfFovTan * FIT_MARGIN;

        camera.setTarget(centerX, centerY, centerZ);
        camera.setPosition(camera.getYaw(), camera.getPitch(),
                Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, fitDistance)));

        renderer.setPatternBlocks(localBlocks.keySet());
        renderer.requestBake();
    }

    public void close() {
        renderer.close();
    }
}
