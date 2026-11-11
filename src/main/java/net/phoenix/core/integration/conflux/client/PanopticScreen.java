package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.panoptic.C2SPanopticAnswerPacket;
import net.phoenix.core.integration.conflux.panoptic.PanopticTreeModel;
import net.phoenix.core.integration.conflux.panoptic.S2CPanopticStatePacket;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class PanopticScreen extends Screen {

    private static final int GROWTH_STEPS = 240;

    private static final float INTRO_SECONDS = 19f;
    private static final float T_OPEN = 11.6f;
    private static final float T_GROW = 13.8f;
    private static final float T_UI = 16.6f;
    private static final float[] BEATS = { 0.9f, 1.25f, 2.4f, 2.75f, 3.8f, 4.1f, 5.1f, 5.4f, 6.3f, 6.6f, 7.4f, 7.7f,
            8.4f, 8.7f, 9.4f, 9.7f, 10.3f, 10.6f, 11.1f, 11.35f };

    private S2CPanopticStatePacket state;

    private float shownGrowth;
    private long lastNanos = -1L;
    private float seconds;
    private float flash;
    private float shock = -1f;
    private int[] solutionValues;
    private List<String> solutionLines = List.of();
    private int beatIndex;
    private float beatPulse;
    private boolean playedReveal;
    private float glitch;
    private final Random rng = new Random();

    private final List<Integer> picked = new ArrayList<>();
    private int[] amounts = new int[0];

    private String message = "";
    private int messageColor = 0xFFCCCCEE;

    private final List<Hit> hits = new ArrayList<>();

    private int cachedStep = -1;
    private long cachedSeed;
    private String cachedDiscipline;
    private PanopticTreeModel.Params cachedParams;
    private List<PanopticTreeModel.Segment> cachedBranches = List.of();
    private List<PanopticTreeModel.Leaf> cachedLeaves = List.of();
    private float minX;
    private float maxX;
    private float maxY;

    private record Hit(int x, int y, int w, int h, Runnable action) {

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public PanopticScreen(S2CPanopticStatePacket state) {
        super(Component.literal("Panoptic Engine"));
        this.state = state;
        this.shownGrowth = state.growth();
        resetAnswer();
    }

    public BlockPos controller() {
        return state.controller();
    }

    public void update(S2CPanopticStatePacket next) {
        boolean newRound = !next.title().equals(state.title());
        this.state = next;
        if (newRound || next.solved()) resetAnswer();
        if (!next.message().isEmpty()) {
            message = next.message();
            messageColor = next.solved() ? 0xFF7DE8A0 : 0xFFE8A07D;
            if (next.solved()) {
                flash = 1f;
                shock = 0f;
            } else {
                glitch = 1f;
            }
        }
        if (next.complete()) {
            message = "The tree is whole.";
            messageColor = 0xFFFFE08A;
        }
    }

    public void showSolution(net.phoenix.core.integration.conflux.panoptic.S2CPanopticSolutionPacket msg) {
        solutionValues = msg.values();
        solutionLines = msg.lines();
    }

    private void fillSolution() {
        if (solutionValues == null) return;
        if (discipline().equals("void") || discipline().equals("sealed_b")) {
            for (int i = 0; i < amounts.length && i < solutionValues.length; i++) amounts[i] = solutionValues[i];
        } else {
            picked.clear();
            for (int v : solutionValues) picked.add(v);
        }
    }

    private void resetAnswer() {
        picked.clear();
        solutionValues = null;
        solutionLines = List.of();
        amounts = new int[state.choices().size()];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected void renderBlurredBackground(float partialTick) {}

    private String discipline() {
        return state.discipline() == null ? "" : state.discipline();
    }

    private int accent() {
        return ConfluxTheme.disciplineAccent(state.discipline());
    }

    private int panelW() {
        return Mth.clamp((int) (width * 0.26f), 150, 200);
    }

    private static int argb(int rgb, float alpha) {
        return (Mth.clamp((int) (alpha * 255f), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private float treeIntro() {
        float t = Mth.clamp((seconds - T_GROW) / 3.9f, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private void ensureTree() {
        int step = Math.max(1, Math.round(shownGrowth * treeIntro() * GROWTH_STEPS));
        if (step == cachedStep && state.treeSeed() == cachedSeed && discipline().equals(cachedDiscipline)) return;
        cachedStep = step;
        cachedSeed = state.treeSeed();
        cachedDiscipline = discipline();
        cachedParams = PanopticTreeModel.params(state.discipline(), step / (float) GROWTH_STEPS);
        cachedBranches = PanopticTreeModel.branches(cachedSeed, cachedParams);
        cachedLeaves = PanopticTreeModel.leaves(cachedSeed, cachedBranches, cachedParams);
        minX = -1f;
        maxX = 1f;
        maxY = 1f;
        for (PanopticTreeModel.Segment s : cachedBranches) {
            minX = Math.min(minX, Math.min(px(s.x0(), s.z0()), px(s.x1(), s.z1())));
            maxX = Math.max(maxX, Math.max(px(s.x0(), s.z0()), px(s.x1(), s.z1())));
            maxY = Math.max(maxY, Math.max(s.y0(), s.y1()));
        }
        float margin = cachedParams.leafSize();
        minX -= margin;
        maxX += margin;
        maxY += margin;
    }

    private static float px(float x, float z) {
        return x + z * 0.35f;
    }

    private void line(BufferBuilder buf, Matrix4f pose, float x0, float y0, float x1, float y1, float w0, float w1,
                      int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float nx = -dy / len;
        float ny = dx / len;
        buf.addVertex(pose, x0 + nx * w0, y0 + ny * w0, 0).setColor(color);
        buf.addVertex(pose, x0 - nx * w0, y0 - ny * w0, 0).setColor(color);
        buf.addVertex(pose, x1 - nx * w1, y1 - ny * w1, 0).setColor(color);
        buf.addVertex(pose, x1 + nx * w1, y1 + ny * w1, 0).setColor(color);
    }

    private void ring(BufferBuilder buf, Matrix4f pose, float cx, float cy, float r, float thick, float start,
                      float sweep, int color) {
        int segments = Math.max(12, (int) (r / 3f));
        for (int i = 0; i < segments; i++) {
            float a0 = start + sweep * i / segments;
            float a1 = start + sweep * (i + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            buf.addVertex(pose, cx + c0 * (r - thick), cy + s0 * (r - thick), 0).setColor(color);
            buf.addVertex(pose, cx + c0 * (r + thick), cy + s0 * (r + thick), 0).setColor(color);
            buf.addVertex(pose, cx + c1 * (r + thick), cy + s1 * (r + thick), 0).setColor(color);
            buf.addVertex(pose, cx + c1 * (r - thick), cy + s1 * (r - thick), 0).setColor(color);
        }
    }

    private BufferBuilder begin(boolean additive) {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private void end(BufferBuilder buf) {
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    private void drawEye(GuiGraphics g, float cx, float cy, int mx, int my, float open) {
        float r = Math.min(width, height) * 0.46f * open;
        if (r < 4f) return;
        int accent = accent();
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin(true);
        float spin = seconds * 0.05f;
        ring(buf, pose, cx, cy, r, 0.8f, 0f, Mth.TWO_PI, argb(accent, 0.34f));
        ring(buf, pose, cx, cy, r * 0.84f, 0.6f, spin, Mth.TWO_PI * 0.92f, argb(accent, 0.26f));
        ring(buf, pose, cx, cy, r * 0.62f, 0.9f, -spin * 1.4f, Mth.TWO_PI * 0.85f, argb(accent, 0.3f));
        ring(buf, pose, cx, cy, r * 0.36f, 0.6f, spin * 2f, Mth.TWO_PI * 0.75f, argb(accent, 0.34f));
        for (int i = 0; i < 72; i++) {
            float a = spin * 0.6f + i * Mth.TWO_PI / 72f;
            float inner = r * (i % 3 == 0 ? 0.36f : 0.52f);
            float outer = r * (i % 2 == 0 ? 0.96f : 0.78f);
            line(buf, pose, cx + (float) Math.cos(a) * inner, cy + (float) Math.sin(a) * inner,
                    cx + (float) Math.cos(a) * outer, cy + (float) Math.sin(a) * outer, 0.35f, 0.35f,
                    argb(accent, 0.12f));
        }
        end(buf);

        float dx = Mth.clamp((mx - cx) / Math.max(1f, width), -1f, 1f);
        float dy = Mth.clamp((my - cy) / Math.max(1f, height), -1f, 1f);
        float pr = r * 0.2f;
        float pxx = cx + dx * r * 0.14f;
        float pyy = cy + dy * r * 0.14f;
        BufferBuilder dark = begin(false);
        ring(dark, pose, pxx, pyy, pr * 0.5f, pr * 0.5f, 0f, Mth.TWO_PI, 0xE6000000);
        end(dark);
        BufferBuilder rim = begin(true);
        ring(rim, pose, pxx, pyy, pr, 1.2f, 0f, Mth.TWO_PI, argb(accent, 0.5f));
        end(rim);
    }

    private void drawTree(GuiGraphics g, float scale, float cx, float baseY, boolean mirror, float alphaMul) {
        PanopticTreeModel.Params p = cachedParams;
        Matrix4f pose = g.pose().last().pose();
        float flip = mirror ? -0.32f : 1f;
        int bark = PanopticTreeModel.lerpColor(p.trunkColor(), p.glowColor(), 0.3f);

        if (!mirror) {
            BufferBuilder glow = begin(true);
            for (PanopticTreeModel.Segment s : cachedBranches) {
                line(glow, pose, cx + px(s.x0(), s.z0()) * scale, baseY - s.y0() * scale,
                        cx + px(s.x1(), s.z1()) * scale, baseY - s.y1() * scale,
                        Math.max(1.5f, s.radius0() * scale * 3f), Math.max(1.2f, s.radius1() * scale * 3f),
                        argb(p.glowColor(), 0.09f * alphaMul));
            }
            end(glow);
        }

        BufferBuilder wood = begin(false);
        for (PanopticTreeModel.Segment s : cachedBranches) {
            int color = PanopticTreeModel.lerpColor(bark, 0xFFFFFFFF, Math.min(0.4f, s.depth() * 0.06f));
            color = argb(color, alphaMul);
            line(wood, pose, cx + px(s.x0(), s.z0()) * scale, baseY - s.y0() * scale * flip,
                    cx + px(s.x1(), s.z1()) * scale, baseY - s.y1() * scale * flip,
                    Math.max(0.7f, s.radius0() * scale), Math.max(0.5f, s.radius1() * scale), color);
        }
        end(wood);

        if (!mirror) {

            BufferBuilder edge = begin(false);
            for (PanopticTreeModel.Segment s : cachedBranches) {
                float x0 = cx + px(s.x0(), s.z0()) * scale;
                float y0 = baseY - s.y0() * scale;
                float x1 = cx + px(s.x1(), s.z1()) * scale;
                float y1 = baseY - s.y1() * scale;
                float dx = x1 - x0;
                float dy = y1 - y0;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len < 0.01f) continue;
                float nx = -dy / len;
                float ny = dx / len;
                float w0 = Math.max(0.7f, s.radius0() * scale);
                float w1 = Math.max(0.5f, s.radius1() * scale);
                if (w0 < 1.6f) continue;
                int hi = argb(PanopticTreeModel.lerpColor(bark, 0xFFFFFFFF, 0.5f), 0.7f * alphaMul);
                line(edge, pose, x0 + nx * w0 * 0.5f, y0 + ny * w0 * 0.5f, x1 + nx * w1 * 0.5f, y1 + ny * w1 * 0.5f,
                        w0 * 0.2f, w1 * 0.2f, hi);
            }
            end(edge);
        }

        BufferBuilder leaves = begin(true);
        float pulse = 0.8f + 0.2f * (float) Math.sin(seconds * 1.6f);
        for (PanopticTreeModel.Leaf leaf : cachedLeaves) {
            float ox = 0;
            float oy = 0;
            float alpha = 0.7f * pulse * alphaMul;
            float[] fall = PanopticTreeModel.leafFall(leaf, p, seconds);
            if (fall != null) {
                ox = fall[0];
                oy = fall[1];
                float drop = Math.min(1f, -oy / Math.max(1f, p.height() * 0.5f));
                alpha *= 1f - drop * 0.9f;
            }
            int color = PanopticTreeModel.lerpColor(p.leafColorA(), p.leafColorB(), leaf.phase());
            float x = cx + px(leaf.x() + ox, leaf.z()) * scale;
            float y = baseY - (leaf.y() + oy) * scale * flip;
            float r = Math.max(1f, leaf.size() * 0.2f * scale);
            int c = argb(color, alpha);
            leaves.addVertex(pose, x, y - r, 0).setColor(c);
            leaves.addVertex(pose, x - r * 0.7f, y, 0).setColor(c);
            leaves.addVertex(pose, x, y + r, 0).setColor(c);
            leaves.addVertex(pose, x + r * 0.7f, y, 0).setColor(c);

            int halo = argb(color, alpha * 0.14f);
            float hr = r * 2.6f;
            leaves.addVertex(pose, x, y - hr, 0).setColor(halo);
            leaves.addVertex(pose, x - hr, y, 0).setColor(halo);
            leaves.addVertex(pose, x, y + hr, 0).setColor(halo);
            leaves.addVertex(pose, x + hr, y, 0).setColor(halo);
        }
        end(leaves);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        ConfluxTheme.refresh();
        long now = System.nanoTime();
        float dt = lastNanos < 0 ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        seconds += dt;
        flash = Math.max(0f, flash - dt * 0.8f);
        glitch = Math.max(0f, glitch - dt * 1.6f);
        shownGrowth += (state.growth() - shownGrowth) * Math.min(1f, dt * 1.1f);
        if (Math.abs(state.growth() - shownGrowth) < 0.0005f) shownGrowth = state.growth();

        hits.clear();
        float intro = seconds / INTRO_SECONDS;
        tickCinematic(dt);
        float reveal = Mth.clamp((seconds - (T_OPEN + 0.3f)) / 3f, 0f, 1f);
        float ui = Mth.clamp((seconds - T_UI) / 2.2f, 0f, 1f);

        g.fill(0, 0, width, height, 0xFF020204);
        float cx = width / 2f;
        float eyeY = height * 0.42f;

        float push = 1f + 0.05f * easeOut(reveal) + 0.004f * (float) Math.sin(seconds * 0.6f);
        float parX = Mth.clamp((mouseX - cx) / width, -1f, 1f) * -7f * reveal;
        float parY = Mth.clamp((mouseY - height / 2f) / height, -1f, 1f) * -4f * reveal;

        g.pose().pushPose();
        if (glitch > 0f)
            g.pose().translate((rng.nextFloat() - 0.5f) * 6f * glitch, (rng.nextFloat() - 0.5f) * 3f * glitch,
                    0);
        g.pose().translate(cx + parX, height / 2f + parY, 0);
        g.pose().scale(push, push, 1f);
        g.pose().translate(-cx, -height / 2f, 0);

        float breathe = 1f + 0.015f * (float) Math.sin(seconds * 1.1f);
        drawEye(g, cx, eyeY, mouseX, mouseY, easeOut(reveal) * breathe);

        ensureTree();
        float areaTop = height * 0.12f;
        float areaBottom = height * 0.84f;
        float scale = Math.min(30f, Math.min(width * 0.8f / Math.max(1f, maxX - minX),
                (areaBottom - areaTop) / Math.max(1f, maxY)));
        float treeCx = cx - (minX + maxX) / 2f * scale;
        float baseY = areaBottom;
        float crownY = baseY - maxY * 0.82f * scale;
        if (reveal > 0f) {
            drawRays(g, cx, crownY, baseY, reveal);
            drawTree(g, scale, treeCx, baseY, true, 0.22f * reveal);
            drawTree(g, scale, treeCx, baseY, false, reveal);

            g.fillGradient((int) (cx - width * 0.36f), (int) baseY - 10, (int) (cx + width * 0.36f), (int) baseY + 1,
                    0x00000000, argb(accent(), 0.1f * reveal));
            g.fill((int) (cx - width * 0.3f), (int) baseY, (int) (cx + width * 0.3f), (int) baseY + 1,
                    argb(accent(), 0.3f * reveal));
        }
        drawEmbers(g, reveal);
        drawMemories(g, reveal);
        drawShock(g, cx, baseY - maxY * 0.5f * scale, dt);

        g.fillGradient(0, 0, width, (int) (height * 0.2f), 0xCC000000, 0x00000000);
        g.fillGradient(0, (int) (height * 0.8f), width, height, 0x00000000, 0xCC000000);
        g.pose().popPose();

        drawLids(g, eyeY);
        drawHeartbeat(g, cx, eyeY);

        int bars = (int) (height * 0.075f * easeOut(Mth.clamp(seconds / 1.4f, 0f, 1f)));
        g.fill(0, 0, width, bars, 0xFF000000);
        g.fill(0, height - bars, width, height, 0xFF000000);

        if (ui > 0f) {
            drawLeftPanel(g, ui);
            drawSolution(g, ui);
            drawRightPanel(g, mouseX, mouseY, ui);
            drawFooter(g, mouseX, mouseY, ui);
            if (!state.title().isEmpty() && !state.complete()) {
                g.pose().pushPose();
                g.pose().translate(cx, bars + 10, 0);
                g.pose().scale(1.7f, 1.7f, 1f);
                g.drawCenteredString(font, state.title(), 0, 0, argb(accent(), ui));
                g.pose().popPose();
            }
        }

        if (glitch > 0f) drawGlitch(g);
        if (flash > 0f) g.fill(0, 0, width, height, argb(accent(), 0.35f * flash));
        if (intro < 1f) drawIntro(g, intro);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void fadePanel(GuiGraphics g, int x, int w, boolean leftEdge, float ui) {
        int strip = 3;
        for (int i = 0; i < w; i += strip) {
            float t = i / (float) w;
            float a = 0.8f * (float) Math.pow(1f - t, 1.4f) * ui;
            int sx = leftEdge ? x + i : x + w - i - strip;
            g.fill(sx, 0, sx + strip, height, argb(0x03030A, a));
        }
    }

    private void drawEmbers(GuiGraphics g, float reveal) {
        if (reveal <= 0f) return;
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin(true);
        int accent = accent();
        for (int i = 0; i < 110; i++) {
            float seed = i * 0.6180339f;
            float x = ((seed * 7.3f) % 1f) * width + (float) Math.sin(seconds * 0.35f + i) * 16f;
            float speed = 9f + (i % 9) * 3.4f;
            float y = height + 12f - ((seconds * speed + i * 53f) % (height + 24f));
            float twinkle = 0.3f + 0.35f * (float) Math.sin(seconds * 2.2f + i * 1.7f);
            float r = 0.8f + (i % 4) * 0.45f;
            int c = argb(i % 3 == 0 ? 0xFFFFFF : accent, Math.max(0f, twinkle) * reveal);
            buf.addVertex(pose, x - r, y - r, 0).setColor(c);
            buf.addVertex(pose, x - r, y + r, 0).setColor(c);
            buf.addVertex(pose, x + r, y + r, 0).setColor(c);
            buf.addVertex(pose, x + r, y - r, 0).setColor(c);
        }
        end(buf);
    }

    private void drawRays(GuiGraphics g, float cx, float crownY, float baseY, float reveal) {
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin(true);
        int accent = accent();
        float strength = 0.04f + 0.07f * shownGrowth;
        for (int i = 0; i < 9; i++) {
            float spread = (i - 4) / 4f;
            float bottomX = cx + spread * width * 0.32f + (float) Math.sin(seconds * 0.25f + i) * 12f;
            float halfTop = 3f + (i % 3) * 2f;
            float halfBottom = 18f + (i % 4) * 9f;
            float pulse = 0.65f + 0.35f * (float) Math.sin(seconds * 0.7f + i * 1.3f);
            int top = argb(accent, strength * pulse * reveal);
            int bottom = argb(accent, 0f);
            buf.addVertex(pose, cx - halfTop, crownY, 0).setColor(top);
            buf.addVertex(pose, cx + halfTop, crownY, 0).setColor(top);
            buf.addVertex(pose, bottomX + halfBottom, baseY, 0).setColor(bottom);
            buf.addVertex(pose, bottomX - halfBottom, baseY, 0).setColor(bottom);
        }
        end(buf);
    }

    private void drawShock(GuiGraphics g, float cx, float cy, float dt) {
        if (shock < 0f) return;
        shock += dt * 0.7f;
        if (shock > 1f) {
            shock = -1f;
            return;
        }
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin(true);
        float reach = Math.max(width, height) * 0.8f;
        for (int k = 0; k < 2; k++) {
            float t = Mth.clamp(shock - k * 0.12f, 0f, 1f);
            if (t <= 0f) continue;
            float radius = easeOut(t) * reach;
            ring(buf, pose, cx, cy, radius, 1f + 5f * (1f - t), 0f, Mth.TWO_PI, argb(accent(), (1f - t) * 0.55f));
        }
        end(buf);
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private void drawGlitch(GuiGraphics g) {
        for (int i = 0; i < 9; i++) {
            int y = rng.nextInt(Math.max(1, height));
            int h = 1 + rng.nextInt(6);
            int shift = (int) ((rng.nextFloat() - 0.5f) * 40f * glitch);
            g.fill(Math.max(0, shift), y, Math.min(width, width + shift), y + h, argb(0xE8503A, 0.25f * glitch));
        }
    }

    private void drawMemories(GuiGraphics g, float reveal) {
        List<String> memories = state.memories();
        if (memories.isEmpty() || reveal <= 0f) return;
        for (int i = 0; i < memories.size(); i++) {
            String text = memories.get(i);
            int hash = text.hashCode() * 31 + i;
            float x = width * (0.08f + 0.84f * (((hash >>> 3) & 0xFF) / 255f));
            float cycle = height + 60f;
            float y = height + 20f - ((seconds * (10f + (hash & 7)) + i * 61f) % cycle);
            float fade = (float) Math.sin(Math.PI * Mth.clamp((height + 20f - y) / cycle, 0f, 1f));
            int w = font.width(text);
            g.drawString(font, text, (int) (x - w / 2f), (int) y, argb(accent(), 0.5f * fade * reveal), false);
        }
    }

    private void play(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager()
                .play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private void tickCinematic(float dt) {
        beatPulse = Math.max(0f, beatPulse - dt * 3.2f);
        if (seconds >= INTRO_SECONDS) return;
        while (beatIndex < BEATS.length && seconds >= BEATS[beatIndex]) {
            play(net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, beatIndex % 2 == 0 ? 0.92f : 0.78f, 1f);
            beatPulse = 1f;
            beatIndex++;
        }
        if (!playedReveal && seconds >= T_GROW) {
            playedReveal = true;
            shock = 0f;
            flash = 0.7f;
            play(net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.6f, 0.9f);
            play(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5f, 1f);
        }
    }

    private void drawLids(GuiGraphics g, float eyeY) {
        float t = Mth.clamp((seconds - T_OPEN) / 2.4f, 0f, 1f);
        if (t >= 1f) return;
        float open = t * t * (3f - 2f * t);
        float gap = open * height * 0.72f;
        int top = Mth.clamp((int) (eyeY - gap), 0, height);
        int bottom = Mth.clamp((int) (eyeY + gap), 0, height);
        g.fill(0, 0, width, top, 0xFF000000);
        g.fill(0, bottom, width, height, 0xFF000000);
        if (gap > 2f) {
            g.fillGradient(0, top, width, Math.min(height, top + 26), 0xFF000000, 0x00000000);
            g.fillGradient(0, Math.max(0, bottom - 26), width, bottom, 0x00000000, 0xFF000000);
        }
    }

    private void drawHeartbeat(GuiGraphics g, float cx, float cy) {
        float fade = 1f - Mth.clamp((seconds - T_OPEN) / 1.6f, 0f, 1f);
        if (fade <= 0f) return;
        float appear = Mth.clamp((seconds - 0.4f) / 0.6f, 0f, 1f);
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin(true);
        float r = 3f + 16f * beatPulse;
        int accent = accent();
        ring(buf, pose, cx, cy, r, 1f + 5f * beatPulse, 0f, Mth.TWO_PI,
                argb(accent, (0.25f + 0.65f * beatPulse) * fade * appear));
        ring(buf, pose, cx, cy, r * 3.2f, 0.7f, 0f, Mth.TWO_PI, argb(accent, 0.18f * beatPulse * fade * appear));
        end(buf);
        g.fill(0, 0, width, height, argb(accent, 0.06f * beatPulse * fade));
    }

    private record Line(String text, float start, float end, float scale, float y, int color) {}

    private void drawIntro(GuiGraphics g, float intro) {
        Minecraft mc = Minecraft.getInstance();
        String name = mc.player == null ? "You" : mc.player.getName().getString();
        String chosen = discipline().isEmpty() ? "nothing yet" : discipline().replace('_', ' ');
        List<String> memories = state.memories();
        List<Line> lines = new ArrayList<>();
        float h = height;
        lines.add(new Line(name, 2.3f, 6.1f, 2.2f, h * 0.38f, accent()));
        lines.add(new Line("You are being observed.", 3.6f, 6.1f, 1f, h * 0.38f + 34, 0xCCCCEE));
        float y = h * 0.32f;
        float t = 6.4f;
        lines.add(new Line("You chose " + chosen + ".", t, 11.1f, 1f, y, accent()));
        if (!memories.isEmpty()) {
            lines.add(new Line("You began with " + memories.get(0) + ".", t += 0.9f, 11.1f, 1f, y += 16, 0xCCCCEE));
            if (memories.size() > 1) {
                lines.add(new Line("You reached " + memories.get(memories.size() - 1) + ".", t += 0.9f, 11.1f, 1f,
                        y += 16, 0xCCCCEE));
            }
        }
        lines.add(new Line("You have learned " + state.known() + " things.", t += 0.9f, 11.1f, 1f, y += 16, 0xCCCCEE));
        lines.add(new Line("None of it was forgotten.", t += 0.9f, 11.1f, 1f, y += 16, 0x9999BB));
        lines.add(new Line("Open.", 11.0f, 12.7f, 2.4f, h * 0.44f, accent()));

        for (Line line : lines) {
            if (seconds < line.start() || seconds > line.end()) continue;
            float typed = (seconds - line.start()) * 22f;
            String text = line.text().substring(0, Math.min(line.text().length(), (int) typed));
            float a = Mth.clamp((seconds - line.start()) / 0.35f, 0f, 1f) *
                    Mth.clamp((line.end() - seconds) / 0.6f, 0f, 1f);
            if (a <= 0f || text.isEmpty()) continue;

            float jitter = ((int) (seconds * 31f) % 23 == 0) ? 2f : 0f;
            g.pose().pushPose();
            g.pose().translate(width / 2f + jitter, line.y(), 0);
            g.pose().scale(line.scale(), line.scale(), 1f);
            int w = font.width(text);
            g.drawString(font, text, (int) (-w / 2f) - 1, 0, argb(0xFF3050, a * 0.32f), false);
            g.drawString(font, text, (int) (-w / 2f) + 1, 0, argb(0x30D0FF, a * 0.32f), false);
            g.drawString(font, text, (int) (-w / 2f), 0, argb(line.color(), a), false);
            g.pose().popPose();
        }
        float skip = Mth.clamp(1f - seconds / INTRO_SECONDS, 0f, 1f);
        g.drawCenteredString(font, "click to skip", width / 2, height - 18, argb(0x777799, 0.5f * skip));
    }

    private void drawLeftPanel(GuiGraphics g, float ui) {
        int pw = panelW();
        int x = 14;
        int y = 22;
        int w = pw - 20;
        fadePanel(g, 0, pw, true, ui);
        g.drawString(font, "PANOPTIC", x, y, argb(accent(), ui), false);
        y += 11;
        String stage = state.complete() ? "complete" :
                "round " + Math.min(PanopticTreeModel.STAGES, state.stage() + 1) +
                        " of " + PanopticTreeModel.STAGES;
        g.drawString(font, stage, x, y, argb(0x777799, ui), false);
        y += 22;
        if (state.title().isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.literal(state.complete() ?
                    "It has seen you, and you have seen it." : "It has nothing to ask yet. Choose a discipline."), w)) {
                g.drawString(font, line, x, y, argb(0xCCCCEE, ui), false);
                y += 10;
            }
            return;
        }
        g.drawString(font, state.title(), x, y, argb(0xCCCCEE, ui), false);
        y += 20;
        for (String clue : state.clues()) {
            for (FormattedCharSequence line : font.split(Component.literal(clue), w)) {
                g.drawString(font, line, x, y, argb(0x9999BB, ui), false);
                y += 10;
            }
            y += 7;
        }
    }

    private void drawSolution(GuiGraphics g, float ui) {
        if (solutionLines.isEmpty()) return;
        int x = 14;
        int y = height - 14 - solutionLines.size() * 10 - 14;
        g.drawString(font, "ANSWER (dev)", x, y, argb(0xE8C060, ui), false);
        y += 12;
        for (String line : solutionLines) {
            g.drawString(font, font.plainSubstrByWidth(line, panelW() - 20), x, y, argb(0xE8D8A0, ui), false);
            y += 10;
        }
    }

    private void drawRightPanel(GuiGraphics g, int mx, int my, float ui) {
        int pw = panelW();
        int x = width - pw + 10;
        fadePanel(g, width - pw, pw, false, ui);
        if (state.title().isEmpty() || state.complete()) return;
        switch (discipline()) {
            case "phoenix" -> phoenixWidget(g, x, 22, mx, my);
            case "sculk" -> sculkWidget(g, x, 22, mx, my);
            case "void" -> voidWidget(g, x, 22, mx, my);
            case "sealed_a" -> sculkWidget(g, x, 22, mx, my, "Tumblers", "Kinds", "Turn");
            case "sealed_b" -> vigilWidget(g, x, 22, mx, my);
            default -> {}
        }
    }

    private void drawFooter(GuiGraphics g, int mx, int my, float ui) {
        int cx = width / 2;
        if (!message.isEmpty()) g.drawCenteredString(font, message, cx, height - 40, messageColor);
        int barW = 160;
        g.fill(cx - barW / 2, height - 24, cx + barW / 2, height - 22, argb(0x3A3A55, ui));
        g.fill(cx - barW / 2, height - 24, cx - barW / 2 + (int) (barW * state.growth()), height - 22,
                argb(accent(), ui));
        button(g, width - panelW() + 10, height - 28, 52, 15, "Leave", mx, my, true, this::onClose);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasPermissions(2)) {
            String as = state.discipline() == null ? "none" : state.discipline().replace('_', ' ');
            button(g, width - panelW() + 10, height - 68, panelW() - 22, 15, "Puzzle: " + as + " (switch)", mx, my,
                    true,
                    () -> PacketDistributor.sendToServer(
                            new net.phoenix.core.integration.conflux.panoptic.C2SPanopticDevDisciplinePacket(
                                    state.controller())));
        }
        if (mc.player != null && mc.player.hasPermissions(2) && !state.title().isEmpty() && !state.complete()) {
            button(g, width - panelW() + 10, height - 48, 84, 15, "Show answer", mx, my, true,
                    () -> PacketDistributor.sendToServer(
                            new net.phoenix.core.integration.conflux.panoptic.C2SPanopticRevealPacket(
                                    state.controller())));
            if (solutionValues != null) {
                button(g, width - panelW() + 98, height - 48, 40, 15, "Fill", mx, my, true, this::fillSolution);
            }
        }
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean enabled,
                        Runnable action) {
        boolean over = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, over ? argb(accent(), 0.28f) : argb(0x0A0A15, 0.8f));
        g.renderOutline(x, y, w, h, enabled ? argb(accent(), 0.8f) : 0xFF3A3A55);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, enabled ? 0xFFCCCCEE : 0xFF3A3A55);
        if (enabled) hits.add(new Hit(x, y, w, h, action));
    }

    private int innerW() {
        return panelW() - 22;
    }

    private void phoenixWidget(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, "Order of lighting", x, y, 0xFFCCCCEE, false);
        y += 14;
        for (int i = 0; i < state.slots(); i++) {
            String text = i < picked.size() ? (i + 1) + ". " + state.choices().get(picked.get(i)) : (i + 1) + ". -";
            final int slot = i;
            int w = innerW();
            g.fill(x, y, x + w, y + 12, argb(0x0A0A15, 0.8f));
            g.drawString(font, font.plainSubstrByWidth(text, w - 4), x + 2, y + 2,
                    i < picked.size() ? accent() : 0xFF3A3A55, false);
            if (i < picked.size()) hits.add(new Hit(x, y, w, 12, () -> picked.remove(slot)));
            y += 14;
        }
        y += 6;
        g.drawString(font, "Embers", x, y, 0xFF777799, false);
        y += 12;
        for (int i = 0; i < state.choices().size(); i++) {
            final int choice = i;
            boolean used = picked.contains(i);
            button(g, x, y, innerW(), 14, font.plainSubstrByWidth(state.choices().get(i), innerW() - 8), mx, my,
                    !used, () -> picked.add(choice));
            y += 16;
        }
        y += 4;
        submitRow(g, x, y, mx, my, picked.size() == state.slots(), "Kindle");
        costLines(g, x, y + 22);
    }

    private void sculkWidget(GuiGraphics g, int x, int y, int mx, int my) {
        sculkWidget(g, x, y, mx, my, "Echo", "Pulse", "Sound");
    }

    private void sculkWidget(GuiGraphics g, int x, int y, int mx, int my, String title, String pick, String submit) {
        g.drawString(font, title, x, y, 0xFFCCCCEE, false);
        y += 14;
        int box = 18;
        int perRow = Math.max(1, innerW() / (box + 2));
        for (int i = 0; i < state.slots(); i++) {
            int bx = x + (i % perRow) * (box + 2);
            int by = y + (i / perRow) * (box + 2);
            boolean filled = i < picked.size();
            int color = filled ? 0xFF000000 | ConfluxTheme.typeColor(ConfluxDataType.values()[picked.get(i)]) :
                    0xCC0A0A15;
            g.fill(bx, by, bx + box, by + box, color);
            g.renderOutline(bx, by, box, box, filled ? 0xFFCCCCEE : 0xFF3A3A55);
            final int slot = i;
            if (filled) hits.add(new Hit(bx, by, box, box, () -> picked.remove(slot)));
        }
        y += ((state.slots() + perRow - 1) / perRow) * (box + 2) + 10;
        g.drawString(font, pick, x, y, 0xFF777799, false);
        y += 12;
        for (int i = 0; i < state.choices().size(); i++) {
            final int type = i;
            int color = 0xFF000000 | ConfluxTheme.typeColor(ConfluxDataType.values()[i]);
            int w = innerW();
            boolean room = picked.size() < state.slots();
            boolean over = room && mx >= x && mx < x + w && my >= y && my < y + 16;
            g.fill(x, y, x + w, y + 16, over ? color : 0xCC0A0A15);
            g.fill(x, y, x + 4, y + 16, color);
            g.drawString(font, state.choices().get(i), x + 9, y + 4, room ? 0xFFCCCCEE : 0xFF3A3A55, false);
            if (room) hits.add(new Hit(x, y, w, 16, () -> picked.add(type)));
            y += 18;
        }
        y += 4;
        submitRow(g, x, y, mx, my, picked.size() == state.slots(), submit);
        costLines(g, x, y + 22);
    }

    private void vigilWidget(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, "The vigil", x, y, 0xFFCCCCEE, false);
        y += 14;
        String[] levels = { "silent", "soft", "loud" };
        float[] heights = { 0.12f, 0.5f, 1f };
        int w = innerW();
        for (int i = 0; i < state.choices().size(); i++) {
            final int beat = i;
            int rowH = 18;
            g.fill(x, y, x + w, y + rowH, 0xCC0A0A15);

            int level = amounts[i];
            int barW = (int) ((w - 2) * heights[level]);
            g.fill(x + 1, y + rowH - 3, x + 1 + barW, y + rowH - 1, argb(accent(), 0.4f + 0.6f * heights[level]));
            g.drawString(font, font.plainSubstrByWidth(state.choices().get(i), w - 46), x + 3, y + 3, 0xFFCCCCEE,
                    false);
            g.drawString(font, levels[level], x + w - font.width(levels[level]) - 3, y + 3, argb(accent(), 0.9f),
                    false);
            hits.add(new Hit(x, y, w, rowH, () -> amounts[beat] = (amounts[beat] + 1) % 3));
            y += rowH + 2;
        }
        y += 6;
        submitRow(g, x, y, mx, my, true, "Hold");
        costLines(g, x, y + 22);
    }

    private void voidWidget(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, "Reserves", x, y, 0xFFCCCCEE, false);
        y += 14;
        int sum = 0;
        for (int v : amounts) sum += v;
        int right = x + innerW();
        for (int i = 0; i < state.choices().size(); i++) {
            final int reserve = i;
            int color = 0xFF000000 | ConfluxTheme.typeColor(ConfluxDataType.values()[i]);
            g.fill(x, y, x + 3, y + 16, color);
            g.drawString(font, font.plainSubstrByWidth(state.choices().get(i), 60), x + 7, y + 4, 0xFFCCCCEE, false);
            button(g, right - 66, y, 14, 16, "-", mx, my, amounts[i] > 0, () -> adjust(reserve, -step()));
            g.drawCenteredString(font, Integer.toString(amounts[i]), right - 40, y + 4, accent());
            button(g, right - 14, y, 14, 16, "+", mx, my, amounts[i] < state.maxValue(), () -> adjust(reserve, step()));
            y += 20;
        }
        y += 6;
        g.drawString(font, "Held: " + sum, x, y, 0xFF777799, false);
        g.drawString(font, "Shift: steps of 5", x, y + 12, 0xFF3A3A55, false);
        y += 30;
        submitRow(g, x, y, mx, my, true, "Measure");
        costLines(g, x, y + 22);
    }

    private void costLines(GuiGraphics g, int x, int y) {
        if (state.costHint().isEmpty()) return;
        g.drawString(font, "Cost", x, y, 0xFF777799, false);
        y += 11;
        for (FormattedCharSequence line : font.split(Component.literal(state.costHint()), innerW())) {
            g.drawString(font, line, x, y, 0xFF55557A, false);
            y += 10;
        }
    }

    private int step() {
        return hasShiftDown() ? 5 : 1;
    }

    private void adjust(int reserve, int delta) {
        amounts[reserve] = Math.max(0, Math.min(state.maxValue(), amounts[reserve] + delta));
    }

    private void submitRow(GuiGraphics g, int x, int y, int mx, int my, boolean ready, String label) {
        button(g, x, y, 62, 16, label, mx, my, ready, this::submit);
        button(g, x + 68, y, 46, 16, "Clear", mx, my, true, () -> {
            picked.clear();
            amounts = new int[amounts.length];
            message = "";
        });
    }

    private void submit() {
        int[] values;
        if (discipline().equals("void") || discipline().equals("sealed_b")) {
            values = amounts.clone();
        } else {
            values = new int[picked.size()];
            for (int i = 0; i < values.length; i++) values[i] = picked.get(i);
        }
        message = "The Engine considers...";
        messageColor = 0xFF777799;
        PacketDistributor.sendToServer(new C2SPanopticAnswerPacket(state.controller(), values));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (seconds < INTRO_SECONDS) {
            seconds = INTRO_SECONDS;
            shownGrowth = state.growth();
            beatIndex = BEATS.length;
            playedReveal = true;
            return true;
        }
        if (button == 0) {
            for (Hit h : new ArrayList<>(hits)) {
                if (h.contains(mx, my)) {
                    h.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
