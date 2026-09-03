package net.phoenix.core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.UpdateWingSettingsPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Deliberately a plain Screen rather than a ModularUI panel - ModularUI's own API differs
 * incompatibly between branches (brachy modularui on 8.0 vs lowdraglib's older ModularUI here,
 * and even GTCEu's own texture-drawable classes don't have a common standalone draw() that works
 * outside each framework's own widget/render context), so a raw Screen using only vanilla
 * GuiGraphics primitives is the one implementation that ports cleanly across both without a
 * rewrite. The panel/button look is hand-drawn (drawPanel: flat fill + a light bevel edge
 * top-left, dark shadow edge bottom-right) rather than borrowed GT textures, purely so this file
 * has zero framework dependency at all.
 * <p>
 * Every interactive element (mode button, +/-, slider segments) is hit-tested manually in
 * mouseClicked() against the clickRegions list render() just rebuilt, instead of using vanilla
 * Button widgets - a vanilla Button still paints its own grey sprite/hover overlay even with an
 * empty label, which was rendering on top of the segmented bars' color fill every frame
 * (super.render() draws all added widgets after this class's own render() body) and is what
 * actually read as "a bunch of vanilla buttons" pasted over the custom bar.
 */
@OnlyIn(Dist.CLIENT)
public class WingFlightScreen extends Screen {

    private String flightMode;
    private int flightSpeed;
    private int flightDrift;
    private int flightVertical;

    private static final int W = 210;
    private static final int H = 260;
    private static final int STEPS = 10;

    private static final int SLIDER_SPEED = 0;
    private static final int SLIDER_DRIFT = 1;
    private static final int SLIDER_VERTICAL = 2;

    private static final int COLOR_TITLE = 0xFFB000FF;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_BASIC = 0xFF00FF88;
    private static final int COLOR_POWERED = 0xFFFFAA00;
    private static final int COLOR_CREATIVE = 0xFFFF55FF;
    private static final int COLOR_WINGED = 0xFF55FFFF;
    private static final int COLOR_FILLED = 0xFF8800CC;
    private static final int COLOR_HIGHLIGHT = 0xFFC480E6;
    private static final int COLOR_EMPTY = 0xFF3A3A3A;

    private static final int PANEL_BG = 0xEE0A0512;
    private static final int PANEL_BEVEL = 0xFF3A2050;
    private static final int PANEL_SHADOW = 0xC0000000;
    private static final int DISPLAY_BG = 0xFF120A1E;
    private static final int BUTTON_BG = 0xFF241436;
    private static final int BUTTON_BG_HOVER = 0xFF3A2050;
    private static final int BUTTON_BORDER = 0xFF7A3FBF;

    private float uiScale = 1f;
    private int vw, vh;

    private record ClickRegion(int x, int y, int w, int h, Runnable action) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private final List<ClickRegion> clickRegions = new ArrayList<>();

    public WingFlightScreen() {
        super(Component.literal("Wing Flight Control"));
        ItemStack chest = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.CHEST);
        CompoundTag tag = chest.getOrCreateTag();
        this.flightMode = tag.contains("FlightMode") ? tag.getString("FlightMode") : "basic";
        this.flightSpeed = tag.contains("FlightSpeed") ? tag.getInt("FlightSpeed") : 5;
        this.flightDrift = tag.contains("FlightDrift") ? tag.getInt("FlightDrift") : 5;
        this.flightVertical = tag.contains("FlightVertical") ? tag.getInt("FlightVertical") : 5;
    }

    @Override
    protected void init() {
        float neededW = W + 40f;
        float neededH = H + 40f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);
    }

    private int getSliderMax(int kind) {
        return kind == SLIDER_VERTICAL ? 20 : STEPS;
    }

    private int getSliderValue(int kind) {
        return switch (kind) {
            case SLIDER_SPEED -> flightSpeed;
            case SLIDER_VERTICAL -> flightVertical;
            default -> flightDrift;
        };
    }

    private void setSliderValue(int kind, int value) {
        value = Math.max(0, Math.min(getSliderMax(kind), value));
        switch (kind) {
            case SLIDER_SPEED -> flightSpeed = value;
            case SLIDER_VERTICAL -> flightVertical = value;
            default -> flightDrift = value;
        }
        sendUpdate();
    }

    @Override
    public void render(GuiGraphics gfx, int rmx, int rmy, float partialTick) {
        renderBackground(gfx);

        int mouseX = Math.round(rmx / uiScale);
        int mouseY = Math.round(rmy / uiScale);

        clickRegions.clear();

        gfx.pose().pushPose();
        gfx.pose().scale(uiScale, uiScale, 1f);

        boolean isCreativeType = flightMode.startsWith("creative");
        boolean showSpeed = isCreativeType || flightMode.equals("powered");
        boolean showVertical = showSpeed;
        boolean showDrift = showSpeed;

        int currentH = 100;
        if (showSpeed) currentH = 150;
        if (showVertical) currentH = 195;
        if (showDrift) currentH = 240;

        int left = (vw - W) / 2;
        int top = (vh - H) / 2;

        drawPanel(gfx, left, top, W, currentH, PANEL_BG);

        gfx.drawString(font, "Wing Flight Control", left + 8, top + 7, COLOR_TITLE, false);

        int modeBtnY = top + 22;
        drawButton(gfx, mouseX, mouseY, left + 7, modeBtnY, W - 14, 20, "Mode: " + getModeDisplayName(),
                getModeColor());
        addClickRegion(left + 7, modeBtnY, W - 14, 20, () -> {
            cycleMode();
            sendUpdate();
        });

        int headerY = top + 46;
        gfx.fill(left + 7, headerY, left + W - 7, headerY + 46, DISPLAY_BG);
        gfx.drawString(font, "Current: " + getModeDisplayName(), left + 12, headerY + 5, getModeColor(), false);
        gfx.drawString(font, getDrainDescription(), left + 12, headerY + 17, COLOR_LABEL, false);
        gfx.drawString(font, "Press ESC to save", left + 12, headerY + 29, 0xFF777777, false);

        int rowY = top + 100;
        if (showSpeed) {
            gfx.drawString(font, "Flight Speed", left + 8, rowY - 10, COLOR_LABEL, false);
            renderSliderRow(gfx, mouseX, mouseY, left, rowY, SLIDER_SPEED);
            rowY += 45;
        }
        if (showVertical) {
            gfx.drawString(font, "Flight Vertical Speed", left + 8, rowY - 10, COLOR_LABEL, false);
            renderSliderRow(gfx, mouseX, mouseY, left, rowY, SLIDER_VERTICAL);
            rowY += 45;
        }
        if (showDrift) {
            gfx.drawString(font, "Flight Drift", left + 8, rowY - 10, COLOR_LABEL, false);
            renderSliderRow(gfx, mouseX, mouseY, left, rowY, SLIDER_DRIFT);
        }

        gfx.pose().popPose();
    }

    private void renderSliderRow(GuiGraphics gfx, int mouseX, int mouseY, int left, int y, int kind) {
        int max = getSliderMax(kind);
        int val = getSliderValue(kind);
        int barLeft = left + 27;
        int barWidth = W - 14 - 44;
        int segW = barWidth / max;

        drawButton(gfx, mouseX, mouseY, left + 7, y, 18, 18, "-", COLOR_LABEL);
        addClickRegion(left + 7, y, 18, 18, () -> setSliderValue(kind, getSliderValue(kind) - 1));

        drawButton(gfx, mouseX, mouseY, left + W - 25, y, 18, 18, "+", COLOR_LABEL);
        addClickRegion(left + W - 25, y, 18, 18, () -> setSliderValue(kind, getSliderValue(kind) + 1));

        for (int i = 0; i < max; i++) {
            int step = i + 1;
            int xPos = barLeft + (i * segW);
            int color = step == val ? COLOR_HIGHLIGHT : step < val ? COLOR_FILLED : COLOR_EMPTY;

            gfx.fill(xPos, y, xPos + segW - 2, y + 18, color);

            addClickRegion(xPos, y, segW - 2, 18, () -> setSliderValue(kind, step));
        }
    }

    /** Flat fill + a light bevel edge (top/left) and dark shadow edge (bottom/right) - a cheap
     *  "raised panel" look using only plain fills, no textures at all. */
    private void drawPanel(GuiGraphics gfx, int x, int y, int w, int h, int bgColor) {
        gfx.fill(x, y, x + w, y + h, bgColor);
        gfx.fill(x, y, x + w, y + 1, PANEL_BEVEL);
        gfx.fill(x, y, x + 1, y + h, PANEL_BEVEL);
        gfx.fill(x, y + h - 1, x + w, y + h, PANEL_SHADOW);
        gfx.fill(x + w - 1, y, x + w, y + h, PANEL_SHADOW);
    }

    private void drawButton(GuiGraphics gfx, int mouseX, int mouseY, int x, int y, int w, int h,
                            String label, int labelColor) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        drawPanel(gfx, x, y, w, h, hovered ? BUTTON_BG_HOVER : BUTTON_BG);
        gfx.fill(x, y, x + w, y + 1, BUTTON_BORDER);
        gfx.fill(x, y + h - 1, x + w, y + h, BUTTON_BORDER);
        gfx.fill(x, y, x + 1, y + h, BUTTON_BORDER);
        gfx.fill(x + w - 1, y, x + w, y + h, BUTTON_BORDER);
        gfx.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, labelColor);
    }

    private void addClickRegion(int x, int y, int w, int h, Runnable action) {
        clickRegions.add(new ClickRegion(x, y, w, h, action));
    }

    private void cycleMode() {
        this.flightMode = switch (this.flightMode) {
            case "basic" -> "powered";
            case "powered" -> "creative";
            case "creative" -> "creative+wings";
            default -> "basic";
        };
    }

    private String getModeDisplayName() {
        return switch (flightMode) {
            case "basic" -> "Vanilla Elytra";
            case "powered" -> "Powered Elytra";
            case "creative" -> "Creative";
            case "creative+wings" -> "Creative + Wings";
            default -> "Unknown";
        };
    }

    private String formatTeslaEnergy(java.math.BigInteger energy) {
        String[] units = new String[] { "", "K", "M", "G", "T", "P", "E" };
        java.math.BigDecimal display = new java.math.BigDecimal(energy);
        int unitIndex = 0;

        while (display.compareTo(new java.math.BigDecimal(1000)) >= 0 && unitIndex < units.length - 1) {
            display = display.divide(new java.math.BigDecimal(1000), 2, java.math.RoundingMode.HALF_UP);
            unitIndex++;
        }
        return String.format("%.2f %sEU", display.floatValue(), units[unitIndex]);
    }

    private String getDrainDescription() {
        var cfg = PhoenixConfigs.wingFlight;

        java.util.function.Function<Long, String> fmt = (val) -> formatTeslaEnergy(java.math.BigInteger.valueOf(val)) +
                " EU/t";

        return switch (flightMode) {
            case "basic" -> "No EU drain";

            case "powered" -> {
                long base = cfg.poweredFlightEUt;
                long actualDrain = base + (long) (base * (flightSpeed / 10.0));
                yield fmt.apply(actualDrain) + " - High EU Sonic Flight";
            }

            case "creative" -> {
                if (cfg.creativeFlightEUt <= 0) yield "FREE - Precision Hover";
                long actualDrain = (long) (cfg.creativeFlightEUt * (flightSpeed / 5.0));
                yield fmt.apply(actualDrain) + " - Precision Hover";
            }

            case "creative+wings" -> {
                if (cfg.creativeFlightEUt <= 0) yield "FREE - Hover & Glide";
                long actualDrain = (long) (cfg.creativeFlightEUt * (flightSpeed / 5.0));
                yield fmt.apply(actualDrain) + " - Hover & Glide";
            }

            default -> "N/A";
        };
    }

    private int getModeColor() {
        return switch (flightMode) {
            case "basic" -> COLOR_BASIC;
            case "powered" -> COLOR_POWERED;
            case "creative" -> COLOR_CREATIVE;
            case "creative+wings" -> COLOR_WINGED;
            default -> COLOR_LABEL;
        };
    }

    private void sendUpdate() {
        PhoenixNetwork.CHANNEL.sendToServer(
                new UpdateWingSettingsPacket(flightMode, flightSpeed, flightDrift, flightVertical));
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        if (button == 0) {
            double mx = rmx / uiScale;
            double my = rmy / uiScale;
            for (ClickRegion region : clickRegions) {
                if (region.contains(mx, my)) {
                    region.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(rmx / uiScale, rmy / uiScale, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
