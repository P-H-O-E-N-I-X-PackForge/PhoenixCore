package net.phoenix.core.client.gui.cinder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.phoenix.core.common.item.cinder.CinderAtlasData;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.common.item.cinder.CinderAtlasUpgrades;
import net.phoenix.core.common.item.cinder.CinderSchemaData;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCinderAtlasSetActiveLoadoutPacket;
import net.phoenix.core.network.packet.C2SCinderAtlasSetActiveSlotPacket;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Design doc feature #6 - radial quick-select for swapping the Cinder Atlas's active loadout/slot
 * without opening the full {@link CinderAtlasScreen} editor. Structurally a straight copy of
 * {@code PhoenixRadialMenu}'s donut/arc drawing approach (labeled pie segments via a raw
 * {@link Tesselator} triangle strip, hover-highlighted, picked by angle+distance in
 * {@link #mouseClicked}) rather than {@code ColorRadialMenuScreen}'s item-icon segments, since a
 * loadout/slot is identified by name, not a renderable stack.
 * <p>
 * Two levels sharing the same donut machinery, switched via {@link #viewingLoadout}: with it {@code null}
 * the ring shows loadouts - picking one doesn't commit anything yet, it drills into a second ring showing
 * that loadout's {@value CinderAtlasData#SLOT_COUNT} pattern slots (target name or "empty"); picking a
 * slot there is what actually sets the active loadout+slot and closes. Clicking the center hub while
 * viewing slots backs out to the loadout ring instead of closing.
 * <p>
 * Carries the same phoenixwiki flourishes {@link CinderAtlasScreen} was themed with (ported from that
 * library's {@code PhoenixThemeEditorScreen}, its one screen with real motion): a pulsing glow arc behind
 * the active segment, and a small spark continuously orbiting the ring, both via the same
 * {@code animPulse} technique. Respects {@link PhoenixTheme#isReduceMotion()}.
 */
public class CinderAtlasRadialScreen extends Screen {

    private static final int RADIUS = 90;
    private static final int INNER_RADIUS = 30;

    private final InteractionHand hand;
    private ItemStack atlasStack = ItemStack.EMPTY;
    private int activeLoadout = 0;
    private int activeSlot = 0;

    /** {@code null} = showing the loadout ring; otherwise the loadout whose slots are currently shown. */
    private @Nullable Integer viewingLoadout = null;

    private int cAccent, cText, cPanel, cCore, cSelected;

    private float uiScale = 1f;
    private int vw, vh;

    public CinderAtlasRadialScreen(InteractionHand hand) {
        super(Component.literal("Cinder Atlas Loadouts"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        int neededSide = (RADIUS + 30) * 2;
        uiScale = (width < neededSide || height < neededSide) ?
                Math.min((float) width / neededSide, (float) height / neededSide) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        refreshTheme();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        atlasStack = player.getItemInHand(hand);
        if (atlasStack.getItem() instanceof CinderAtlasItem) {
            activeLoadout = CinderAtlasData.getActiveLoadout(atlasStack);
            activeSlot = CinderAtlasData.getActiveSlot(atlasStack);
        }
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cAccent = t.accent.getColor();
        cText = t.text.getColor();
        cPanel = (t.panel.getColor() & 0x00FFFFFF) | 0xCC000000;
        cCore = t.border.getColor();
        cSelected = (t.accent.getColor() & 0x00FFFFFF) | 0x66000000;
    }

    private int segmentCount() {
        return viewingLoadout == null ? CinderAtlasUpgrades.getLoadoutCapacity(atlasStack) :
                CinderAtlasData.SLOT_COUNT;
    }

    @Override
    public void render(GuiGraphics graphics, int rmx, int rmy, float partialTick) {
        if (!(atlasStack.getItem() instanceof CinderAtlasItem)) {
            onClose();
            return;
        }

        graphics.fill(0, 0, this.width, this.height, 0x55000000);
        refreshTheme();

        int mouseX = Math.round(rmx / uiScale);
        int mouseY = Math.round(rmy / uiScale);

        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1f);

        int centerX = vw / 2;
        int centerY = vh / 2;
        int count = segmentCount();
        float angleStep = 360.0f / count;

        drawDonut(graphics, centerX, centerY, INNER_RADIUS, RADIUS, cPanel);
        drawOrbitingSpark(graphics, centerX, centerY, INNER_RADIUS, RADIUS);

        Integer hoveredIndex = null;
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(i * angleStep - 90);
            double stepRad = Math.toRadians(angleStep);

            boolean active = viewingLoadout == null ? i == activeLoadout :
                    (viewingLoadout == activeLoadout && i == activeSlot);
            boolean hovered = isMouseInSector(mouseX, mouseY, centerX, centerY, angle, stepRad);
            if (hovered) hoveredIndex = i;

            if (active) {
                drawGlowArc(graphics, centerX, centerY, INNER_RADIUS, RADIUS, (float) angle, (float) stepRad,
                        cAccent);
                drawArc(graphics, centerX, centerY, INNER_RADIUS, RADIUS, (float) angle, (float) stepRad, cSelected);
            }
            if (hovered) {
                drawArc(graphics, centerX, centerY, INNER_RADIUS, RADIUS, (float) angle, (float) stepRad,
                        (cAccent & 0x00FFFFFF) | 0x44000000);
            }

            float textRadius = (INNER_RADIUS + RADIUS) / 2.0f;
            int textX = centerX + (int) (Math.cos(angle) * textRadius);
            int textY = centerY + (int) (Math.sin(angle) * textRadius);
            int color = hovered ? cAccent : (active ? ChatFormatting.GOLD.getColor() : cText);

            if (viewingLoadout == null) {
                graphics.drawCenteredString(this.font, CinderAtlasData.getLoadoutName(atlasStack, i), textX,
                        textY - 4, color);
            } else {
                ItemStack icon = slotIcon(viewingLoadout, i);
                if (!icon.isEmpty()) {
                    graphics.renderItem(icon, textX - 8, textY - 8);
                } else {
                    graphics.drawCenteredString(this.font, "empty", textX, textY - 4, color);
                }
            }
        }

        graphics.fill(centerX - INNER_RADIUS + 2, centerY - INNER_RADIUS + 2, centerX + INNER_RADIUS - 2,
                centerY + INNER_RADIUS - 2, cCore);
        if (viewingLoadout == null) {
            graphics.drawCenteredString(this.font, "ATLAS", centerX, centerY - 4, cText);
        } else {
            graphics.drawCenteredString(this.font,
                    truncate(CinderAtlasData.getLoadoutName(atlasStack, viewingLoadout), INNER_RADIUS * 2 - 6),
                    centerX, centerY - 9, cText);
            graphics.drawCenteredString(this.font, "§7< back", centerX, centerY + 1, cText);
        }

        if (hoveredIndex != null) {
            graphics.renderComponentTooltip(this.font, hoverTooltip(hoveredIndex), mouseX, mouseY);
        }

        graphics.pose().popPose();
    }

    private List<Component> hoverTooltip(int index) {
        List<Component> tooltip = new ArrayList<>();
        if (viewingLoadout == null) {
            int configured = 0;
            for (int slot = 0; slot < CinderAtlasData.SLOT_COUNT; slot++) {
                if (CinderAtlasData.isSlotConfigured(atlasStack, index, slot)) configured++;
            }
            tooltip.add(Component.literal(CinderAtlasData.getLoadoutName(atlasStack, index))
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(configured + "/" + CinderAtlasData.SLOT_COUNT + " slots configured")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            boolean configured = CinderAtlasData.isSlotConfigured(atlasStack, viewingLoadout, index);
            tooltip.add(Component.literal(slotLabel(viewingLoadout, index)).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(configured ? "Click to deploy this" : "Not configured yet")
                    .withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }

    private String slotLabel(int loadout, int slot) {
        CompoundTag tag = CinderAtlasData.peekSlotTag(atlasStack, loadout, slot);
        var definition = tag != null ? CinderSchemaData.getTargetDefinition(tag) : null;
        return definition != null ? definition.getBlock().getName().getString() : "Slot " + (slot + 1);
    }

    /** The controller block's own item form, shown in place of a text label for a configured slot -
     *  a slot's target is a real, renderable block, unlike a loadout (a bucket of several slots). */
    private ItemStack slotIcon(int loadout, int slot) {
        CompoundTag tag = CinderAtlasData.peekSlotTag(atlasStack, loadout, slot);
        var definition = tag != null ? CinderSchemaData.getTargetDefinition(tag) : null;
        return definition != null ? new ItemStack(definition.getBlock()) : ItemStack.EMPTY;
    }

    private String truncate(String s, int maxPx) {
        if (s == null) return "";
        while (font.width(s) > maxPx && s.length() > 2) {
            s = s.substring(0, s.length() - 2) + "…";
        }
        return s;
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

    /** Soft pulsing halo behind the active segment, same {@code animPulse} technique
     *  {@link CinderAtlasScreen#drawGlowBorder} uses for its active tab/slot, adapted to an arc. */
    private void drawGlowArc(GuiGraphics graphics, int cx, int cy, int inner, int outer, float startAngle,
                             float step, int rgb) {
        float pulse = animPulse(0.6f, 0.4f, 500.0);
        int glowA = Math.min(255, (int) (0x55 * pulse));
        drawArc(graphics, cx, cy, inner - 3, outer + 3, startAngle, step, (glowA << 24) | (rgb & 0xFFFFFF));
    }

    /** Ambient "energy flowing" touch - a small spark continuously orbiting the ring, ported from the
     *  same traveling-spark technique {@link CinderAtlasScreen#renderTitleUnderline} uses along its
     *  title's underline, adapted to a circular path instead of a straight one. */
    private void drawOrbitingSpark(GuiGraphics graphics, int cx, int cy, int inner, int outer) {
        if (PhoenixTheme.isReduceMotion()) return;
        double sparkAngle = (System.currentTimeMillis() / 1800.0) % (Math.PI * 2);
        float sparkPulse = animPulse(0.75f, 0.25f, 200.0);
        int sparkA = Math.min(255, (int) (0xFF * sparkPulse));
        float radius = (inner + outer) / 2f;
        int sx = cx + (int) (Math.cos(sparkAngle) * radius);
        int sy = cy + (int) (Math.sin(sparkAngle) * radius);
        graphics.fill(sx - 2, sy - 2, sx + 2, sy + 2, (sparkA << 24) | (cAccent & 0xFFFFFF));
    }

    private static float animPulse(float base, float amplitude, double periodDivisor) {
        if (PhoenixTheme.isReduceMotion()) return base;
        return base + amplitude * (float) Math.sin(System.currentTimeMillis() / periodDivisor);
    }

    private boolean isMouseInSector(int mx, int my, int cx, int cy, double angle, double step) {
        double dist = Math.sqrt(Math.pow(mx - cx, 2) + Math.pow(my - cy, 2));
        if (dist < INNER_RADIUS || dist > RADIUS) return false;
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

        double distToCenter = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        if (distToCenter < INNER_RADIUS) {
            if (viewingLoadout != null) {
                viewingLoadout = null;
                playClick(0.8F);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int count = segmentCount();
        double angleStep = Math.PI * 2 / count;
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2 + (i * angleStep);
            if (isMouseInSector((int) mouseX, (int) mouseY, centerX, centerY, angle, angleStep)) {
                if (viewingLoadout == null) {
                    viewingLoadout = i;
                    playClick(0.8F);
                } else {
                    confirmSlot(i);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Only reached from the slot-level ring - committing a slot pick from the loadout ring (with no
     *  slot chosen yet) would leave the deploy target ambiguous, so the loadout ring only ever drills
     *  into the slot ring instead of setting anything itself. */
    private void confirmSlot(int slotIndex) {
        if (viewingLoadout == null) return;
        int loadout = viewingLoadout;

        if (loadout != activeLoadout) {
            activeLoadout = loadout;
            CinderAtlasData.setActiveLoadout(atlasStack, loadout);
            PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderAtlasSetActiveLoadoutPacket(hand, loadout));
        }
        activeSlot = slotIndex;
        CinderAtlasData.setActiveSlot(atlasStack, slotIndex);
        PhoenixNetwork.CHANNEL.sendToServer(new C2SCinderAtlasSetActiveSlotPacket(hand, slotIndex));

        playClick(1.0F);
        onClose();
    }

    private void playClick(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
