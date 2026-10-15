package net.phoenix.core.integration.continuum.client.screen;


import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.common.MissionLog;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.network.C2SRequestHistoryPacket;
import net.phoenix.core.network.PhoenixNetwork;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

public class ContinuumHistoryScreen extends Screen {

    private static final int ROW = 14;

    private enum Filter {
        ALL("All"),
        LANDINGS("Landings"),
        PROBLEMS("Problems");

        final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    private final Screen parent;
    private Filter filter = Filter.ALL;
    private int scroll;
    private int refresh;

    public ContinuumHistoryScreen(Screen parent) {
        super(Component.literal("History"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        PacketDistributor.sendToServer(new C2SRequestHistoryPacket());
    }

    @Override
    public void tick() {
        super.tick();
        
        if (++refresh >= 100) {
            refresh = 0;
            PacketDistributor.sendToServer(new C2SRequestHistoryPacket());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int color(MissionLog.Kind kind) {
        return switch (kind) {
            case LAUNCH -> MapUi.TEXT;
            case LANDED, RESCUED, STATION, COMET -> MapUi.GOOD;
            case COLLECTED -> MapUi.DIM;
            case STRANDED -> MapUi.WARN;
            case FAILED, LOST -> MapUi.BAD;
        };
    }

    private static String tag(MissionLog.Kind kind) {
        return switch (kind) {
            case LAUNCH -> "LAUNCH";
            case LANDED -> "LANDED";
            case FAILED -> "FAILED";
            case STRANDED -> "STRANDED";
            case RESCUED -> "RESCUED";
            case LOST -> "LOST";
            case COLLECTED -> "COLLECTED";
            case STATION -> "STATION";
            case COMET -> "COMET";
        };
    }

    private List<MissionLog.Entry> visible() {
        List<MissionLog.Entry> all = ContinuumClientState.history();
        List<MissionLog.Entry> list = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            MissionLog.Entry entry = all.get(i);
            boolean keep = switch (filter) {
                case ALL -> true;
                case LANDINGS -> entry.kind() == MissionLog.Kind.LANDED || entry.kind() == MissionLog.Kind.RESCUED ||
                        entry.kind() == MissionLog.Kind.STATION || entry.kind() == MissionLog.Kind.COLLECTED;
                case PROBLEMS -> entry.kind().problem();
            };
            if (keep) list.add(entry);
        }
        return list;
    }

    private static String ago(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        if (seconds < 90) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 90) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 48) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    private int filterX(int index) {
        return 10 + index * 78;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        MapUi.refreshTheme();
        graphics.fill(0, 0, width, height, MapUi.SCREEN_BG);
        graphics.renderOutline(1, 1, width - 2, height - 2, MapUi.FRAME);
        graphics.drawString(font, "HISTORY", 10, 8, MapUi.TITLE);
        graphics.drawString(font, "Every launch, landing and rescue, newest first", 10, 19, MapUi.DIM);

        graphics.fill(width - 62, 8, width - 10, 22, MapUi.CHROME);
        graphics.renderOutline(width - 62, 8, 52, 14, MapUi.PANEL_LINE);
        graphics.drawCenteredString(font, "< Back", width - 36, 11, MapUi.TEXT);

        Filter[] filters = Filter.values();
        for (int i = 0; i < filters.length; i++) {
            boolean on = filters[i] == filter;
            int x = filterX(i);
            graphics.fill(x, 32, x + 72, 46, on ? MapUi.BUTTON_ON : MapUi.BUTTON);
            graphics.renderOutline(x, 32, 72, 14, on ? MapUi.FRAME : MapUi.PANEL_LINE);
            graphics.drawCenteredString(font, filters[i].label, x + 36, 35, on ? MapUi.TITLE : MapUi.TEXT);
        }

        int top = 54;
        MapUi.panel(graphics, 10, top, width - 20, height - top - 10);
        List<MissionLog.Entry> list = visible();
        if (list.isEmpty()) {
            graphics.drawString(font, "Nothing recorded yet.", 20, top + 8, MapUi.DIM);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int rows = Math.max(1, (height - top - 16) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - rows)));
        long now = ContinuumClientState.now();
        MissionLog.Entry hovered = null;
        graphics.enableScissor(11, top + 1, width - 11, height - 11);
        for (int i = 0; i < rows && scroll + i < list.size(); i++) {
            MissionLog.Entry entry = list.get(scroll + i);
            int y = top + 6 + i * ROW;
            boolean hover = MapUi.inside(mouseX, mouseY, 12, y - 2, width - 24, ROW);
            if (hover) {
                graphics.fill(12, y - 2, width - 12, y - 2 + ROW, 0x33ffffff);
                hovered = entry;
            }
            String when = ago(now - entry.time());
            graphics.drawString(font, when, 18, y, MapUi.DIM);
            graphics.drawString(font, tag(entry.kind()), 18 + 58, y, color(entry.kind()));
            ContinuumBody body = ContinuumData.body(entry.body());
            int textX = 18 + 58 + 66;
            String text = font.plainSubstrByWidth(entry.text(), Math.max(20, width - 24 - textX));
            graphics.drawString(font, text, textX, y, MapUi.TEXT);
        }
        graphics.disableScissor();

        if (hovered != null && font.width(hovered.text()) > width - 24 - (18 + 58 + 66)) {
            graphics.renderTooltip(font, font.split(Component.literal(hovered.text()), Math.min(320, width - 40)),
                    mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            onClose();
            return true;
        }
        if (button == 0) {
            if (MapUi.inside(mouseX, mouseY, width - 62, 8, 52, 14)) {
                ContinuumSounds.click();
                onClose();
                return true;
            }
            Filter[] filters = Filter.values();
            for (int i = 0; i < filters.length; i++) {
                if (MapUi.inside(mouseX, mouseY, filterX(i), 32, 72, 14)) {
                    ContinuumSounds.click();
                    filter = filters[i];
                    scroll = 0;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta) * 3);
        return true;
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
