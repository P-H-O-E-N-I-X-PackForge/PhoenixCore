package net.phoenix.core.integration.vocal_resonance;

import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.MachineHud;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.vocal_resonance.ingredient.NotifiableSoundHandler;
import net.phoenix.core.integration.vocal_resonance.puzzle.HarmonicLockMachine;
import net.phoenix.core.integration.vocal_vibrancy.WorldAcousticSensor;
import net.phoenix.core.network.packet.C2SSelectSoundPacket;
import net.phoenix.core.network.packet.S2CPlaySoundPacket;
import net.phoenix.core.network.packet.S2CPlayStreamPacket;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResonantJukeboxMachine extends WorkableElectricMultiblockMachine implements IMuiMachine {

    private boolean hasDiscHatch = false;
    private boolean hasLibraryHatch = false;
    private boolean hasStreamHatch = false;

    @SaveField
    @NotNull

    private final NotifiableItemStackHandler discInventory = attachTrait(
            new NotifiableItemStackHandler(1, IO.IN, IO.NONE));

    @NotNull

    private final NotifiableSoundHandler soundHandler = attachTrait(new NotifiableSoundHandler(this, IO.IN));

    @SaveField
    @SyncToClient
    public String selectedLibrarySound = "";

    @SaveField
    private String lastPlayingStreamUrl = "";
    private String lastPlayingLibrarySound = "";

    @SaveField
    @SyncToClient
    public String currentStreamUrl = "";

    @Setter
    @SaveField
    @SyncToClient
    public String streamTitle = "Ready...";

    @SaveField
    public String searchTerm = "";

    private int totalSpeakerRange = 0;
    private float resonancePower = 1.0f;
    private static final int BASE_RANGE = 16;
    private static final long MUSIC_ENERGY_DRAIN = 32L;

    @SaveField
    @SyncToClient
    private int remainingSoundTicks = -1;

    @SaveField
    @SyncToClient
    public float currentLiveBass = 0.0f;

    @SaveField
    @SyncToClient
    public int currentLiveBPM = 0;

    private int lastKnownDuration = 0;

    private TickableSubscription acousticSub;

    public ResonantJukeboxMachine(BlockEntityCreationInfo holder) {
        super(holder);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            this.acousticSub = subscribeServerTick(this::acousticStateMachineTick);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (this.acousticSub != null) {
            unsubscribe(this.acousticSub);
            this.acousticSub = null;
        }
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);

        if (getLevel() == null || getLevel().isClientSide) return;

        PatternState patternState = this.getPatternState(substructureName);
        if (patternState == null || patternState.getCache() == null) return;

        this.hasDiscHatch = false;
        this.hasLibraryHatch = false;
        this.hasStreamHatch = false;
        this.totalSpeakerRange = 0;

        int totalSpeakerValue = 0;
        float amplifierSum = 0.0f;
        int speakerCount = 0;

        for (var entry : patternState.getCache().long2ObjectEntrySet()) {
            if (entry.getValue().getBlockState().getBlock() instanceof SpeakerBlock speaker) {
                totalSpeakerValue += speaker.getSpeakerType().getRangeBonus();
                amplifierSum += speaker.getSpeakerType().getResonanceAmplifier();
                speakerCount++;
            }

            BlockEntity blockEntity = entry.getValue().getBlockEntity();

            if (blockEntity instanceof SoundHatchPartMachine hatch) {
                if (hatch.getSoundType() != null) {
                    switch (hatch.getSoundType()) {
                        case DISC -> this.hasDiscHatch = true;
                        case LIBRARY -> this.hasLibraryHatch = true;
                        case STREAM -> this.hasStreamHatch = true;
                    }
                }
            }

        }

        this.totalSpeakerRange = totalSpeakerValue;

        this.resonancePower = speakerCount == 0 ? 1.0f : amplifierSum / speakerCount;
        this.lastPlayingStreamUrl = "";
        this.lastPlayingLibrarySound = "";
        this.resetAcousticData();

        WorldAcousticSensor.register(getBlockPos(), getFinalRange());
    }

    @Override
    public void invalidateStructure(String substructureName) {
        killAllMachineAudio();
        super.invalidateStructure(substructureName);
        this.lastPlayingStreamUrl = "";
        this.resetAcousticData();

        WorldAcousticSensor.unregister(getBlockPos());
    }

    public void acousticStateMachineTick() {
        if (getLevel() == null || getLevel().isClientSide || !isFormed()) return;

        boolean hasActiveSelection = (!selectedLibrarySound.isEmpty() && currentStreamUrl.isEmpty()) ||
                !currentStreamUrl.isEmpty();
        boolean canPlay = hasActiveSelection && isWorkingEnabled();

        if (!canPlay) {
            if (!lastPlayingStreamUrl.isEmpty() || remainingSoundTicks != -1) {
                killAllMachineAudio();
            }
            return;
        }

        var energyContainer = this.getEnergyContainer();
        if (energyContainer == null || energyContainer.getEnergyStored() < MUSIC_ENERGY_DRAIN) {
            killAllMachineAudio();
            return;
        }

        energyContainer.changeEnergy(-MUSIC_ENERGY_DRAIN);

        if (hasStreamHatch && !currentStreamUrl.isEmpty()) {
            if (!currentStreamUrl.equals(lastPlayingStreamUrl)) {
                this.selectedLibrarySound = "";
                this.lastPlayingLibrarySound = "";
                this.remainingSoundTicks = -1;

                boolean isYt = currentStreamUrl.contains("youtube.com") || currentStreamUrl.contains("youtu.be");
                String label = isYt ? "YT Audio" :
                        (currentStreamUrl.length() > 30 ? currentStreamUrl.substring(0, 27) + "..." : currentStreamUrl);
                this.streamTitle = "§7Status: §aStreaming §f" + label;
                playStreamSound();
                lastPlayingStreamUrl = currentStreamUrl;
            }
        } else {
            if (!lastPlayingStreamUrl.isEmpty()) {
                killAllMachineAudio();
                this.lastPlayingStreamUrl = "";
            }
        }

        if (hasLibraryHatch && !selectedLibrarySound.isEmpty() && currentStreamUrl.isEmpty()) {
            if (!selectedLibrarySound.equals(lastPlayingLibrarySound)) {
                playLibrarySound();
                lastPlayingLibrarySound = selectedLibrarySound;
            }
            this.markAsChanged();
        }
    }

    private void killAllMachineAudio() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;

        PacketDistributor.sendToPlayersTrackingChunk((net.minecraft.server.level.ServerLevel) level,
                level.getChunkAt(getBlockPos()).getPos(),
                new S2CPlaySoundPacket(getBlockPos(), ResourceLocation.fromNamespaceAndPath("minecraft", "empty"), 0.0f,
                        1.0f,
                        (float) getFinalRange()));

        this.lastPlayingLibrarySound = "";
        this.resetAcousticData();
        this.markAsChanged();
    }

    public void onLibrarySoundChosen(String soundLoc) {
        if (getLevel() == null || getLevel().isClientSide || !isFormed() || !hasLibraryHatch) return;
        HarmonicLockMachine.broadcast(getLevel(), getBlockPos(), getFinalRange(), soundLoc);
    }

    public void syncAndGeneralUpdate() {
        if (this.getLevel() != null && this.getLevel().isClientSide) {
            PacketDistributor.sendToServer(
                    new C2SSelectSoundPacket(this.getBlockPos(), this.selectedLibrarySound, this.currentStreamUrl));
        }
    }

    public int getFinalRange() {
        return BASE_RANGE + totalSpeakerRange;
    }

    public float getResonancePower() {
        return resonancePower;
    }

    public void syncAcousticData(int duration, float bass, int bpm) {
        if (duration > 0) {
            this.lastKnownDuration = duration;
            if (this.remainingSoundTicks <= 0 || duration > this.remainingSoundTicks) {
                this.remainingSoundTicks = duration;
            }
        }
        this.currentLiveBass = bass;
        this.currentLiveBPM = bpm;
        this.markAsChanged();
    }

    public void resetAcousticData() {
        this.currentLiveBass = 0.0f;
        this.currentLiveBPM = 0;
        this.remainingSoundTicks = -1;
        this.lastKnownDuration = 0;
    }

    private void playLibrarySound() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;
        if (selectedLibrarySound == null || selectedLibrarySound.isEmpty()) return;

        ResourceLocation soundLoc = ResourceLocation.tryParse(selectedLibrarySound);
        if (soundLoc == null) return;

        PacketDistributor.sendToPlayersTrackingChunk((net.minecraft.server.level.ServerLevel) level,
                level.getChunkAt(getBlockPos()).getPos(),
                new S2CPlaySoundPacket(getBlockPos(), soundLoc, resonancePower, 1.0f, (float) getFinalRange()));
    }

    private void playStreamSound() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;

        PacketDistributor.sendToPlayersTrackingChunk((net.minecraft.server.level.ServerLevel) level,
                level.getChunkAt(getBlockPos()).getPos(),
                new S2CPlayStreamPacket(currentStreamUrl, getBlockPos(), getFinalRange(), resonancePower));
    }

    private void hudAction(int id, int value) {
        HudRecipes.handle(this, id);
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData data,
                                                           PanelSyncManager syncManager,
                                                           brachy.modularui.screen.UISettings settings) {
        syncManager.syncValue("jukebox_disc", 0, new BooleanSyncValue(() -> hasDiscHatch, v -> hasDiscHatch = v));
        syncManager.syncValue("jukebox_library", 0, new BooleanSyncValue(() -> hasLibraryHatch, v -> hasLibraryHatch = v));
        syncManager.syncValue("jukebox_stream", 0, new BooleanSyncValue(() -> hasStreamHatch, v -> hasStreamHatch = v));
        syncManager.syncValue("jukebox_range", 0, new IntSyncValue(() -> totalSpeakerRange, v -> totalSpeakerRange = v));
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private boolean playing() {
        return (!selectedLibrarySound.isEmpty() || !currentStreamUrl.isEmpty()) && isWorkingEnabled();
    }

    private HudModel hudModel() {
        return HudModel.of("Resonant Jukebox")
                .formed(this::isFormed)
                .badge(() -> playing() ? new HudModel.Badge("PLAYING", HudModel.GREEN) : HudRecipes.badge(this))
                .subtitle(() -> !currentStreamUrl.isEmpty() ? streamTitle : !selectedLibrarySound.isEmpty() ?
                        selectedLibrarySound : "Nothing playing")
                .cells(() -> {
                    var cells = new java.util.ArrayList<HudModel.Cell>();
                    cells.add(HudModel.cell("RADIUS", getFinalRange() + " m", "base " + BASE_RANGE + " + " +
                            totalSpeakerRange, HudModel.CYAN));
                    cells.add(HudModel.cell("USAGE", (playing() ? MUSIC_ENERGY_DRAIN : 0L) + " EU/t",
                            playing() ? "playing" : "idle", HudModel.ORANGE));
                    cells.add(HudModel.cell("BPM", currentLiveBPM > 0 ? String.valueOf(currentLiveBPM) : "-",
                            currentLiveBPM > 0 ? "detected" : "awaiting analysis", HudModel.PURPLE));
                    cells.add(HudModel.cell("DISCS", hasDiscHatch ? "ready" : "no hatch", "physical discs",
                            hasDiscHatch ? HudModel.GREEN : HudModel.RED));
                    cells.add(HudModel.cell("LIBRARY", hasLibraryHatch ? "ready" : "no hatch", "sound library",
                            hasLibraryHatch ? HudModel.GREEN : HudModel.RED));
                    cells.add(HudModel.cell("STREAMING", hasStreamHatch ? "ready" : "no hatch", "YT streaming",
                            hasStreamHatch ? HudModel.GREEN : HudModel.RED));
                    return cells;
                })
                .bars(() -> java.util.List.of(new HudModel.Bar("Bass", Math.min(1.0f, currentLiveBass),
                        String.format(java.util.Locale.ROOT, "%.0f%%", currentLiveBass * 100), HudModel.PURPLE)))
                .button(() -> getRecipeLogic().isWorkingEnabled() ? "PAUSE" : "RESUME", HudRecipes.TOGGLE, HudModel.GOLD,
                        this::isFormed)
                .math(() -> java.util.List.of(
                        HudModel.heading("RANGE"),
                        HudModel.line("radius = base + speaker bonus", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %d + %d = %d m", BASE_RANGE, totalSpeakerRange,
                                getFinalRange()), HudModel.WHITE),
                        HudModel.heading("ENERGY"),
                        HudModel.line("music costs " + MUSIC_ENERGY_DRAIN + " EU/t while playing", HudModel.MID)))
                .graph("bass %", HudModel.PURPLE, () -> currentLiveBass * 100.0f, 100.0f)
                .graph("BPM", HudModel.CYAN, () -> (float) currentLiveBPM, 0.0f);
    }

    private String getGateStatus(String name, boolean active) {
        return (active ? "  §a✔ " : "  §c✘ ") + "§7" + name;
    }
}
