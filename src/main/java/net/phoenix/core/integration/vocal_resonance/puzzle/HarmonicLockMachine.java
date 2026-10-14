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
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SAttuneStartPacket;
import net.phoenix.core.network.packet.S2CAttuneResultPacket;
import net.phoenix.core.network.packet.S2CAttuneStartPacket;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.TextWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A sound puzzle as a multiblock of its own: a state machine that listens for sounds played by Resonant Jukeboxes in
 * range, and steps through a puzzle sequence (see {@link AcousticPuzzle}). It runs only on the server sound table, so what
 * a client hears never decides the outcome.
 *
 * <p>
 * A lock starts DORMANT and has to be attuned once, by clearing a short rhythm chart ({@link AcousticChart}); until then
 * it ignores every sound. The server judges the run from the presses the player sends, and the grade it earns decides
 * how many of the puzzle's first notes the lock reveals.
 */
public class HarmonicLockMachine extends MultiblockControllerMachine implements IMuiMachine {

    public static final int DORMANT = 0;
    public static final int ATTUNING = 1;
    public static final int ATTUNED = 2;

    private static final int PERFECT_MS = 60;
    private static final int GOOD_MS = 120;
    /** How much later than the lowest offset seen a press may arrive before it is thrown out. */
    private static final int OFFSET_SLACK_MS = 250;
    private static final int RETRY_COOLDOWN_TICKS = 100;
    private static final double PASS = 0.6;
    private static final double GOOD_GRADE = 0.8;
    private static final double PERFECT_GRADE = 0.95;

    /** The formed locks. Jukeboxes tell these about the sounds they play. */
    private static final Set<HarmonicLockMachine> LOCKS = ConcurrentHashMap.newKeySet();

    private final AcousticStateMachine machine = new AcousticStateMachine();

    /** The puzzle this lock is set to; empty means the first one loaded. */
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

    /** 0 = passed, 1 = good, 2 = perfect */
    @SaveField
    @SyncToClient
    private int attuneGrade = 0;

    @SaveField
    private long cooldownUntil = 0;

    // the run in progress (server only)
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

    // ---------------------------------------------------------------- attunement

    /** A player asked to start the rhythm game. Server side only. */
    public void startAttunement(ServerPlayer player) {
        if (!isFormed() || attuneState != DORMANT) return;
        Level level = getLevel();
        if (level == null) return;
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
        runStart = level.getGameTime();
        judged = new boolean[picked.beats().size()];
        perfects = 0;
        goods = 0;
        minOffset = Integer.MAX_VALUE;
        attuneState = ATTUNING;
        markAsChanged();
        PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CAttuneStartPacket(getBlockPos(), picked));
    }

    /**
     * A lane press from the runner, with the time on the player's own clock. The server trusts that time to judge the beat,
     * but checks it against its own clock: it may not be ahead of the server, and it may not drift later than the lowest
     * delay seen so far by more than a little.
     */
    public void attunePress(ServerPlayer player, int lane, int elapsedMs) {
        AcousticChart current = chart;
        Level level = getLevel();
        if (attuneState != ATTUNING || current == null || level == null || !player.getUUID().equals(runner)) return;

        int serverMs = (int) ((level.getGameTime() - runStart) * AcousticChart.MS_PER_TICK);
        int offset = serverMs - elapsedMs;
        if (offset < -AcousticChart.MS_PER_TICK * 2) return;
        if (minOffset != Integer.MAX_VALUE && offset > minOffset + OFFSET_SLACK_MS) return;
        minOffset = Math.min(minOffset, offset);

        int best = -1;
        int bestDelta = Integer.MAX_VALUE;
        List<AcousticChart.Beat> beats = current.beats();
        for (int i = 0; i < beats.size(); i++) {
            if (judged[i] || beats.get(i).lane() != lane) continue;
            int delta = Math.abs(beats.get(i).tick() * AcousticChart.MS_PER_TICK - elapsedMs);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = i;
            }
        }
        if (best < 0 || bestDelta > GOOD_MS) return;
        judged[best] = true;
        if (bestDelta <= PERFECT_MS) perfects++;
        else goods++;
    }

    private void runTick() {
        if (attuneState != ATTUNING || chart == null || getLevel() == null) return;
        long endTick = runStart + chart.lastBeatTick() + 60;
        if (getLevel().getGameTime() >= endTick) finishRun();
    }

    private void finishRun() {
        AcousticChart current = chart;
        Level level = getLevel();
        if (current == null || level == null) return;
        int total = current.beats().size();
        double accuracy = (perfects + goods * 0.6) / Math.max(1, total);
        boolean passed = accuracy >= PASS;
        int grade = accuracy >= PERFECT_GRADE ? 2 : accuracy >= GOOD_GRADE ? 1 : 0;

        if (passed) {
            attuneState = ATTUNED;
            attuneGrade = grade;
            machine.reset();
            state = 0;
            step = 0;
        } else {
            attuneState = DORMANT;
            cooldownUntil = level.getGameTime() + RETRY_COOLDOWN_TICKS;
        }
        markAsChanged();

        if (runner != null && level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(runner);
            if (player != null) {
                PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new S2CAttuneResultPacket(getBlockPos(), passed, grade, perfects, goods, total));
            }
        }
        chart = null;
        runner = null;
    }

    private void abortRun() {
        if (attuneState == ATTUNING) {
            attuneState = DORMANT;
            chart = null;
            runner = null;
            markAsChanged();
        }
    }

    // ---------------------------------------------------------------- the puzzle

    /** A jukebox played a library sound: tell every lock in range of it. Server side only. */
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
        if (machine.onSound(getPuzzle(), sound)) {
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

    /** What the lock reveals of the puzzle's first steps, by attunement grade: 1, 2 or 3 steps. */
    private String hint(AcousticPuzzle puzzle) {
        int shown = Math.min(puzzle.steps().size(), attuneGrade + 1);
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
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.dynamic(() -> switch (attuneState) {
            case ATTUNING -> Component.literal("§7Lock: §eattuning... §7(play the rhythm game)");
            case ATTUNED -> Component.literal("§7Lock: §aattuned §7(" +
                    (attuneGrade >= 2 ? "perfect" : attuneGrade == 1 ? "good" : "passed") + ")");
            default -> Component.literal("§7Lock: §cdormant §7- attune it to wake the puzzle");
        })));
        widgets.add(new ButtonWidget<>()
                .overlay(new ItemDrawable(new ItemStack(Items.NOTE_BLOCK)).asIcon().size(16))
                .tooltip(new RichTooltip().addLine(Text.of(Component
                        .literal("Start the attunement rhythm game (keys D F J K)"))))
                .onMousePressed((context, button) -> {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SAttuneStartPacket(getBlockPos()));
                    return true;
                }));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            AcousticPuzzle puzzle = getPuzzle();
            if (attuneState != ATTUNED) return Component.literal("§7Puzzle: §8asleep");
            if (puzzle == null) return Component.literal("§7Puzzle: §8none loaded");
            String progress = step + "/" + puzzle.steps().size();
            return Component.literal("§7Puzzle: " + switch (getPuzzleState()) {
                case IDLE -> "§8waiting §7(" + progress + ")";
                case LISTENING -> "§elistening §7(" + progress + ")";
                case SOLVED -> "§aSOLVED";
                case FAILED -> "§cwrong note, start again";
            });
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() -> {
            AcousticPuzzle puzzle = getPuzzle();
            if (attuneState != ATTUNED || puzzle == null) return Component.empty();
            return Component.literal("§7Opens with: §f" + hint(puzzle));
        })));
        widgets.add(new TextWidget<>(Text.dynamic(() -> Component
                .literal("§7Listens to Resonant Jukeboxes in range of this lock."))));
        return widgets;
    }
}
