package net.phoenix.core.integration.vocal_resonance.puzzle;

import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.MachineHud;
import brachy.modularui.value.sync.BooleanSyncValue;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.network.packet.S2CAttuneResultPacket;
import net.phoenix.core.network.packet.S2CAttuneStartPacket;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.layout.Flow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HarmonicLockMachine extends MultiblockControllerMachine implements IMuiMachine {

    public static final int DORMANT = 0;
    public static final int ATTUNING = 1;
    public static final int ATTUNED = 2;

    private static final int OFFSET_SLACK_MS = 250;
    private static final int RETRY_COOLDOWN_TICKS = 100;
    private static final double GOOD_GRADE = 0.8;
    private static final double PERFECT_GRADE = 0.95;

    private static final Set<HarmonicLockMachine> LOCKS = ConcurrentHashMap.newKeySet();

    private final AcousticStateMachine machine = new AcousticStateMachine();

    @SaveField
    public String puzzleId = "";

    @SaveField
    @SyncToClient
    private int state = 0;

    @SaveField
    @SyncToClient
    private int step = 0;

    @SaveField
    @SyncToClient
    private int attuneState = DORMANT;

    @SaveField
    @SyncToClient
    private int attuneGrade = 0;

    @SaveField
    @SyncToClient
    private int tuneLevel = 0;

    @SaveField
    private long cooldownUntil = 0;

    private int strikes = 0;
    private AttuneDifficulty runDifficulty = AttuneDifficulty.EASY;

    private @Nullable AcousticChart chart;
    private @Nullable UUID runner;
    private long runStart;
    private boolean[] judged = new boolean[0];

    private boolean[] holding = new boolean[0];
    private boolean[] holdPerfect = new boolean[0];
    private int perfects;
    private int goods;
    private int minOffset = Integer.MAX_VALUE;

    private @Nullable TickableSubscription runSub;

    public HarmonicLockMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) runSub = subscribeServerTick(this::runTick);
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        if (!isRemote()) LOCKS.add(this);
    }

    @Override
    public void invalidateStructure(String substructureName) {
        LOCKS.remove(this);
        abortRun();
        super.invalidateStructure(substructureName);
    }

    @Override
    public void onUnload() {
        LOCKS.remove(this);
        abortRun();
        if (runSub != null) {
            unsubscribe(runSub);
            runSub = null;
        }
        super.onUnload();
    }

    public int getTuneLevel() {
        return tuneLevel;
    }

    public void startAttunement(ServerPlayer player, int difficultyLevel) {
        if (!isFormed() || attuneState == ATTUNING) return;
        Level level = getLevel();
        if (level == null) return;
        if (difficultyLevel < 1 || difficultyLevel > AttuneDifficulty.values().length ||
                difficultyLevel > tuneLevel + 1) {
            player.displayClientMessage(Component.literal("§cClear the level before it first."), true);
            return;
        }
        runDifficulty = AttuneDifficulty.fromLevel(difficultyLevel);
        if (level.getGameTime() < cooldownUntil) {
            player.displayClientMessage(Component.literal("§cThe lock is still ringing, wait a moment."), true);
            return;
        }
        AcousticChart picked = AcousticData.defaultChart();
        if (picked == null || picked.beats().isEmpty()) {
            player.displayClientMessage(Component.literal("§cNo attunement chart is loaded."), true);
            return;
        }
        chart = picked;
        runner = player.getUUID();

        runStart = level.getGameTime() + AttuneDifficulty.LEAD_MS / AcousticChart.MS_PER_TICK;
        judged = new boolean[picked.beats().size()];
        holding = new boolean[picked.beats().size()];
        holdPerfect = new boolean[picked.beats().size()];
        perfects = 0;
        goods = 0;
        minOffset = Integer.MAX_VALUE;
        attuneState = ATTUNING;
        markAsChanged();

        player.closeContainer();
        PacketDistributor.sendToPlayer(player, new S2CAttuneStartPacket(getBlockPos(), runDifficulty.level(), picked));
    }

    public void attunePress(ServerPlayer player, int lane, int elapsedMs) {
        AcousticChart current = chart;
        Level level = getLevel();
        if (attuneState != ATTUNING || current == null || level == null || !player.getUUID().equals(runner)) return;

        int serverMs = (int) ((level.getGameTime() - runStart) * AcousticChart.MS_PER_TICK);
        int offset = serverMs - elapsedMs;
        if (offset < -AcousticChart.MS_PER_TICK * 2 - runDifficulty.goodMs()) return;
        if (minOffset != Integer.MAX_VALUE && offset > minOffset + OFFSET_SLACK_MS) return;
        minOffset = Math.min(minOffset, offset);

        int best = -1;
        int bestDelta = Integer.MAX_VALUE;
        List<AcousticChart.Beat> beats = current.beats();
        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] || beats.get(i).lane() != lane) continue;
            int delta = Math.abs(runDifficulty.beatMs(beats.get(i).tick()) - elapsedMs);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = i;
            }
        }
        if (best < 0 || bestDelta > runDifficulty.goodMs()) return;
        judged[best] = true;
        boolean perfect = bestDelta <= runDifficulty.perfectMs();
        if (beats.get(best).isHold()) {

            holding[best] = true;
            holdPerfect[best] = perfect;
            return;
        }
        if (perfect) perfects++;
        else goods++;
    }

    public void attuneRelease(ServerPlayer player, int lane, int elapsedMs) {
        AcousticChart current = chart;
        Level level = getLevel();
        if (attuneState != ATTUNING || current == null || level == null || !player.getUUID().equals(runner)) return;

        List<AcousticChart.Beat> beats = current.beats();
        for (int i = 0; i < beats.size(); i++) {
            if (!holding[i] || beats.get(i).lane() != lane) continue;
            holding[i] = false;
            int tail = runDifficulty.beatMs(beats.get(i).endTick());
            if (elapsedMs >= tail - runDifficulty.goodMs()) {
                if (holdPerfect[i]) perfects++;
                else goods++;
            }
            return;
        }
    }

    private void runTick() {
        if (attuneState != ATTUNING || chart == null || getLevel() == null) return;
        long endTick = runStart + runDifficulty.beatMs(chart.lastBeatTick()) / AcousticChart.MS_PER_TICK + 60;
        if (getLevel().getGameTime() >= endTick) finishRun();
    }

    private void finishRun() {
        AcousticChart current = chart;
        Level level = getLevel();
        if (current == null || level == null) return;

        for (int i = 0; i < current.beats().size(); i++) {
            if (!holding[i]) continue;
            holding[i] = false;
            if (holdPerfect[i]) perfects++;
            else goods++;
        }
        int total = current.beats().size();
        double accuracy = (perfects + goods * 0.6) / Math.max(1, total);
        boolean passed = accuracy >= runDifficulty.pass();
        int grade = accuracy >= PERFECT_GRADE ? 2 : accuracy >= GOOD_GRADE ? 1 : 0;

        if (passed) {
            attuneState = ATTUNED;

            if (runDifficulty.level() >= tuneLevel) attuneGrade = grade;
            tuneLevel = Math.max(tuneLevel, runDifficulty.level());
            machine.reset();
            state = 0;
            step = 0;
            strikes = 0;
        } else {

            attuneState = tuneLevel > 0 ? ATTUNED : DORMANT;
            cooldownUntil = level.getGameTime() + RETRY_COOLDOWN_TICKS;
        }
        markAsChanged();

        if (runner != null && level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(runner);
            if (player != null) {
                PacketDistributor.sendToPlayer(player,
                        new S2CAttuneResultPacket(getBlockPos(), passed, grade, perfects, goods, total));
            }
        }
        chart = null;
        runner = null;
    }

    private void abortRun() {
        if (attuneState == ATTUNING) {
            attuneState = tuneLevel > 0 ? ATTUNED : DORMANT;
            chart = null;
            runner = null;
            markAsChanged();
        }
    }

    public static void broadcast(Level level, BlockPos source, int range, String soundLoc) {
        if (level.isClientSide) return;
        ResourceLocation sound = ResourceLocation.tryParse(soundLoc);
        if (sound == null) return;
        double rangeSq = (double) range * range;
        for (HarmonicLockMachine lock : LOCKS) {
            if (lock.getLevel() == level && lock.isFormed() && lock.getBlockPos().distSqr(source) <= rangeSq) {
                lock.hear(sound);
            }
        }
    }

    private void hear(ResourceLocation sound) {
        if (attuneState != ATTUNED) return;
        machine.load(state, step);
        AcousticPuzzle puzzle = getPuzzle();

        if (puzzle != null && getPuzzleState() == AcousticStateMachine.State.LISTENING &&
                !machine.wouldAccept(puzzle, sound) && strikes < tuneLevel - 1) {
            strikes++;
            return;
        }
        if (machine.onSound(puzzle, sound)) {
            strikes = 0;
            state = machine.state().ordinal();
            step = machine.step();
            markAsChanged();
        }
    }

    public @Nullable AcousticPuzzle getPuzzle() {
        AcousticPuzzle chosen = puzzleId.isEmpty() ? null : AcousticData.puzzle(ResourceLocation.tryParse(puzzleId));
        return chosen != null ? chosen : AcousticData.defaultPuzzle();
    }

    public AcousticStateMachine.State getPuzzleState() {
        AcousticStateMachine.State[] values = AcousticStateMachine.State.values();
        return values[Math.max(0, Math.min(state, values.length - 1))];
    }

    public boolean isAttuned() {
        return attuneState == ATTUNED;
    }

    public boolean isSolved() {
        return isAttuned() && getPuzzleState() == AcousticStateMachine.State.SOLVED;
    }

    private String hint(AcousticPuzzle puzzle) {
        int shown = Math.min(puzzle.steps().size(), Math.max(1, tuneLevel) + attuneGrade);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            AcousticPuzzle.Step s = puzzle.steps().get(i);
            if (i > 0) out.append(", ");
            if (s.note() != AcousticSoundEntry.NO_NOTE) out.append(noteName(s.note()));
            else if (s.tag() != null) out.append(s.tag());
            else out.append("a specific sound");
        }
        return out.toString();
    }

    private static String noteName(int midi) {
        String[] names = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
        return names[Math.floorMod(midi, 12)] + (midi / 12 - 1);
    }

    private static AcousticStateMachine.State stateOf(int ordinal) {
        AcousticStateMachine.State[] values = AcousticStateMachine.State.values();
        return values[Math.max(0, Math.min(ordinal, values.length - 1))];
    }

    private void hudAction(int id, int value) {}

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData data,
                                                           PanelSyncManager syncManager,
                                                           brachy.modularui.screen.UISettings settings) {
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private HudModel hudModel() {
        return HudModel.of("Harmonic Lock")
                .formed(this::isFormed)
                .theme(net.phoenix.core.client.gui.hud.HudTheme.LOCK)
                .hero(net.phoenix.core.client.gui.hud.Heroes.tumblers(() -> attuneState == ATTUNED ? step : 0, () -> getPuzzle() == null ? 5 : getPuzzle().steps().size(), net.phoenix.core.client.gui.hud.HudTheme.LOCK.accent()), 26)
                .badge(() -> switch (attuneState) {
                    case ATTUNING -> new HudModel.Badge("ATTUNING", HudModel.GOLD);
                    case ATTUNED -> new HudModel.Badge("ATTUNED", HudModel.GREEN);
                    default -> new HudModel.Badge("DORMANT", HudModel.RED);
                })
                .subtitle(() -> attuneState == DORMANT ? "Attune it to wake the puzzle" :
                        "Listens to Resonant Jukeboxes in range")
                .cells(() -> {
                    var cells = new java.util.ArrayList<HudModel.Cell>();
                    cells.add(HudModel.cell("LOCK", attuneState == ATTUNED ? "attuned" : attuneState == ATTUNING ?
                            "attuning" : "dormant", attuneState == ATTUNING ? "play the rhythm game" : "",
                            attuneState == ATTUNED ? HudModel.GREEN : HudModel.RED));
                    cells.add(HudModel.cell("LEVEL", tuneLevel == 0 ? "-" : AttuneDifficulty.fromLevel(tuneLevel).label(),
                            tuneLevel == 0 ? "not tuned" : "forgives " + Math.max(0, tuneLevel - 1) + " wrong",
                            HudModel.PURPLE));
                    cells.add(HudModel.cell("GRADE", attuneState != ATTUNED ? "-" : attuneGrade >= 2 ? "perfect" :
                            attuneGrade == 1 ? "good" : "passed", "attunement", HudModel.GOLD));
                    AcousticPuzzle puzzle = getPuzzle();
                    String progress = puzzle == null ? "-" : step + "/" + puzzle.steps().size();
                    cells.add(HudModel.cell("PUZZLE", attuneState != ATTUNED ? "asleep" : puzzle == null ? "none loaded" :
                            switch (stateOf(state)) {
                                case IDLE -> "waiting";
                                case LISTENING -> "listening";
                                case SOLVED -> "SOLVED";
                                case FAILED -> "wrong note";
                            }, progress, attuneState == ATTUNED && stateOf(state) == AcousticStateMachine.State.SOLVED ?
                                    HudModel.GREEN : HudModel.CYAN));
                    return cells;
                })
                .bars(() -> {
                    AcousticPuzzle puzzle = getPuzzle();
                    if (attuneState != ATTUNED || puzzle == null || puzzle.steps().isEmpty()) return java.util.List.of();
                    return java.util.List.of(new HudModel.Bar("Puzzle", step / (float) puzzle.steps().size(),
                            step + " / " + puzzle.steps().size(), HudModel.CYAN));
                })
                .localButton("ATTUNE", HudModel.PURPLE, this::isFormed, () ->
                        net.phoenix.core.integration.vocal_resonance.puzzle.client.AttuneClient
                                .openDifficulty(getBlockPos(), tuneLevel))
                .notes(() -> {
                    AcousticPuzzle puzzle = getPuzzle();
                    if (attuneState != ATTUNED || puzzle == null) {
                        return java.util.List.of(HudModel.line(tuneLevel == 0 ?
                                "Attune the lock: a rhythm game (D F J K)." : "Retune for a harder level and bigger boosts.",
                                HudModel.MID));
                    }
                    return java.util.List.of(HudModel.line("Opens with: " + hint(puzzle), HudModel.WHITE));
                })
                .math(() -> java.util.List.of(
                        HudModel.heading("ATTUNEMENT"),
                        HudModel.line("level " + tuneLevel + ": forgives " + Math.max(0, tuneLevel - 1) +
                                " wrong notes in a row", HudModel.MID),
                        HudModel.line("grade is set by how many notes were perfect or good", HudModel.DIM),
                        HudModel.heading("PUZZLE"),
                        HudModel.line("play the notes in order; a wrong note starts over", HudModel.MID)));
    }
}
