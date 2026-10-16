package net.phoenix.core.integration.vocal_resonance.puzzle.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.vocal_resonance.puzzle.AcousticChart;
import net.phoenix.core.integration.vocal_resonance.puzzle.AttuneDifficulty;
import net.phoenix.core.network.packet.C2SAttunePressPacket;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AttuneScreen extends Screen {

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    private static final int[] KEYS = { GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K,
            GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_SEMICOLON };
    private static final String[] KEY_NAMES = { "D", "F", "J", "K", "S", "L", "A", ";" };
    private static final int[] LANE_COLORS = { 0xFFFF5577, 0xFFFFC04D, 0xFF5CE6A6, 0xFF58A8FF, 0xFFB57BFF, 0xFFFF8FD6,
            0xFF9AE35B, 0xFF6EE7F5 };
    private static final int LANE_W = 54;
    private static final int BURST_MS = 280;

    private static int timingOffsetMs = 0;

    private record Burst(int lane, long at, int color, boolean perfect) {}

    private final BlockPos pos;
    private final AttuneDifficulty difficulty;
    private final List<Integer> laneNotes;
    private final List<AcousticChart.Beat> beats;

    private final byte[] judged;
    private final boolean[] holdPerfect;
    private final long[] lastSustain;
    private final float[] laneFlash;
    private final boolean[] held;
    private final List<Burst> bursts = new ArrayList<>();

    private final long zeroMs = Util.getMillis() + AttuneDifficulty.LEAD_MS;

    private int perfect;
    private int good;
    private int missed;
    private int combo;
    private int bestCombo;
    private long judgementAt = -10_000;
    private String judgement = "";
    private String timing = "";
    private int judgementColor = 0xFFFFFFFF;
    private float shake;

    private boolean resultIn;
    private boolean passed;
    private int grade;

    public AttuneScreen(BlockPos pos, AttuneDifficulty difficulty, int bpm, List<Integer> laneNotes,
                        List<AcousticChart.Beat> beats) {
        super(Component.literal("Attunement"));
        this.pos = pos;
        this.difficulty = difficulty;
        this.laneNotes = laneNotes;
        this.beats = beats;
        this.judged = new byte[beats.size()];
        this.holdPerfect = new boolean[beats.size()];
        this.lastSustain = new long[laneNotes.size()];
        this.laneFlash = new float[laneNotes.size()];
        this.held = new boolean[laneNotes.size()];
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
        return (int) (Util.getMillis() - zeroMs) - timingOffsetMs;
    }

    private int beatMs(AcousticChart.Beat beat) {
        return difficulty.beatMs(beat.tick());
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT_BRACKET || key == GLFW.GLFW_KEY_MINUS) {
            timingOffsetMs -= 5;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_BRACKET || key == GLFW.GLFW_KEY_EQUAL) {
            timingOffsetMs += 5;
            return true;
        }
        for (int lane = 0; lane < laneNotes.size() && lane < KEYS.length; lane++) {
            if (key == KEYS[lane]) {

                if (!held[lane] && !resultIn) press(lane);
                held[lane] = true;
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        for (int lane = 0; lane < laneNotes.size() && lane < KEYS.length; lane++) {
            if (key == KEYS[lane]) {
                held[lane] = false;
                if (!resultIn) release(lane);
            }
        }
        return super.keyReleased(key, scan, mods);
    }

    private void press(int lane) {
        int now = elapsed();
        laneFlash[lane] = 1.0f;
        playNote(lane, laneNotes.get(lane), 1.0f);
        PacketDistributor.sendToServer(new C2SAttunePressPacket(pos, lane, now));

        int best = -1;
        int bestDelta = Integer.MAX_VALUE;
        int bestSigned = 0;
        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] != 0 || beats.get(i).lane() != lane) continue;
            int signed = now - beatMs(beats.get(i));
            if (Math.abs(signed) < bestDelta) {
                bestDelta = Math.abs(signed);
                bestSigned = signed;
                best = i;
            }
        }
        int color = LANE_COLORS[lane % LANE_COLORS.length];
        if (best < 0 || bestDelta > difficulty.goodMs()) {

            bursts.add(new Burst(lane, Util.getMillis(), 0x66FFFFFF, false));
            return;
        }
        boolean isPerfect = bestDelta <= difficulty.perfectMs();
        if (beats.get(best).isHold()) {

            judged[best] = 4;
            holdPerfect[best] = isPerfect;
            lastSustain[lane] = Util.getMillis();
            say(isPerfect ? "PERFECT" : "GOOD", isPerfect ? 0xFF7AFFB0 : 0xFFFFE066);
            timing = "hold";
            bursts.add(new Burst(lane, Util.getMillis(), color, isPerfect));
            combo++;
            bestCombo = Math.max(bestCombo, combo);
            return;
        }
        judged[best] = (byte) (isPerfect ? 2 : 1);
        if (isPerfect) {
            perfect++;
            say("PERFECT", 0xFF7AFFB0);
            shake = 4.0f;
        } else {
            good++;
            say("GOOD", 0xFFFFE066);
        }
        timing = isPerfect ? "" : bestSigned < 0 ? "early" : "late";
        bursts.add(new Burst(lane, Util.getMillis(), color, isPerfect));
        combo++;
        bestCombo = Math.max(bestCombo, combo);
    }

    private void release(int lane) {
        int now = elapsed();
        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] != 4 || beats.get(i).lane() != lane) continue;
            PacketDistributor.sendToServer(new C2SAttunePressPacket(pos, lane, now, true));
            if (now >= tailMs(beats.get(i)) - difficulty.goodMs()) {
                finishHold(i);
            } else {
                judged[i] = 3;
                missed++;
                combo = 0;
                say("DROPPED", 0xFFFF6B6B);
            }
            return;
        }
    }

    private void finishHold(int i) {
        judged[i] = (byte) (holdPerfect[i] ? 2 : 1);
        if (holdPerfect[i]) {
            perfect++;
            shake = 3.0f;
        } else {
            good++;
        }
        int lane = beats.get(i).lane();
        bursts.add(new Burst(lane, Util.getMillis(), LANE_COLORS[lane % LANE_COLORS.length], holdPerfect[i]));
    }

    private int tailMs(AcousticChart.Beat beat) {
        return difficulty.beatMs(beat.endTick());
    }

    private int lastEndMs() {
        int end = 0;
        for (AcousticChart.Beat beat : beats) end = Math.max(end, tailMs(beat));
        return end;
    }

    private void say(String text, int color) {
        judgement = text;
        judgementColor = color;
        judgementAt = Util.getMillis();
        if (text.equals("MISS")) timing = "";
    }

    private static java.util.List<java.util.function.Supplier<net.minecraft.sounds.SoundEvent>> voices() {
        return java.util.List.of(SoundEvents.NOTE_BLOCK_HARP::value, SoundEvents.NOTE_BLOCK_BELL::value,
                SoundEvents.NOTE_BLOCK_FLUTE::value, SoundEvents.NOTE_BLOCK_CHIME::value,
                SoundEvents.NOTE_BLOCK_XYLOPHONE::value, SoundEvents.NOTE_BLOCK_GUITAR::value,
                SoundEvents.NOTE_BLOCK_PLING::value, SoundEvents.NOTE_BLOCK_BIT::value);
    }

    private static void playNote(int lane, int midi, float volume) {
        if (midi < 0) return;

        float pitch = (float) Math.pow(2.0, (midi - 66) / 12.0);
        pitch = Math.max(0.5f, Math.min(2.0f, pitch));
        java.util.List<java.util.function.Supplier<net.minecraft.sounds.SoundEvent>> voices = voices();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                voices.get(lane % voices.size()).get(), pitch, volume));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int now = elapsed();

        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] == 0 && now - beatMs(beats.get(i)) > difficulty.goodMs()) {
                judged[i] = 3;
                missed++;
                combo = 0;
                say("MISS", 0xFFFF6B6B);
            }
        }

        long wallNow = Util.getMillis();
        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] != 4) continue;
            AcousticChart.Beat beat = beats.get(i);
            if (now >= tailMs(beat)) {

                PacketDistributor.sendToServer(new C2SAttunePressPacket(pos, beat.lane(), now, true));
                finishHold(i);
                say(holdPerfect[i] ? "PERFECT" : "GOOD", holdPerfect[i] ? 0xFF7AFFB0 : 0xFFFFE066);
            } else if (wallNow - lastSustain[beat.lane()] > 320) {
                lastSustain[beat.lane()] = wallNow;
                playNote(beat.lane(), laneNotes.get(beat.lane()), 0.45f);
                laneFlash[beat.lane()] = Math.max(laneFlash[beat.lane()], 0.6f);
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
            laneFlash[lane] = Math.max(0.0f, laneFlash[lane] - 0.12f);
            g.fill(x + 2, hitY, x + LANE_W - 2, hitY + 4, color);
            g.drawCenteredString(font, KEY_NAMES[lane], x + LANE_W / 2, hitY + 8, color);
        }

        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] == 1 || judged[i] == 2) continue;
            AcousticChart.Beat beat = beats.get(i);
            int until = beatMs(beat) - now;
            int tailUntil = tailMs(beat) - now;
            if (until > difficulty.fallMs() || (beat.isHold() ? tailUntil : until) < -300) continue;
            int color = judged[i] == 3 ? 0xFF55556A : LANE_COLORS[beat.lane() % LANE_COLORS.length];
            int x = left + beat.lane() * LANE_W;
            boolean inProgress = judged[i] == 4;
            if (beat.isHold()) {
                float tt = 1.0f - tailUntil / (float) difficulty.fallMs();
                int tailY = top + Math.round(tt * (hitY - top));
                int headY = inProgress ? hitY :
                        top + Math.round((1.0f - until / (float) difficulty.fallMs()) * (hitY - top));
                int bodyTop = Math.max(top, Math.min(tailY, headY));
                int bodyBottom = Math.min(hitY + 4, Math.max(tailY, headY));
                if (bodyBottom > bodyTop) {
                    int body = inProgress ? ((0xB0 << 24) | (color & 0xFFFFFF)) : ((0x66 << 24) | (color & 0xFFFFFF));
                    g.fill(x + 14, bodyTop, x + LANE_W - 14, bodyBottom, body);
                    g.fill(x + 12, bodyTop, x + 14, bodyBottom, color);
                    g.fill(x + LANE_W - 14, bodyTop, x + LANE_W - 12, bodyBottom, color);
                }
                if (inProgress) {
                    g.fill(x + 4, hitY - 4, x + LANE_W - 4, hitY + 8, 0xFFFFFFFF);
                    continue;
                }
            }
            float t = 1.0f - until / (float) difficulty.fallMs();
            int y = top + Math.round(t * (hitY - top));
            g.fill(x + 6, y - 7, x + LANE_W - 6, y + 7, color);
            g.fill(x + 6, y - 7, x + LANE_W - 6, y - 5, 0x66FFFFFF);
        }

        long wall = Util.getMillis();
        bursts.removeIf(b -> wall - b.at() > BURST_MS);
        for (Burst b : bursts) {
            float p = (wall - b.at()) / (float) BURST_MS;
            int x = left + b.lane() * LANE_W;
            int grow = Math.round(p * (b.perfect() ? 16 : 9));
            int alpha = (int) ((1.0f - p) * 200);
            int c = (alpha << 24) | (b.color() & 0xFFFFFF);
            g.renderOutline(x + 2 - grow, hitY - 6 - grow, LANE_W - 4 + grow * 2, 16 + grow * 2, c);
            if (b.perfect()) g.fill(x + 2, hitY - 2, x + LANE_W - 2, hitY + 6, c);
        }
        g.pose().popPose();

        if (now < 0) {
            int n = (int) Math.ceil(-now / 1000.0);
            g.pose().pushPose();
            g.pose().translate(width / 2.0f, height / 2.0f - 30, 0);
            g.pose().scale(4.0f, 4.0f, 1.0f);
            g.drawCenteredString(font, String.valueOf(n), 0, 0, 0xFFFFE066);
            g.pose().popPose();
            g.drawCenteredString(font, difficulty.label() + " - get ready", width / 2, height / 2 + 14,
                    difficulty.color());
        }

        long since = Util.getMillis() - judgementAt;
        if (since < 600) {
            int alpha = (int) (255 * (1.0f - since / 600.0f));
            int y = hitY - 70 - (int) (since / 30);
            g.drawCenteredString(font, judgement, width / 2, y, (alpha << 24) | (judgementColor & 0xFFFFFF));
            if (!timing.isEmpty()) {
                g.drawCenteredString(font, timing, width / 2, y + 11, (alpha << 24) | 0xB8B0D8);
            }
        }
        if (combo >= 2) {
            g.pose().pushPose();
            g.pose().translate(width / 2.0f, 40, 0);
            float pop = 1.0f + Math.min(0.5f, kick * 0.08f);
            g.pose().scale(2.0f * pop, 2.0f * pop, 1.0f);
            g.drawCenteredString(font, combo + "x", 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
        }

        int endMs = lastEndMs() + difficulty.goodMs();
        int barW = lanes * LANE_W;
        g.fill(left, 14, left + barW, 18, 0xFF22223A);
        g.fill(left, 14, left + Math.round(barW * Math.max(0.0f, Math.min(1.0f, now / (float) endMs))), 18,
                difficulty.color());
        g.drawString(font, difficulty.label(), left, 4, difficulty.color(), false);
        g.drawString(font, "Esc to leave   [ ] timing " + (timingOffsetMs >= 0 ? "+" : "") + timingOffsetMs + " ms", 6,
                height - 14, 0xFF6C6C8A, false);

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
            g.drawCenteredString(font, difficulty.label() + ":  " + perfect + " perfect   " + good + " good   " +
                    missed + " missed   best " + bestCombo + "x", width / 2, height / 2 + 6, 0xFFB8B0D8);
            g.drawCenteredString(font, passed && difficulty.level() < AttuneDifficulty.values().length ?
                    "Esc to close. Open the lock again to try a harder level." : "Esc to close", width / 2,
                    height / 2 + 22, 0xFF6C6C8A);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }
}
