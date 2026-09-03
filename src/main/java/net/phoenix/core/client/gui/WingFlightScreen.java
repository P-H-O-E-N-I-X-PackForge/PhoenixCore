package net.phoenix.core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
import net.phoenixvine.wiki.theme.PhoenixTheme;

@OnlyIn(Dist.CLIENT)
public class WingFlightScreen extends Screen {

    private String flightMode;
    private int flightSpeed;
    private int flightDrift;
    private int flightVertical;
    private int walkSpeed;

    private static final int W = 200;
    private static final int H = 270;
    private static final int STEPS = 10;

    private static final int SLIDER_SPEED = 0;
    private static final int SLIDER_DRIFT = 1;
    private static final int SLIDER_VERTICAL = 2;

    private int cTitle, cLabel, cBasic, cPowered, cCreative, cWinged, cFilled, cEmpty, cPanelBg, cBorder;

    private float uiScale = 1f;
    private int vw, vh;

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

        refreshTheme();

        int left = (vw - W) / 2;
        int top = (vh - H) / 2;
        this.clearWidgets();

        addRenderableWidget(Button.builder(Component.literal("Mode: " + getModeDisplayName()), btn -> {
            cycleMode();
            sendUpdate();
            rebuildWidgets();
        }).bounds(left + 5, top + 72, W - 10, 20).build());

        boolean isCreativeType = flightMode.startsWith("creative");
        boolean showSpeed = isCreativeType || flightMode.equals("powered");
        boolean showVertical = showSpeed;
        boolean showDrift = isCreativeType;

        if (showSpeed) {
            createSliderRow(left, top + 118, SLIDER_SPEED);
        }

        if (showVertical) {
            createSliderRow(left, top + 168, SLIDER_VERTICAL);
        }

        if (showDrift) {
            createSliderRow(left, top + 218, SLIDER_DRIFT);
        }
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cTitle = t.accent.getColor();
        cLabel = t.textDim.getColor();
        cBasic = t.done.getColor();
        cPowered = t.activeColor.getColor();
        cCreative = t.ally.getColor();
        cWinged = t.locked.getColor();
        cFilled = t.accent.getColor();
        cEmpty = (t.border.getColor() & 0x00FFFFFF) | 0xFF000000;
        cPanelBg = (t.panel.getColor() & 0x00FFFFFF) | 0xEE000000;
        cBorder = (t.accent.getColor() & 0x00FFFFFF) | 0x66000000;
    }

    private void createSliderRow(int left, int y, int kind) {
        int barLeft = left + 27;
        int barWidth = W - 10 - 44;
        int segW = barWidth / STEPS;

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            setSliderValue(kind, getSliderValue(kind) - 1);
            sendUpdate();
        }).bounds(left + 5, y, 18, 18).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            setSliderValue(kind, getSliderValue(kind) + 1);
            sendUpdate();
        }).bounds(left + W - 23, y, 18, 18).build());

        for (int i = 0; i < STEPS; i++) {
            final int seg = i + 1;
            addRenderableWidget(Button.builder(Component.empty(), btn -> {
                setSliderValue(kind, seg);
                sendUpdate();
            }).bounds(barLeft + (i * segW), y, segW - 1, 18).build());
        }
    }

    private int getSliderValue(int kind) {
        return switch (kind) {
            case SLIDER_SPEED -> flightSpeed;
            case SLIDER_VERTICAL -> flightVertical;
            default -> flightDrift;
        };
    }

    private void setSliderValue(int kind, int value) {
        value = Math.max(1, Math.min(STEPS, value));
        switch (kind) {
            case SLIDER_SPEED -> flightSpeed = value;
            case SLIDER_VERTICAL -> flightVertical = value;
            default -> flightDrift = value;
        }
    }

    @Override
    public void render(GuiGraphics gfx, int rmx, int rmy, float partialTick) {
        renderBackground(gfx);
        refreshTheme();

        int mouseX = Math.round(rmx / uiScale);
        int mouseY = Math.round(rmy / uiScale);

        gfx.pose().pushPose();
        gfx.pose().scale(uiScale, uiScale, 1f);

        boolean isCreativeType = flightMode.startsWith("creative");
        boolean showSpeed = isCreativeType || flightMode.equals("powered");
        boolean showVertical = showSpeed;
        boolean showDrift = isCreativeType;

        int currentH = 100;
        if (showSpeed) currentH = 150;
        if (showVertical) currentH = 200;
        if (showDrift) currentH = 255;

        int left = (vw - W) / 2;
        int top = (vh - H) / 2;

        gfx.fill(left, top, left + W, top + currentH, cPanelBg);
        renderBorders(gfx, left, top, W, currentH);

        gfx.drawString(font, "Wing Flight Control", left + 8, top + 6, cTitle, false);

        gfx.fill(left + 5, top + 18, left + W - 5, top + 66, 0x55000000);
        gfx.drawString(font, "Current: " + getModeDisplayName(), left + 10, top + 24, getModeColor(), false);
        gfx.drawString(font, getDrainDescription(), left + 10, top + 36, cLabel, false);
        gfx.drawString(font, "Press ESC to save", left + 10, top + 48, 0x77AAAAAA, false);

        if (showSpeed) {
            gfx.drawString(font, "Flight Speed", left + 8, top + 104, cLabel, false);
            renderSegmentedBar(gfx, left + 27, top + 118, flightSpeed);
        }

        if (showVertical) {
            gfx.drawString(font, "Flight Vertical Speed", left + 8, top + 154, cLabel, false);
            renderSegmentedBar(gfx, left + 27, top + 168, flightVertical);
        }

        if (showDrift) {
            gfx.drawString(font, "Flight Drift", left + 8, top + 204, cLabel, false);
            renderSegmentedBar(gfx, left + 27, top + 218, flightDrift);
        }

        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.pose().popPose();
    }

    private void renderSegmentedBar(GuiGraphics gfx, int x, int y, int val) {
        int barWidth = W - 10 - 44;
        int segW = barWidth / STEPS;

        for (int i = 0; i < STEPS; i++) {
            int step = i + 1;
            int xPos = x + (i * segW);
            int color = step <= val ? cFilled : cEmpty;

            gfx.fill(xPos, y, xPos + segW - 2, y + 18, color);

            if (step == val) {
                String s = String.valueOf(step);
                gfx.drawCenteredString(font, s, xPos + (segW / 2), y + 5, 0xFFFFFF);
            }
        }
    }

    private void renderBorders(GuiGraphics gfx, int left, int top, int w, int h) {
        gfx.fill(left, top, left + w, top + 1, cBorder);
        gfx.fill(left, top + h - 1, left + w, top + h, cBorder);
        gfx.fill(left, top, left + 1, top + h, cBorder);
        gfx.fill(left + w - 1, top, left + w, top + h, cBorder);
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
                long actualDrain = base + (long) (base * ((flightSpeed - 1) / 9.0));
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
            case "basic" -> cBasic;
            case "powered" -> cPowered;
            case "creative" -> cCreative;
            case "creative+wings" -> cWinged;
            default -> cLabel;
        };
    }

    private void sendUpdate() {
        PhoenixNetwork.CHANNEL.sendToServer(
                new UpdateWingSettingsPacket(flightMode, flightSpeed, flightDrift, flightVertical));
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        return super.mouseClicked(rmx / uiScale, rmy / uiScale, button);
    }

    @Override
    public boolean mouseDragged(double rmx, double rmy, int button, double dragX, double dragY) {
        return super.mouseDragged(rmx / uiScale, rmy / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    @Override
    public boolean mouseReleased(double rmx, double rmy, int button) {
        return super.mouseReleased(rmx / uiScale, rmy / uiScale, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
