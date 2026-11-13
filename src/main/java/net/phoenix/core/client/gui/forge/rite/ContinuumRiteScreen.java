package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.client.worldfx.WorldFXShaders;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.ContinuumRite;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ContinuumRiteScreen extends RiteScreen {

    private static final int SPACE = 0x7FA8FF;
    private static final String[] LETTERS = { "A", "B", "C", "D", "E", "F" };

    private float time;
    private boolean scrubbing;
    private int stage = 1;
    private int selected = -1;
    private boolean linkMode;
    private final Map<Integer, Integer> probed = new HashMap<>();
    private final Set<Integer> claimed = new HashSet<>();

    public ContinuumRiteScreen(S2CRitePacket p) {
        super(Capstone.CONTINUUM, p, "rite_continuum");
        stage = data[0];
    }

    @Override
    protected int accent() {
        return SPACE;
    }

    @Override
    protected int accent2() {
        return 0xFFD8A0;
    }

    @Override
    protected int bgTop() {
        return 0x01020A;
    }

    @Override
    protected int bgBottom() {
        return 0x050A20;
    }

    @Override
    protected String introTitle() {
        return "LAUNCH WINDOW";
    }

    @Override
    protected String introSub() {
        return "Four satellites. Six relays. One chance to go up.";
    }

    @Override
    protected String rules() {
        return stage == 1 ?
                "Four satellites circle on rings at different rates. A launch needs all four to cross together." +
                        " Scrub the clock (drag the bar, scroll, or use the arrow keys) until they line up, then lock the window." :
                "Six relays are tied into a hidden ring. Probe a pair to learn how many hops apart they are on it:" +
                        " 1 means they are linked. Probes are few. Mark the six links in Link mode, then submit them." +
                        " Only a few submissions are allowed.";
    }

    @Override
    protected void stats(GuiGraphics g, int x, int y, float ui) {
        if (stage == 1) {
            stat(g, "Clock", Math.round(time) + " / " + ContinuumRite.PERIOD, 0xFFEBD0, x, y, ui);
            int spread = spread(Math.round(time));
            stat(g, "Satellites apart", spread + (spread <= ContinuumRite.TOLERANCE ? "  (open)" : ""),
                    spread <= ContinuumRite.TOLERANCE ? 0x9CF0FF : 0xFFEBD0, x, y + 24, ui);
        } else {
            int probes = data[2 + ContinuumRite.SATELLITES * 2];
            int submits = data[3 + ContinuumRite.SATELLITES * 2];
            stat(g, "Probes left", Integer.toString(probes), probes > 2 ? 0xFFEBD0 : 0xFF8A70, x, y, ui);
            stat(g, "Submissions left", Integer.toString(submits), submits > 1 ? 0xFFEBD0 : 0xFF8A70, x, y + 24, ui);
            stat(g, "Links marked", claimed.size() + " of " + ContinuumRite.RELAYS, 0xFFEBD0, x, y + 48, ui);
        }
    }

    @Override
    protected void onData(S2CRitePacket p) {
        int newStage = data[0];
        if (newStage != stage || p.status() == S2CRitePacket.FAILED) {
            stage = newStage;
            selected = -1;
            probed.clear();
            claimed.clear();
            if (stage == 1) time = 0f;
        }
        if (p.extra().length == 3) {
            int a = p.extra()[0];
            int b = p.extra()[1];
            probed.put(key(a, b), p.extra()[2]);
        }
    }

    private static int key(int a, int b) {
        return Math.min(a, b) * 8 + Math.max(a, b);
    }

    private int spread(int t) {
        int worst = 0;
        for (int i = 0; i < ContinuumRite.SATELLITES; i++) {
            for (int j = i + 1; j < ContinuumRite.SATELLITES; j++) {
                int a = ContinuumRite.position(data[2 + i], data[2 + ContinuumRite.SATELLITES + i], t);
                int b = ContinuumRite.position(data[2 + j], data[2 + ContinuumRite.SATELLITES + j], t);
                int d = Math.abs(a - b);
                worst = Math.max(worst, Math.min(d, ContinuumRite.PERIOD - d));
            }
        }
        return worst;
    }

    private void shaderQuad(float px, float py, float half, Consumer<ShaderInstance> setup, boolean full) {
        ShaderInstance sh = WorldFXShaders.REBIRTH_GALAXY;
        if (sh == null) return;
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        if (!full) {
            mv.translate(px / width * 2f - 1f, 1f - py / height * 2f, 0f);
            mv.scale(half * 2f / width, half * 2f / height, 1f);
        }
        RenderSystem.applyModelViewMatrix();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.setShader(() -> sh);
            setup.accept(sh);
            BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            buf.addVertex(-1f, -1f, 0f);
            buf.addVertex(-1f, 1f, 0f);
            buf.addVertex(1f, 1f, 0f);
            buf.addVertex(1f, -1f, 0f);
            BufferUploader.drawWithShader(buf.buildOrThrow());
        } finally {
            mv.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
        }
    }

    private void drawSpace() {
        long t0 = prof.start();
        float aspect = width / (float) height;
        shaderQuad(0, 0, 0, sh -> {
            sh.safeGetUniform("Mode").set(1f);
            sh.safeGetUniform("Aspect").set(aspect);
            sh.safeGetUniform("Yaw").set(seconds * 0.025f);
            sh.safeGetUniform("Time").set(seconds * 30f);
            sh.safeGetUniform("Octaves").set(4f);
            sh.safeGetUniform("UseBake").set(0f);
            sh.safeGetUniform("PrimaryColor").set(stage == 1 ? 0.2f : 0.45f, 0.15f, stage == 1 ? 0.6f : 0.5f);
            sh.safeGetUniform("SecondaryColor").set(0.08f, 0.28f, stage == 1 ? 0.7f : 0.55f);
        }, true);

        float s = Math.min(width, height) * 0.2f;
        galaxy(width * 0.86f, height * 0.2f, s, 0.31f, seconds * 0.01f);
        galaxy(width * 0.3f, height * 0.82f, s * 0.8f, 0.77f, 1.2f + seconds * 0.008f);
        prof.end("space", t0);
    }

    private void galaxy(float x, float y, float half, float seed, float rot) {
        shaderQuad(x, y, half, sh -> {
            sh.safeGetUniform("Mode").set(0f);
            sh.safeGetUniform("Seed").set(seed);
            sh.safeGetUniform("Cells").set(0f);
            sh.safeGetUniform("Detail").set(3f);
            sh.safeGetUniform("Tilt").set(0.5f);
            sh.safeGetUniform("Rot").set(rot);
            sh.safeGetUniform("Intensity").set(0.7f);
            sh.safeGetUniform("CoreColor").set(1f, 0.84f, 0.6f);
            sh.safeGetUniform("ArmColor").set(0.6f, 0.78f, 1f);
        }, false);
    }

    @Override
    protected void drawScene(GuiGraphics g, int mx, int my) {
        drawSpace();
        if (stage == 1) drawWindow(g, mx, my);
        else drawRelays(g, mx, my);
    }

    private float cr() {
        return Math.min((width - panelW()) * 0.4f, height * 0.36f);
    }

    private void drawWindow(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        float cx = cx();
        float cy = cy() - height * 0.04f;
        float rmax = cr();
        int t = Math.round(time);
        boolean open = spread(t) <= ContinuumRite.TOLERANCE;

        BufferBuilder buf = begin();

        float pr = rmax * 0.17f;
        disc(buf, pose, cx, cy, pr * 2.2f, argb(0x4080FF, 0.12f), argb(0x4080FF, 0f));
        disc(buf, pose, cx, cy, pr, argb(0x7FB0FF, 0.9f), argb(0x1030A0, 0.6f));

        float[] sx = new float[ContinuumRite.SATELLITES];
        float[] sy = new float[ContinuumRite.SATELLITES];
        for (int i = 0; i < ContinuumRite.SATELLITES; i++) {
            float r = rmax * (0.38f + 0.2f * i);
            ring(buf, pose, cx, cy, r, 0.6f, 0f, Mth.TWO_PI, argb(SPACE, 0.3f));
            float ang = ContinuumRite.position(data[2 + i], data[2 + ContinuumRite.SATELLITES + i], t) /
                    (float) ContinuumRite.PERIOD * Mth.TWO_PI - Mth.HALF_PI;
            sx[i] = cx + (float) Math.cos(ang) * r;
            sy[i] = cy + (float) Math.sin(ang) * r;
        }
        for (int i = 0; i < ContinuumRite.SATELLITES; i++) {
            int c = open ? 0xFFE8A0 : 0xBFD8FF;
            disc(buf, pose, sx[i], sy[i], 16f, argb(c, 0.4f), argb(c, 0f));
            quad(buf, pose, sx[i], sy[i], 3.6f, argb(0xFFFFFF, 1f));

            float r = rmax * (0.38f + 0.2f * i);
            float ang = (float) Math.atan2(sy[i] - cy, sx[i] - cx);
            ring(buf, pose, cx, cy, r, 1.4f, ang - 0.5f * (float) Math.signum(1f), 0.5f, argb(c, 0.4f));
        }
        if (open) {

            float ang = (float) Math.atan2(sy[0] - cy, sx[0] - cx);
            float bx = cx + (float) Math.cos(ang) * rmax * 1.8f;
            float by = cy + (float) Math.sin(ang) * rmax * 1.8f;
            line(buf, pose, cx, cy, bx, by, 2f, 7f, argb(0xFFE8A0, 0.5f), argb(0xFFE8A0, 0f));
            disc(buf, pose, cx, cy, pr * 2.8f, argb(0xFFE8A0, 0.18f), argb(0xFFE8A0, 0f));
        }
        end(buf);

        float bw = Math.min((width - panelW()) * 0.7f, 520f);
        float x0 = cx - bw / 2f;
        float by = height * 0.8f;
        buf = begin();
        line(buf, pose, x0, by, x0 + bw, by, 1.2f, argb(SPACE, 0.5f));
        for (int i = 0; i <= 24; i++) {
            float tx = x0 + bw * i / 24f;
            line(buf, pose, tx, by - (i % 6 == 0 ? 6f : 3f), tx, by, 0.5f, argb(SPACE, 0.5f));
        }
        float kx = x0 + bw * time / ContinuumRite.PERIOD;
        disc(buf, pose, kx, by, 12f, argb(open ? 0xFFE8A0 : SPACE, 0.5f), argb(SPACE, 0f));
        quad(buf, pose, kx, by, 4f, argb(0xFFFFFF, 1f));
        end(buf);
        hits.add(new Hit((int) x0 - 8, (int) by - 14, (int) bw + 16, 28, () -> {
            scrubbing = true;
            setTimeFromMouse(mx, x0, bw);
        }));
        button(g, (int) (cx - 55), (int) by + 16, 110, 18, "LOCK WINDOW", mx, my, open && !bound, uiAlpha(),
                () -> send(ContinuumRite.LOCK, Math.round(time)));
    }

    private void setTimeFromMouse(double mx, float x0, float bw) {
        time = Mth.clamp((float) ((mx - x0) / bw) * ContinuumRite.PERIOD, 0f, ContinuumRite.PERIOD - 1f);
    }

    private float[] relay(int i) {
        double a = Math.toRadians(-90 + 60 * i);
        float r = cr() * 0.82f;
        return new float[] { cx() + (float) Math.cos(a) * r, cy() - height * 0.02f + (float) Math.sin(a) * r * 0.9f };
    }

    private void drawRelays(GuiGraphics g, int mx, int my) {
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = begin();

        for (var e : probed.entrySet()) {
            int a = e.getKey() / 8;
            int b = e.getKey() % 8;
            float[] p = relay(a);
            float[] q = relay(b);
            int hops = e.getValue();
            int c = hops == 1 ? 0xFFE8A0 : hops == 2 ? 0x59A8FF : 0x9A7CFF;
            line(buf, pose, p[0], p[1], q[0], q[1], hops == 1 ? 1.6f : 0.8f, argb(c, hops == 1 ? 0.9f : 0.5f));
        }

        for (int k : claimed) {
            float[] p = relay(k / 8);
            float[] q = relay(k % 8);
            line(buf, pose, p[0], p[1], q[0], q[1], 3f, argb(0xFFFFFF, 0.25f));
            line(buf, pose, p[0], p[1], q[0], q[1], 1.2f, argb(0xFFFFFF, 0.95f));
        }
        float size = Math.min(width, height) * 0.032f;
        for (int i = 0; i < ContinuumRite.RELAYS; i++) {
            float[] p = relay(i);
            boolean sel = i == selected;
            int c = sel ? 0xFFE8A0 : 0xBFD8FF;
            float tw = 0.8f + 0.2f * (float) Math.sin(seconds * 2.4f + i * 1.7f);
            disc(buf, pose, p[0], p[1], size * 2.2f * tw, argb(c, sel ? 0.4f : 0.22f), argb(c, 0f));

            line(buf, pose, p[0] - size * 1.6f, p[1], p[0] + size * 1.6f, p[1], 1.2f, 0.2f, argb(c, 0.9f),
                    argb(c, 0.9f));
            line(buf, pose, p[0], p[1] - size * 1.6f, p[0], p[1] + size * 1.6f, 1.2f, 0.2f, argb(c, 0.9f),
                    argb(c, 0.9f));
            quad(buf, pose, p[0], p[1], size * 0.4f, argb(0xFFFFFF, 1f));
            if (sel) ring(buf, pose, p[0], p[1], size * 1.8f, 0.8f, seconds, Mth.TWO_PI * 0.75f, argb(0xFFE8A0, 0.9f));
            if (!bound) {
                final int node = i;
                hits.add(new Hit((int) (p[0] - size * 1.6f), (int) (p[1] - size * 1.6f), (int) (size * 3.2f),
                        (int) (size * 3.2f), () -> pick(node)));
            }
        }
        end(buf);
        for (int i = 0; i < ContinuumRite.RELAYS; i++) {
            float[] p = relay(i);
            g.drawCenteredString(font, LETTERS[i], (int) p[0], (int) (p[1] + size * 2f), argb(0xDDE8FF, 0.95f));
        }
        for (var e : probed.entrySet()) {
            float[] p = relay(e.getKey() / 8);
            float[] q = relay(e.getKey() % 8);
            g.drawCenteredString(font, Integer.toString(e.getValue()), (int) ((p[0] + q[0]) / 2f),
                    (int) ((p[1] + q[1]) / 2f) - 4, argb(0xFFFFFF, 0.9f));
        }

        float ui = uiAlpha();
        int bx = (int) (cx() - 150);
        int by = (int) (height * 0.8f) + 6;
        button(g, bx, by, 88, 18, linkMode ? "Mode: LINK" : "Mode: PROBE", mx, my, !bound, ui, () -> {
            linkMode = !linkMode;
            selected = -1;
        });
        button(g, bx + 94, by, 88, 18, "Clear links", mx, my, !claimed.isEmpty() && !bound, ui, claimed::clear);
        button(g, bx + 188, by, 110, 18, "SUBMIT RING", mx, my, claimed.size() == ContinuumRite.RELAYS && !bound, ui,
                this::submit);
    }

    private void pick(int node) {
        if (selected < 0) {
            selected = node;
            return;
        }
        if (selected == node) {
            selected = -1;
            return;
        }
        int a = selected;
        selected = -1;
        if (linkMode) {
            int k = key(a, node);
            if (!claimed.remove(k) && claimed.size() < ContinuumRite.RELAYS) claimed.add(k);
            play(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.5f);
        } else if (!probed.containsKey(key(a, node))) {
            send(ContinuumRite.PROBE, a, node);
        }
    }

    private void submit() {
        int[] args = new int[ContinuumRite.RELAYS * 2];
        int i = 0;
        for (int k : claimed) {
            args[i++] = k / 8;
            args[i++] = k % 8;
        }
        send(ContinuumRite.SUBMIT, args);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (scrubbing && stage == 1) {
            float bw = Math.min((width - panelW()) * 0.7f, 520f);
            setTimeFromMouse(vx(mx), cx() - bw / 2f, bw);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        scrubbing = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (stage == 1) {
            time = Mth.clamp(time + (float) Math.signum(sy), 0f, ContinuumRite.PERIOD - 1f);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (stage == 1 && (key == 263 || key == 262)) {
            time = Mth.clamp(time + (key == 262 ? 1f : -1f), 0f, ContinuumRite.PERIOD - 1f);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
