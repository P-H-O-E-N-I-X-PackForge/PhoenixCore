package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.client.pdim.PdimClientState;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.network.C2SPdimPacket;
import net.phoenix.core.integration.continuum.pdim.PdimCosts;
import net.phoenix.core.integration.continuum.pdim.PdimDimensions;
import net.phoenix.core.network.PhoenixNetwork;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContinuumPdimScreen extends Screen {

    private static final int W = 300;
    private static final int H = 206;

    private record Preset(String label, float gravity) {}

    private final Screen parent;
    private final ContinuumBody body;
    private final List<Preset> presets = new ArrayList<>();
    private final List<Object[]> rects = new ArrayList<>();
    private int selected;
    private float scale = 1.0f;

    public ContinuumPdimScreen(Screen parent, ContinuumBody body) {
        super(Component.literal("Personal dimension"));
        this.parent = parent;
        this.body = body;

        float natural = PdimDimensions.naturalGravity(body);
        presets.add(new Preset("Natural", natural));
        presets.add(new Preset("Moon", 0.16f));
        presets.add(new Preset("Low", 0.5f));
        presets.add(new Preset("Standard", 1.0f));
        presets.add(new Preset("Heavy", 1.5f));
        presets.add(new Preset("Crushing", 2.5f));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Float existing() {
        return PdimClientState.dimension(body.id());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xCC000000);

        scale = Math.max(0.4f, Math.min(1.0f, Math.min((width - 40.0f) / W, (height - 40.0f) / H)));
        int mx = Math.round(mouseX / scale);
        int my = Math.round(mouseY / scale);
        int vw = Math.round(width / scale);
        int vh = Math.round(height / scale);
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);

        int x = (vw - W) / 2;
        int y = (vh - H) / 2;
        g.fill(x, y, x + W, y + H, 0xFF0b0a18);
        g.renderOutline(x, y, W, H, MapUi.PANEL_LINE);
        rects.clear();

        g.drawString(font, "Personal dimension  -  " + body.name(), x + 10, y + 9, MapUi.TITLE);
        int ty = y + 24;
        ty = wrapped(g, "An empty void with an obsidian platform, under the sky of " + body.name() +
                ". Falling out of the bottom puts you back at the top.", x + 10, ty, W - 20, MapUi.DIM) + 4;

        var cost = PdimClientState.cost(body.id());
        boolean charged = cost != null && !cost.free() && (existing() == null || PdimClientState.chargesEveryVisit());
        boolean creative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative();
        int have = cost == null ? 0 : PdimCosts.count(Minecraft.getInstance().player, cost);
        boolean canPay = !charged || creative || have >= cost.count();

        Float gravity = existing();
        if (gravity != null) {
            g.drawString(font, String.format(Locale.ROOT, "Your dimension here has gravity x%.2f", gravity), x + 10, ty,
                    MapUi.TEXT);
            ty += 11;
            ty = wrapped(g,
                    "Gravity is fixed when a dimension is made. /pdim delete forgets it so you can make a new one.",
                    x + 10, ty, W - 20, MapUi.DIM);
            costLine(g, x + 10, y + H - 62, charged, cost, have, canPay);
            button(g, x + 10, y + H - 48, W - 20, "Enter", "enter", canPay, false, mx, my);
        } else {
            g.drawString(font, "Choose its gravity (fixed once made):", x + 10, ty, MapUi.TEXT);
            ty += 14;
            int bw = (W - 20 - 10) / 3;
            for (int i = 0; i < presets.size(); i++) {
                Preset p = presets.get(i);
                int bx = x + 10 + (i % 3) * (bw + 5);
                int by = ty + (i / 3) * 24;
                String label = p.label() + String.format(Locale.ROOT, "  x%.2f", p.gravity());
                button(g, bx, by, bw, label, "preset" + i, true, i == selected, mx, my);
            }
            costLine(g, x + 10, y + H - 62, charged, cost, have, canPay);
            button(g, x + 10, y + H - 48, W - 20, "Create and enter", "create", canPay, false, mx, my);
        }

        boolean inside = PdimClientState.inside();
        if (inside) {
            button(g, x + 10, y + H - 26, (W - 25) / 2, "Leave this dimension", "leave", true, false, mx, my);
            button(g, x + 15 + (W - 25) / 2, y + H - 26, (W - 25) / 2, "Close", "close", true, false, mx, my);
        } else {
            button(g, x + 10, y + H - 26, W - 20, "Close", "close", true, false, mx, my);
        }

        g.pose().popPose();
    }

    private void costLine(GuiGraphics g, int x, int y, boolean charged, PdimCosts.Cost cost, int have, boolean canPay) {
        if (!charged) {
            g.drawString(font, "This trip is free.", x, y, MapUi.GOOD);
            return;
        }
        g.drawString(font, "Cost: " + cost.describe().getString() + "   (you have " + have + ")", x, y,
                canPay ? MapUi.TEXT : MapUi.BAD);
    }

    private int wrapped(GuiGraphics g, String text, int x, int y, int w, int color) {
        for (FormattedCharSequence line : font.split(FormattedText.of(text), w)) {
            g.drawString(font, line, x, y, color);
            y += 10;
        }
        return y;
    }

    private void button(GuiGraphics g, int x, int y, int w, String label, String id, boolean enabled, boolean selected,
                        int mx, int my) {
        boolean hover = enabled && MapUi.inside(mx, my, x, y, w, 18);
        g.fill(x, y, x + w, y + 18, !enabled ? 0xFF14122a : selected ? 0xFF3a3380 : hover ? 0xFF2c2760 : 0xFF1d1a40);
        g.renderOutline(x, y, w, 18, selected ? MapUi.FRAME : MapUi.PANEL_LINE);
        g.drawCenteredString(font, label, x + w / 2, y + 5, enabled ? MapUi.TITLE : 0xFF565070);
        if (enabled) rects.add(new Object[] { id, x, y, w });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = mouseX / scale;
        double my = mouseY / scale;
        if (button == 0) {
            for (Object[] r : rects) {
                if (!MapUi.inside(mx, my, (int) r[1], (int) r[2], (int) r[3], 18)) continue;
                ContinuumSounds.click();
                String id = (String) r[0];
                if (id.startsWith("preset")) {
                    selected = Integer.parseInt(id.substring(6));
                } else if (id.equals("create") || id.equals("enter")) {
                    float gravity = existing() != null ? existing() : presets.get(selected).gravity();
                    PhoenixNetwork.CHANNEL.sendToServer(
                            new C2SPdimPacket(C2SPdimPacket.Action.ENTER, body.id(), gravity));
                    Minecraft.getInstance().setScreen(null);
                } else if (id.equals("leave")) {
                    PhoenixNetwork.CHANNEL.sendToServer(
                            new C2SPdimPacket(C2SPdimPacket.Action.LEAVE, body.id(), 1.0f));
                    Minecraft.getInstance().setScreen(null);
                } else {
                    onClose();
                }
                return true;
            }
        } else if (button == 1) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
