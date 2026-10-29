package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
        // whatever was drawn before may have left the depth test off or the depth buffer unwritten: a screen must hide the
        // ones behind it
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);
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
        // drawing the picture into its framebuffer changes the state again
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);
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
        try (var ignored = net.phoenix.continuum.client.ContinuumProfiler.scope("cinema:page")) {
            renderPage(blockEntity, level, poseStack, bufferSource, solo ? 0.0f : du, solo ? 0.0f : dv, halfSize,
                    solo || layout.isCenterCell());
        }

        poseStack.popPose();
    }

    /** How squarely the player is looking at something {@code (-dx, -dy, -dz)} away: 1 straight at it, 0 sideways on. */
    private static double lookDot(net.minecraft.world.phys.Vec3 look, double dx, double dy, double dz) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return len < 1.0e-4 ? 1.0 : -(look.x * dx + look.y * dy + look.z * dz) / len;
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
    private static final float LAYER = 0.003f;

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

        // live Continuum pictures, turned to the way the player looks at them (drawn first: it changes the render target)
        int[] live = new int[layout.pictures.size()];
        boolean anyLive = false;
        for (int i = 0; i < live.length; i++) {
            live[i] = -1;
            CinemaLayout.Slot slot = layout.pictures.get(i);
            if (slot.picture().item() || !CinemaContinuumPictures.is(slot.picture().id()) || mc.player == null) continue;
            anyLive = true;
            Direction face = be.getBlockState().getValue(CinemaScreenBlock.FACING);
            double nx = face.getStepX();
            double nz = face.getStepZ();
            double rx = nz;
            double rz = -nx;
            var eye = mc.player.getEyePosition();
            double cx = be.getBlockPos().getX() + 0.5 + rx * ((slot.rect().x0() + slot.rect().x1()) / 2 - du) + nx * FACE_OFFSET;
            double cy = be.getBlockPos().getY() + 0.5 + ((slot.rect().y0() + slot.rect().y1()) / 2 - dv);
            double cz = be.getBlockPos().getZ() + 0.5 + rz * ((slot.rect().x0() + slot.rect().x1()) / 2 - du) + nz * FACE_OFFSET;
            double dx = eye.x - cx;
            double dy = eye.y - cy;
            double dz = eye.z - cz;
            double lateral = dx * rx + dz * rz;
            double depth = dx * nx + dz * nz;
            // the body turns away from a viewer who is off to one side, so that side of it is the one they see
            float yaw = (float) Math.max(-80, Math.min(80, -Math.toDegrees(Math.atan2(lateral, Math.max(0.05, depth)))));
            float pitch = (float) Math.max(-60, Math.min(60, Math.toDegrees(Math.atan2(dy, Math.hypot(lateral, depth)))));
            live[i] = CinemaContinuumPictures.texture(slot.picture().id(), yaw, pitch, dx * dx + dy * dy + dz * dz,
                    lookDot(mc.player.getLookAngle(), dx, dy, dz));
        }
        if (anyLive) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(true);
        }

        Matrix4f m = poseStack.last().pose();
        int accent = shown.getTextColor() | 0xFF000000;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Clip clip = new Clip(du, dv, half + 0.002f);
        beginBatch();
        // pulled towards the viewer by the depth buffer itself, so it holds at any distance

        // solid under the title strip, see-through in it so the screen's own background shows there
        if (layout.header != null) {
            fill(m, new CinemaLayout.Rect(layout.plate.x0(), layout.plate.y0(), layout.plate.x1(), layout.header.y0()),
                    PLATE_Z, 0xFF090C16, clip);
            fill(m, layout.header, PLATE_Z, 0xA8090C16, clip);
        } else {
            fill(m, layout.plate, PLATE_Z, 0xFF090C16, clip);
        }
        border(m, layout.plate, PLATE_Z + LAYER, 0.014f, (accent & 0xFFFFFF) | 0xFF000000, clip);
        // a lighter tile behind each picture, so a dark icon does not vanish into the dark plate
        for (CinemaLayout.Slot slot : layout.pictures) {
            fill(m, slot.rect().grow(0.07f), PLATE_Z + LAYER, 0xFF2A3350, clip);
            border(m, slot.rect().grow(0.07f), PLATE_Z + 2 * LAYER, 0.01f, 0xFF5A6EA0, clip);
        }
        for (CinemaLayout.Hit hit : layout.hits) {
            boolean nav = hit.action().equals("next") || hit.action().equals("prev");
            boolean hot = hit == hovered;
            if (hit.label().isEmpty() && !nav) {
                // a picture: only lit up when pointed at
                if (hot) border(m, hit.rect(), PLATE_Z + 4 * LAYER, 0.02f, 0xFFFFFFFF, clip);
                continue;
            }
            fill(m, hit.rect(), PLATE_Z + LAYER, hot ? 0xFF2F5C9E : nav ? 0xFF1E2638 : 0xFF182B4A, clip);
            border(m, hit.rect(), PLATE_Z + 2 * LAYER, 0.012f, hot ? 0xFFBFD9FF : 0xFF6FA8FF, clip);
        }
        flushBatch();
        // pictures: textures here, cut to this block; items are drawn with the text, by the middle block
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        for (int i = 0; i < layout.pictures.size(); i++) {
            CinemaLayout.Slot slot = layout.pictures.get(i);
            if (slot.picture().item()) continue;
            if (CinemaContinuumPictures.is(slot.picture().id())) {
                if (live[i] < 0) continue;
                RenderSystem.setShaderTexture(0, live[i]);
                drawTexture(m, slot.rect(), PLATE_Z + 3 * LAYER, clip, true);
                continue;
            }
            ResourceLocation texture = ResourceLocation.tryParse(slot.picture().id());
            if (texture == null) continue;
            RenderSystem.setShaderTexture(0, texture);
            drawTexture(m, slot.rect(), PLATE_Z + 3 * LAYER, clip, false);
        }
        RenderSystem.enableCull();
        if (!drawText) return;
        for (CinemaLayout.Slot slot : layout.pictures) {
            if (!slot.picture().item()) continue;
            ResourceLocation id = ResourceLocation.tryParse(slot.picture().id());
            if (id == null) continue;
            var stack = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
            if (stack.isEmpty()) continue;
            float size = slot.rect().x1() - slot.rect().x0();
            poseStack.pushPose();
            poseStack.translate((slot.rect().x0() + slot.rect().x1()) / 2, (slot.rect().y0() + slot.rect().y1()) / 2,
                    PLATE_Z + 7 * LAYER);
            // flattened, so a block does not bulge off the screen
            poseStack.scale(size, size, size * 0.1f);
            mc.getItemRenderer().renderStatic(stack, net.minecraft.world.item.ItemDisplayContext.GUI, FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0);
            poseStack.popPose();
        }

        Font font = mc.font;
        float scale = shown.getTextScale();
        for (CinemaLayout.TextLine line : layout.titleLines) drawLine(font, poseStack, bufferSource, line, accent);
        for (CinemaLayout.TextLine line : layout.bodyLines) drawLine(font, poseStack, bufferSource, line, 0xFFDCE2F2);
        if (!layout.scrollLines.isEmpty()) {
            // like the poem at the end of a game: rises from the bottom of the free part, fades at both edges, and starts again
            float viewH = layout.viewTop - layout.viewBottom;
            float cycle = viewH + layout.scrollHeight;
            float s = (float) (((System.nanoTime() / 1.0e9) * layout.scrollSpeed) % cycle);
            float fade = Math.min(0.3f, viewH / 4);
            for (CinemaLayout.TextLine line : layout.scrollLines) {
                float y = layout.viewBottom + line.y() + s;
                float a = Math.min(y - layout.viewBottom, layout.viewTop - y) / fade;
                if (a <= 0.02f) continue;
                int alpha = Math.max(4, Math.min(255, (int) (a * 255)));
                drawLine(font, poseStack, bufferSource, new CinemaLayout.TextLine(line.text(), line.x(), y, line.scale(),
                        line.centred()), (alpha << 24) | 0xDCE2F2);
            }
        }
        if (layout.counter != null) drawLine(font, poseStack, bufferSource, layout.counter, 0xFFAAB4CC);
        if (layout.hint != null) drawLine(font, poseStack, bufferSource, layout.hint, 0xFF7C869E);
        for (CinemaLayout.Hit hit : layout.hits) {
            boolean nav = hit.action().equals("next") || hit.action().equals("prev");
            if (hit.label().isEmpty() && !nav) continue;
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
        poseStack.translate(line.x(), line.y(), PLATE_Z + 6 * LAYER);
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
        BufferBuilder b = batch;
        b.addVertex(m, x0, y0, z).setColor(argb);
        b.addVertex(m, x1, y0, z).setColor(argb);
        b.addVertex(m, x1, y1, z).setColor(argb);
        b.addVertex(m, x0, y1, z).setColor(argb);
    }

    /** All the flat colour of a block (plate, tiles, buttons, borders) goes out as one draw instead of dozens. */
    private static BufferBuilder batch;

    private static void beginBatch() {
        batch = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void flushBatch() {
        var mesh = batch.build();
        batch = null;
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    /** The whole of the texture on a rectangle, cut to the block being drawn (the picture is cut the same way). */
    private static void drawTexture(Matrix4f m, CinemaLayout.Rect r, float z, Clip c, boolean flip) {
        float x0 = Math.max(r.x0(), c.du() - c.half());
        float x1 = Math.min(r.x1(), c.du() + c.half());
        float y0 = Math.max(r.y0(), c.dv() - c.half());
        float y1 = Math.min(r.y1(), c.dv() + c.half());
        if (x1 <= x0 || y1 <= y0) return;
        float w = r.x1() - r.x0();
        float h = r.y1() - r.y0();
        float u0 = (x0 - r.x0()) / w;
        float u1 = (x1 - r.x0()) / w;
        float vTop = (r.y1() - y1) / h;
        float vBottom = (r.y1() - y0) / h;
        // a render target is stored with its bottom row first
        if (flip) {
            vTop = 1.0f - vTop;
            vBottom = 1.0f - vBottom;
        }
        x0 -= c.du();
        x1 -= c.du();
        y0 -= c.dv();
        y1 -= c.dv();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, x0, y0, z).setUv(u0, vBottom);
        b.addVertex(m, x1, y0, z).setUv(u1, vBottom);
        b.addVertex(m, x1, y1, z).setUv(u1, vTop);
        b.addVertex(m, x0, y1, z).setUv(u0, vTop);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void border(Matrix4f m, CinemaLayout.Rect r, float z, float t, int argb, Clip c) {
        fill(m, new CinemaLayout.Rect(r.x0(), r.y1() - t, r.x1(), r.y1()), z, argb, c);
        fill(m, new CinemaLayout.Rect(r.x0(), r.y0(), r.x1(), r.y0() + t), z, argb, c);
        fill(m, new CinemaLayout.Rect(r.x0(), r.y0() + t, r.x0() + t, r.y1() - t), z, argb, c);
        fill(m, new CinemaLayout.Rect(r.x1() - t, r.y0() + t, r.x1(), r.y1() - t), z, argb, c);
    }
}
