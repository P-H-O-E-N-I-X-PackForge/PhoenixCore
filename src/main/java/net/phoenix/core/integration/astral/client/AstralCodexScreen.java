package net.phoenix.core.integration.astral.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.phoenix.chromatic_codes.api.ChromaticEffectsRegistry;
import net.phoenix.core.integration.conflux.client.render.MotionClock;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class AstralCodexScreen extends Screen {

    // Refreshed from the shared Phoenix theme at the top of every render() call - palette feeds
    // the drifting mote particles, the rest color the panel chrome/text. Re-read every frame (not
    // cached) because an animated theme choice (e.g. a rainbow-style theme) changes color from
    // frame to frame.
    private int[] palette;
    private int cText, cDim, cBorder, cBorderDim, cBg1, cBg2, cPanel1, cPanel2;

    private static final String[] PAGE_NAMES = { "Overview", "The Machine Chain", "The Ritual Pedestal", "The Wand" };

    private enum LT { HEADING, SUBHEADING, TEXT, SPACER, DIVIDER }

    private record WLine(LT type, String a) {

        static WLine sh(String s) {
            return new WLine(LT.SUBHEADING, s);
        }

        static WLine t(String s) {
            return new WLine(LT.TEXT, s);
        }

        static WLine sp() {
            return new WLine(LT.SPACER, "");
        }

        static WLine div() {
            return new WLine(LT.DIVIDER, "");
        }
    }

    private static final class Mote {

        float x, y, vx, vy, life, maxLife, size;
        int color;
    }

    private final List<Mote> motes = new ArrayList<>();
    private final Random random = new Random();
    private final MotionClock clock = new MotionClock();
    private long lastNanos = System.nanoTime();

    private int activePage = 0;
    private int scrollY = 0;
    private int cachedContentH = 0;

    private float pageFade = 1f;

    // Minimum usable real-estate for the two-page book layout; below this we shrink the whole
    // screen via a pose scale (same idea used across the rest of the Phoenix Suite) instead of
    // letting the un-clamped Math.min(width-40, 480) sizing squeeze the two page panels' fixed
    // internal layout (index rows, wrapped text) at small windows/high GUI scale.
    private static final int MIN_BOOK_W = 480;
    private static final int MIN_BOOK_H = 300;
    private float uiScale = 1f;
    private int vw, vh;

    public AstralCodexScreen() {
        super(Component.literal("Astral Codex"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new AstralCodexScreen());
    }

    @Override
    protected void init() {
        float neededW = MIN_BOOK_W + 40f;
        float neededH = MIN_BOOK_H + 40f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        refreshTheme();
        scrollY = 0;
        for (int i = 0; i < 40; i++) motes.add(spawnMote(true));
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        palette = new int[] { t.accent.getColor(), t.done.getColor(), t.activeColor.getColor(),
                t.ally.getColor(), t.locked.getColor() };
        cText = t.text.getColor();
        cDim = t.textDim.getColor();
        cBorder = t.accent.getColor();
        cBorderDim = t.border.getColor();
        cBg1 = t.bg.getColor();
        cBg2 = t.panel.getColor();
        cPanel1 = (t.panel.getColor() & 0x00FFFFFF) | 0xF0000000;
        cPanel2 = (t.header.getColor() & 0x00FFFFFF) | 0xF0000000;
    }

    private Mote spawnMote(boolean randomY) {
        Mote m = new Mote();
        m.x = random.nextFloat() * vw;
        m.y = randomY ? random.nextFloat() * vh : vh + 4f;
        m.vx = (random.nextFloat() - 0.5f) * 4f;
        m.vy = -2f - random.nextFloat() * 5f;
        m.size = 0.6f + random.nextFloat() * 1.6f;
        m.maxLife = m.life = 4f + random.nextFloat() * 6f;
        m.color = palette[random.nextInt(palette.length)];
        return m;
    }

    private void tickMotes(float dt) {
        for (int i = motes.size() - 1; i >= 0; i--) {
            Mote m = motes.get(i);
            m.x += m.vx * dt;
            m.y += m.vy * dt;
            m.life -= dt;
            if (m.life <= 0) motes.set(i, spawnMote(false));
        }
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        clock.tick(dt);
        tickMotes(dt);
        pageFade = Math.min(1f, pageFade + dt * 6f);
        refreshTheme();

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        float pulse = MotionClock.Signature.DEFAULT.pulse(clock.getElapsed());

        renderVoidBackdrop(g);
        renderMotes(g, false);

        int bookW = Math.min(vw - 40, MIN_BOOK_W);
        int bookH = Math.min(vh - 40, MIN_BOOK_H);
        int bookX = (vw - bookW) / 2;
        int bookY = (vh - bookH) / 2;
        int leftW = bookW * 34 / 100;
        int rightX = bookX + leftW + 4;
        int rightW = bookW - leftW - 4;

        renderPagePanel(g, bookX, bookY, leftW, bookH, pulse);
        renderPagePanel(g, rightX, bookY, rightW, bookH, pulse);

        renderIndex(g, bookX, bookY, leftW, bookH, mx, my, pulse);
        renderContentPage(g, rightX, bookY, rightW, bookH, pulse);
        if (pageFade < 1f) {
            int fadeAlpha = (int) ((1f - pageFade) * 200);
            g.fill(rightX, bookY, rightX + rightW, bookY + bookH, (fadeAlpha << 24));
        }

        renderMotes(g, true);

        String hint = "Esc to close";
        g.drawString(font, hint, vw - font.width(hint) - 10, 8, cDim, false);

        super.render(g, mx, my, pt);

        g.pose().popPose();
    }

    private void renderVoidBackdrop(GuiGraphics g) {
        g.fillGradient(0, 0, vw, vh, cBg1, cBg2);
    }

    private void renderMotes(GuiGraphics g, boolean front) {
        for (Mote m : motes) {
            float lifeT = m.life / m.maxLife;
            int alpha = (int) (Math.min(1f, lifeT * 3f) * (front ? 140 : 90));
            if (alpha <= 0) continue;
            int col = (alpha << 24) | (m.color & 0xFFFFFF);
            int s = Math.max(1, Math.round(m.size));
            g.fill((int) m.x, (int) m.y, (int) m.x + s, (int) m.y + s, col);
        }
    }

    private void renderPagePanel(GuiGraphics g, int x, int y, int w, int h, float pulse) {
        g.fillGradient(x, y, x + w, y + h, cPanel1, cPanel2);

        int vignette = 22;
        g.fillGradient(x, y, x + w, y + vignette, 0x99000000, 0x00000000);
        g.fillGradient(x, y + h - vignette, x + w, y + h, 0x00000000, 0x99000000);
        g.fillGradient(x, y, x + vignette, y + h, 0x66000000, 0x00000000);
        g.fillGradient(x + w - vignette, y, x + w, y + h, 0x00000000, 0x66000000);

        int glow = (int) (100 + 90 * pulse);
        int borderCol = (0xFF << 24) | (blend(cBorderDim, cBorder, pulse) & 0xFFFFFF);
        g.fill(x, y, x + w, y + 1, borderCol);
        g.fill(x, y + h - 1, x + w, y + h, borderCol);
        g.fill(x, y, x + 1, y + h, borderCol);
        g.fill(x + w - 1, y, x + w, y + h, borderCol);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, cBorderDim);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, cBorderDim);

        int cs = 3;
        int cornerCol = (Math.min(255, glow) << 24) | (cBorder & 0xFFFFFF);
        g.fill(x, y, x + cs, y + cs, cornerCol);
        g.fill(x + w - cs, y, x + w, y + cs, cornerCol);
        g.fill(x, y + h - cs, x + cs, y + h, cornerCol);
        g.fill(x + w - cs, y + h - cs, x + w, y + h, cornerCol);
    }

    private static int blend(int a, int b, float t) {
        return MotionClock.lerpColor(0xFF000000 | a, 0xFF000000 | b, t);
    }

    private void renderIndex(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pulse) {
        int padding = 8;
        int rowH = 18;
        Component title = ChromaticEffectsRegistry.parseCustomEffects("&p Astral Codex");
        g.drawString(font, title, x + padding, y + padding, cText, false);

        int rowY = y + padding + 16;
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            boolean active = i == activePage;
            boolean hover = mx >= x && mx < x + w && my >= rowY && my < rowY + rowH - 2;
            if (active) {
                int a = (int) (140 + 60 * pulse);
                g.fill(x + 2, rowY, x + w - 2, rowY + rowH - 2, (a << 24) | (cPanel2 & 0xFFFFFF));
                g.fill(x + 2, rowY, x + 4, rowY + rowH - 2, cBorder);
            } else if (hover) {
                g.fill(x + 2, rowY, x + w - 2, rowY + rowH - 2, 0x33000000 | (cPanel2 & 0xFFFFFF));
            }
            g.drawString(font, PAGE_NAMES[i], x + padding + 4, rowY + 5, active ? cText : cDim, false);
            rowY += rowH;
        }
    }

    private void renderContentPage(GuiGraphics g, int x, int y, int w, int h, float pulse) {
        int padding = 10;
        List<WLine> lines = buildPage(activePage);

        int contentX = x + padding;
        int contentTop = y + padding;
        int contentW = w - padding * 2;
        int contentBottom = y + h - padding;

        Component title = ChromaticEffectsRegistry.parseCustomEffects("&p " + PAGE_NAMES[activePage]);
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), contentX, contentTop, cText, false);
        int bodyTop = contentTop + 14;

        g.enableScissor(x, y, x + w, y + h);
        int cy = bodyTop - scrollY;
        for (WLine ln : lines) {
            cy = renderLine(g, ln, contentX, cy, contentW, pulse);
        }
        cachedContentH = cy + scrollY - bodyTop;
        g.disableScissor();

        int visibleH = contentBottom - bodyTop;
        int maxScroll = Math.max(0, cachedContentH - visibleH);
        if (maxScroll > 0) {
            int trackX = x + w - 5;
            g.fill(trackX, bodyTop, trackX + 2, contentBottom, 0x33FFFFFF);
            int thumbH = Math.max(12, visibleH * visibleH / cachedContentH);
            int thumbY = bodyTop + (int) ((visibleH - thumbH) * (scrollY / (float) maxScroll));
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, cBorder);
        }
    }

    private int renderLine(GuiGraphics g, WLine ln, int x, int y, int w, float pulse) {
        switch (ln.type()) {
            case SUBHEADING -> {
                return drawWrapped(g,
                        Component.literal(ln.a()).withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE), x, y,
                        w, cText, 11);
            }
            case TEXT -> {
                return drawWrapped(g, Component.literal(ln.a()), x, y, w, cDim, 10);
            }
            case DIVIDER -> {
                g.fill(x, y + 2, x + w, y + 3, cBorderDim);
                return y + 6;
            }
            case SPACER -> {
                return y + 4;
            }
            default -> {
                return y;
            }
        }
    }

    private int drawWrapped(GuiGraphics g, Component text, int x, int y, int maxWidth, int color, int lineHeight) {
        List<net.minecraft.util.FormattedCharSequence> wrapped = font.split(text, maxWidth);
        for (net.minecraft.util.FormattedCharSequence line : wrapped) {
            g.drawString(font, line, x, y, color, false);
            y += lineHeight;
        }
        return y + 2;
    }

    private List<WLine> buildPage(int page) {
        return switch (page) {
            case 0 -> pageOverview();
            case 1 -> pageMachineChain();
            case 2 -> pageRitual();
            case 3 -> pageWand();
            default -> List.of();
        };
    }

    private List<WLine> pageOverview() {
        var L = new ArrayList<WLine>();
        L.add(WLine.t("A native magic-tech pillar, picking up where Ars Nouveau leaves off."));
        L.add(WLine.sp());
        L.add(WLine.div());
        L.add(WLine.sh("Two paths in"));
        L.add(WLine.t(
                "The machine chain: Confluence Hatch, Spinning Wheel, and Loom - built on GTCEU tech, tiered IV through ZPM/UV."));
        L.add(WLine.sp());
        L.add(WLine.t(
                "The Ritual Pedestal: a wholly separate, non-machine path straight from the world - no multiblock required."));
        return L;
    }

    private List<WLine> pageMachineChain() {
        var L = new ArrayList<WLine>();
        L.add(WLine.sh("Astral Confluence Hatch (IV)"));
        L.add(WLine.t("Mints Astral Filament - and optionally Ars Nouveau Source, if present - into Thread."));
        L.add(WLine.sp());
        L.add(WLine.sh("Astral Spinning Wheel (LuV)"));
        L.add(WLine.t("Spins Thread into Skein."));
        L.add(WLine.sp());
        L.add(WLine.sh("Astral Loom (ZPM, cheaper at UV+)"));
        L.add(WLine.t(
                "Weaves Skein into Ensorcelled Weave. The ZPM recipe is deliberately expensive; the UV recipe produces the same output for far less."));
        return L;
    }

    private List<WLine> pageRitual() {
        var L = new ArrayList<WLine>();
        L.add(WLine.sh("Placing the ring"));
        L.add(WLine.t(
                "Place four Astral Rune Blocks exactly 2 blocks north, south, east, and west of the pedestal, on the same level. If you right-click with an incomplete ring, the missing spots are marked with particles."));
        L.add(WLine.sp());
        L.add(WLine.sh("Channeling"));
        L.add(WLine.t(
                "Hold the Wand, keep a catalyst in your offhand, and right-click the pedestal at night."));
        L.add(WLine.sp());
        L.add(WLine.sh("Known catalysts"));
        L.add(WLine.t("Amethyst Shard - Glowstone Dust"));
        L.add(WLine.sp());
        L.add(WLine.t(
                "Charges a carried Astral Thread Cell, or drops Astral Filament if you aren't carrying one."));
        return L;
    }

    private List<WLine> pageWand() {
        var L = new ArrayList<WLine>();
        L.add(WLine.t("Right-click air to open this Codex."));
        L.add(WLine.sp());
        L.add(WLine.t(
                "Right-click an Astral Thread Hatch to transfer Thread between the hatch and a carried Thread Cell."));
        L.add(WLine.sp());
        L.add(WLine.t("Right-click a Ritual Pedestal to channel it."));
        return L;
    }

    @Override
    public boolean mouseScrolled(double rmx, double rmy, double delta) {
        int bookH = Math.min(vh - 40, MIN_BOOK_H);
        int visibleH = bookH - 20 - 24;
        int maxScroll = Math.max(0, cachedContentH - visibleH);
        scrollY = Math.max(0, Math.min(maxScroll, (int) (scrollY - delta * 14)));
        return true;
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int btn) {
        double mx = rmx / uiScale;
        double my = rmy / uiScale;
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int bookW = Math.min(vw - 40, MIN_BOOK_W);
        int bookH = Math.min(vh - 40, MIN_BOOK_H);
        int bookX = (vw - bookW) / 2;
        int bookY = (vh - bookH) / 2;
        int leftW = bookW * 34 / 100;

        if (mx >= bookX && mx < bookX + leftW && my >= bookY) {
            int rowY = bookY + 8 + 16;
            for (int i = 0; i < PAGE_NAMES.length; i++) {
                if (my >= rowY && my < rowY + 16) {
                    if (i != activePage) {
                        activePage = i;
                        scrollY = 0;
                        pageFade = 0f;
                    }
                    return true;
                }
                rowY += 18;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
