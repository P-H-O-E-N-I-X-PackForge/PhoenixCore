package net.phoenix.core.client.gui.forge.rite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.client.gui.forge.CineScreen;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.rite.C2SRitePacket;
import net.phoenix.core.rebirth.rite.S2CRitePacket;

@OnlyIn(Dist.CLIENT)
public abstract class RiteScreen extends CineScreen {

    protected final Capstone cap;
    protected final BlockPos pos;
    protected int status;
    protected int[] data;
    protected int[] extra = new int[0];
    protected boolean bound;
    private float closeAt = -1f;

    protected RiteScreen(Capstone cap, S2CRitePacket packet, String profiler) {
        super(cap.displayName, profiler);
        this.cap = cap;
        this.pos = packet.pos();
        this.data = packet.data();
        this.message = packet.message();
    }

    public BlockPos pos() {
        return pos;
    }

    protected abstract String rules();

    protected abstract void stats(GuiGraphics g, int x, int y, float ui);

    protected void onData(S2CRitePacket p) {}

    public final void update(S2CRitePacket p) {
        int oldStatus = status;
        status = p.status();
        data = p.data();
        extra = p.extra();
        if (!p.message().isEmpty()) message = p.message();
        if (status == S2CRitePacket.BOUND && !bound) {
            bound = true;
            flash = 1.4f;
            shake = 0.9f;
            messageColor = 0xFFFFE08A;
            closeAt = seconds + 6f;
            play(SoundEvents.BEACON_ACTIVATE, 1.2f, 0.9f);
            play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.9f);
        } else if (status == S2CRitePacket.FAILED) {
            shake = 1f;
            flash = 0.35f;
            flashColor = 0xFF5030;
            messageColor = 0xFFFF8A70;
            play(SoundEvents.ANVIL_LAND, 0.6f, 0.5f);
        } else if (oldStatus != status) {
            messageColor = 0xFFFFEBD0;
        }
        onData(p);
    }

    protected final void send(int action, int... args) {
        PacketDistributor.sendToServer(new C2SRitePacket(pos, action, args));
    }

    protected float cx() {
        return panelW() + (width - panelW()) / 2f;
    }

    protected float cy() {
        return height * 0.5f;
    }

    @Override
    protected float introSeconds() {
        return 5.2f;
    }

    @Override
    protected String introSub() {
        return "Prove the link.";
    }

    @Override
    protected void drawUi(GuiGraphics g, int mx, int my, float ui) {
        fadePanel(g, true, ui);
        int x = 14;
        int w = panelW() - 28;
        int y = bar() + 14;
        heading(g, "BINDING RITE", x, y, ui);
        y += 20;
        g.drawString(font, font.plainSubstrByWidth(cap.displayName, w), x, y, argb(cap.color, ui), false);
        y += 16;
        wrapped(g, rules(), x, y, w, 0xB8A890, ui);
        y += wrappedHeight(rules(), w) + 14;
        stats(g, x, y, ui);

        if (!message.isEmpty()) {
            int mw = (int) Math.min(width - panelW() - 40, 420);
            int lines = font.split(net.minecraft.network.chat.Component.literal(message), mw).size();
            int my0 = height - bar() - 14 - lines * 10;
            int i = 0;
            for (var line : font.split(net.minecraft.network.chat.Component.literal(message), mw)) {
                int lw = font.width(line);
                g.drawString(font, line, (int) (cx() - lw / 2f), my0 + i * 10, argb(messageColor, ui), false);
                i++;
            }
        }
        if (bound) {
            glowText(g, "BOUND", cx(), bar() + 26, 3f, 0xFFE08A, ui, cap.color);
        }
        button(g, 14, height - bar() - 24, 56, 15, bound ? "Done" : "Leave", mx, my, true, ui, this::onClose);
    }

    @Override
    protected void afterFrame(GuiGraphics g) {
        if (closeAt > 0f && seconds > closeAt) onClose();
    }

    protected static float lerpColorChannel(float a, float b, float t) {
        return Mth.lerp(t, a, b);
    }
}
