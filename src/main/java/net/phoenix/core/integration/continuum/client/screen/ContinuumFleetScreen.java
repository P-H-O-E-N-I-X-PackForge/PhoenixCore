package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.common.ContinuumMissions;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.integration.continuum.common.Stations;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.network.C2SCollectAllPacket;
import net.phoenix.core.integration.continuum.network.C2SCollectMissionPacket;
import net.phoenix.core.network.PhoenixNetwork;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContinuumFleetScreen extends Screen {

    private static final int ROW = 30;

    private final Screen parent;
    private final @Nullable BlockPos pad;
    private final List<Object[]> rows = new ArrayList<>();
    private int scroll;

    public ContinuumFleetScreen(Screen parent, @Nullable BlockPos pad) {
        super(Component.literal("Fleet"));
        this.parent = parent;
        this.pad = pad;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int listW() {
        return Math.max(220, width - 240);
    }

    private static String bodyName(ResourceLocation id) {
        ContinuumBody body = ContinuumData.body(id);
        return body != null ? body.name() : id.getPath();
    }

    private static String label(ContinuumStateSnapshot.MissionView mission) {
        String name = bodyName(mission.destination());
        return switch (mission.type()) {
            case SURVEY -> "Survey  -  " + name;
            case EXTRACT -> "Extraction x" + mission.probes() + "  -  " + name;
            case DEPLOY -> "Outpost x" + mission.probes() + "  -  " + name;
            case HAUL -> "Haul  -  " + name;
            case REPAIR -> "Repair  -  " + name;
            case RESCUE -> "Rescue  -  " + name;
            case STATION -> "Station  -  " + name;
        };
    }

    private List<ContinuumStateSnapshot.MissionView> sorted() {
        List<ContinuumStateSnapshot.MissionView> list = new ArrayList<>(ContinuumClientState.missions());

        list.sort(Comparator.comparing((ContinuumStateSnapshot.MissionView m) -> !m.stranded())
                .thenComparing(m -> !m.state().finished())
                .thenComparing(ContinuumStateSnapshot.MissionView::endMillis));
        return list;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF07061a);
        graphics.renderOutline(1, 1, width - 2, height - 2, MapUi.FRAME);
        graphics.drawString(font, "FLEET", 10, 8, MapUi.TITLE);

        List<ContinuumStateSnapshot.MissionView> list = sorted();
        long now = ContinuumClientState.now();
        int flying = 0;
        int landed = 0;
        int stranded = 0;
        for (var mission : list) {
            if (mission.stranded()) stranded++;
            else if (mission.state().finished()) landed++;
            else flying++;
        }
        graphics.drawString(font,
                list.size() + " of " + ContinuumClientState.missionSlots() + " slots   -   " + flying +
                        " in flight, " + landed + " landed, " + stranded + " stranded",
                10, 19, MapUi.DIM);

        graphics.fill(width - 62, 8, width - 10, 22, 0xAA1a1830);
        graphics.renderOutline(width - 62, 8, 52, 14, MapUi.PANEL_LINE);
        graphics.drawCenteredString(font, "< Back", width - 36, 11, MapUi.TEXT);

        boolean anyLanded = landed > 0;
        int allX = width - 62 - 96;
        graphics.fill(allX, 8, allX + 90, 22, anyLanded ? 0xAA2c2760 : 0xAA1a1830);
        graphics.renderOutline(allX, 8, 90, 14, anyLanded ? MapUi.FRAME : MapUi.PANEL_LINE);
        graphics.drawCenteredString(font, "Collect all", allX + 45, 11, anyLanded ? MapUi.TITLE : MapUi.DIM);

        drawList(graphics, mouseX, mouseY, list, now);
        drawStations(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawList(GuiGraphics graphics, int mouseX, int mouseY, List<ContinuumStateSnapshot.MissionView> list,
                          long now) {
        rows.clear();
        int x = 10;
        int top = 36;
        int w = listW();
        MapUi.panel(graphics, x, top, w, height - top - 10);
        if (list.isEmpty()) {
            graphics.drawString(font, "No rockets are out. Launch one from a Launch Complex.", x + 8, top + 10,
                    MapUi.DIM);
            return;
        }

        int visible = Math.max(1, (height - top - 20) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - visible)));
        graphics.enableScissor(x + 1, top + 1, x + w - 1, height - 11);
        for (int i = scroll; i < list.size() && i < scroll + visible + 1; i++) {
            var mission = list.get(i);
            int ry = top + 6 + (i - scroll) * ROW;
            boolean hover = MapUi.inside(mouseX, mouseY, x + 3, ry, w - 6, ROW - 3);
            if (hover) graphics.fill(x + 3, ry, x + w - 3, ry + ROW - 3, 0x33ffffff);

            String status;
            int color;
            if (mission.stranded()) {
                status = mission.rescuing() ? "STRANDED - rescue en route" :
                        "STRANDED  " + ContinuumMissions.durationText(mission.distressUntil() - now);
                color = MapUi.WARN;
            } else if (mission.state() == Mission.State.SUCCESS) {
                status = "Landed - click to collect";
                color = MapUi.GOOD;
            } else if (mission.state() == Mission.State.FAILED) {
                status = "Failed - click to collect";
                color = MapUi.BAD;
            } else {
                status = MapUi.duration(mission.endMillis() - now);
                color = MapUi.TEXT;
            }
            graphics.drawString(font, label(mission), x + 8, ry + 2, MapUi.TITLE);
            graphics.drawString(font, status, x + w - 8 - font.width(status), ry + 2, color);
            MapUi.bar(graphics, x + 8, ry + 14, w - 130, 4, mission.progress(now),
                    mission.stranded() ? MapUi.WARN : mission.state() == Mission.State.FAILED ? MapUi.BAD :
                            mission.state().finished() ? MapUi.GOOD : MapUi.FRAME);
            String wear = String.format(Locale.ROOT, "wear %d%%", Math.round(mission.wearAfter() * 100.0f));
            graphics.drawString(font, wear, x + w - 8 - font.width(wear), ry + 13,
                    MapUi.wearColor(mission.wearAfter()));

            rows.add(new Object[] { mission.id(), mission.state().finished() && !mission.stranded(), x + 3, ry, w - 6,
                    ROW - 3 });
        }
        graphics.disableScissor();
    }

    private void drawStations(GuiGraphics graphics) {
        int x = 10 + listW() + 10;
        int top = 36;
        int w = width - x - 10;
        MapUi.panel(graphics, x, top, w, height - top - 10);
        graphics.drawString(font, "Orbital stations", x + 8, top + 8, MapUi.TITLE);
        graphics.drawString(font, "Each adds a mission slot.", x + 8, top + 20, MapUi.DIM);

        Map<ResourceLocation, Integer> stations = ContinuumClientState.stations();
        if (stations.isEmpty()) {
            graphics.drawString(font, "None built yet. Plan a", x + 8, top + 38, MapUi.DIM);
            graphics.drawString(font, "station run from a body's page.", x + 8, top + 49, MapUi.DIM);
            return;
        }
        int y = top + 38;
        for (var entry : stations.entrySet()) {
            graphics.drawString(font, bodyName(entry.getKey()), x + 8, y, MapUi.TEXT);
            String level = "Lv " + entry.getValue() + "/" + Stations.MAX_LEVEL;
            graphics.drawString(font, level, x + w - 8 - font.width(level), y,
                    entry.getValue() >= Stations.MAX_LEVEL ? MapUi.GOOD : MapUi.WARN);
            y += 12;
        }
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
            if (MapUi.inside(mouseX, mouseY, width - 62 - 96, 8, 90, 14)) {
                ContinuumSounds.click();
                PhoenixNetwork.CHANNEL.sendToServer(new C2SCollectAllPacket());
                return true;
            }
            for (Object[] row : rows) {
                if (!MapUi.inside(mouseX, mouseY, (int) row[2], (int) row[3], (int) row[4], (int) row[5])) continue;
                ContinuumSounds.click();
                var id = (java.util.UUID) row[0];
                if ((boolean) row[1]) {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SCollectMissionPacket(id));
                } else {
                    Minecraft.getInstance().setScreen(new ContinuumTransitScreen(id, pad));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
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
