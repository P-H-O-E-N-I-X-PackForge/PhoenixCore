package net.phoenix.core.integration.astral.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.chromatic_codes.api.ChromaticEffectsRegistry;
import net.phoenix.core.integration.astral.item.AstralThreadCellItem;
import net.phoenix.core.integration.conflux.client.render.MotionClock;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class AstralThreadCellScreen extends Screen {

    private int[] palette;
    private int cText, cDim, cBorder, cBorderDim, cBg1, cBg2, cPanel1, cPanel2, cAccent1, cAccent2, cGaugeBg;

    private static final class Mote {

        float x, y, vx, vy, life, maxLife, size;
        int color;
    }

    private final InteractionHand hand;
    private final List<Mote> motes = new ArrayList<>();
    private final Random random = new Random();
    private final MotionClock clock = new MotionClock();
    private long lastNanos = System.nanoTime();

    private static final int MIN_PANEL_W = 220;
    private static final int MIN_PANEL_H = 150;
    private float uiScale = 1f;
    private int vw, vh;

    public AstralThreadCellScreen(InteractionHand hand) {
        super(Component.literal("Astral Thread Cell"));
        this.hand = hand;
    }

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new AstralThreadCellScreen(hand));
    }

    @Override
    protected void init() {
        float neededW = MIN_PANEL_W + 60f;
        float neededH = MIN_PANEL_H + 40f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        refreshTheme();
        for (int i = 0; i < 24; i++) motes.add(spawnMote(true));
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
        cAccent1 = t.accent.getColor();
        cAccent2 = t.done.getColor();
        cGaugeBg = (t.panel.getColor() & 0x00FFFFFF) | 0xFF000000;
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

    private ItemStack getStack() {
        if (minecraft == null || minecraft.player == null) return ItemStack.EMPTY;
        ItemStack stack = minecraft.player.getItemInHand(hand);
        return stack.getItem() instanceof AstralThreadCellItem ? stack : ItemStack.EMPTY;
    }

    @Override
    public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        clock.tick(dt);
        tickMotes(dt);
        refreshTheme();
        float pulse = MotionClock.Signature.DEFAULT.pulse(clock.getElapsed());

        int mx = Math.round(rmx / uiScale);
        int my = Math.round(rmy / uiScale);

        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1f);

        g.fillGradient(0, 0, vw, vh, cBg1, cBg2);
        renderMotes(g, false);

        int panelW = Math.min(vw - 60, MIN_PANEL_W);
        int panelH = Math.min(vh - 40, MIN_PANEL_H);
        int px = (vw - panelW) / 2;
        int py = (vh - panelH) / 2;

        renderPanel(g, px, py, panelW, panelH, pulse);

        ItemStack stack = getStack();
        int current = AstralThreadCellItem.getThread(stack);
        int max = AstralThreadCellItem.CAPACITY;
        float fill = max > 0 ? current / (float) max : 0f;

        Component title = ChromaticEffectsRegistry.parseCustomEffects("&p Astral Thread Cell");
        g.drawString(font, title, px + 12, py + 12, cText, false);

        int barX = px + 16;
        int barY = py + 40;
        int barW = panelW - 32;
        int barH = 18;
        renderGauge(g, barX, barY, barW, barH, fill, pulse);

        String amountStr = current + " / " + max;
        g.drawCenteredString(font, amountStr, px + panelW / 2, barY + barH + 8, cText);

        int pct = (int) (fill * 100);
        g.drawCenteredString(font, pct + "% charged", px + panelW / 2, barY + barH + 20, cDim);

        g.drawCenteredString(font,
                Component.literal("Right-click a Thread Hatch to transfer").withStyle(ChatFormatting.ITALIC),
                px + panelW / 2, py + panelH - 22, cDim);
        g.drawCenteredString(font,
                Component.literal("or channel it at a Ritual Pedestal").withStyle(ChatFormatting.ITALIC),
                px + panelW / 2, py + panelH - 12, cDim);

        renderMotes(g, true);

        String hint = "Esc to close";
        g.drawString(font, hint, vw - font.width(hint) - 10, 8, cDim, false);

        super.render(g, mx, my, pt);

        g.pose().popPose();
    }

    private void renderGauge(GuiGraphics g, int x, int y, int w, int h, float fill, float pulse) {
        g.fill(x, y, x + w, y + h, cGaugeBg);
        int fillW = (int) (w * Math.max(0f, Math.min(1f, fill)));
        if (fillW > 0) {
            int glowCol = MotionClock.lerpColor(0xFF000000 | cAccent1, 0xFF000000 | cAccent2, 0.3f + 0.3f * pulse);
            g.fillGradient(x, y, x + fillW, y + h, 0xFF000000 | cAccent1, glowCol);
        }
        int borderCol = (0xFF << 24) |
                (MotionClock.lerpColor(0xFF000000 | cBorderDim, 0xFF000000 | cBorder, pulse) & 0xFFFFFF);
        g.fill(x, y, x + w, y + 1, borderCol);
        g.fill(x, y + h - 1, x + w, y + h, borderCol);
        g.fill(x, y, x + 1, y + h, borderCol);
        g.fill(x + w - 1, y, x + w, y + h, borderCol);
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

    private void renderPanel(GuiGraphics g, int x, int y, int w, int h, float pulse) {
        g.fillGradient(x, y, x + w, y + h, cPanel1, cPanel2);

        int vignette = 18;
        g.fillGradient(x, y, x + w, y + vignette, 0x99000000, 0x00000000);
        g.fillGradient(x, y + h - vignette, x + w, y + h, 0x00000000, 0x99000000);
        g.fillGradient(x, y, x + vignette, y + h, 0x66000000, 0x00000000);
        g.fillGradient(x + w - vignette, y, x + w, y + h, 0x00000000, 0x66000000);

        int borderCol = (0xFF << 24) |
                (MotionClock.lerpColor(0xFF000000 | cBorderDim, 0xFF000000 | cBorder, pulse) & 0xFFFFFF);
        g.fill(x, y, x + w, y + 1, borderCol);
        g.fill(x, y + h - 1, x + w, y + h, borderCol);
        g.fill(x, y, x + 1, y + h, borderCol);
        g.fill(x + w - 1, y, x + w, y + h, borderCol);

        int cs = 3;
        int glow = (int) (100 + 90 * pulse);
        int cornerCol = (Math.min(255, glow) << 24) | (cBorder & 0xFFFFFF);
        g.fill(x, y, x + cs, y + cs, cornerCol);
        g.fill(x + w - cs, y, x + w, y + cs, cornerCol);
        g.fill(x, y + h - cs, x + cs, y + h, cornerCol);
        g.fill(x + w - cs, y + h - cs, x + w, y + h, cornerCol);
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
