package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.phoenix.core.common.block.cinema.CinemaScreenBlock;
import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import java.util.List;

public class CinemaScreenRenderer implements BlockEntityRenderer<CinemaScreenBlockEntity> {

    private static final float SOLO_HALF_SIZE = 0.4f;
    private static final float GROUPED_HALF_SIZE = 0.5f;

    private static final float FACE_OFFSET = 0.49f;

    public CinemaScreenRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CinemaScreenBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        CinemaGroupUtil.GroupLayout layout = level != null ?
                CinemaGroupUtil.getLayout(level, blockEntity.getBlockPos()) :
                new CinemaGroupUtil.GroupLayout(0, 0, 1, 1, blockEntity.getBlockPos());
        boolean solo = layout.width() == 1 && layout.height() == 1;
        float halfSize = solo ? SOLO_HALF_SIZE : GROUPED_HALF_SIZE;

        CinemaScreenBlockEntity backgroundSource = blockEntity;
        if (!solo && layout.anchor() != null && level != null &&
                level.getBlockEntity(layout.anchor()) instanceof CinemaScreenBlockEntity anchorBE) {
            backgroundSource = anchorBE;
        }
        int textureId = CinemaRenderTarget.getOrRenderTexture(layout, backgroundSource.getBackground());
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
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -halfSize, -halfSize, 0).setUv(u0, v1);
        buffer.addVertex(matrix, halfSize, -halfSize, 0).setUv(u1, v1);
        buffer.addVertex(matrix, halfSize, halfSize, 0).setUv(u1, v0);
        buffer.addVertex(matrix, -halfSize, halfSize, 0).setUv(u0, v0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.enableCull();

        if (solo || layout.isCenterCell()) {
            boolean editingThis = CinemaEditState.isEditing(blockEntity.getBlockPos());
            String text = editingThis ? liveTypedText() : blockEntity.getCurrentLine().getString();

            if (!editingThis) {
                text = CinemaStatTokens.resolve(text, Minecraft.getInstance().player);
            }
            if (!text.isEmpty()) {
                renderText(text, blockEntity, poseStack, bufferSource, packedLight, halfSize, layout.width());
            }
        }

        poseStack.popPose();
    }

    private String liveTypedText() {
        boolean cursorOn = (System.currentTimeMillis() / 500L) % 2 == 0;
        return CinemaEditState.getBuffer() + (cursorOn ? "_" : "");
    }

    private void renderText(String text, CinemaScreenBlockEntity blockEntity, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, float halfSize, int widthCells) {
        Component line = Component.literal(text);
        float scale = blockEntity.getTextScale();
        Font font = Minecraft.getInstance().font;

        float margin = 4.0f;
        // the whole screen is the room the text has, so a long line wraps instead of running off the edge
        float halfWidthFontSpace = widthCells * halfSize / scale;
        int maxWidth = Math.max(16, (int) (halfWidthFontSpace * 2.0f - margin * 2.0f));
        List<FormattedCharSequence> lines = font.split(line, maxWidth);
        if (lines.isEmpty()) return;

        poseStack.pushPose();
        // one line sits a little above the middle as it always did; several are centred on it
        poseStack.translate(0.0, lines.size() == 1 ? halfSize * 0.7 : 0.0, 0.001);
        poseStack.scale(scale, -scale, scale);
        Matrix4f matrix = poseStack.last().pose();

        int lineHeight = font.lineHeight + 2;
        float top = -(lines.size() - 1) * lineHeight / 2.0f;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence part = lines.get(i);
            int width = font.width(part);
            float x = switch (blockEntity.getTextAlign()) {
                case LEFT -> -halfWidthFontSpace + margin;
                case RIGHT -> halfWidthFontSpace - width - margin;
                case CENTER -> -width / 2f;
            };
            font.drawInBatch(part, x, top + i * lineHeight, blockEntity.getTextColor(), false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, packedLight);
        }

        poseStack.popPose();
    }
}
