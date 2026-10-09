package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.ContinuumSystem;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ContinuumArchiveScreen extends Screen {

    private final Screen parent;
    private final List<Object[]> rows = new ArrayList<>();
    private @Nullable ContinuumBody selected;
    private int scroll;

    public ContinuumArchiveScreen(Screen parent) {
        super(Component.literal("Archive"));
        this.parent = parent;
    }

    private int listW() {
        return Math.min(190, width / 3);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF07061a);
        graphics.renderOutline(1, 1, width - 2, height - 2, MapUi.FRAME);
        graphics.drawString(font, "ARCHIVE", 10, 8, MapUi.TITLE);
        graphics.drawString(font, "Entries are written as bodies are detected and surveyed", 10, 19, MapUi.DIM);

        graphics.fill(width - 62, 8, width - 10, 22, 0xAA1a1830);
        graphics.renderOutline(width - 62, 8, 52, 14, MapUi.PANEL_LINE);
        graphics.drawCenteredString(font, "< Back", width - 36, 11, MapUi.TEXT);

        drawList(graphics, mouseX, mouseY);
        drawEntry(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawList(GuiGraphics graphics, int mouseX, int mouseY) {
        rows.clear();
        int x = 10;
        int top = 36;
        int w = listW();
        MapUi.panel(graphics, x, top, w, height - top - 10);

        int y = top + 6 - scroll;
        int known = 0;
        for (ContinuumSystem system : ContinuumData.systems()) {
            List<ContinuumBody> bodies = new ArrayList<>();
            for (ContinuumBody body : ContinuumData.bodiesOf(system.id())) {
                if (ContinuumClientState.stage(body.id()) != DiscoveryStage.UNKNOWN && hasLore(body)) bodies.add(body);
            }
            if (bodies.isEmpty()) continue;

            if (y > top && y < height - 22) graphics.drawString(font, system.name(), x + 6, y, MapUi.TITLE);
            y += 12;
            for (ContinuumBody body : bodies) {
                known++;
                if (y > top && y < height - 22) {
                    boolean hover = MapUi.inside(mouseX, mouseY, x + 3, y - 2, w - 6, 12);
                    boolean active = body == selected;
                    if (active || hover) {
                        graphics.fill(x + 3, y - 2, x + w - 3, y + 10, active ? 0x557a5cff : 0x33ffffff);
                    }
                    DiscoveryStage stage = ContinuumClientState.stage(body.id());
                    graphics.drawString(font, "  " + body.name(), x + 6, y,
                            stage == DiscoveryStage.SURVEYED ? MapUi.TEXT : MapUi.DIM);
                    rows.add(new Object[] { body, y - 2 });
                }
                y += 12;
            }
            y += 4;
        }
        if (known == 0) {
            graphics.drawString(font, "Nothing recorded yet.", x + 6, top + 8, MapUi.DIM);
        }
    }

    private static boolean hasLore(ContinuumBody body) {
        return !body.loreDetected().isEmpty() || !body.loreSurveyed().isEmpty();
    }

    private void drawEntry(GuiGraphics graphics) {
        int x = 10 + listW() + 10;
        int top = 36;
        int w = width - x - 10;
        MapUi.panel(graphics, x, top, w, height - top - 10);
        if (selected == null) {
            graphics.drawString(font, "Select a body.", x + 8, top + 8, MapUi.DIM);
            return;
        }

        DiscoveryStage stage = ContinuumClientState.stage(selected.id());
        graphics.drawString(font, selected.name(), x + 8, top + 8, MapUi.TITLE);
        graphics.drawString(font, selected.type().label() + "  -  " + stage.label(), x + 8, top + 20, MapUi.DIM);

        int y = top + 38;
        y = paragraph(graphics, "DETECTED", selected.loreDetected(), x + 8, y, w - 16, MapUi.WARN);
        if (stage == DiscoveryStage.SURVEYED) {
            y += 8;
            paragraph(graphics, "SURVEYED", selected.loreSurveyed(), x + 8, y, w - 16, MapUi.GOOD);
        } else {
            graphics.drawString(font, "Survey this body to write the next entry.", x + 8, y + 10, MapUi.DIM);
        }
    }

    private int paragraph(GuiGraphics graphics, String heading, String text, int x, int y, int w, int headingColor) {
        if (text.isEmpty()) return y;
        graphics.drawString(font, heading, x, y, headingColor);
        y += 12;
        for (FormattedCharSequence line : font.split(FormattedText.of(text), w)) {
            graphics.drawString(font, line, x, y, MapUi.TEXT);
            y += 11;
        }
        return y;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MapUi.inside(mouseX, mouseY, width - 62, 8, 52, 14)) {
                ContinuumSounds.click();
                onClose();
                return true;
            }
            for (Object[] row : rows) {
                if (MapUi.inside(mouseX, mouseY, 13, (int) row[1], listW() - 6, 12)) {
                    selected = (ContinuumBody) row[0];
                    ContinuumSounds.click();
                    return true;
                }
            }
        } else if (button == 1) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, scroll - (int) (delta * 14));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_BACKSPACE) {
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
