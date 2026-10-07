package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.integration.continuum.common.OutpostPower;
import net.phoenix.core.integration.continuum.common.RocketStats;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The mission planner: a modal over the map that lets the player choose what kind of trip to fly (survey, extraction
 * run, outpost deployment or outpost haul), how many probes to risk, and see what it would cost with the rocket they
 * are
 * carrying. It only builds a request; the server validates and decides everything.
 */
final class MissionPlannerPanel {

    /** What a click did. */
    record Result(boolean consumed, @Nullable Mission.Type launchType, int probes) {

        static final Result CONSUMED = new Result(true, null, 0);
    }

    private static final int W = 332;
    private static final int H = 268;

    private boolean open;
    private Mission.Type mode = Mission.Type.SURVEY;
    private int probes = 1;

    private final int[][] modeRects = new int[Mission.Type.values().length][];
    private int[] minusRect;
    private int[] plusRect;
    private int[] launchRect;
    private int[] cancelRect;

    boolean isOpen() {
        return open;
    }

    void close() {
        open = false;
    }

    void open(DiscoveryStage stage, boolean hasOutputWaiting) {
        open = true;
        probes = 1;
        mode = stage == DiscoveryStage.SURVEYED ? (hasOutputWaiting ? Mission.Type.HAUL : Mission.Type.EXTRACT) :
                Mission.Type.SURVEY;
    }

    private static boolean hit(double mx, double my, @Nullable int[] r) {
        return r != null && MapUi.inside(mx, my, r[0], r[1], r[2], r[3]);
    }

    private static String itemName(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? id.getPath() : item.getDescription().getString();
    }

    private int[] button(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean enabled,
                         boolean selected, double mx, double my) {
        boolean hover = enabled && MapUi.inside(mx, my, x, y, w, h);
        int fill = !enabled ? 0xFF14122a : selected ? 0xFF3a3380 : hover ? 0xFF2c2760 : 0xFF1d1a40;
        g.fill(x, y, x + w, y + h, fill);
        g.renderOutline(x, y, w, h, enabled ? (selected ? MapUi.FRAME : MapUi.PANEL_LINE) : 0xFF2a2548);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, enabled ? MapUi.TITLE : 0xFF565070);
        return enabled ? new int[] { x, y, w, h } : null;
    }

    private static int row(GuiGraphics g, Font font, String label, String value, int x, int y, int color) {
        g.drawString(font, label, x, y, MapUi.DIM);
        g.drawString(font, value, x + 92, y, color);
        return y + 11;
    }

    void draw(GuiGraphics g, Font font, int screenW, int screenH, int mx, int my, ContinuumBody body,
              DiscoveryStage stage, ItemStack rocket, int probesOwned,
              @Nullable ContinuumStateSnapshot.OutpostView outpost, long now) {
        g.fill(0, 0, screenW, screenH, 0x99000000);

        int x = (screenW - W) / 2;
        int y = (screenH - H) / 2;
        MapUi.panel(g, x, y, W, H);

        var cfg = PhoenixConfigs.INSTANCE.continuum;
        int existingProbes = outpost == null ? 0 : outpost.probes();
        int ready = outpost == null ? 0 : outpost.readyAt(now);
        boolean producible = !body.yields().isEmpty();

        boolean canSurvey = stage == DiscoveryStage.DETECTED;
        boolean canExtract = stage == DiscoveryStage.SURVEYED && producible;
        boolean canDeploy = stage == DiscoveryStage.SURVEYED && producible && !body.isCentral() &&
                body.type() != ContinuumBody.Type.BLACK_HOLE && existingProbes < cfg.maxProbesPerOutpost;
        boolean canHaul = outpost != null && ready > 0;

        // keep the selection on something that is actually available
        if (!available(mode, canSurvey, canExtract, canDeploy, canHaul)) {
            for (Mission.Type candidate : Mission.Type.values()) {
                if (available(candidate, canSurvey, canExtract, canDeploy, canHaul)) {
                    mode = candidate;
                    break;
                }
            }
        }

        int maxProbes = cfg.maxProbesPerMission;
        if (mode == Mission.Type.DEPLOY) maxProbes = Math.min(maxProbes, cfg.maxProbesPerOutpost - existingProbes);
        maxProbes = Math.max(1, Math.min(maxProbes, Math.max(1, probesOwned)));
        probes = Math.max(1, Math.min(probes, maxProbes));

        g.drawString(font, "Plan mission  -  " + body.name(), x + 10, y + 9, MapUi.TITLE);

        modeRects[Mission.Type.SURVEY.ordinal()] = button(g, font, x + 10, y + 24, 150, 16, "Survey", canSurvey,
                mode == Mission.Type.SURVEY, mx, my);
        modeRects[Mission.Type.EXTRACT.ordinal()] = button(g, font, x + 170, y + 24, 152, 16, "Extract resources",
                canExtract, mode == Mission.Type.EXTRACT, mx, my);
        modeRects[Mission.Type.DEPLOY.ordinal()] = button(g, font, x + 10, y + 44, 150, 16, "Deploy outpost",
                canDeploy, mode == Mission.Type.DEPLOY, mx, my);
        modeRects[Mission.Type.HAUL.ordinal()] = button(g, font, x + 170, y + 44, 152, 16,
                "Haul output" + (ready > 0 ? " (" + ready + ")" : ""), canHaul, mode == Mission.Type.HAUL, mx, my);

        int ty = y + 70;
        minusRect = null;
        plusRect = null;

        boolean carriesProbes = mode == Mission.Type.EXTRACT || mode == Mission.Type.DEPLOY;
        if (carriesProbes) {
            g.drawString(font, "Probes", x + 10, ty + 3, MapUi.DIM);
            minusRect = button(g, font, x + 92, ty, 16, 14, "-", probes > 1, false, mx, my);
            g.drawCenteredString(font, String.valueOf(probes), x + 124, ty + 3, MapUi.TITLE);
            plusRect = button(g, font, x + 140, ty, 16, 14, "+", probes < maxProbes, false, mx, my);
            g.drawString(font, "(you carry " + probesOwned + ")", x + 166, ty + 3, MapUi.DIM);
        } else {
            g.drawString(font, switch (mode) {
                case SURVEY -> "Resolves this body's surface and sky. No payload.";
                case HAUL -> "Brings home everything the outpost has stockpiled.";
                default -> "";
            }, x + 10, ty + 3, MapUi.DIM);
        }
        ty += 20;

        g.fill(x + 10, ty, x + W - 10, ty + 1, MapUi.PANEL_LINE);
        ty += 6;

        boolean hasRocket = !rocket.isEmpty();
        boolean refused = hasRocket && RocketStats.interlockRefuses(rocket);

        if (!hasRocket) {
            g.drawString(font, "No rocket in your inventory.", x + 10, ty, 0xFFff9a9a);
            ty += 12;
        } else {
            float wear = RocketStats.wear(rocket);
            float cost = RocketStats.wearCost(body, rocket);
            float risk = RocketStats.failureChance(rocket, stage);

            ty = row(g, font, "Trip time", MapUi.duration(RocketStats.tripMillis(body, rocket)), x + 10, ty,
                    MapUi.TEXT);
            ty = row(g, font, "Rocket wear",
                    Math.round(wear * 100) + "% -> " + Math.round(Math.min(1.0f, wear + cost) * 100) + "%", x + 10, ty,
                    MapUi.wearColor(wear + cost));
            ty = row(g, font, "Failure risk", Math.round(risk * 100) + "%", x + 10, ty,
                    risk < 0.05f ? MapUi.GOOD : risk < 0.3f ? MapUi.WARN : MapUi.BAD);
            if (risk > 0.0f && mode != Mission.Type.SURVEY) {
                String loss = mode == Mission.Type.HAUL ? "A failed haul loses the whole stockpile." :
                        "A failed run loses the probes.";
                g.drawString(font, loss, x + 10, ty, 0xFFff9a9a);
                ty += 11;
            }
            if (refused) {
                g.drawString(font, "Hull interlock engaged: repair the rocket first.", x + 10, ty, 0xFFff9a9a);
                ty += 11;
            }
        }

        ty += 3;
        switch (mode) {
            case EXTRACT -> ty = yields(g, font, body, x + 10, ty, probes,
                    "Expected from " + probes + " probe" + (probes == 1 ? "" : "s") + ":");
            case DEPLOY -> {
                ty = row(g, font, "Outpost after", (existingProbes + probes) + " of " + cfg.maxProbesPerOutpost +
                        " probes", x + 10, ty, MapUi.TEXT);
                ty = row(g, font, "Cycle length", String.format(Locale.ROOT, "%.1f min", cfg.outpostCycleMinutes),
                        x + 10, ty, MapUi.TEXT);
                long upkeep = OutpostPower.upkeepPerCycle(existingProbes + probes);
                if (upkeep > 0) {
                    ty = row(g, font, "Upkeep", String.format(Locale.ROOT, "%,d EU / cycle (Tesla Network)", upkeep),
                            x + 10, ty, MapUi.WARN);
                }
                ty = yields(g, font, body, x + 10, ty, 1, "Each probe, each cycle:");
            }
            case HAUL -> {
                ty = row(g, font, "Outpost", existingProbes + " probes, " + ready + " of " +
                        (outpost == null ? 0 : outpost.maxReady()) + " cycles ready", x + 10, ty, MapUi.TEXT);
                if (outpost != null && outpost.lostCycles() > 0) {
                    ty = row(g, font, "Lost to power", outpost.lostCycles() + " cycles", x + 10, ty, MapUi.BAD);
                }
                ty = yields(g, font, body, x + 10, ty, existingProbes * ready, "Expected haul:");
            }
            default -> {}
        }

        boolean enoughProbes = !carriesProbes || probesOwned >= 1;
        boolean launchable = hasRocket && !refused && enoughProbes &&
                available(mode, canSurvey, canExtract, canDeploy, canHaul);
        launchRect = button(g, font, x + 10, y + H - 26, 150, 18, "Launch", launchable, false, mx, my);
        cancelRect = button(g, font, x + 170, y + H - 26, 152, 18, "Cancel", true, false, mx, my);
    }

    private static boolean available(Mission.Type type, boolean survey, boolean extract, boolean deploy,
                                     boolean haul) {
        return switch (type) {
            case SURVEY -> survey;
            case EXTRACT -> extract;
            case DEPLOY -> deploy;
            case HAUL -> haul;
        };
    }

    private int yields(GuiGraphics g, Font font, ContinuumBody body, int x, int y, int rolls, String heading) {
        g.drawString(font, heading, x, y, MapUi.DIM);
        y += 11;
        for (ContinuumBody.Yield yield : body.yields()) {
            String range = (long) yield.min() * rolls + "-" + (long) yield.max() * rolls;
            String chance = yield.chance() < 0.999f ?
                    String.format(Locale.ROOT, "  (%d%% per roll)", Math.round(yield.chance() * 100)) : "";
            g.drawString(font, "  " + itemName(yield.item()) + "  x" + range + chance, x, y, MapUi.TEXT);
            y += 11;
        }
        return y;
    }

    Result click(double mx, double my) {
        for (Mission.Type type : Mission.Type.values()) {
            if (hit(mx, my, modeRects[type.ordinal()])) {
                mode = type;
                return Result.CONSUMED;
            }
        }
        if (hit(mx, my, minusRect)) {
            probes = Math.max(1, probes - 1);
            return Result.CONSUMED;
        }
        if (hit(mx, my, plusRect)) {
            probes++;
            return Result.CONSUMED;
        }
        if (hit(mx, my, launchRect)) {
            open = false;
            boolean carries = mode == Mission.Type.EXTRACT || mode == Mission.Type.DEPLOY;
            return new Result(true, mode, carries ? probes : 0);
        }
        if (hit(mx, my, cancelRect)) {
            open = false;
            return Result.CONSUMED;
        }
        // a modal swallows every click
        return Result.CONSUMED;
    }
}
