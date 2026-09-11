package net.phoenix.core.client.renderer.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.block.cinder.CinderConstructionBlockEntity;

import java.util.Map;

/**
 * The client half of the "drop from the sky" construction sequence: while a
 * {@link CinderConstructionBlockEntity} is falling, renders the ENTIRE resolved structure as one
 * rigid ghost batch - not staged layer by layer - dropping from
 * {@link CinderConstructionBlockEntity#FALL_START_HEIGHT} blocks above down to its final position
 * with an accelerating, gravity-like ease, trailing flame particles the whole way down, escorted by a
 * particle-only "phoenix" silhouette (no real creature model exists in this codebase to render, and
 * building/rigging one from scratch is out of scope - see {@link #renderPhoenixSilhouette}) that
 * swoops in from the side, hovers above the structure as it descends, then peels away just before
 * impact. The real blocks all land at once (with their own impact beat) exactly when the fall
 * completes - this is purely the lead-in visual, using the same {@code renderSingleBlock} primitive as
 * {@link net.phoenix.core.client.gui.cinder.CinderStructurePreview}, just driven from world space via
 * a {@link BlockEntityRenderer} instead of GUI space.
 */
public class CinderConstructionRenderer implements BlockEntityRenderer<CinderConstructionBlockEntity> {

    private static final double PHOENIX_SIDE_OFFSET = 12.0;
    private static final double PHOENIX_HOVER_HEIGHT = 6.0;
    private static final int PHOENIX_WING_POINTS = 5;

    public CinderConstructionRenderer(BlockEntityRendererProvider.Context context) {}

    // Logs once per distinct pending-set (not every frame) to confirm this renderer is actually being
    // invoked at all - a BlockEntityRenderer silently never getting resolved for this BE type would
    // otherwise look identical to "the animation is just too subtle to notice".
    private static int lastLoggedSize = -1;

    @Override
    public void render(CinderConstructionBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || be.getFallStartGameTime() < 0) {
            if (lastLoggedSize != 0) {
                PhoenixCore.LOGGER.info("[CinderConstruction] client: not falling (fallStartGameTime={})",
                        be.getFallStartGameTime());
                lastLoggedSize = 0;
            }
            return;
        }

        Map<BlockPos, BlockInfo> pending = be.getPendingBlocks();
        if (pending.size() != lastLoggedSize) {
            PhoenixCore.LOGGER.info("[CinderConstruction] client: rendering {} falling block(s) at {}",
                    pending.size(), be.getBlockPos());
            lastLoggedSize = pending.size();
        }
        if (pending.isEmpty()) return;

        float progress = Mth.clamp((level.getGameTime() - be.getFallStartGameTime() + partialTick) /
                (float) CinderConstructionBlockEntity.FALL_TICKS, 0.0f, 1.0f);
        // Ease-in: starts slow, accelerates like gravity, crashes down hard at the end - reads as an
        // actual drop instead of a gentle float, and (unlike the old per-Y-layer rise, which started
        // only ~2.5 blocks below its own final position and was mostly hidden underground or behind
        // terrain) this is unmissable no matter where the structure sits.
        float eased = progress * progress;
        double yOffset = (1.0 - eased) * CinderConstructionBlockEntity.FALL_START_HEIGHT;

        BlockPos bePos = be.getBlockPos();
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        double topY = -Double.MAX_VALUE;

        for (var entry : pending.entrySet()) {
            BlockPos pos = entry.getKey();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - bePos.getX(), pos.getY() - bePos.getY() + yOffset,
                    pos.getZ() - bePos.getZ());
            blockRenderer.renderSingleBlock(entry.getValue().getBlockState(), poseStack, bufferSource, packedLight,
                    packedOverlay);
            poseStack.popPose();

            // Sparser per-block than the old per-layer version - the whole structure renders at once
            // now (potentially hundreds of blocks), not just one Y-layer's worth.
            if (level.getRandom().nextInt(30) == 0) {
                double px = pos.getX() + level.getRandom().nextDouble();
                double py = pos.getY() + yOffset + level.getRandom().nextDouble();
                double pz = pos.getZ() + level.getRandom().nextDouble();
                level.addParticle(ParticleTypes.FLAME, px, py, pz, 0.0, -0.08, 0.0);
            }

            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
            topY = Math.max(topY, pos.getY());
        }

        renderPhoenixSilhouette(level, minX, maxX, minZ, maxZ, topY, yOffset, progress);
    }

    /**
     * No real Phoenix creature model exists anywhere in this codebase to render here (checked - the
     * only phoenix-shaped assets are wearable GeckoLib wing/chestplate armor, not a standalone
     * entity), and building/rigging one from scratch is well outside what's practical to add as code.
     * This fakes the impression of one instead: a moving cluster of fire particles shaped like a body
     * with drooping wings either side, on a simple three-phase flight path - swoop in from the side
     * and above during the first 75% of the fall, hover roughly overhead as the structure nears the
     * ground, then peel further up and away in the last 15% right before impact, so it reads as "flew
     * it in and left" rather than just hanging there.
     */
    private static void renderPhoenixSilhouette(ClientLevel level, double minX, double maxX, double minZ,
                                                 double maxZ, double topY, double yOffset, float progress) {
        if (minX > maxX) return;

        double centerX = (minX + maxX) / 2.0 + 0.5;
        double centerZ = (minZ + maxZ) / 2.0 + 0.5;
        double structureTopY = topY + yOffset + 1.5;

        float approach = Mth.clamp(progress / 0.75f, 0f, 1f);
        float depart = progress > 0.85f ? (progress - 0.85f) / 0.15f : 0f;

        double sideOffset = PHOENIX_SIDE_OFFSET * (1f - approach);
        double hoverHeight = PHOENIX_HOVER_HEIGHT + PHOENIX_HOVER_HEIGHT * 2 * depart;

        double bodyX = centerX + sideOffset;
        double bodyZ = centerZ + sideOffset * 0.6;
        double bodyY = structureTopY + hoverHeight;

        var random = level.getRandom();
        for (int i = -PHOENIX_WING_POINTS; i <= PHOENIX_WING_POINTS; i++) {
            if (random.nextInt(2) != 0) continue;
            double t = i / (double) PHOENIX_WING_POINTS;
            double wx = bodyX + t * 3.0;
            double wy = bodyY - Math.abs(t) * 0.8;
            double wz = bodyZ - Math.abs(t) * 0.3;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, wx, wy, wz, 0.0, 0.0, 0.0);
        }
        level.addParticle(ParticleTypes.FLAME, bodyX, bodyY, bodyZ, 0.0, 0.0, 0.0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, bodyX, bodyY + 0.3, bodyZ + 0.5, 0.0, 0.0, 0.0);
        }
    }
}
