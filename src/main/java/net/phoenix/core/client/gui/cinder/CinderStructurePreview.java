package net.phoenix.core.client.gui.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.phoenix.core.client.render.structure.PhoenixTrackedDummyWorld;
import net.phoenix.core.client.render.structure.StructureRenderer;
import net.phoenix.core.client.render.structure.camera.CameraView;
import net.phoenix.core.client.render.structure.camera.StructureCamera;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
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
                       float partialTick, int mouseX, int mouseY) {
        if (localBlocks == null || localBlocks.isEmpty()) return;
        // Content comparison, not reference (`!=`) - MultiblockSchemaInfo#refreshSchema clears and
        // repopulates its own structureBlocks map *in place* on every call, so getStructureBlocks()
        // always hands back the exact same Map instance even after a real change (e.g. picking a
        // different block variant). A reference check against that live, mutating map is trivially
        // always "unchanged" after the first render, so the preview never re-baked after the very first
        // schema edit. Snapshotting into a plain HashMap (whose equals() is content-based) instead of
        // holding onto the live reference is what makes the comparison actually detect a real change.
        if (!localBlocks.equals(lastBlocks)) {
            rebuild(localBlocks);
            lastBlocks = new HashMap<>(localBlocks);
        }

        if (!manuallyRotated) camera.orbit(-AUTO_SPIN_DEG_PER_TICK, 0f);
        camera.tick();

        g.flush();
        CameraView view = camera.getView(partialTick);
        renderer.setMousePos(mouseX, mouseY);
        renderer.render(view, x, y, w, h);
    }

    /** The schema-local {@link BlockPos} currently under the cursor, resolved from
     *  {@link StructureRenderer}'s own real GPU depth-buffer pick (a genuine ray-cast against the
     *  actual rendered blocks, not an approximation) - {@code null} off the model or before the first
     *  {@link #render} call this frame has updated it. */
    public @Nullable BlockPos getHoveredLocalPos() {
        var hit = renderer.getLastHitResult();
        return hit != null ? hit.getBlockPos() : null;
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
