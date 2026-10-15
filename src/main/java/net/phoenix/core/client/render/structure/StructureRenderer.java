package net.phoenix.core.client.render.structure;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.render.structure.camera.CameraView;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

public final class StructureRenderer {

    private static final float FOV = 60f;
    private static final float NEAR = 0.1f;
    private static final float FAR = 10_000f;

    private static final ExecutorService BAKE_POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PhoenixCore-StructureBake");
        t.setDaemon(true);
        return t;
    });

    private static final FloatBuffer SCRATCH_MV = direct(64).asFloatBuffer();
    private static final FloatBuffer SCRATCH_PROJ = direct(64).asFloatBuffer();
    private static final IntBuffer SCRATCH_VP = direct(16 * 4).asIntBuffer();
    private static final FloatBuffer UNPROJECT_OUT = direct(12).asFloatBuffer();

    private static ByteBuffer direct(int bytes) {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }

    @Nullable
    private static java.lang.reflect.Field GAME_RENDERER_CAMERA_FIELD = null;
    private static boolean GAME_RENDERER_CAMERA_FIELD_RESOLVED = false;

    private static java.lang.reflect.Field resolveGameRendererCameraField() {
        if (GAME_RENDERER_CAMERA_FIELD_RESOLVED) return GAME_RENDERER_CAMERA_FIELD;
        GAME_RENDERER_CAMERA_FIELD_RESOLVED = true;
        try {
            for (java.lang.reflect.Field f : Minecraft.getInstance().gameRenderer.getClass().getDeclaredFields()) {
                if (f.getType() == Camera.class) {
                    f.setAccessible(true);
                    GAME_RENDERER_CAMERA_FIELD = f;
                    break;
                }
            }
        } catch (Throwable ignored) {}
        return GAME_RENDERER_CAMERA_FIELD;
    }

    private final List<RenderType> LAYERS = RenderType.chunkBufferLayers();
    private final int LAYER_COUNT = LAYERS.size();

    private final VertexBuffer[] front;
    private final VertexBuffer[] back;

    private volatile boolean frontHasContent = false;
    private volatile boolean backReady = false;
    private volatile boolean fullBakeNeeded = true;
    private volatile boolean closed = false;

    private final AtomicInteger pendingUploads = new AtomicInteger(0);
    @Nullable
    private Future<?> bakeFuture = null;

    private Set<BlockPos> patternBlocks = Collections.emptySet();
    private volatile Set<BlockPos> backTileEntities = null;
    private Set<BlockPos> frontTileEntities = Collections.emptySet();

    private final PhoenixTrackedDummyWorld world;
    private final PhoenixCameraEntity cameraEntity;
    private final Camera camera;

    private double guiMouseX;
    private double guiMouseY;
    @Nullable
    private BlockHitResult lastHitResult;
    @Nullable
    private BlockHitResult cachedPickResult = null;
    private long lastTraceTime = 0;
    private final float[] snapMV = new float[16];
    private final float[] snapProj = new float[16];
    private final int[] snapVP = new int[4];

    public StructureRenderer(PhoenixTrackedDummyWorld world) {
        this.world = world;
        this.cameraEntity = new PhoenixCameraEntity(world);
        this.camera = new Camera();

        this.front = new VertexBuffer[LAYER_COUNT];
        this.back = new VertexBuffer[LAYER_COUNT];
        for (int i = 0; i < LAYER_COUNT; i++) {
            front[i] = new VertexBuffer(VertexBuffer.Usage.STATIC);
            back[i] = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
    }

    public void setPatternBlocks(Set<BlockPos> all) {
        this.patternBlocks = Set.copyOf(all);
        fullBakeNeeded = true;
    }

    public void setMousePos(int mx, int my) {
        this.guiMouseX = mx;
        this.guiMouseY = my;
    }

    public void requestBake() {
        fullBakeNeeded = true;
    }

    public boolean isSceneReady() {
        return frontHasContent;
    }

    @Nullable
    public BlockHitResult getLastHitResult() {
        return this.lastHitResult;
    }

    public void render(CameraView view, int guiX, int guiY, int guiW, int guiH) {
        if (guiW <= 0 || guiH <= 0) return;

        if (backReady) swapBuffers();

        boolean bakeIdle = bakeFuture == null || bakeFuture.isDone();
        if (bakeIdle && fullBakeNeeded) {
            fullBakeNeeded = false;
            scheduleBake();
        }

        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int windowH = mc.getWindow().getHeight();
        int glX = (int) (guiX * scale);
        int glY = (int) (windowH - (guiY + guiH) * scale);
        int glW = (int) (guiW * scale);
        int glH = (int) (guiH * scale);

        setupCamera(view, glX, glY, glW, glH);

        long currentTick = mc.level != null ? mc.level.getGameTime() : 0;
        RenderSystem.setShaderGameTime(currentTick, (currentTick + mc.getFrameTime()) / 20f);
        snapshotMatrices();

        Camera originalCamera = null;
        java.lang.reflect.Field cameraField = resolveGameRendererCameraField();
        try {
            if (cameraField != null) {
                originalCamera = (Camera) cameraField.get(mc.gameRenderer);
                cameraField.set(mc.gameRenderer, this.camera);
            }
        } catch (Throwable ignored) {}

        try {
            drawVBOs();

            float partial = mc.getFrameTime();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            turnOnLight();
            float camX = view.eyeX(), camY = view.eyeY(), camZ = view.eyeZ();

            PoseStack localPoseStack = new PoseStack();
            drawTileEntities(localPoseStack, buffers, partial);
            drawEntities(localPoseStack, buffers, partial);
            buffers.endBatch();

            world.tickWorld();

            turnOffLight();

            long currentSystemTime = System.currentTimeMillis();
            if (currentSystemTime - lastTraceTime > 33 || cachedPickResult == null) {
                cachedPickResult = doDepthSampleRead(glX, glY, glW, glH, scale, windowH);
                lastHitResult = cachedPickResult;
                lastTraceTime = currentSystemTime;
            } else {
                cachedPickResult = lastHitResult;
            }

            resetCamera();
        } finally {
            if (cameraField != null && originalCamera != null) {
                try {
                    cameraField.set(mc.gameRenderer, originalCamera);
                } catch (Throwable ignored) {}
            }
        }
    }

    private void swapBuffers() {
        for (int i = 0; i < LAYER_COUNT; i++) {
            VertexBuffer tmp = front[i];
            front[i] = back[i];
            back[i] = tmp;
        }
        frontTileEntities = backTileEntities != null ? backTileEntities : Collections.emptySet();
        frontHasContent = true;
        backReady = false;
        backTileEntities = null;
    }

    private void setupCamera(CameraView view, int glX, int glY, int glW, int glH) {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.viewport(glX, glY, glW, glH);
        RenderSystem.depthMask(true);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.backupProjectionMatrix();

        float aspect = (float) glW / glH;
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setPerspective((float) Math.toRadians(FOV), aspect, NEAR, FAR),
                VertexSorting.byDistance(new Vector3f(view.eyeX(), view.eyeY(), view.eyeZ())));

        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.setIdentity();
        gluLookAt(mv, view.eyeX(), view.eyeY(), view.eyeZ(), view.lookAtX(), view.lookAtY(), view.lookAtZ(), 0f, 1f,
                0f);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.activeTexture(33984);

        syncCameraEntity(view);
        camera.setup(world, cameraEntity, false, false, Minecraft.getInstance().getFrameTime());
    }

    private void syncCameraEntity(CameraView view) {
        Vector3f dir = view.direction();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x(), dir.z()));
        float hd = (float) Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z());
        float pitch = (float) Math.toDegrees(Math.atan2(-dir.y(), hd));

        cameraEntity.setPos(view.eyeX(), view.eyeY(), view.eyeZ());
        cameraEntity.setYRot(yaw);
        cameraEntity.setXRot(pitch);
        cameraEntity.xo = view.eyeX();
        cameraEntity.yo = view.eyeY();
        cameraEntity.zo = view.eyeZ();
        cameraEntity.yRotO = yaw;
        cameraEntity.xRotO = pitch;
    }

    private void snapshotMatrices() {
        RenderSystem.getModelViewMatrix().get(SCRATCH_MV);
        SCRATCH_MV.rewind();
        RenderSystem.getProjectionMatrix().get(SCRATCH_PROJ);
        SCRATCH_PROJ.rewind();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, SCRATCH_VP);
        SCRATCH_VP.rewind();
        for (int i = 0; i < 16; i++) snapMV[i] = SCRATCH_MV.get(i);
        for (int i = 0; i < 16; i++) snapProj[i] = SCRATCH_PROJ.get(i);
        for (int i = 0; i < 4; i++) snapVP[i] = SCRATCH_VP.get(i);
        SCRATCH_MV.rewind();
        SCRATCH_PROJ.rewind();
        SCRATCH_VP.rewind();
    }

    private void resetCamera() {
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        RenderSystem.restoreProjectionMatrix();
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
    }

    private void turnOnLight() {
        try {
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        } catch (Exception ignored) {}
    }

    private void turnOffLight() {
        try {
            Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        } catch (Exception ignored) {}
    }

    private void drawVBOs() {
        for (int i = 0; i < LAYER_COUNT; i++) {
            VertexBuffer vbo = front[i];
            RenderType layer = LAYERS.get(i);
            if (vbo.isInvalid() || vbo.getFormat() == null) continue;

            layer.setupRenderState();
            applyLayerBlend(layer);

            ShaderInstance shader = RenderSystem.getShader();
            if (shader == null) {
                layer.clearRenderState();
                continue;
            }

            bindShaderSamplers(shader);
            setShaderUniforms(shader);
            RenderSystem.setupShaderLights(shader);
            shader.apply();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            vbo.bind();
            vbo.draw();
            VertexBuffer.unbind();
            shader.clear();
            layer.clearRenderState();
        }
    }

    private void drawTileEntities(PoseStack poseStack, MultiBufferSource.BufferSource buffers, float partial) {
        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (BlockPos pos : frontTileEntities) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be == null || be.isRemoved()) continue;

            BlockEntityRenderer<BlockEntity> ber = dispatcher.getRenderer(be);
            if (ber == null) continue;

            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            try {
                ber.render(be, partial, poseStack, buffers, 15728880, OverlayTexture.NO_OVERLAY);
            } catch (Exception e) {
                PhoenixCore.LOGGER.warn("[StructurePreview] BE render error at {} ({}): {}", pos,
                        be.getClass().getSimpleName(), e.toString());
            }
            poseStack.popPose();
        }
    }

    private void drawEntities(PoseStack poseStack, MultiBufferSource.BufferSource buffers, float partial) {
        var erd = Minecraft.getInstance().getEntityRenderDispatcher();
        for (Entity entity : world.getAllEntities()) {
            try {
                double d0 = net.minecraft.util.Mth.lerp(partial, entity.xOld, entity.getX());
                double d1 = net.minecraft.util.Mth.lerp(partial, entity.yOld, entity.getY());
                double d2 = net.minecraft.util.Mth.lerp(partial, entity.zOld, entity.getZ());
                float yRot = net.minecraft.util.Mth.lerp(partial, entity.yRotO, entity.getYRot());

                poseStack.pushPose();
                poseStack.translate(d0, d1, d2);
                erd.render(entity, 0.0, 0.0, 0.0, yRot, partial, poseStack, buffers,
                        erd.getRenderer(entity).getPackedLightCoords(entity, partial));
                poseStack.popPose();
            } catch (Exception ignored) {}
        }
    }

    private static void bindShaderSamplers(ShaderInstance s) {
        for (int j = 0; j < 12; j++) s.setSampler("Sampler" + j, RenderSystem.getShaderTexture(j));
    }

    private static void setShaderUniforms(ShaderInstance s) {
        if (s.MODEL_VIEW_MATRIX != null) s.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        if (s.PROJECTION_MATRIX != null) s.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (s.COLOR_MODULATOR != null) s.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (s.FOG_START != null) s.FOG_START.set(RenderSystem.getShaderFogStart());
        if (s.FOG_END != null) s.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (s.FOG_COLOR != null) s.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (s.FOG_SHAPE != null) s.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        if (s.TEXTURE_MATRIX != null) s.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (s.GAME_TIME != null) s.GAME_TIME.set(RenderSystem.getShaderGameTime());
    }

    private static void applyLayerBlend(RenderType layer) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (layer == RenderType.translucent()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 771);
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }

    @Nullable
    private BlockHitResult doDepthSampleRead(int glX, int glY, int glW, int glH, double guiScale, int windowH) {
        int mouseGlX = (int) (guiMouseX * guiScale);
        int mouseGlY = (int) (windowH - guiMouseY * guiScale);
        if (mouseGlX < glX || mouseGlX >= glX + glW || mouseGlY < glY || mouseGlY >= glY + glH) return null;

        for (int i = 0; i < 16; i++) SCRATCH_MV.put(i, snapMV[i]);
        for (int i = 0; i < 16; i++) SCRATCH_PROJ.put(i, snapProj[i]);
        for (int i = 0; i < 4; i++) SCRATCH_VP.put(i, snapVP[i]);
        SCRATCH_MV.rewind();
        SCRATCH_PROJ.rewind();
        SCRATCH_VP.rewind();
        UNPROJECT_OUT.rewind();
        if (!gluUnProject(mouseGlX, mouseGlY, 0f, SCRATCH_MV, SCRATCH_PROJ, SCRATCH_VP, UNPROJECT_OUT)) return null;
        double nearX = UNPROJECT_OUT.get(0), nearY = UNPROJECT_OUT.get(1), nearZ = UNPROJECT_OUT.get(2);

        for (int i = 0; i < 16; i++) SCRATCH_MV.put(i, snapMV[i]);
        for (int i = 0; i < 16; i++) SCRATCH_PROJ.put(i, snapProj[i]);
        for (int i = 0; i < 4; i++) SCRATCH_VP.put(i, snapVP[i]);
        SCRATCH_MV.rewind();
        SCRATCH_PROJ.rewind();
        SCRATCH_VP.rewind();
        UNPROJECT_OUT.rewind();
        if (!gluUnProject(mouseGlX, mouseGlY, 1f, SCRATCH_MV, SCRATCH_PROJ, SCRATCH_VP, UNPROJECT_OUT)) return null;
        double farX = UNPROJECT_OUT.get(0), farY = UNPROJECT_OUT.get(1), farZ = UNPROJECT_OUT.get(2);

        Vec3 rayStart = new Vec3(nearX, nearY, nearZ);
        Vec3 farPt = new Vec3(farX, farY, farZ);
        Vec3 lookDir = farPt.subtract(rayStart).normalize();
        if (lookDir.lengthSqr() < 1e-10) return null;
        Vec3 rayEnd = rayStart.add(lookDir.scale(200.0));

        for (int attempt = 0; attempt < 32; attempt++) {
            net.minecraft.world.level.ClipContext ctx = new net.minecraft.world.level.ClipContext(rayStart, rayEnd,
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    cameraEntity);
            BlockHitResult result = world.clip(ctx);
            if (result == null || result.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;

            BlockPos pos = result.getBlockPos();
            if (patternBlocks.contains(pos)) return result;

            rayStart = result.getLocation().add(lookDir.scale(0.02));
        }
        return null;
    }

    private void scheduleBake() {
        Set<BlockPos> snapshot = new HashSet<>(patternBlocks);
        snapshot.removeIf(pos -> {
            BlockState s = world.getBlockState(pos);
            return s == null || s.isAir() || s.getRenderShape() == RenderShape.INVISIBLE;
        });

        if (snapshot.isEmpty()) {
            uploadEmptyBuffers();
            return;
        }

        bakeFuture = BAKE_POOL.submit(() -> {
            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher brd = mc.getBlockRenderer();
            RandomSource random = RandomSource.createNewThreadLocalInstance();
            ModelBlockRenderer.enableCaching();

            Map<RenderType, List<BlockPos>> solidBuckets = new HashMap<>(LAYER_COUNT);
            Map<RenderType, List<BlockPos>> fluidBuckets = new HashMap<>(LAYER_COUNT);
            bucket(brd, random, snapshot, solidBuckets, fluidBuckets);

            BakedLayer[] baked = new BakedLayer[LAYER_COUNT];
            boolean bakeDone = false;
            try {
                for (int i = 0; i < LAYER_COUNT; i++) {
                    if (Thread.interrupted()) return;
                    RenderType layer = LAYERS.get(i);
                    List<BlockPos> solid = solidBuckets.getOrDefault(layer, List.of());
                    List<BlockPos> fluid = fluidBuckets.getOrDefault(layer, List.of());
                    if (solid.isEmpty() && fluid.isEmpty()) {
                        baked[i] = null;
                        continue;
                    }
                    baked[i] = bakeLayerToBuffer(brd, random, layer, solid, fluid);
                    if (baked[i] == null) return;
                }
                bakeDone = true;
            } finally {
                ModelBlockRenderer.clearCache();
                if (!bakeDone) {
                    for (BakedLayer bl : baked) if (bl != null) bl.renderedBuffer().release();
                }
            }
            if (!bakeDone) return;

            pendingUploads.set(LAYER_COUNT);
            for (int i = 0; i < LAYER_COUNT; i++) {
                final int fi = i;
                final BakedLayer bl = baked[fi];
                RenderSystem.recordRenderCall(() -> {
                    if (closed) {
                        pendingUploads.decrementAndGet();
                        return;
                    }
                    if (bl != null) uploadToVBO(back[fi], bl);
                    else uploadEmptyVBO(back[fi], LAYERS.get(fi));
                    if (pendingUploads.decrementAndGet() == 0) backReady = true;
                });
            }

            Set<BlockPos> tes = new HashSet<>();
            for (BlockPos pos : snapshot) {
                if (Thread.interrupted()) return;
                BlockEntity be = world.getBlockEntity(pos);
                if (be != null && mc.getBlockEntityRenderDispatcher().getRenderer(be) != null) tes.add(pos);
            }
            backTileEntities = tes;
        });
    }

    private void bucket(BlockRenderDispatcher brd, RandomSource random, Set<BlockPos> positions,
                        Map<RenderType, List<BlockPos>> solidOut, Map<RenderType, List<BlockPos>> fluidOut) {
        for (BlockPos pos : positions) {
            BlockState state = world.getBlockState(pos);
            if (state == null || state.isAir()) continue;

            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                for (RenderType layer : LAYERS) {
                    if (canRenderInLayer(brd, state, pos, world, layer, random)) {
                        solidOut.computeIfAbsent(layer, k -> new ArrayList<>()).add(pos);
                    }
                }
            }
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                RenderType fl = net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderLayer(fluid);
                fluidOut.computeIfAbsent(fl, k -> new ArrayList<>()).add(pos);
            }
        }
    }

    private record BakedLayer(BufferBuilder.RenderedBuffer renderedBuffer) {}

    private BakedLayer bakeLayerToBuffer(BlockRenderDispatcher brd, RandomSource random, RenderType layer,
                                         List<BlockPos> solid, List<BlockPos> fluid) {
        int count = solid.size() + fluid.size();
        int sizeHint = Math.max(layer.bufferSize(), Math.min(count * 512, 64 * 1024 * 1024));
        BufferBuilder bb = new BufferBuilder(sizeHint);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        PoseStack ps = new PoseStack();
        TintedVertexConsumer tinted = new TintedVertexConsumer(bb);

        for (BlockPos pos : solid) {
            if (Thread.interrupted()) {
                bb.end().release();
                return null;
            }
            BlockState state = world.getBlockState(pos);
            ps.pushPose();
            ps.translate(pos.getX(), pos.getY(), pos.getZ());
            WorldSceneRenderer.renderBlocksForge(brd, state, pos, world, ps, tinted, random, layer);
            ps.popPose();
            tinted.resetTint();
        }

        for (BlockPos pos : fluid) {
            BlockState state = world.getBlockState(pos);
            FluidState fs = state.getFluidState();
            if (fs.isEmpty()) continue;
            tinted.addOffset(pos.getX() - (pos.getX() & 15), pos.getY() - (pos.getY() & 15),
                    pos.getZ() - (pos.getZ() & 15));
            brd.renderLiquid(pos, world, tinted, state, fs);
            tinted.clearOffset();
            tinted.resetTint();
        }

        return new BakedLayer(bb.end());
    }

    private void uploadToVBO(VertexBuffer vbo, BakedLayer bl) {
        if (!vbo.isInvalid()) {
            vbo.bind();
            vbo.upload(bl.renderedBuffer());
            VertexBuffer.unbind();
        }
    }

    private void uploadEmptyVBO(VertexBuffer vbo, RenderType layer) {
        if (!vbo.isInvalid()) {
            BufferBuilder bb = new BufferBuilder(layer.bufferSize());
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            vbo.bind();
            vbo.upload(bb.end());
            VertexBuffer.unbind();
        }
    }

    private void uploadEmptyBuffers() {
        pendingUploads.set(LAYER_COUNT);
        for (int i = 0; i < LAYER_COUNT; i++) {
            RenderType layer = LAYERS.get(i);
            BufferBuilder bb = new BufferBuilder(layer.bufferSize());
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            BufferBuilder.RenderedBuffer rb = bb.end();
            final int fi = i;
            RenderSystem.recordRenderCall(() -> {
                if (!back[fi].isInvalid()) {
                    back[fi].bind();
                    back[fi].upload(rb);
                    VertexBuffer.unbind();
                }
                if (pendingUploads.decrementAndGet() == 0) backReady = true;
            });
        }
        backTileEntities = Collections.emptySet();
    }

    private static boolean canRenderInLayer(BlockRenderDispatcher brd, BlockState state, BlockPos pos,
                                            net.minecraft.world.level.BlockAndTintGetter world, RenderType layer,
                                            RandomSource random) {
        return WorldSceneRenderer.canRenderInLayer(brd, state, pos, world, layer, random);
    }

    private static void gluLookAt(PoseStack mv, float eyeX, float eyeY, float eyeZ, float centerX, float centerY,
                                  float centerZ, float upX, float upY, float upZ) {
        float fx = centerX - eyeX, fy = centerY - eyeY, fz = centerZ - eyeZ;
        float rLen = 1f / (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx *= rLen;
        fy *= rLen;
        fz *= rLen;
        float sx = fy * upZ - fz * upY, sy = fz * upX - fx * upZ, sz = fx * upY - fy * upX;
        float sLen = 1f / (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= sLen;
        sy *= sLen;
        sz *= sLen;
        float ux = sy * fz - sz * fy, uy = sz * fx - sx * fz, uz = sx * fy - sy * fx;
        Matrix4f m = new Matrix4f(
                sx, ux, -fx, 0,
                sy, uy, -fy, 0,
                sz, uz, -fz, 0,
                -(sx * eyeX + sy * eyeY + sz * eyeZ),
                -(ux * eyeX + uy * eyeY + uz * eyeZ),
                (fx * eyeX + fy * eyeY + fz * eyeZ),
                1);
        mv.mulPoseMatrix(m);
    }

    private static boolean gluUnProject(float winX, float winY, float winZ, FloatBuffer mv, FloatBuffer proj,
                                        IntBuffer vp, FloatBuffer out) {
        Matrix4f combined = new Matrix4f(proj).mul(new Matrix4f(mv));
        if (combined.determinant() == 0) return false;
        Matrix4f inv = combined.invert(new Matrix4f());
        int vpX = vp.get(0), vpY = vp.get(1), vpW = vp.get(2), vpH = vp.get(3);
        float ix = (winX - vpX) / vpW * 2f - 1f;
        float iy = (winY - vpY) / vpH * 2f - 1f;
        float iz = winZ * 2f - 1f;
        Vector4f v = inv.transform(new Vector4f(ix, iy, iz, 1f));
        if (v.w == 0) return false;
        out.put(0, v.x / v.w);
        out.put(1, v.y / v.w);
        out.put(2, v.z / v.w);
        return true;
    }

    public void close() {
        closed = true;
        if (bakeFuture != null) {
            bakeFuture.cancel(true);
            bakeFuture = null;
        }
        for (int i = 0; i < LAYER_COUNT; i++) {
            if (front[i] != null && !front[i].isInvalid()) front[i].close();
            if (back[i] != null && !back[i].isInvalid()) back[i].close();
        }
    }
}
