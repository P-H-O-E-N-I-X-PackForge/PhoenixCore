package net.phoenix.core.client.renderer.cinder;

import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

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

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Map;

public class CinderConstructionRenderer implements BlockEntityRenderer<CinderConstructionBlockEntity> {

    private static final double PHOENIX_SIDE_OFFSET = 12.0;
    private static final double PHOENIX_HOVER_HEIGHT = 6.0;
    private static final int PHOENIX_WING_POINTS = 5;

    public CinderConstructionRenderer(BlockEntityRendererProvider.Context context) {}

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
