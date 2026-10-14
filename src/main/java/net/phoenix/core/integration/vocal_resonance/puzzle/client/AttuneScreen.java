package net.phoenix.core.integration.vocal_resonance.puzzle.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.vocal_resonance.puzzle.AcousticChart;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SAttunePressPacket;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The attunement rhythm game. Notes fall down four lanes; press the lane's key as a note crosses the line. The screen
 * judges hits for feedback only. Each press is sent to the lock, and the lock's verdict is the one that counts.
 */
@OnlyIn(Dist.CLIENT)
public class AttuneScreen extends Screen {

    private static final int[] KEYS = { GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K,
            GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_SEMICOLON };
    private static final String[] KEY_NAMES = { "D", "F", "J", "K", "S", "L", "A", ";" };
    private static final int[] LANE_COLORS = { 0xFFFF5577, 0xFFFFC04D, 0xFF5CE6A6, 0xFF58A8FF, 0xFFB57BFF, 0xFFFF8FD6,
            0xFF9AE35B, 0xFF6EE7F5 };

    private static final int PERFECT_MS = 60;
    private static final int GOOD_MS = 120;
    private static final int FALL_MS = 1100;
    private static final int LANE_W = 54;

    private final BlockPos pos;
    private final List<Integer> laneNotes;
    private final List<AcousticChart.Beat> beats;
    /** 0 = untouched, 1 = good, 2 = perfect, 3 = missed */
    private final byte[] judged;
    private final float[] laneFlash;

    private final long startMs = Util.getMillis();
    private int perfect;
    private int good;
    private int missed;
    private int combo;
    private int bestCombo;
    private long judgementAt = -10_000;
    private String judgement = "";
    private int judgementColor = 0xFFFFFFFF;
    private float shake;

    private boolean resultIn;
    private boolean passed;
    private int grade;

    public AttuneScreen(BlockPos pos, int bpm, List<Integer> laneNotes, List<AcousticChart.Beat> beats) {
        super(Component.literal("Attunement"));
        this.pos = pos;
        this.laneNotes = laneNotes;
        this.beats = beats;
        this.judged = new byte[beats.size()];
        this.laneFlash = new float[laneNotes.size()];
    }

    public boolean isFor(BlockPos other) {
        return pos.equals(other);
    }

    public void showResult(boolean passed, int grade, int perfect, int good, int total) {
        this.resultIn = true;
        this.passed = passed;
        this.grade = grade;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int elapsed() {
        return (int) (Util.getMillis() - startMs);
    }

    private static int beatMs(AcousticChart.Beat beat) {
        return beat.tick() * AcousticChart.MS_PER_TICK;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        for (int lane = 0; lane < laneNotes.size() && lane < KEYS.length; lane++) {
            if (key == KEYS[lane]) {
                if (!resultIn) press(lane);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    private void press(int lane) {
        int now = elapsed();
        laneFlash[lane] = 1.0f;
        playNote(laneNotes.get(lane));
        PhoenixNetwork.CHANNEL.sendToServer(new C2SAttunePressPacket(pos, lane, now));

        int best = -1;
        int bestDelta = Integer.MAX_VALUE;
        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] != 0 || beats.get(i).lane() != lane) continue;
            int delta = Math.abs(beatMs(beats.get(i)) - now);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = i;
            }
        }
        if (best < 0 || bestDelta > GOOD_MS) return;
        if (bestDelta <= PERFECT_MS) {
            judged[best] = 2;
            perfect++;
            say("PERFECT", 0xFF7AFFB0);
            shake = 4.0f;
        } else {
            judged[best] = 1;
            good++;
            say("GOOD", 0xFFFFE066);
        }
        combo++;
        bestCombo = Math.max(bestCombo, combo);
    }

    private void say(String text, int color) {
        judgement = text;
        judgementColor = color;
        judgementAt = Util.getMillis();
    }

    private static void playNote(int midi) {
        if (midi < 0) return;
        // note blocks run F#3 (midi 54) to F#5 (midi 78) at pitch 0.5 to 2.0
        float pitch = (float) Math.pow(2.0, (midi - 66) / 12.0);
        pitch = Math.max(0.5f, Math.min(2.0f, pitch));
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.get(), pitch, 1.0f));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int now = elapsed();

        // notes that slid past the window are misses
        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] == 0 && now - beatMs(beats.get(i)) > GOOD_MS) {
                judged[i] = 3;
                missed++;
                combo = 0;
                say("MISS", 0xFFFF6B6B);
            }
        }

        int lanes = laneNotes.size();
        int left = (width - lanes * LANE_W) / 2;
        int top = 24;
        int hitY = height - 56;
        float kick = shake;
        shake = Math.max(0.0f, shake - 0.6f);
        int yOff = kick > 0.5f ? Math.round((float) Math.sin(now * 0.9) * kick) : 0;

        g.fill(0, 0, width, height, 0xE60A0A18);
        g.pose().pushPose();
        g.pose().translate(0, yOff, 0);

        for (int lane = 0; lane < lanes; lane++) {
            int x = left + lane * LANE_W;
            int color = LANE_COLORS[lane % LANE_COLORS.length];
            g.fill(x + 1, top, x + LANE_W - 1, hitY + 20, 0xFF14142A);
            int flash = (int) (laneFlash[lane] * 110);
            if (flash > 0) {
                g.fill(x + 1, top, x + LANE_W - 1, hitY + 20, (flash << 24) | (color & 0xFFFFFF));
            }
            laneFlash[lane] = Math.max(0.0f, laneFlash[lane] - 0.08f);
            g.fill(x + 2, hitY, x + LANE_W - 2, hitY + 4, color);
            g.drawCenteredString(font, KEY_NAMES[lane], x + LANE_W / 2, hitY + 8, color);
        }

        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] == 1 || judged[i] == 2) continue;
            AcousticChart.Beat beat = beats.get(i);
            int until = beatMs(beat) - now;
            if (until > FALL_MS || until < -300) continue;
            float t = 1.0f - until / (float) FALL_MS;
            int y = top + Math.round(t * (hitY - top));
            int x = left + beat.lane() * LANE_W;
            int color = judged[i] == 3 ? 0xFF55556A : LANE_COLORS[beat.lane() % LANE_COLORS.length];
            g.fill(x + 6, y - 7, x + LANE_W - 6, y + 7, color);
            g.fill(x + 6, y - 7, x + LANE_W - 6, y - 5, 0x66FFFFFF);
        }
        g.pose().popPose();

        // judgement and combo
        long since = Util.getMillis() - judgementAt;
        if (since < 600) {
            int alpha = (int) (255 * (1.0f - since / 600.0f));
            g.drawCenteredString(font, judgement, width / 2, hitY - 70 - (int) (since / 30), (alpha << 24) |
                    (judgementColor & 0xFFFFFF));
        }
        if (combo >= 2) {
            g.pose().pushPose();
            g.pose().translate(width / 2.0f, 40, 0);
            float pop = 1.0f + Math.min(0.5f, kick * 0.08f);
            g.pose().scale(2.0f * pop, 2.0f * pop, 1.0f);
            g.drawCenteredString(font, combo + "x", 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
        }

        // progress
        int endMs = beatMs(beats.get(beats.size() - 1)) + GOOD_MS;
        int barW = lanes * LANE_W;
        g.fill(left, 14, left + barW, 18, 0xFF22223A);
        g.fill(left, 14, left + Math.round(barW * Math.min(1.0f, now / (float) endMs)), 18, 0xFF7A6BFF);
        g.drawString(font, "Esc to leave", 6, height - 14, 0xFF6C6C8A);

        if (now > endMs && !resultIn) {
            g.drawCenteredString(font, "Waiting for the lock...", width / 2, height / 2, 0xFFB8B0D8);
        }
        if (resultIn) {
            g.fill(0, height / 2 - 38, width, height / 2 + 38, 0xE6000010);
            String title = passed ? (grade >= 2 ? "PERFECT ATTUNEMENT" : grade == 1 ? "ATTUNED - GOOD" : "ATTUNED") :
                    "OUT OF TUNE - TRY AGAIN";
            g.pose().pushPose();
            g.pose().translate(width / 2.0f, height / 2.0f - 20, 0);
            g.pose().scale(2.0f, 2.0f, 1.0f);
            g.drawCenteredString(font, title, 0, 0, passed ? 0xFF7AFFB0 : 0xFFFF6B6B);
            g.pose().popPose();
            g.drawCenteredString(font, perfect + " perfect   " + good + " good   " + missed + " missed   best " +
                    bestCombo + "x", width / 2, height / 2 + 6, 0xFFB8B0D8);
            g.drawCenteredString(font, "Esc to close", width / 2, height / 2 + 22, 0xFF6C6C8A);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }
}
