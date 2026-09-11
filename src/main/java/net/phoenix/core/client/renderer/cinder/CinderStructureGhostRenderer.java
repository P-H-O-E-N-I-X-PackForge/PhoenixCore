package net.phoenix.core.client.renderer.cinder;

import com.gregtechceu.gtceu.client.mui.schema.MutableSchema;

import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;

import brachy.modularui.drawable.schema.ISchema;
import brachy.modularui.drawable.schema.RenderFilter;
import brachy.modularui.drawable.schema.RenderLevel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;

import net.phoenix.core.client.render.structure.TintedVertexConsumer;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * A fork of GTCEu's own {@code PatternPreviewRenderer} - the exact renderer an unformed multiblock
 * controller uses for its own shift-right-click ghost preview - not a hand-rolled replacement: same
 * chunk-buffer VBO baking/async compile pipeline, same real block models and textures, driven by the
 * same {@code RenderLevelStageEvent}/tick pattern. The one real addition is a settable green/red tint
 * (see {@link #showPreview}) so the Cinder Core preview can still show build-site validity at a
 * glance, which GT's own version has no concept of at all - everything else here is GT's own approach,
 * kept as close to verbatim as this project's dependency versions allow.
 * <p>
 * One thing deliberately NOT ported: fluid rendering. GT's version routes liquid blocks through a
 * {@code LiquidVertexConsumer} class that isn't resolvable against this project's actual compiled
 * GTCEu/Forge jars (a version/mapping mismatch between the current GitHub source this was ported from
 * and the {@code 8.0.0-SNAPSHOT} build this project is pinned to). No Cinder Forge target's structure
 * predicates require an actual fluid-state block as part of the structure itself (fluid *tanks* are
 * just regular casing blocks structurally), so this wasn't worth chasing further rather than guessing
 * at an unverifiable API.
 * <p>
 * Driven by {@code CinderPreviewTickHandler} ({@link #clientTick()} every client tick,
 * {@link #draw} every {@code RenderLevelStageEvent}) - GT's own copy is driven identically by its own
 * internal client event listener, which only drives GT's own singleton, not this fork.
 */
@OnlyIn(Dist.CLIENT)
public class CinderStructureGhostRenderer {

    public static final CinderStructureGhostRenderer INSTANCE = new CinderStructureGhostRenderer();

    private static final Map<RenderLevelStageEvent.Stage, RenderType> STAGE_RENDER_TYPES = Util
            .make(new IdentityHashMap<>(), map -> {
                for (RenderType renderType : RenderType.chunkBufferLayers()) {
                    map.put(RenderLevelStageEvent.Stage.fromRenderType(renderType), renderType);
                }
            });

    private @Nullable ISchema schema;
    private @Nullable RenderLevel renderLevel;
    private @Nullable BlockPos anchorPos;
    private final AtomicInteger timeout = new AtomicInteger(-1);
    private boolean valid = true;

    private @Nullable RenderCompileTask lastRenderCompileTask;
    private final ChunkBufferBuilderPack chunkBufferBuilders = new ChunkBufferBuilderPack();
    private final AtomicReference<CompileStatus> compileStatus = new AtomicReference<>();
    private final AtomicReference<RenderCompileResults> compiledRenderResult = new AtomicReference<>();
    private boolean dirty = true;

    private CinderStructureGhostRenderer() {}

    /** @param valid green tint if true, red if false - the one thing GT's own PatternPreviewRenderer
     *              can't do, since it has no concept of "is this build site currently valid" at all. */
    public void showPreview(BlockPos anchorPos, MutableSchema schema, boolean valid, int duration) {
        this.anchorPos = anchorPos;
        this.schema = schema;
        this.valid = valid;
        this.renderLevel = new RenderLevel(schema, RenderFilter.ALL);
        timeout.set(duration);
        notifyRecompile();
    }

    public void clientTick() {
        if (timeout.get() > 0 && timeout.decrementAndGet() <= 0) {
            dispose();
        }
    }

    public void draw(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera,
                     RenderLevelStageEvent.Stage stage, float partialTick) {
        if (timeout.get() <= 0) return;
        if (this.schema == null || this.anchorPos == null) return;
        if (!camera.isInitialized()) return;

        RenderType renderType = STAGE_RENDER_TYPES.get(stage);
        if (renderType == null && stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;

        poseStack.pushPose();
        poseStack.translate(anchorPos.getX(), anchorPos.getY(), anchorPos.getZ());

        Vec3 cameraPos = camera.getPosition();
        if (renderType != null) {
            renderBlocks(renderType, poseStack, cameraPos);
        } else {
            renderBlockEntities(poseStack, bufferSource, partialTick, cameraPos);
        }

        poseStack.popPose();
    }

    public void notifyRecompile() {
        this.dirty = true;
    }

    protected void cancelCompilation() {
        if (this.lastRenderCompileTask != null) {
            this.lastRenderCompileTask.cancel();
            this.lastRenderCompileTask = null;
        }
    }

    private boolean shouldDiscard(CompileStatus status) {
        return status == CompileStatus.CANCELED;
    }

    protected void recompile(final Vec3 cameraPos) {
        cancelCompilation();

        this.lastRenderCompileTask = new RenderCompileTask();
        this.compileStatus.set(CompileStatus.COMPILING);

        RenderCompileResults compileResults = new RenderCompileResults();
        CompletableFuture.supplyAsync(
                Util.wrapThreadWithTaskName("cinder_preview_chunk_rebuild",
                        () -> this.lastRenderCompileTask.compileBlockBuffers(compileResults, cameraPos)),
                Util.backgroundExecutor())
                .thenCompose(Function.identity())
                .whenComplete((result, error) -> {
                    if (error != null) {
                        Minecraft.getInstance()
                                .delayCrash(CrashReport.forThrowable(error, "Batching Cinder preview chunks"));
                    } else {
                        CompileStatus status = result.status;
                        if (shouldDiscard(status)) {
                            this.chunkBufferBuilders.discardAll();
                        } else {
                            this.chunkBufferBuilders.clearAll();
                        }
                        if (status == CompileStatus.SUCCESS) {
                            if (this.compiledRenderResult.get() != null) {
                                this.compiledRenderResult.get().clearBuffers();
                            }
                            this.compiledRenderResult.set(result);
                        }
                        this.compileStatus.set(status);
                    }
                });
    }

    public void dispose() {
        cancelCompilation();
        if (this.compiledRenderResult.get() != null) {
            this.compiledRenderResult.get().clearBuffers();
            this.compiledRenderResult.set(null);
        }
        this.chunkBufferBuilders.discardAll();
        this.compileStatus.set(CompileStatus.CANCELED);

        this.schema = null;
        this.anchorPos = null;
        this.timeout.set(-1);

        this.dirty = false;
    }

    private @Nullable RenderCompileResults checkRecompile(Vec3 cameraPos) {
        CompileStatus status = this.compileStatus.get();
        RenderCompileResults results = this.compiledRenderResult.get();

        if (status != CompileStatus.COMPILING && (status == CompileStatus.CANCELED || this.dirty)) {
            this.dirty = false;
            recompile(cameraPos);
        }

        return results;
    }

    protected void renderBlocks(RenderType renderType, PoseStack poseStack, Vec3 cameraPos) {
        RenderCompileResults compileResults = checkRecompile(cameraPos);
        if (compileResults == null) return;

        renderType.setupRenderState();
        ModelBlockRenderer.enableCaching();

        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            renderType.clearRenderState();
            return;
        }

        for (int i = 0; i < 12; ++i) {
            int textureId = RenderSystem.getShaderTexture(i);
            shader.setSampler("Sampler" + i, textureId);
        }

        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
            shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        }
        if (shader.COLOR_MODULATOR != null) shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (shader.GLINT_ALPHA != null) shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        if (shader.FOG_START != null) shader.FOG_START.set(RenderSystem.getShaderFogStart());
        if (shader.FOG_END != null) shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (shader.FOG_COLOR != null) shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (shader.FOG_SHAPE != null) shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        if (shader.TEXTURE_MATRIX != null) shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (shader.GAME_TIME != null) shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        if (shader.CHUNK_OFFSET != null) {
            shader.CHUNK_OFFSET.set((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        }

        RenderSystem.setupShaderLights(shader);
        shader.apply();

        if (!compileResults.isEmpty(renderType)) {
            VertexBuffer vertexBuffer = compileResults.getOrCreateChunkBuffers().get(renderType);
            if (vertexBuffer.isInvalid() || vertexBuffer.getFormat() == null) return;

            vertexBuffer.bind();
            vertexBuffer.draw();
        }

        shader.clear();
        VertexBuffer.unbind();
        renderType.clearRenderState();
    }

    protected void renderBlockEntities(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                       float partialTick, Vec3 cameraPos) {
        RenderCompileResults compileResults = checkRecompile(cameraPos);
        if (compileResults == null) return;

        for (BlockEntity blockEntity : compileResults.blockEntities) {
            if (blockEntity == null) continue;
            BlockPos pos = blockEntity.getBlockPos();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            Minecraft.getInstance().getBlockEntityRenderDispatcher()
                    .render(blockEntity, partialTick, poseStack, bufferSource);

            poseStack.popPose();
        }

        bufferSource.endBatch(RenderType.solid());
        bufferSource.endBatch(RenderType.endPortal());
        bufferSource.endBatch(RenderType.endGateway());
        bufferSource.endBatch(Sheets.solidBlockSheet());
        bufferSource.endBatch(Sheets.cutoutBlockSheet());
        bufferSource.endBatch(Sheets.bedSheet());
        bufferSource.endBatch(Sheets.shulkerBoxSheet());
        bufferSource.endBatch(Sheets.signSheet());
        bufferSource.endBatch(Sheets.hangingSignSheet());
        bufferSource.endBatch(Sheets.chestSheet());
    }

    protected enum CompileStatus {
        COMPILING,
        SUCCESS,
        CANCELED
    }

    protected class RenderCompileTask {

        private final AtomicBoolean isCanceled = new AtomicBoolean(false);

        public void cancel() {
            this.isCanceled.set(true);
        }

        protected CompletableFuture<RenderCompileResults> compileBlockBuffers(RenderCompileResults compileResults,
                                                                              Vec3 cameraPos) {
            if (this.isCanceled.get()) {
                return CompletableFuture.completedFuture(compileResults.withStatus(CompileStatus.CANCELED));
            }
            if (CinderStructureGhostRenderer.this.schema == null ||
                    CinderStructureGhostRenderer.this.renderLevel == null) {
                return CompletableFuture.completedFuture(compileResults.withStatus(CompileStatus.CANCELED));
            }
            RenderLevel fakeLevel = CinderStructureGhostRenderer.this.renderLevel;
            boolean tintValid = CinderStructureGhostRenderer.this.valid;

            var blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
            ChunkBufferBuilderPack chunkBufferBuilders = CinderStructureGhostRenderer.this.chunkBufferBuilders;

            RandomSource randomSource = RandomSource.create();
            PoseStack poseStack = new PoseStack();
            Set<RenderType> startedBuffers = new ReferenceArraySet<>(RenderType.chunkBufferLayers().size());

            // Same tint for every emitted vertex this pass - the one real addition over GT's own
            // version, which has no concept of build-site validity to show at all. Kept fairly
            // subtle (not a pure flat color) so the real block textures still read clearly through it.
            float tintR = tintValid ? 0.6f : 1.0f;
            float tintG = tintValid ? 1.0f : 0.55f;
            float tintB = tintValid ? 0.6f : 0.55f;

            ModelBlockRenderer.enableCaching();
            for (var blockEntry : CinderStructureGhostRenderer.this.schema) {
                BlockPos pos = blockEntry.getKey();
                BlockState blockState = fakeLevel.getBlockState(pos);
                if (blockState.isAir()) continue;

                if (blockState.hasBlockEntity()) {
                    BlockEntity blockEntity = fakeLevel.getBlockEntity(pos);
                    if (blockEntity != null) {
                        compileResults.blockEntities.add(blockEntity);
                    }
                }

                if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                    BakedModel model = blockRenderDispatcher.getBlockModel(blockState);

                    BlockEntity blockEntity = fakeLevel.getBlockEntity(pos);
                    ModelData modelData = ModelData.EMPTY;
                    if (blockEntity != null) {
                        modelData = blockEntity.getModelData();
                    }
                    modelData = model.getModelData(fakeLevel, pos, blockState, modelData);

                    randomSource.setSeed(blockState.getSeed(pos));

                    for (RenderType renderType : model.getRenderTypes(blockState, randomSource, modelData)) {
                        BufferBuilder builder = chunkBufferBuilders.builder(renderType);
                        if (startedBuffers.add(renderType)) {
                            builder.begin(renderType.mode(), renderType.format());
                        }

                        TintedVertexConsumer tinted = new TintedVertexConsumer(builder);
                        tinted.setTint(tintR, tintG, tintB);

                        poseStack.pushPose();
                        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                        blockRenderDispatcher.renderBatched(blockState, pos, fakeLevel, poseStack, tinted, false,
                                randomSource, modelData, renderType);
                        poseStack.popPose();
                    }
                }
            }

            if (startedBuffers.contains(RenderType.translucent())) {
                BufferBuilder bufferBuilder = chunkBufferBuilders.builder(RenderType.translucent());
                if (!bufferBuilder.isCurrentBatchEmpty()) {
                    bufferBuilder.setQuadSorting(
                            VertexSorting.byDistance((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z));
                }
            }

            for (RenderType renderType : startedBuffers) {
                BufferBuilder.RenderedBuffer renderedBuffer = chunkBufferBuilders.builder(renderType)
                        .endOrDiscardIfEmpty();
                if (renderedBuffer != null) {
                    compileResults.renderedLayers.put(renderType, renderedBuffer);
                }
            }
            ModelBlockRenderer.clearCache();

            if (this.isCanceled.get()) {
                compileResults.renderedLayers.values().forEach(BufferBuilder.RenderedBuffer::release);
                return CompletableFuture.completedFuture(compileResults.withStatus(CompileStatus.CANCELED));
            }

            List<CompletableFuture<Void>> uploads = new ArrayList<>();
            compileResults.renderedLayers.forEach((renderType, buffer) -> {
                uploads.add(uploadChunkLayer(compileResults, buffer, renderType));
                compileResults.hasBlocks.add(renderType);
            });
            return Util.sequenceFailFast(uploads).handle((result, error) -> {
                if (error != null && !(error instanceof CancellationException) &&
                        !(error instanceof InterruptedException)) {
                    Minecraft.getInstance()
                            .delayCrash(CrashReport.forThrowable(error, "Rendering Cinder preview chunk"));
                }
                if (this.isCanceled.get()) {
                    return compileResults.withStatus(CompileStatus.CANCELED);
                } else {
                    return compileResults.withStatus(CompileStatus.SUCCESS);
                }
            });
        }

        protected CompletableFuture<Void> uploadChunkLayer(RenderCompileResults results,
                                                           BufferBuilder.RenderedBuffer builder,
                                                           RenderType renderType) {
            return CompletableFuture.runAsync(() -> {
                VertexBuffer buffer = results.getOrCreateChunkBuffers().get(renderType);
                if (!buffer.isInvalid()) {
                    buffer.bind();
                    buffer.upload(builder);
                    VertexBuffer.unbind();
                }
            }, runnable -> RenderSystem.recordRenderCall(runnable::run));
        }
    }

    protected static class RenderCompileResults {

        protected CompileStatus status = CompileStatus.COMPILING;
        protected final List<BlockEntity> blockEntities = new ArrayList<>();
        protected final Map<RenderType, BufferBuilder.RenderedBuffer> renderedLayers =
                new Reference2ObjectArrayMap<>();
        protected final Set<RenderType> hasBlocks = new ObjectArraySet<>(RenderType.chunkBufferLayers().size());
        private @Nullable Map<RenderType, VertexBuffer> chunkBuffers;

        protected Map<RenderType, VertexBuffer> getOrCreateChunkBuffers() {
            if (this.chunkBuffers == null || this.chunkBuffers.isEmpty()) {
                List<RenderType> chunkRenderTypes = RenderType.chunkBufferLayers();
                this.chunkBuffers = new Reference2ObjectLinkedOpenHashMap<>();
                for (RenderType type : chunkRenderTypes) {
                    this.chunkBuffers.put(type, new VertexBuffer(VertexBuffer.Usage.STATIC));
                }
            }
            return this.chunkBuffers;
        }

        protected void clearBuffers() {
            if (this.chunkBuffers != null && !this.chunkBuffers.isEmpty()) {
                this.chunkBuffers.values().forEach(VertexBuffer::close);
                this.chunkBuffers.clear();
            }
        }

        public boolean isEmpty(RenderType renderType) {
            return !this.hasBlocks.contains(renderType);
        }

        public RenderCompileResults withStatus(CompileStatus status) {
            this.status = status;
            return this;
        }
    }
}
