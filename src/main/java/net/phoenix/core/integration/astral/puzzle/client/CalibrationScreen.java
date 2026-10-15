package net.phoenix.core.integration.astral.puzzle.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.astral.puzzle.StarChart;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCalibratePacket;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CalibrationScreen extends Screen {

    private final BlockPos pos;
    private final StarChart.Kind kind;
    private final int[] stars;
    private final List<int[]> lines = new ArrayList<>();
    private int picked = -1;

    private String feedback = "";
    private int feedbackColor = 0xFFB8B0D8;
    private long feedbackAt;

    private StarMap map;
    private final List<Object[]> buttons = new ArrayList<>();

    public CalibrationScreen(BlockPos pos, StarChart.Kind kind, int[] stars, boolean calibrated) {
        super(Component.literal("Calibration"));
        this.pos = pos;
        this.kind = kind;
        this.stars = stars;
        if (calibrated) feedback = "Already calibrated. Drawing a new chart changes nothing.";
    }

    public boolean isFor(BlockPos other) {
        return pos.equals(other);
    }

    public void showResult(boolean solved, boolean aligned, int right, int total, int drawn, String lost) {
        feedbackAt = Util.getMillis();
        if (solved) {
            feedback = aligned ? "Calibrated - and aligned with the moon. It will run faster." :
                    "Calibrated. The machine will run.";
            feedbackColor = 0xFF7AFFB0;
        } else {
            feedback = right + " of " + total + " lines are right" + (drawn > total ? " (too many drawn)" : "") + "." +
                    (lost.isEmpty() ? "" : " The stars scatter " + lost + ".");
            feedbackColor = right == 0 ? 0xFFFF6B6B : 0xFFFFD27A;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int starX(int i) {
        return (stars[i] >> 4) & 15;
    }

    private int starY(int i) {
        return stars[i] & 15;
    }

    @Override
    protected void init() {
        int size = Math.max(120, Math.min(Math.min(width - 60, height - 130), 300));
        map = new StarMap((width - size) / 2, 34, size);
    }

    private static int key(StarChart.Kind kind, int a, int b) {
        return kind == StarChart.Kind.WEB ? Math.min(a, b) * 16 + Math.max(a, b) : a * 16 + b;
    }

    private void toggleLine(int a, int b) {
        for (int i = 0; i < lines.size(); i++) {
            if (key(kind, lines.get(i)[0], lines.get(i)[1]) == key(kind, a, b)) {
                lines.remove(i);
                return;
            }
        }
        if (lines.size() < StarChart.MAX_LINES) lines.add(new int[] { a, b });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        buttons.clear();
        g.fill(0, 0, width, height, 0xE60A0A18);
        g.drawCenteredString(font, kind == StarChart.Kind.CHAIN ?
                "Spinning Wheel - draw the thread through the stars, in order" :
                "Loom - weave the stars into the right web", width / 2, 12, 0xFFE8D8FF);
        g.drawCenteredString(font, kind == StarChart.Kind.CHAIN ? "Click a star, then the star it leads to." :
                "Click two stars to join them. Order does not matter.", width / 2, 23, 0xFF9A94B8);

        map.background(g);
        for (int i = 0; i < lines.size(); i++) {
            int[] l = lines.get(i);
            StarMap.line(g, map.px(starX(l[0])), map.py(starY(l[0])), map.px(starX(l[1])), map.py(starY(l[1])),
                    StarMap.LINE_COLOR, kind == StarChart.Kind.CHAIN);
            if (kind == StarChart.Kind.CHAIN) {
                int mx = (map.px(starX(l[0])) + map.px(starX(l[1]))) / 2;
                int my = (map.py(starY(l[0])) + map.py(starY(l[1]))) / 2;
                g.drawString(font, String.valueOf(i + 1), mx + 3, my - 9, 0xFFB8B0D8, false);
            }
        }
        for (int i = 0; i < stars.length; i++) {
            int x = map.px(starX(i));
            int y = map.py(starY(i));
            boolean hover = Math.abs(mouseX - x) <= 8 && Math.abs(mouseY - y) <= 8;
            map.star(g, x, y, i == picked ? StarMap.PICK_COLOR : StarMap.STAR_COLOR, hover || i == picked);
        }
        if (picked >= 0) {
            StarMap.line(g, map.px(starX(picked)), map.py(starY(picked)), mouseX, mouseY, 0x66FFFFFF, false);
        }

        int by = map.top() + map.size() + 10;
        int bw = 76;
        int bx = (width - (bw * 4 + 18)) / 2;
        button(g, bx, by, bw, "Undo", "undo", mouseX, mouseY);
        button(g, bx + bw + 6, by, bw, "Clear", "clear", mouseX, mouseY);
        button(g, bx + (bw + 6) * 2, by, bw, "Calibrate", "go", mouseX, mouseY);
        button(g, bx + (bw + 6) * 3, by, bw, "Close", "close", mouseX, mouseY);

        if (!feedback.isEmpty()) {
            g.drawCenteredString(font, feedback, width / 2, by + 28, feedbackColor);
        }
        g.drawCenteredString(font, lines.size() + " lines drawn", width / 2, by + 42, 0xFF6C6C8A);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void button(GuiGraphics g, int x, int y, int w, String label, String id, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 18;
        g.fill(x, y, x + w, y + 18, hover ? 0xFF2C2760 : 0xFF1D1A40);
        g.renderOutline(x, y, w, 18, 0xFF3A2F7A);
        g.drawCenteredString(font, label, x + w / 2, y + 5, 0xFFE8D8FF);
        buttons.add(new Object[] { id, x, y, w });
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (Object[] b : buttons) {
                int x = (int) b[1];
                int y = (int) b[2];
                int w = (int) b[3];
                if (mx >= x && mx < x + w && my >= y && my < y + 18) {
                    switch ((String) b[0]) {
                        case "undo" -> {
                            if (!lines.isEmpty()) lines.remove(lines.size() - 1);
                            picked = -1;
                        }
                        case "clear" -> {
                            lines.clear();
                            picked = -1;
                        }
                        case "go" -> {
                            if (!lines.isEmpty()) {
                                PhoenixNetwork.CHANNEL.sendToServer(new C2SCalibratePacket(pos, lines));
                            }
                        }
                        default -> onClose();
                    }
                    return true;
                }
            }
            for (int i = 0; i < stars.length; i++) {
                if (Math.abs(mx - map.px(starX(i))) <= 8 && Math.abs(my - map.py(starY(i))) <= 8) {
                    if (picked < 0) {
                        picked = i;
                    } else if (picked == i) {
                        picked = -1;
                    } else {
                        toggleLine(picked, i);
                        picked = -1;
                    }
                    return true;
                }
            }
            picked = -1;
        } else if (button == 1) {
            picked = -1;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
