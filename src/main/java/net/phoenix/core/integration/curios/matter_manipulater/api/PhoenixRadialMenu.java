package net.phoenix.core.integration.matter_manipulater.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.PacketPhoenixModeSync;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class PhoenixRadialMenu extends Screen {

    private final int radius = 100;
    private final int innerRadius = 35;

    private int cAccent, cText, cPanel, cCore;

    private float uiScale = 1f;
    private int vw, vh;

    public PhoenixRadialMenu() {
        super(Component.literal("Phoenix Manipulator Modes"));
    }

    @Override
    protected void init() {
        int neededSide = (radius + 30) * 2;
        uiScale = (width < neededSide || height < neededSide) ?
                Math.min((float) width / neededSide, (float) height / neededSide) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        refreshTheme();
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cAccent = t.accent.getColor();
        cText = t.text.getColor();
        cPanel = (t.panel.getColor() & 0x00FFFFFF) | 0xCC000000;
        cCore = t.border.getColor();
    }

    @Override
    public void render(GuiGraphics graphics, int rmx, int rmy, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x55000000);
        refreshTheme();

        int mouseX = Math.round(rmx / uiScale);
        int mouseY = Math.round(rmy / uiScale);

        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1f);

        int centerX = vw / 2;
        int centerY = vh / 2;
        PhoenixManipulatorMode[] modes = PhoenixManipulatorMode.values();
        float angleStep = 360.0f / modes.length;

        drawDonut(graphics, centerX, centerY, innerRadius, radius, cPanel);

        PhoenixManipulatorMode hoveredMode = null;
        for (int i = 0; i < modes.length; i++) {
            double angle = Math.toRadians(i * angleStep - 90);
            double stepRad = Math.toRadians(angleStep);

            boolean hovered = isMouseInSector(mouseX, mouseY, centerX, centerY, angle, stepRad);
            if (hovered) {
                hoveredMode = modes[i];

                drawArc(graphics, centerX, centerY, innerRadius, radius, (float) angle, (float) stepRad,
                        (cAccent & 0x00FFFFFF) | 0x66000000);
            }

            float textRadius = (innerRadius + radius) / 2.0f;
            int textX = centerX + (int) (Math.cos(angle) * textRadius);
            int textY = centerY + (int) (Math.sin(angle) * textRadius);

            int color = hovered ? cAccent : cText;
            graphics.drawCenteredString(this.font, modes[i].getName(), textX, textY - 4, color);
        }

        graphics.fill(centerX - innerRadius + 2, centerY - innerRadius + 2, centerX + innerRadius - 2,
                centerY + innerRadius - 2, cCore);
        graphics.drawCenteredString(this.font, "CORE", centerX, centerY - 4, cText);

        if (hoveredMode != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(hoveredMode.getName()).withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.literal(getModeDescription(hoveredMode)).withStyle(net.minecraft.ChatFormatting.GRAY));
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        graphics.pose().popPose();
    }

    private String getModeDescription(PhoenixManipulatorMode mode) {
        return switch (mode) {
            case LINE -> "Places pipes in a single axis line.";
            case WALL -> "Creates a 2D plane of pipes.";
            case GRID -> "Fills the entire 3D selection.";
            case CONNECT_ONLY -> "Forces connections without placing blocks.";
            case DISCONNECT -> "Severs all connections in the area.";
            default -> "Blocks manipulation mode.";
        };
    }

    private void drawDonut(GuiGraphics graphics, int cx, int cy, int inner, int outer, int color) {
        RenderSystem.enableBlend();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        for (int i = 0; i <= 360; i += 5) {
            float rad = (float) Math.toRadians(i);
            buffer.vertex(matrix, cx + Mth.cos(rad) * outer, cy + Mth.sin(rad) * outer, 0).color(color).endVertex();
            buffer.vertex(matrix, cx + Mth.cos(rad) * inner, cy + Mth.sin(rad) * inner, 0).color(color).endVertex();
        }
        tesselator.end();
    }

    private void drawArc(GuiGraphics graphics, int cx, int cy, int inner, int outer, float startAngle, float step,
                         int color) {
        RenderSystem.enableBlend();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        for (float a = startAngle - step / 2; a <= startAngle + step / 2; a += 0.05f) {
            buffer.vertex(matrix, cx + Mth.cos(a) * outer, cy + Mth.sin(a) * outer, 0).color(color).endVertex();
            buffer.vertex(matrix, cx + Mth.cos(a) * inner, cy + Mth.sin(a) * inner, 0).color(color).endVertex();
        }
        tesselator.end();
    }

    private boolean isMouseInSector(int mx, int my, int cx, int cy, double angle, double step) {
        double dist = Math.sqrt(Math.pow(mx - cx, 2) + Math.pow(my - cy, 2));
        if (dist < innerRadius || dist > radius) return false;
        double mouseAngle = Math.atan2(my - cy, mx - cx);
        double diff = mouseAngle - angle;
        while (diff < -Math.PI) diff += Math.PI * 2;
        while (diff > Math.PI) diff -= Math.PI * 2;
        return Math.abs(diff) < step / 2;
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        double mouseX = rmx / uiScale;
        double mouseY = rmy / uiScale;
        int centerX = vw / 2;
        int centerY = vh / 2;
        PhoenixManipulatorMode[] modes = PhoenixManipulatorMode.values();
        double angleStep = Math.PI * 2 / modes.length;

        for (int i = 0; i < modes.length; i++) {
            double angle = -Math.PI / 2 + (i * angleStep);
            if (isMouseInSector((int) mouseX, (int) mouseY, centerX, centerY, angle, angleStep)) {
                PhoenixNetwork.CHANNEL.sendToServer(new PacketPhoenixModeSync(i));
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
