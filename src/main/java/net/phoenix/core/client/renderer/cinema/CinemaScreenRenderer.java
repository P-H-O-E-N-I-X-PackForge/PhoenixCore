package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.phoenix.core.common.block.cinema.CinemaPage;
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
        // the front faces the way the block does: a block turned to the east (a yaw of 270) is rotated back by that
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
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
        RenderSystem.enableCull();

        RenderSystem.disableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        // the picture's top is at v = 1 (it was drawn into a framebuffer, which starts at the bottom), and the top row of
        // the screen is row 0: without this the rows of a tall screen are in the wrong order and the seams show
        float vTop = 1.0f - v0;
        float vBottom = 1.0f - v1;
        buffer.addVertex(matrix, -halfSize, -halfSize, 0).setUv(u0, vBottom);
        buffer.addVertex(matrix, halfSize, -halfSize, 0).setUv(u1, vBottom);
        buffer.addVertex(matrix, halfSize, halfSize, 0).setUv(u1, vTop);
        buffer.addVertex(matrix, -halfSize, halfSize, 0).setUv(u0, vTop);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();

        // seen from behind: a plain dark face, not the picture and not the text backwards
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder back = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        back.addVertex(matrix, -halfSize, -halfSize, -PLATE_Z).setColor(0xFF0B0E16);
        back.addVertex(matrix, -halfSize, halfSize, -PLATE_Z).setColor(0xFF0B0E16);
        back.addVertex(matrix, halfSize, halfSize, -PLATE_Z).setColor(0xFF0B0E16);
        back.addVertex(matrix, halfSize, -halfSize, -PLATE_Z).setColor(0xFF0B0E16);
        BufferUploader.drawWithShader(back.buildOrThrow());
        RenderSystem.enableCull();

        // every block draws its own part of the plate and buttons (so it is drawn over its own part of the background,
        // whatever order the blocks are drawn in); the text is drawn once, by the middle block
        float du = layout.col() - (layout.width() - 1) / 2.0f;
        float dv = (layout.height() - 1) / 2.0f - layout.row();
        renderPage(blockEntity, level, poseStack, bufferSource, solo ? 0.0f : du, solo ? 0.0f : dv, halfSize,
                solo || layout.isCenterCell());

        poseStack.popPose();
    }

    private String liveTypedText() {
        boolean cursorOn = (System.currentTimeMillis() / 500L) % 2 == 0;
        return CinemaEditState.getBuffer() + (cursorOn ? "_" : "");
    }

    private static final int FULL_BRIGHT = 0xF000F0;
    /**
     * How far in front of the picture the plate is: a few millimetres, because at a distance the depth buffer cannot tell
     * surfaces a fraction of a millimetre apart (they fought, and the plate showed torn).
     */
    private static final float PLATE_Z = 0.003f;

    /** The page the screen shows now, on a solid plate: title, body, buttons and the page arrows. */
    private void renderPage(CinemaScreenBlockEntity be, Level level, PoseStack poseStack,
                            MultiBufferSource bufferSource, float du, float dv, float half, boolean drawText) {
        Minecraft mc = Minecraft.getInstance();
        boolean editing = CinemaEditState.isEditing(be.getBlockPos());
        CinemaClientState.Target target = level != null ? CinemaClientState.target(level, be.getBlockPos()) : null;
        CinemaScreenBlockEntity shown = target != null ? target.centre() : be;
        CinemaLayout layout;
        if (editing || target == null) {
            float unit = 1.0f;
            layout = CinemaLayout.of(mc.font, CinemaPage.parse(editing ? liveTypedText() : be.getCurrentLine().getString()),
                    be.getTextScale(), unit, unit, 0, 1, true);
        } else {
            layout = CinemaClientState.layout(target);
        }

        CinemaLayout.Hit hovered = null;
        if (target != null && mc.hitResult instanceof BlockHitResult bhr &&
                bhr.getType() == HitResult.Type.BLOCK) {
            CinemaClientState.Target looked = CinemaClientState.target(level, bhr.getBlockPos());
            if (looked != null && looked.centre() == target.centre()) {
                float[] at = CinemaClientState.local(looked, bhr.getLocation());
                hovered = layout.hitAt(at[0], at[1]);
            }
        }

        Matrix4f m = poseStack.last().pose();
        int accent = shown.getTextColor() | 0xFF000000;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Clip clip = new Clip(du, dv, half);
        fill(m, layout.plate, PLATE_Z, 0xF2090C16, clip);
        border(m, layout.plate, PLATE_Z + 0.0005f, 0.014f, (accent & 0xFFFFFF) | 0xE0000000, clip);
        for (CinemaLayout.Hit hit : layout.hits) {
            boolean nav = hit.action().equals("next") || hit.action().equals("prev");
            boolean hot = hit == hovered;
            fill(m, hit.rect(), PLATE_Z + 0.001f, hot ? 0xFF2F5C9E : nav ? 0xFF1E2638 : 0xFF182B4A, clip);
            border(m, hit.rect(), PLATE_Z + 0.0015f, 0.012f, hot ? 0xFFBFD9FF : 0xFF6FA8FF, clip);
        }
        RenderSystem.enableCull();
        if (!drawText) return;

        Font font = mc.font;
        float scale = shown.getTextScale();
        for (CinemaLayout.TextLine line : layout.titleLines) drawLine(font, poseStack, bufferSource, line, accent);
        for (CinemaLayout.TextLine line : layout.bodyLines) drawLine(font, poseStack, bufferSource, line, 0xFFDCE2F2);
        if (layout.counter != null) drawLine(font, poseStack, bufferSource, layout.counter, 0xFFAAB4CC);
        if (layout.hint != null) drawLine(font, poseStack, bufferSource, layout.hint, 0xFF7C869E);
        for (CinemaLayout.Hit hit : layout.hits) {
            boolean nav = hit.action().equals("next") || hit.action().equals("prev");
            float s = scale * (nav ? 1.7f : 1.15f);
            float cx = (hit.rect().x0() + hit.rect().x1()) / 2;
            float cy = (hit.rect().y0() + hit.rect().y1()) / 2;
            String label = nav ? (hit.action().equals("next") ? ">" : "<") : hit.label();
            // a long label shrinks to fit its button
            int width = font.width(label);
            float fit = (hit.rect().x1() - hit.rect().x0() - 0.12f) / Math.max(1, width);
            drawLine(font, poseStack, bufferSource, new CinemaLayout.TextLine(
                    Component.literal(label).getVisualOrderText(), cx, cy, Math.min(s, fit), true), 0xFFFFFFFF);
        }
    }

    private static void drawLine(Font font, PoseStack poseStack, MultiBufferSource bufferSource,
                                 CinemaLayout.TextLine line, int color) {
        poseStack.pushPose();
        poseStack.translate(line.x(), line.y(), PLATE_Z + 0.003);
        poseStack.scale(line.scale(), -line.scale(), line.scale());
        Matrix4f matrix = poseStack.last().pose();
        float x = line.centred() ? -font.width(line.text()) / 2.0f : 0.0f;
        font.drawInBatch(line.text(), x, -4.0f, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0,
                FULL_BRIGHT);
        poseStack.popPose();
    }

    /** The part of the whole screen that one block shows, in blocks from the middle of the screen. */
    private record Clip(float du, float dv, float half) {}

    /** A rectangle given from the middle of the screen, cut to the block being drawn and moved to that block's own middle. */
    private static void fill(Matrix4f m, CinemaLayout.Rect r, float z, int argb, Clip c) {
        float x0 = Math.max(r.x0(), c.du() - c.half());
        float x1 = Math.min(r.x1(), c.du() + c.half());
        float y0 = Math.max(r.y0(), c.dv() - c.half());
        float y1 = Math.min(r.y1(), c.dv() + c.half());
        if (x1 <= x0 || y1 <= y0) return;
        x0 -= c.du();
        x1 -= c.du();
        y0 -= c.dv();
        y1 -= c.dv();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        b.addVertex(m, x0, y0, z).setColor(argb);
        b.addVertex(m, x1, y0, z).setColor(argb);
        b.addVertex(m, x1, y1, z).setColor(argb);
        b.addVertex(m, x0, y1, z).setColor(argb);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void border(Matrix4f m, CinemaLayout.Rect r, float z, float t, int argb, Clip c) {
        fill(m, new CinemaLayout.Rect(r.x0(), r.y1() - t, r.x1(), r.y1()), z, argb, c);
        fill(m, new CinemaLayout.Rect(r.x0(), r.y0(), r.x1(), r.y0() + t), z, argb, c);
        fill(m, new CinemaLayout.Rect(r.x0(), r.y0() + t, r.x0() + t, r.y1() - t), z, argb, c);
        fill(m, new CinemaLayout.Rect(r.x1() - t, r.y0() + t, r.x1(), r.y1() - t), z, argb, c);
    }
}
