package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.client.ContinuumClientState;
import net.phoenix.core.integration.continuum.common.Deposits;
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
    private static final int H = 312;

    /** The scale the panel was last drawn at, so clicks can be mapped back into its layout space. */
    private float scale = 1.0f;
    private @Nullable Component pendingTooltip;

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

    void open(DiscoveryStage stage, boolean hasOutputWaiting, boolean outpostDamaged) {
        open = true;
        probes = 1;
        mode = outpostDamaged ? Mission.Type.REPAIR :
                stage == DiscoveryStage.SURVEYED ? (hasOutputWaiting ? Mission.Type.HAUL : Mission.Type.EXTRACT) :
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

    /** A label and a value that wraps within the panel instead of running off its edge. */
    private static int row(GuiGraphics g, Font font, String label, String value, int x, int y, int color) {
        g.drawString(font, label, x, y, MapUi.DIM);
        for (FormattedCharSequence line : font.split(FormattedText.of(value), W - 20 - 92)) {
            g.drawString(font, line, x + 92, y, color);
            y += 11;
        }
        return y;
    }

    /** Wrapped text across the panel's width. Returns the y below it. */
    private static int wrapped(GuiGraphics g, Font font, String text, int x, int y, int color) {
        for (FormattedCharSequence line : font.split(FormattedText.of(text), W - 20)) {
            g.drawString(font, line, x, y, color);
            y += 10;
        }
        return y;
    }

    void draw(GuiGraphics g, Font font, int screenW, int screenH, int mx, int my, ContinuumBody body,
              @Nullable net.minecraft.core.BlockPos padPos,
              DiscoveryStage stage, ItemStack rocket, int probesOwned, int kitsOwned,
              @Nullable ContinuumStateSnapshot.OutpostView outpost, long now) {
        g.fill(0, 0, screenW, screenH, 0xCC000000);

        // shrink to fit small windows and high GUI scales: lay everything out in a virtual space the panel fits
        scale = Math.max(0.4f, Math.min(1.0f, Math.min((screenW - 12.0f) / W, (screenH - 12.0f) / H)));
        int realMx = mx;
        int realMy = my;
        mx = Math.round(mx / scale);
        my = Math.round(my / scale);
        int virtualW = Math.round(screenW / scale);
        int virtualH = Math.round(screenH / scale);
        pendingTooltip = null;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);

        int x = (virtualW - W) / 2;
        int y = (virtualH - H) / 2;
        // fully opaque: whatever is behind the planner must not show through its text
        g.fill(x, y, x + W, y + H, 0xFF0b0a18);
        g.renderOutline(x, y, W, H, MapUi.PANEL_LINE);

        var cfg = PhoenixConfigs.INSTANCE.continuum;
        int existingProbes = outpost == null ? 0 : outpost.probes();
        int ready = outpost == null ? 0 : outpost.readyAt(now);
        boolean producible = !body.yields().isEmpty();

        // where the rocket, probes and kits come from: the launch complex's buses, or the player for a test pad
        String loaded = ContinuumClientState.padUsesBuses(padPos) ? "in the item input buses" : "in your inventory";
        int mapped = ContinuumClientState.mappedDeposits(body.id());
        int deposits = body.yields().size();
        boolean deepSurvey = stage == DiscoveryStage.SURVEYED;
        boolean canSurvey = stage == DiscoveryStage.DETECTED || (deepSurvey && mapped < deposits);
        boolean canExtract = stage == DiscoveryStage.SURVEYED && producible;
        boolean canDeploy = stage == DiscoveryStage.SURVEYED && producible && !body.isCentral() &&
                body.type() != ContinuumBody.Type.BLACK_HOLE && existingProbes < cfg.maxProbesPerOutpost;
        boolean canHaul = outpost != null && ready > 0;
        boolean damaged = outpost != null && outpost.damaged();
        boolean canRepair = damaged;
        int kitsNeeded = Math.max(1, cfg.outpostRepairKits);

        // keep the selection on something that is actually available
        if (!available(mode, canSurvey, canExtract, canDeploy, canHaul, canRepair)) {
            for (Mission.Type candidate : Mission.Type.values()) {
                if (available(candidate, canSurvey, canExtract, canDeploy, canHaul, canRepair)) {
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

        modeRects[Mission.Type.SURVEY.ordinal()] = button(g, font, x + 10, y + 24, 150, 16,
                deepSurvey ? "Deep survey (" + mapped + "/" + deposits + ")" : "Survey", canSurvey,
                mode == Mission.Type.SURVEY, mx, my);
        modeRects[Mission.Type.EXTRACT.ordinal()] = button(g, font, x + 170, y + 24, 152, 16, "Extract resources",
                canExtract, mode == Mission.Type.EXTRACT, mx, my);
        modeRects[Mission.Type.DEPLOY.ordinal()] = button(g, font, x + 10, y + 44, 150, 16, "Deploy outpost",
                canDeploy, mode == Mission.Type.DEPLOY, mx, my);
        modeRects[Mission.Type.HAUL.ordinal()] = button(g, font, x + 170, y + 44, 152, 16,
                "Haul output" + (ready > 0 ? " (" + ready + ")" : ""), canHaul, mode == Mission.Type.HAUL, mx, my);
        modeRects[Mission.Type.REPAIR.ordinal()] = button(g, font, x + 10, y + 64, 312, 16,
                damaged ? "Repair outpost  (DAMAGED)" : "Repair outpost", canRepair, mode == Mission.Type.REPAIR, mx,
                my);

        int ty = y + 90;
        int noteEnd = ty;
        minusRect = null;
        plusRect = null;

        boolean carriesProbes = mode == Mission.Type.EXTRACT || mode == Mission.Type.DEPLOY;
        if (carriesProbes) {
            g.drawString(font, "Probes", x + 10, ty + 3, MapUi.DIM);
            minusRect = button(g, font, x + 92, ty, 16, 14, "-", probes > 1, false, mx, my);
            g.drawCenteredString(font, String.valueOf(probes), x + 124, ty + 3, MapUi.TITLE);
            plusRect = button(g, font, x + 140, ty, 16, 14, "+", probes < maxProbes, false, mx, my);
            g.drawString(font, "(" + probesOwned + " " + loaded + ")", x + 166, ty + 3, MapUi.DIM);
        } else {
            String note = switch (mode) {
                case SURVEY -> deepSurvey ?
                        "Maps one more deposit's richness, and may turn up an anomaly. No payload." :
                        "Resolves this body's surface and sky, maps a deposit, and may turn up an anomaly.";
                case HAUL -> "Brings home everything the outpost has stockpiled.";
                case REPAIR -> "Delivers " + kitsNeeded + " repair kit" + (kitsNeeded == 1 ? "" : "s") +
                        " and gets the broken outpost producing again.";
                default -> "";
            };
            noteEnd = wrapped(g, font, note, x + 10, ty + 3, MapUi.DIM);
        }
        ty = Math.max(ty + 20, noteEnd + 4);

        g.fill(x + 10, ty, x + W - 10, ty + 1, MapUi.PANEL_LINE);
        ty += 6;

        boolean hasRocket = !rocket.isEmpty();
        boolean refused = hasRocket && RocketStats.interlockRefuses(rocket);

        if (!hasRocket) {
            ty = wrapped(g, font, "No rocket " + loaded + ".", x + 10, ty, 0xFFff9a9a) + 2;
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
                        mode == Mission.Type.REPAIR ? "A failed run loses the repair kits." :
                                "A failed run loses the probes.";
                ty = wrapped(g, font, loss, x + 10, ty, 0xFFff9a9a) + 1;
            }
            if (refused) {
                ty = wrapped(g, font, "Hull interlock engaged: repair the rocket first.", x + 10, ty, 0xFFff9a9a) + 1;
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
            case REPAIR -> {
                ty = row(g, font, "Repair kits", kitsNeeded + "  (" + kitsOwned + " " + loaded + ")", x + 10, ty,
                        kitsOwned >= kitsNeeded ? MapUi.TEXT : MapUi.BAD);
                if (outpost != null) {
                    ty = row(g, font, "Idle cycles", String.valueOf(outpost.brokenCycles()), x + 10, ty, MapUi.WARN);
                }
            }
            default -> {}
        }
        if (damaged && mode != Mission.Type.REPAIR) {
            wrapped(g, font, "This outpost is damaged and is not producing.", x + 10, ty + 2, MapUi.WARN);
        }

        boolean enoughProbes = !carriesProbes || probesOwned >= 1;
        boolean enoughKits = mode != Mission.Type.REPAIR || kitsOwned >= kitsNeeded;
        boolean launchable = hasRocket && !refused && enoughProbes && enoughKits &&
                available(mode, canSurvey, canExtract, canDeploy, canHaul, canRepair);
        launchRect = button(g, font, x + 10, y + H - 26, 150, 18, "Launch", launchable, false, mx, my);
        cancelRect = button(g, font, x + 170, y + H - 26, 152, 18, "Cancel", true, false, mx, my);

        // a greyed-out mission type says why, on hover
        record Mode(Mission.Type type, int bx, int by, int bw, boolean enabled, String reason) {}
        Mode[] modes = {
                new Mode(Mission.Type.SURVEY, x + 10, y + 24, 150, canSurvey, stage == DiscoveryStage.UNKNOWN ?
                        "Detect this signal first (research)." : "Fully surveyed: every deposit here is mapped."),
                new Mode(Mission.Type.EXTRACT, x + 170, y + 24, 152, canExtract, stage != DiscoveryStage.SURVEYED ?
                        "Survey this body first." : "There is nothing to extract here."),
                new Mode(Mission.Type.DEPLOY, x + 10, y + 44, 150, canDeploy, stage != DiscoveryStage.SURVEYED ?
                        "Survey this body first." : !producible || body.isCentral() ? "Nothing can be built here." :
                                "This outpost is already full."),
                new Mode(Mission.Type.HAUL, x + 170, y + 44, 152, canHaul, outpost == null ?
                        "No outpost here yet. Deploy one first." : "Nothing is ready to haul yet."),
                new Mode(Mission.Type.REPAIR, x + 10, y + 64, 312, canRepair, outpost == null ?
                        "No outpost here yet." : "This outpost is not damaged."),
        };
        for (Mode m : modes) {
            if (!m.enabled() && MapUi.inside(mx, my, m.bx(), m.by(), m.bw(), 16)) {
                pendingTooltip = Component.literal(m.reason());
            }
        }

        g.pose().popPose();
        if (pendingTooltip != null) g.renderTooltip(font, pendingTooltip, realMx, realMy);
    }

    private static boolean available(Mission.Type type, boolean survey, boolean extract, boolean deploy,
                                     boolean haul, boolean repair) {
        return switch (type) {
            case SURVEY -> survey;
            case EXTRACT -> extract;
            case DEPLOY -> deploy;
            case HAUL -> haul;
            case REPAIR -> repair;
        };
    }

    private int yields(GuiGraphics g, Font font, ContinuumBody body, int x, int y, int rolls, String heading) {
        g.drawString(font, heading, x, y, MapUi.DIM);
        y += 11;
        for (int i = 0; i < body.yields().size(); i++) {
            ContinuumBody.Yield yield = body.yields().get(i);
            // a deposit's richness scales the haul, and is only known once surveys have mapped it
            float richness = ContinuumClientState.richness(body.id(), i);
            float scale = richness > 0.0f ? richness : 1.0f;
            String range = Math.round(yield.min() * rolls * scale) + "-" + Math.round(yield.max() * rolls * scale);
            String chance = yield.chance() < 0.999f ?
                    String.format(Locale.ROOT, "  (%d%% per roll)", Math.round(yield.chance() * 100)) : "";
            String left = "  " + itemName(yield.item()) + "  x" + range + chance;
            g.drawString(font, left, x, y, MapUi.TEXT);

            String tag = richness > 0.0f ? Deposits.label(richness) + " " + Deposits.percent(richness) : "unmapped";
            int tagColor = richness <= 0.0f ? MapUi.DIM : richness >= Deposits.RICH - 0.01f ? MapUi.GOOD :
                    richness < Deposits.NORMAL ? MapUi.WARN : MapUi.TEXT;
            int tagX = x + W - 28 - font.width(tag);
            if (x + font.width(left) + 8 > tagX) {
                // no room beside the name: the tag goes on its own line underneath
                y += 10;
                tagX = x + 14;
            }
            g.drawString(font, tag, tagX, y, tagColor);
            y += 11;
        }
        return y;
    }

    Result click(double screenX, double screenY) {
        double mx = screenX / scale;
        double my = screenY / scale;
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
