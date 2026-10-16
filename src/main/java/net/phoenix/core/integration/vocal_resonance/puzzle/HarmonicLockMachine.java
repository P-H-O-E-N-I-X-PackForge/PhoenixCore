package net.phoenix.core.integration.vocal_resonance.puzzle;

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
        perfects = 0;
        goods = 0;
        minOffset = Integer.MAX_VALUE;
        attuneState = ATTUNING;
        markAsChanged();
        // the machine's own menu is still open on the server. If it is left open, a later close or validity check
        // sends the client a close-container packet, which shuts the minigame the instant it opens.
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
        if (bestDelta <= runDifficulty.perfectMs()) perfects++;
        else goods++;
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

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        Flow column = Flow.col()
                .coverChildren()
                .margin(8, 6)
                .child(Text.dynamic(() -> Component.literal("§bHARMONIC LOCK")).asWidget().marginBottom(8));
        if (!isFormed()) {
            column.child(Text.dynamic(() -> Component.literal("§cNot formed")).asWidget());
        }
        for (IWidget widget : getWidgetsForDisplay(syncManager)) {
            column.child(widget);
        }
        mainWidget.child(column);
    }

    private static AcousticStateMachine.State stateOf(int ordinal) {
        AcousticStateMachine.State[] values = AcousticStateMachine.State.values();
        return values[Math.max(0, Math.min(ordinal, values.length - 1))];
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        // the machine's own fields are not reliably mirrored into an open screen, so the display reads dedicated
        // sync values instead (this is what left the text stuck on "dormant" after a pass)
        IntSyncValue attuneSv = syncManager.getOrCreateSyncHandler("lock_attune", IntSyncValue.class,
                () -> new IntSyncValue(() -> attuneState));
        IntSyncValue gradeSv = syncManager.getOrCreateSyncHandler("lock_grade", IntSyncValue.class,
                () -> new IntSyncValue(() -> attuneGrade));
        IntSyncValue levelSv = syncManager.getOrCreateSyncHandler("lock_level", IntSyncValue.class,
                () -> new IntSyncValue(() -> tuneLevel));
        IntSyncValue stepSv = syncManager.getOrCreateSyncHandler("lock_step", IntSyncValue.class,
                () -> new IntSyncValue(() -> step));
        IntSyncValue stateSv = syncManager.getOrCreateSyncHandler("lock_state", IntSyncValue.class,
                () -> new IntSyncValue(() -> state));
        widgets.add(new TextWidget<>(Text.dynamic(() -> switch (attuneSv.getIntValue()) {
            case ATTUNING -> Component.literal("§7Lock: §eattuning... §7(play the rhythm game)");
            case ATTUNED -> Component.literal("§7Lock: §aattuned §7(" + AttuneDifficulty.fromLevel(levelSv.getIntValue()).label() +
                    ", " + (gradeSv.getIntValue() >= 2 ? "perfect" : gradeSv.getIntValue() == 1 ? "good" : "passed") + ")");
            default -> Component.literal("§7Lock: §cdormant §7- attune it to wake the puzzle");
        })));
        widgets.add(new ButtonWidget<>()
                .overlay(new ItemDrawable(new ItemStack(Items.NOTE_BLOCK)).asIcon().size(16))
                .tooltip(new RichTooltip().addLine(Text.of(Component
                        .literal(levelSv.getIntValue() == 0 ? "Attune the lock: a rhythm game (keys D F J K)" :
                                "Retune the lock for a harder level and bigger boosts"))))
                .onMousePressed((context, button) -> {
                    net.phoenix.core.integration.vocal_resonance.puzzle.client.AttuneClient
                            .openDifficulty(getBlockPos(), tuneLevel);
                    return true;
                }));
        widgets.add(new TextWidget<>(Text.dynamic(() -> levelSv.getIntValue() == 0 ? Component.empty() :
                Component.literal("§7Forgives §f" + Math.max(0, levelSv.getIntValue() - 1) + " §7wrong note" +
                        (levelSv.getIntValue() == 2 ? "" : "s") + " in a row."))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            AcousticPuzzle puzzle = getPuzzle();
            if (attuneSv.getIntValue() != ATTUNED) return Component.literal("§7Puzzle: §8asleep");
            if (puzzle == null) return Component.literal("§7Puzzle: §8none loaded");
            String progress = stepSv.getIntValue() + "/" + puzzle.steps().size();
            return Component.literal("§7Puzzle: " + switch (stateOf(stateSv.getIntValue())) {
                case IDLE -> "§8waiting §7(" + progress + ")";
                case LISTENING -> "§elistening §7(" + progress + ")";
                case SOLVED -> "§aSOLVED";
                case FAILED -> "§cwrong note, start again";
            });
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            AcousticPuzzle puzzle = getPuzzle();
            if (attuneSv.getIntValue() != ATTUNED || puzzle == null) return Component.empty();
            return Component.literal("§7Opens with: §f" + hint(puzzle));
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component
                .literal("§7Listens to Resonant Jukeboxes in range of this lock."))));
        return widgets;
    }
}
