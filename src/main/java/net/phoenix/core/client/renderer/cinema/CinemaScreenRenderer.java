package net.phoenix.core.client.renderer.cinema;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import net.phoenix.core.common.block.cinema.CinemaScreenBlock;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import org.joml.Matrix4f;

public class CinemaScreenRenderer implements BlockEntityRenderer<CinemaScreenBlockEntity> {

    private static final float SOLO_HALF_SIZE = 0.4f;
    private static final float GROUPED_HALF_SIZE = 0.5f;

    private static final float FACE_OFFSET = 0.49f;

    public CinemaScreenRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CinemaScreenBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        CinemaGroupUtil.GroupLayout layout = level != null
                ? CinemaGroupUtil.getLayout(level, blockEntity.getBlockPos())
                : new CinemaGroupUtil.GroupLayout(0, 0, 1, 1, blockEntity.getBlockPos());
        boolean solo = layout.width() == 1 && layout.height() == 1;
        float halfSize = solo ? SOLO_HALF_SIZE : GROUPED_HALF_SIZE;

        int textureId = CinemaRenderTarget.getOrRenderTexture(layout);
        Direction facing = blockEntity.getBlockState().getValue(CinemaScreenBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(0.0, 0.0, FACE_OFFSET);

        Matrix4f matrix = poseStack.last().pose();

        float uCellW = 1f / layout.width();
        float vCellH = 1f / layout.height();
        float u0 = layout.col() * uCellW;
        float u1 = u0 + uCellW;
        float v0 = layout.row() * vCellH;
        float v1 = v0 + vCellH;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.disableCull();

        RenderSystem.disableBlend();

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, -halfSize, -halfSize, 0).uv(u0, v1).endVertex();
        buffer.vertex(matrix, halfSize, -halfSize, 0).uv(u1, v1).endVertex();
        buffer.vertex(matrix, halfSize, halfSize, 0).uv(u1, v0).endVertex();
        buffer.vertex(matrix, -halfSize, halfSize, 0).uv(u0, v0).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.enableBlend();
        RenderSystem.enableCull();

        if (solo || layout.isCenterCell()) {
            boolean editingThis = CinemaEditState.isEditing(blockEntity.getBlockPos());
            String text = editingThis ? liveTypedText() : blockEntity.getCurrentLine().getString();
            if (!text.isEmpty()) {
                renderText(text, blockEntity, poseStack, bufferSource, packedLight, halfSize);
            }
        }

        poseStack.popPose();
    }

    private String liveTypedText() {
        boolean cursorOn = (System.currentTimeMillis() / 500L) % 2 == 0;
        return CinemaEditState.getBuffer() + (cursorOn ? "_" : "");
    }

    private void renderText(String text, CinemaScreenBlockEntity blockEntity, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, float halfSize) {
        Component line = Component.literal(text);
        float scale = blockEntity.getTextScale();

        poseStack.pushPose();

        poseStack.translate(0.0, halfSize * 0.7, 0.001);
        poseStack.scale(scale, -scale, scale);

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = poseStack.last().pose();

        int width = font.width(line);

        float halfWidthFontSpace = halfSize / scale;
        float margin = 4.0f;
        float x = switch (blockEntity.getTextAlign()) {
            case LEFT -> -halfWidthFontSpace + margin;
            case RIGHT -> halfWidthFontSpace - width - margin;
            case CENTER -> -width / 2f;
        };

        font.drawInBatch(line, x, 0, blockEntity.getTextColor(), false, matrix, bufferSource,
                Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }
}
