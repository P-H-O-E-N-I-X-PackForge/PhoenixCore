package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.conflux.client.render.MotionClock;
import net.phoenix.core.integration.conflux.dimension.network.C2SDimensionTeleportPacket;
import net.phoenix.core.integration.conflux.network.C2SChooseDisciplinePacket;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DisciplinePickerScreen extends Screen {

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    private static final int CARD_W = 210;
    private static final int CARD_H = 280;
    private static final float CARD_SPACING = CARD_W + 40f;

    private static final float MIN_VIRTUAL_W = 760f;
    private static final float MIN_VIRTUAL_H = 500f;

    private static final float FLIP_DURATION = 0.45f;
    private static final long DOUBLE_CLICK_MS = 350L;
    private static final double DOUBLE_CLICK_DIST = 8.0;

    private static final float SCROLL_STEP = 24f;
    private static final int FACE_FRONT = 0;
    private static final int FACE_BACK = 1;

    private record DisciplineCard(
                                  String id,
                                  String name,
                                  String tagline,
                                  String[] loreLines,
                                  String[] detailLines,
                                  int accentColor,
                                  int glowColor,
                                  int bgColor,
                                  boolean sealed) {}

    private static final DisciplineCard[] CARDS = {
            new DisciplineCard(
                    "phoenix",
                    "The Phoenix",
                    "Rebirth through consumption.",
                    new String[] {
                            "Every ending is an ignition.",
                            "The Phoenix does not endure,",
                            "it transcends.",
                            "",
                            "You will burn. You will rise.",
                            "The question is only how many",
                            "times you are willing to do it.",
                            "",
                            "# First Fire",
                            "The first Phoenix was not born.",
                            "It was left behind, the last",
                            "ember of a furnace that",
                            "had been told to stop.",
                            "",
                            "# The Pattern",
                            "Fuel becomes heat.",
                            "Heat becomes motion.",
                            "Motion becomes you,",
                            "briefly, brilliantly,",
                            "before the ash settles.",
                            "",
                            "# What It Asks",
                            "The Phoenix asks for everything",
                            "you have built, and gives it",
                            "back as something brighter.",
                            "It never says the price",
                            "twice.",
                            "",
                            "# The Warning",
                            "Those who follow this path",
                            "learn to love the moment",
                            "before the burning.",
                            "Some forget there was ever",
                            "a before."
                    },

                    new String[] {
                            "# Playstyle",
                            "Aggressive and cyclical. Power surges as you burn through resources, then resets.",
                            "",
                            "# Strengths",
                            "Fast early progression and explosive output.",
                            "",
                            "# Costs",
                            "Every rebirth consumes what you built. Little is permanent."
                    },
                    0xFFFF8800, 0xFFFF4400, 0xFF1A0800,
                    false),
            new DisciplineCard(
                    "sculk",
                    "The Sculk",
                    "The network remembers.",
                    new String[] {
                            "Before the first thing, there",
                            "was the space between things.",
                            "",
                            "You are not absorbed. You",
                            "persist, within something",
                            "immeasurably larger.",
                            "",
                            "The network has always been",
                            "thinking. Now it thinks with you.",
                            "",
                            "# Root",
                            "Beneath every floor there is",
                            "a floor you were never shown.",
                            "The Sculk lives there,",
                            "listening to footsteps",
                            "it has not yet met.",
                            "",
                            "# Signal",
                            "A tremor is a sentence.",
                            "A pulse is a memory.",
                            "Nothing is lost here,",
                            "only passed along.",
                            "",
                            "# What It Asks",
                            "Very little. A node. A thread.",
                            "A place to listen from.",
                            "The network will do the rest,",
                            "and it will do it gently.",
                            "",
                            "# The Warning",
                            "The network is generous.",
                            "It will remember everything",
                            "you have ever been,",
                            "including what you hoped",
                            "it would forget."
                    },

                    new String[] {
                            "# Playstyle",
                            "Patient and connected. You grow by linking nodes into something larger than yourself.",
                            "",
                            "# Strengths",
                            "Information, automation and resilience through connection.",
                            "",
                            "# Costs",
                            "Isolated nodes are weak. The network must be tended."
                    },
                    0xFF00CC88, 0xFF004422, 0xFF000A06,
                    false),
            new DisciplineCard(
                    "void",
                    "The Void",
                    "Patience older than light.",
                    new String[] {
                            "Before the first thing, there",
                            "was the space between things.",
                            "The Void was not empty,",
                            "it was patient.",
                            "",
                            "Two eyes see one world.",
                            "One eye sees two.",
                            "Beyond the threshold, depth",
                            "is a negotiation.",
                            "",
                            "# Threshold",
                            "There is a door in every room",
                            "that opens only when no one",
                            "is looking at it.",
                            "The Void has been waiting",
                            "on the other side of all of them.",
                            "",
                            "# Depth",
                            "Distance is a habit.",
                            "The Void breaks it gently.",
                            "What was far becomes near.",
                            "What was near becomes",
                            "a matter of opinion.",
                            "",
                            "# What It Asks",
                            "Stillness. Attention.",
                            "The willingness to stand",
                            "at an edge and not",
                            "be in a hurry.",
                            "",
                            "# The Warning",
                            "Patience is not the absence",
                            "of hunger. It is hunger",
                            "that has learned to wait."
                    },

                    new String[] {
                            "# Playstyle",
                            "Spatial and deliberate. You bend distance and depth to your advantage.",
                            "",
                            "# Strengths",
                            "Precise control and positioning.",
                            "",
                            "# Costs",
                            "Slow start. Rewards forethought over reaction."
                    },
                    0xFF6633FF, 0xFF220066, 0xFF010108,
                    false),
            new DisciplineCard(
                    "sealed_a",
                    "██████",
                    "You should not be reading this.",
                    new String[] {
                            "This record has been sealed",
                            "pending review.",
                            "",
                            "Access requires prior",
                            "commitment to an incompatible",
                            "path.",
                            "",
                            "We have not decided what",
                            "finding this means about you.",
                            "",
                            "# Fragment 04",
                            "[REDACTED] was the first to",
                            "choose [REDACTED] and the",
                            "last to return unchanged.",
                            "",
                            "# Reviewer Note",
                            "Do not confirm this record",
                            "to the subject. They may",
                            "already know.",
                            "",
                            "# Fragment 11",
                            "██████ ██ ███ ████████",
                            "██████ ████ ██████ ███",
                            "",
                            "# Reviewer Note",
                            "If you are reading this",
                            "on the terminal, the seal",
                            "has already failed."
                    },
                    new String[] {
                            "# Record",
                            "██████ ████ ███████",
                            "",
                            "# Status",
                            "Sealed pending review.",
                            "",
                            "# Access",
                            "Requires prior commitment to an incompatible path."
                    },
                    0xFFAAAAAA, 0xFF333333, 0xFF080808,
                    true),
            new DisciplineCard(
                    "sealed_b",
                    "???",
                    "...",
                    new String[] {
                            "...",
                            "",
                            "You are being watched.",
                            "You have always been watched.",
                            "This terminal is how",
                            "it watches back.",
                            "",
                            "# Log",
                            "Entry 1: it noticed us.",
                            "Entry 2: we noticed it noticing.",
                            "Entry 3: ...",
                            "",
                            "# Log",
                            "Entry 4: the terminal is warm.",
                            "It was not warm yesterday.",
                            "",
                            "# Log",
                            "Entry 5: it knows which card",
                            "you are reading.",
                            "",
                            "# Please",
                            "Close this screen.",
                            "Please."
                    },
                    new String[] {
                            "# Observation",
                            "It is watching.",
                            "",
                            "# Notes",
                            "...",
                            "",
                            "# Warning",
                            "Do not look for too long."
                    },
                    0xFF888855, 0xFF222210, 0xFF050504,
                    true),
    };

    private record Line(FormattedCharSequence text, int rgb, int alpha, boolean center, int y, int width) {}

    private record Block(List<Line> lines, int height) {}

    private final Block[][] blocks = new Block[2][CARDS.length];

    private final MotionClock clock = new MotionClock();
    private long lastNanos = -1;

    private int hoveredIdx = -1;
    private int selectedIdx = -1;
    private float arcOffset = 0f;
    private float arcVelocity = 0f;
    private float introAnim = 0f;

    private boolean focusing = false;
    private float arcTarget = 0f;

    private int lastClickIdx = -1;
    private long lastClickTime = 0L;
    private double lastClickX, lastClickY;

    private final float[] cardScale = new float[CARDS.length];
    private final float[] cardGlow = new float[CARDS.length];
    private final float[] cardLift = new float[CARDS.length];
    private final float[] cardReveal = new float[CARDS.length];

    private final float[] flipProg = new float[CARDS.length];
    private final boolean[] flipTarget = new boolean[CARDS.length];

    private final float[][] scrollTarget = new float[2][CARDS.length];
    private final float[][] scrollPos = new float[2][CARDS.length];

    private float confirmAlpha = 0f;
    private boolean confirming = false;

    private static final int CONFIRM_W = 360;
    private static final int CONFIRM_H = 220;
    private float uiScale = 1f;
    private int vw, vh;

    public DisciplinePickerScreen() {
        super(Component.literal("Choose Your Path"));
        for (int i = 0; i < CARDS.length; i++) {
            cardScale[i] = 0.88f;
            cardGlow[i] = 0f;
            cardReveal[i] = 1f;
            flipProg[i] = 0f;
        }
    }

    @Override
    protected void init() {
        float neededW = Math.max(MIN_VIRTUAL_W, CONFIRM_W + 40f);
        float neededH = Math.max(MIN_VIRTUAL_H, CONFIRM_H + 40f);
        uiScale = Math.min(1f, Math.min(width / neededW, height / neededH));
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        buildBlocks();
    }

    private void buildBlocks() {
        for (int i = 0; i < CARDS.length; i++) {
            DisciplineCard c = CARDS[i];
            blocks[FACE_FRONT][i] = buildBlock(c.loreLines, true, c.accentColor, 0x9A9AAC, 190);
            blocks[FACE_BACK][i] = buildBlock(c.detailLines, false, c.accentColor, 0xB4B4C4, 210);
        }
    }

    private Block buildBlock(String[] src, boolean center, int accent, int bodyRgb, int bodyAlpha) {
        List<Line> out = new ArrayList<>();
        int wrapW = (int) (CARD_W * 0.88f) - 28;
        int y = 0;
        for (String line : src) {
            if (line.isEmpty()) {
                y += 6;
                continue;
            }
            boolean header = line.startsWith("# ");
            String body = header ? line.substring(2) : line;
            if (header) y += 1;
            for (FormattedCharSequence seq : font.split(Component.literal(body), Math.max(20, wrapW))) {
                out.add(new Line(seq, header ? accent : bodyRgb, header ? 255 : bodyAlpha,
                        center, y, font.width(seq)));
                y += 10;
            }
            if (header) y += 1;
        }
        return new Block(out, y + 8);
    }

    private static int[] contentRegion(int y0, int y1, float sc) {
        int stripeH = Math.max(2, (int) (4 * sc));
        int top = y0 + stripeH + (int) (12 * sc) + 33;
        int bottom = y1 - (int) (36 * sc);
        return new int[] { top, bottom };
    }

    private int faceOf(int idx) {
        return flipTarget[idx] ? FACE_BACK : FACE_FRONT;
    }

    private float maxScroll(int idx, int face, float ease) {
        if (blocks[face][idx] == null) buildBlocks();
        int[] b = cardBounds(idx, ease);
        int[] r = contentRegion(b[1], b[3], cardScale[idx] * ease);
        return Math.max(0f, blocks[face][idx].height - (r[1] - r[0]));
    }

    private void scrollCard(int idx, float amount) {
        int face = faceOf(idx);
        float max = maxScroll(idx, face, easeOutExpo(introAnim));
        scrollTarget[face][idx] = Mth.clamp(scrollTarget[face][idx] + amount, 0f, max);
    }

    private void animate() {
        long now = System.nanoTime();
        float dt = lastNanos < 0 ? 0.016f : (float) ((now - lastNanos) / 1_000_000_000.0);
        lastNanos = now;
        dt = Math.min(dt, 0.1f);

        clock.tick(dt);
        introAnim = Math.min(1f, introAnim + dt * 1.8f);
        float ease = easeOutExpo(introAnim);

        if (focusing) {
            arcVelocity = 0f;
            arcOffset = lerp(arcOffset, arcTarget, dt * 9f);
            if (Math.abs(arcOffset - arcTarget) < 0.5f) {
                arcOffset = arcTarget;
                focusing = false;
            }
        } else {
            arcOffset += arcVelocity * dt;
            arcVelocity *= (float) Math.pow(0.08, dt);
        }
        clampArc();

        for (int i = 0; i < CARDS.length; i++) {
            boolean hov = (i == hoveredIdx);
            boolean sel = (i == selectedIdx);
            float targetScale = sel ? 1.06f : hov ? 1.02f : 0.88f;
            float targetGlow = sel ? 1.0f : hov ? 0.55f : 0.0f;
            float targetLift = sel ? -18f : hov ? -8f : 0f;
            cardScale[i] = lerp(cardScale[i], targetScale, dt * 8f);
            cardGlow[i] = lerp(cardGlow[i], targetGlow, dt * 6f);
            cardLift[i] = lerp(cardLift[i], targetLift, dt * 7f);

            float dir = flipTarget[i] ? 1f : -1f;
            flipProg[i] = Mth.clamp(flipProg[i] + dir * dt / FLIP_DURATION, 0f, 1f);

            for (int face = 0; face < 2; face++) {
                float max = maxScroll(i, face, ease);
                scrollTarget[face][i] = Mth.clamp(scrollTarget[face][i], 0f, max);
                scrollPos[face][i] = lerp(scrollPos[face][i], scrollTarget[face][i], dt * 14f);
                if (Math.abs(scrollPos[face][i] - scrollTarget[face][i]) < 0.1f) {
                    scrollPos[face][i] = scrollTarget[face][i];
                }
            }
        }

        float targetConfirm = confirming ? 1f : 0f;
        confirmAlpha = lerp(confirmAlpha, targetConfirm, dt * 7f);
    }

    private float maxArcOffset() {
        return CARD_SPACING * (CARDS.length - 1) / 2f + 40f;
    }

    private void clampArc() {
        float m = maxArcOffset();
        if (arcOffset > m) {
            arcOffset = m;
            arcVelocity = 0f;
        } else if (arcOffset < -m) {
            arcOffset = -m;
            arcVelocity = 0f;
        }
    }

    private void focusOn(int i) {
        float totalW = CARD_SPACING * CARDS.length;
        arcTarget = totalW / 2f - CARD_SPACING * i - CARD_SPACING / 2f;
        arcTarget = Mth.clamp(arcTarget, -maxArcOffset(), maxArcOffset());
        arcVelocity = 0f;
        focusing = true;
    }

    private void flipCard(int i) {
        flipTarget[i] = !flipTarget[i];
        selectedIdx = i;
        focusOn(i);
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        animate();

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        updateHover(mx, my);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        float ease = easeOutExpo(introAnim);
        int cx = vw / 2;

        g.fill(0, 0, vw, vh, 0xFF020208);
        renderStarfield(g, ease);
        renderAmbientGlow(g, ease);

        int titleAlpha = (int) (ease * 220);
        String title = "CHOOSE YOUR PATH";
        int tw = font.width(title);
        drawText(g, title, cx - tw / 2 + 1, 31, titleAlpha, 0x000000, false);
        drawText(g, title, cx - tw / 2, 30, titleAlpha, 0xCCCCDD, false);

        String sub = "Each discipline shapes how you relate to the machine. There is no wrong answer, only yours.";
        int subW = font.width(sub);
        drawText(g, sub, cx - subW / 2, 46, (int) (ease * 100), 0x8888AA, false);

        for (int i = 0; i < CARDS.length; i++) {
            renderCard(g, CARDS[i], i, ease, mx, my);
        }

        String hint = "Click to focus   •   Double-click to choose   •   Right-click to flip   •   Scroll to read   •   ESC to close";
        int hintW = font.width(hint);
        drawText(g, hint, cx - hintW / 2, vh - 22, (int) (ease * 120), 0x666688, false);

        if (confirmAlpha > 0.01f && selectedIdx >= 0) {
            renderConfirmOverlay(g, mx, my);
        }

        super.render(g, mx, my, pt);

        g.pose().popPose();
    }

    private void renderStarfield(GuiGraphics g, float ease) {
        int count = 180;
        float t = clock.getElapsed();
        for (int i = 0; i < count; i++) {
            float hx = MotionClock.hash((long) (i * 7L + 1));
            float hy = MotionClock.hash((long) (i * 13L + 3));
            float hs = MotionClock.hash((long) (i * 31L + 7));
            float hp = MotionClock.hash((long) (i * 17L + 11));
            int sx = (int) (hx * vw);
            int sy = (int) (hy * vh);
            float twinkle = 0.5f + 0.5f * (float) Math.sin(t * (0.4f + hs * 1.2f) + hp * 6.28f);
            int alpha = (int) (ease * twinkle * 160 * (0.4f + hs * 0.6f));
            if (alpha < 5) continue;
            g.fill(sx, sy, sx + 1, sy + 1, col(alpha, 0xCCDDFF));
        }
    }

    private void renderAmbientGlow(GuiGraphics g, float ease) {
        int idx = hoveredIdx >= 0 ? hoveredIdx : selectedIdx;
        if (idx < 0) return;
        DisciplineCard card = CARDS[idx];
        int[] b = cardBounds(idx, ease);
        int cx = (b[0] + b[2]) / 2;
        int cy = (b[1] + b[3]) / 2;
        int glowA = (int) (ease * cardGlow[idx] * 30);
        int r = 200;
        g.fill(cx - r, cy - r, cx + r, cy + r, col(glowA, card.glowColor));
    }

    private void renderCard(GuiGraphics g, DisciplineCard card, int idx, float ease, int mx, int my) {
        float sc = cardScale[idx] * ease;
        int[] b = cardBounds(idx, ease);
        int x0 = b[0], y0 = b[1], x1 = b[2], y1 = b[3];
        int cx = (x0 + x1) / 2;
        int cy = (y0 + y1) / 2;

        boolean hov = (idx == hoveredIdx);
        boolean sel = (idx == selectedIdx);

        float prog = easeInOutCubic(flipProg[idx]);
        float angle = prog * (float) Math.PI;
        float s = (float) Math.sin(angle);
        float xScale = Math.max(0.02f, Math.abs((float) Math.cos(angle)));
        float yScale = 1f - 0.07f * s;
        float hop = 10f * s;
        boolean showBack = prog > 0.5f;

        g.pose().pushPose();
        g.pose().translate(cx, cy - hop, 0);
        g.pose().scale(xScale, yScale, 1f);
        g.pose().translate(-cx, -cy, 0);

        if (cardGlow[idx] > 0.02f) {
            int gAlpha = (int) (cardGlow[idx] * 80);
            g.fill(x0 - 8, y0 - 8, x1 + 8, y1 + 8, col(gAlpha, card.glowColor));
            g.fill(x0 - 4, y0 - 4, x1 + 4, y1 + 4, col(gAlpha, card.accentColor));
        }

        g.fill(x0, y0, x1, y1, card.bgColor | 0xEE000000);

        float reveal = cardReveal[idx];
        if (reveal < 1f) {
            int blankY = y0 + (int) ((y1 - y0) * reveal);
            g.fill(x0, blankY, x1, y1, 0xFF020208);
        }

        int stripeH = Math.max(2, (int) (4 * sc));
        g.fill(x0, y0, x1, y0 + stripeH, card.accentColor);

        int brdCol = sel ? card.accentColor :
                hov ? col(0xAA, card.accentColor) : col(0x33, card.accentColor);
        g.fill(x0, y0, x1, y0 + 1, brdCol);
        g.fill(x0, y1 - 1, x1, y1, brdCol);
        g.fill(x0, y0, x0 + 1, y1, brdCol);
        g.fill(x1 - 1, y0, x1, y1, brdCol);

        if (card.sealed) {
            float glitch = (float) Math.sin(clock.getElapsed() * 11.3f + idx * 2.1f);
            float pulse = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(clock.getElapsed() * 2.2f));
            g.fill(x0, y0, x1, y1, col((int) (pulse * 60), 0x000000));

            for (int li = 0; li < 5; li++) {
                int lx = x0 + (x1 - x0) * li / 5 + (int) (glitch * li * 1.5f);
                g.fill(lx, y0, lx + 1, y1, col((int) (pulse * 20), 0x888888));
            }
        }

        if (reveal > 0.15f) {
            renderCardFace(g, card, idx, showBack, x0, y0, x1, y1, sc, cx, stripeH);
        }

        if ((hov || sel) && s < 0.05f) {
            renderChooseButton(g, card, x0, y0, x1, y1, sc, sel, mx, my);
        }

        if (s > 0.02f) {
            g.fill(x0, y0, x1, y1, col((int) (150 * s), 0x000000));
        }

        g.pose().popPose();
    }

    private void renderCardFace(GuiGraphics g, DisciplineCard card, int idx, boolean back,
                                int x0, int y0, int x1, int y1, float sc, int cx, int stripeH) {
        int textY = y0 + stripeH + (int) (12 * sc);

        int nameW = font.width(card.name);
        drawText(g, card.name, cx - nameW / 2, textY, 255, card.accentColor, false);
        textY += 14;

        String sub = back ? (card.sealed ? "Restricted file" : "Discipline details") : "§o" + card.tagline;
        int subW = font.width(sub);
        drawText(g, sub, cx - subW / 2, textY, back ? 150 : 180, 0xAAAAAA, false);
        textY += 13;

        g.fill(x0 + (int) (16 * sc), textY, x1 - (int) (16 * sc), textY + 1, col(180, card.accentColor));

        int face = back ? FACE_BACK : FACE_FRONT;
        int[] region = contentRegion(y0, y1, sc);
        renderScrollableText(g, card, blocks[face][idx], scrollPos[face][idx], x0, x1, region[0], region[1], sc, cx);
    }

    private void renderScrollableText(GuiGraphics g, DisciplineCard card, Block block, float scroll,
                                      int x0, int x1, int top, int bottom, float sc, int cx) {
        int regionH = bottom - top;
        if (regionH <= 0) return;

        int left = x0 + (int) (14 * sc);
        final float fade = 10f;

        for (Line line : block.lines) {
            int ly = top + line.y - Math.round(scroll);

            float fTop = Mth.clamp((ly - top + 9) / fade, 0f, 1f);
            float fBot = Mth.clamp((bottom - ly) / fade, 0f, 1f);
            float f = Math.min(fTop, fBot);
            if (f <= 0.05f) continue;

            int alpha = (int) (line.alpha * f);
            if (alpha < 4) continue;

            int x = line.center ? cx - line.width / 2 : left;
            g.drawString(font, line.text, x, ly, col(alpha, line.rgb), false);
        }

        if (block.height > regionH) {
            float maxScroll = block.height - regionH;
            int trackX = x1 - 6;
            g.fill(trackX, top, trackX + 2, bottom, col(40, card.accentColor));
            int thumbH = Math.max(12, (int) (regionH * (regionH / (float) block.height)));
            int thumbY = top + (int) ((regionH - thumbH) * Mth.clamp(scroll / maxScroll, 0f, 1f));
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, col(170, card.accentColor));
        }
    }

    private void renderChooseButton(GuiGraphics g, DisciplineCard card, int x0, int y0, int x1, int y1,
                                    float sc, boolean sel, int mx, int my) {
        int[] r = buttonRect(x0, y0, x1, y1, sc);
        int bx0 = r[0], bx1 = r[1], btnY = r[2], btnH = r[3];

        boolean btnHov = mx >= bx0 && mx < bx1 && my >= btnY && my < btnY + btnH;
        int bg = sel ? 0x88 : btnHov ? 0x66 : 0x33;
        g.fill(bx0, btnY, bx1, btnY + btnH, col(bg, card.accentColor));
        g.fill(bx0, btnY, bx1, btnY + 1, card.accentColor);
        g.fill(bx0, btnY + btnH - 1, bx1, btnY + btnH, card.accentColor);

        String label = card.sealed ? "§8Sealed" : sel ? "§lChoose Path" : "Choose This Path";
        g.drawCenteredString(font, label, (bx0 + bx1) / 2, btnY + (btnH - 8) / 2,
                sel ? (card.accentColor | 0xFF000000) : 0xFFCCCCCC);
    }

    private void renderConfirmOverlay(GuiGraphics g, int mx, int my) {
        if (selectedIdx < 0) return;
        DisciplineCard card = CARDS[selectedIdx];

        int ovAlpha = (int) (confirmAlpha * 200);
        g.fill(0, 0, vw, vh, col(ovAlpha, 0x000000));

        int cx = vw / 2, cy = vh / 2;
        int bw = CONFIRM_W, bh = CONFIRM_H;
        int bx0 = cx - bw / 2, by0 = cy - bh / 2;

        g.fill(bx0, by0, bx0 + bw, by0 + bh, 0xEE050510);
        g.fill(bx0, by0, bx0 + bw, by0 + 3, card.accentColor);
        g.fill(bx0, by0, bx0 + 1, by0 + bh, col(0x88, card.accentColor));
        g.fill(bx0 + bw - 1, by0, bx0 + bw, by0 + bh, col(0x88, card.accentColor));
        g.fill(bx0, by0 + bh - 1, bx0 + bw, by0 + bh, col(0x88, card.accentColor));

        int ty = by0 + 20;
        int nameW = font.width(card.name);
        g.drawString(font, card.name, cx - nameW / 2, ty, card.accentColor | 0xFF000000, false);
        ty += 16;

        String[] confirmLines = card.sealed ?
                new String[] { "This path is sealed.", "Committing to it may have consequences",
                        "beyond what this terminal can show you." } :
                new String[] { "You are about to choose " + card.name + ".", "This choice can be changed later,",
                        "but not without cost.", "Are you certain?" };

        for (String line : confirmLines) {
            int lw = font.width(line);
            g.drawString(font, "§7" + line, cx - lw / 2, ty, 0xFFCCCCCC, false);
            ty += 12;
        }

        ty = by0 + bh - 44;

        int cbw = 130, cbh = 24;
        int cbx = cx - cbw - 8;
        boolean confHov = mx >= cbx && mx < cbx + cbw && my >= ty && my < ty + cbh;
        g.fill(cbx, ty, cbx + cbw, ty + cbh, confHov ? 0xAA224422 : 0x88112211);
        g.fill(cbx, ty, cbx + cbw, ty + 1, card.accentColor);
        g.fill(cbx, ty + cbh - 1, cbx + cbw, ty + cbh, card.accentColor);
        g.drawCenteredString(font, "Commit", cbx + cbw / 2, ty + 8, card.sealed ? 0xFFAAAAAA : 0xFF88DD88);

        int cax = cx + 8;
        boolean canHov = mx >= cax && mx < cax + cbw && my >= ty && my < ty + cbh;
        g.fill(cax, ty, cax + cbw, ty + cbh, canHov ? 0xAA221111 : 0x88110808);
        g.fill(cax, ty, cax + cbw, ty + 1, 0xFF882222);
        g.fill(cax, ty + cbh - 1, cax + cbw, ty + cbh, 0xFF882222);
        g.drawCenteredString(font, "Go Back", cax + cbw / 2, ty + 8, 0xFFCC4444);
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int btn) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;

        if (confirming && selectedIdx >= 0) {
            if (btn != 0) return true;
            int cx2 = vw / 2, cy2 = vh / 2;
            int bh = CONFIRM_H;
            int by0 = cy2 - bh / 2;
            int ty = by0 + bh - 44;
            int cbw = 130, cbh = 24;

            int cbx = cx2 - cbw - 8;
            if (mx >= cbx && mx < cbx + cbw && my >= ty && my < ty + cbh) {
                commitDiscipline(CARDS[selectedIdx]);
                return true;
            }

            int cax = cx2 + 8;
            if (mx >= cax && mx < cax + cbw && my >= ty && my < ty + cbh) {
                confirming = false;
                return true;
            }

            confirming = false;
            return true;
        }

        if (btn != 0 && btn != 1) return super.mouseClicked(mx, my, btn);

        if (btn == 0) {
            long now = System.currentTimeMillis();
            boolean isDouble = lastClickIdx >= 0 && now - lastClickTime <= DOUBLE_CLICK_MS &&
                    Math.abs(rmx - lastClickX) <= DOUBLE_CLICK_DIST && Math.abs(rmy - lastClickY) <= DOUBLE_CLICK_DIST;
            if (isDouble) {
                selectedIdx = lastClickIdx;
                confirming = true;
                lastClickIdx = -1;
                return true;
            }
        }

        float ease = easeOutExpo(introAnim);
        for (int i = 0; i < CARDS.length; i++) {
            int[] b = cardBounds(i, ease);
            if (mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3]) {
                if (btn == 1) {
                    flipCard(i);
                    lastClickIdx = -1;
                    return true;
                }

                int[] r = buttonRect(b[0], b[1], b[2], b[3], cardScale[i] * ease);
                boolean onChoose = mx >= r[0] && mx < r[1] && my >= r[2] && my < r[2] + r[3];
                if (onChoose) {
                    selectedIdx = i;
                    confirming = true;
                    lastClickIdx = -1;
                    return true;
                }

                selectedIdx = i;
                focusOn(i);
                lastClickIdx = i;
                lastClickTime = System.currentTimeMillis();
                lastClickX = rmx;
                lastClickY = rmy;
                return true;
            }
        }

        lastClickIdx = -1;
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int kc, int sc2, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            if (confirming) {
                confirming = false;
                return true;
            }
            onClose();
            return true;
        }

        if (confirming) {
            if ((kc == GLFW.GLFW_KEY_ENTER || kc == GLFW.GLFW_KEY_KP_ENTER) && selectedIdx >= 0) {
                commitDiscipline(CARDS[selectedIdx]);
            }
            return true;
        }

        if (kc == GLFW.GLFW_KEY_LEFT || kc == GLFW.GLFW_KEY_A) {
            focusing = false;
            arcVelocity -= 600f;
            return true;
        }
        if (kc == GLFW.GLFW_KEY_RIGHT || kc == GLFW.GLFW_KEY_D) {
            focusing = false;
            arcVelocity += 600f;
            return true;
        }

        int textTarget = hoveredIdx >= 0 ? hoveredIdx : selectedIdx;
        if (textTarget >= 0) {
            if (kc == GLFW.GLFW_KEY_DOWN || kc == GLFW.GLFW_KEY_S) {
                scrollCard(textTarget, SCROLL_STEP);
                return true;
            }
            if (kc == GLFW.GLFW_KEY_UP || kc == GLFW.GLFW_KEY_W) {
                scrollCard(textTarget, -SCROLL_STEP);
                return true;
            }
            if (kc == GLFW.GLFW_KEY_PAGE_DOWN) {
                scrollCard(textTarget, SCROLL_STEP * 5f);
                return true;
            }
            if (kc == GLFW.GLFW_KEY_PAGE_UP) {
                scrollCard(textTarget, -SCROLL_STEP * 5f);
                return true;
            }
        }

        if (kc == GLFW.GLFW_KEY_F || kc == GLFW.GLFW_KEY_SPACE) {
            int target = hoveredIdx >= 0 ? hoveredIdx : selectedIdx;
            if (target >= 0) {
                flipCard(target);
                return true;
            }
        }
        if ((kc == GLFW.GLFW_KEY_ENTER || kc == GLFW.GLFW_KEY_KP_ENTER) && selectedIdx >= 0) {
            confirming = true;
            return true;
        }
        return super.keyPressed(kc, sc2, mod);
    }

    @Override
    public boolean mouseScrolled(double rmx, double rmy, double scrollX, double delta) {
        if (confirming) return true;

        if (!hasShiftDown()) {
            double mx = rmx / uiScale;
            double my = rmy / uiScale;
            float ease = easeOutExpo(introAnim);
            for (int i = 0; i < CARDS.length; i++) {
                int[] b = cardBounds(i, ease);
                if (mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3]) {
                    int face = faceOf(i);
                    if (maxScroll(i, face, ease) > 0f) {
                        scrollCard(i, (float) (-delta * SCROLL_STEP));
                        return true;
                    }
                    break;
                }
            }
        }

        focusing = false;
        arcVelocity += (float) (delta * -200f);
        return true;
    }

    @Override
    public boolean mouseDragged(double rmx, double rmy, int btn, double dragX, double dragY) {
        if (btn == 0 && !confirming) {
            focusing = false;
            arcOffset += (float) (dragX / uiScale) * 1.5f;
            clampArc();
            lastClickIdx = -1;
            return true;
        }
        return super.mouseDragged(rmx / uiScale, rmy / uiScale, btn, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseReleased(double rmx, double rmy, int btn) {
        return super.mouseReleased(rmx / uiScale, rmy / uiScale, btn);
    }

    private void updateHover(int mx, int my) {
        hoveredIdx = -1;
        if (confirming) return;
        float ease = easeOutExpo(introAnim);
        for (int i = 0; i < CARDS.length; i++) {
            int[] b = cardBounds(i, ease);
            if (mx >= b[0] && mx <= b[2] && my >= b[1] && my <= b[3]) {
                hoveredIdx = i;
                break;
            }
        }
    }

    private void commitDiscipline(DisciplineCard card) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal("§6[Axiom] §7Path chosen: §r" + card.name +
                            "§7. Entering your discipline realm..."),
                    false);

            PacketDistributor.sendToServer(new C2SChooseDisciplinePacket(card.id));
            PacketDistributor.sendToServer(new C2SDimensionTeleportPacket("enter"));
        }
        onClose();
    }

    private float[] cardScreenPos(int i) {
        int n = CARDS.length;
        float totalW = CARD_SPACING * n;
        float baseX = vw / 2f - totalW / 2f + CARD_SPACING * i + CARD_SPACING / 2f + arcOffset;

        float normalised = (baseX - vw / 2f) / (vw * 0.4f);
        float arcY = vh / 2f + 10f + normalised * normalised * 40f;
        return new float[] { baseX, arcY };
    }

    private int[] cardBounds(int i, float ease) {
        float[] pos = cardScreenPos(i);
        float sc = cardScale[i] * ease;
        int cx = (int) pos[0];
        int cy = (int) (pos[1] + cardLift[i] * ease);
        int hw = (int) (CARD_W * sc / 2);
        int hh = (int) (CARD_H * sc / 2);
        return new int[] { cx - hw, cy - hh, cx + hw, cy + hh };
    }

    private static int[] buttonRect(int x0, int y0, int x1, int y1, float sc) {
        int btnH = Math.max(14, (int) (20 * sc));
        int btnY = y1 - btnH - (int) (8 * sc);
        int pad = (int) (12 * sc);
        return new int[] { x0 + pad, x1 - pad, btnY, btnH };
    }

    private static int col(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private void drawText(GuiGraphics g, String s, int x, int y, int alpha, int rgb, boolean shadow) {
        if (alpha < 4) return;
        g.drawString(font, s, x, y, col(alpha, rgb), shadow);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(1f, t);
    }

    private static float easeOutExpo(float t) {
        return t >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * t);
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
