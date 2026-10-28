package net.phoenix.core.common.machine.multiblock.stellar;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.phoenix.core.client.gui.hud.CanvasWidget;
import net.phoenix.core.client.gui.hud.HudPanel;
import net.phoenix.core.client.gui.hud.TabStrip;
import net.phoenix.core.client.stellar.StellarForgeClient;
import net.phoenix.core.client.stellar.ui.StellarGraphsPage;
import net.phoenix.core.client.stellar.ui.StellarHudPage;
import net.phoenix.core.client.stellar.ui.StellarMathPage;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StellarForgeMachine extends MultiblockControllerMachine implements IMuiMachine {

    private static double massPerMb(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        if (isMaterial(fluid, GTMaterials.Neutronium)) return 1.0;
        if (isMaterial(fluid, GTMaterials.Helium)) return 0.02;
        if (isMaterial(fluid, GTMaterials.Hydrogen)) return 0.01;
        return 0.0;
    }

    private static boolean isMaterial(Fluid fluid,
                                      com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry material) {
        if (isFluid(fluid, material.getFluid())) return true;
        if (isFluid(fluid, material.getFluid(FluidStorageKeys.LIQUID))) return true;
        return isFluid(fluid, material.getFluid(FluidStorageKeys.GAS));
    }

    private static boolean isFluid(Fluid a, @Nullable Fluid b) {
        return b != null && a.isSame(b);
    }

    @SaveField
    @SyncToClient
    private int stageOrdinal;

    @SaveField
    @SyncToClient
    private int massX100;

    @SaveField
    @SyncToClient
    private int heatX10;

    @SaveField
    @SyncToClient
    private int stabilityX10;

    @SaveField
    @SyncToClient
    private int feed = 50;

    @SaveField
    @SyncToClient
    private int field;

    @SaveField
    @SyncToClient
    private int spin;

    @SaveField
    @SyncToClient
    private boolean collapseArmed;

    @SaveField
    @SyncToClient
    private String plasmaId = "";

    @SyncToClient
    private String plasmaName = "";

    @SyncToClient
    private String plasmaNote = "";

    @SaveField
    @SyncToClient
    private int runSeed;

    @SaveField
    @SyncToClient
    private int runAge;

    @SaveField
    @SyncToClient
    private int eventCount;

    @SaveField
    @SyncToClient
    private int eventKind;

    @SaveField
    @SyncToClient
    private int starTint = -1;

    @SaveField
    @SyncToClient
    private int holeTint = -1;

    private StellarForgeState.Stage lastStage = StellarForgeState.Stage.IDLE;

    @SyncToClient
    private boolean powered = true;

    @SyncToClient
    private int energyUse;

    @SyncToClient
    private int plasmaRateX10;

    private @Nullable StellarForgeState state;
    private double feedBuffer;
    private double plasmaBuffer;
    private final ConditionalSubscriptionHandler tickHandler;

    public StellarForgeMachine(BlockEntityCreationInfo info) {
        super(info);
        this.tickHandler = new ConditionalSubscriptionHandler(this, this::forgeTick, this::isFormed);
    }

    public StellarForgeState.Stage stage() {
        return StellarForgeState.Stage.of(stageOrdinal);
    }

    private double shownMass;
    private float shownHeat;
    private float shownStability;
    private long shownAt;
    private boolean shownSeeded;

    private void ease() {
        long now = System.nanoTime();
        float dt = shownSeeded ? Math.min(0.25f, (now - shownAt) / 1.0e9f) : 1.0f;
        if (dt <= 0.0f) return;
        shownAt = now;
        float k = shownSeeded ? 1.0f - (float) Math.exp(-dt * 9.0f) : 1.0f;
        shownSeeded = true;
        shownMass += (massX100 / 100.0 - shownMass) * k;
        shownHeat += (heatX10 / 10.0f - shownHeat) * k;
        shownStability += (stabilityX10 / 10.0f - shownStability) * k;
    }

    public double mass() {
        if (isRemote()) {
            ease();
            return shownMass;
        }
        return massX100 / 100.0;
    }

    public float heat() {
        if (isRemote()) {
            ease();
            return shownHeat;
        }
        return heatX10 / 10.0f;
    }

    public float stability() {
        if (isRemote()) {
            ease();
            return shownStability;
        }
        return stabilityX10 / 10.0f;
    }

    public int eventCount() {
        return eventCount;
    }

    public int eventKind() {
        return eventKind;
    }

    public int starTint() {
        return starTint;
    }

    public int holeTint() {
        return holeTint;
    }

    private void applyTints() {
        var stage = s().stage;
        for (FluidHatchPartMachine hatch : hatches(IO.IN)) {
            for (int tank = 0; tank < hatch.tank.getTanks(); tank++) {
                FluidStack inside = hatch.tank.getFluidInTank(tank);
                var tint = StellarTint.match(inside, stage);
                if (tint == null) continue;
                FluidStack taken = hatch.tank.drainInternal(new FluidStack(inside.getFluid(), tint.amount()),
                        IFluidHandler.FluidAction.EXECUTE);
                if (taken.getAmount() < tint.amount()) continue;
                if (stage == StellarForgeState.Stage.STAR) {
                    starTint = tint.color();
                    syncDataHolder.markClientSyncFieldDirty("starTint");
                } else {
                    holeTint = tint.color();
                    syncDataHolder.markClientSyncFieldDirty("holeTint");
                }
                return;
            }
        }
    }

    private StellarForgeState s() {
        return state();
    }

    public int feedSetting() {
        return feed;
    }

    public int fieldSetting() {
        return field;
    }

    public int spinSetting() {
        return spin;
    }

    public boolean armed() {
        return collapseArmed;
    }

    public boolean powerOk() {
        return powered;
    }

    public int energyUse() {
        return energyUse;
    }

    public float plasmaPerTick() {
        return plasmaRateX10 / 10.0f;
    }

    public net.minecraft.world.phys.Vec3 chamberCentre() {
        Direction back = getFrontFacing().getOpposite();
        return new net.minecraft.world.phys.Vec3(getBlockPos().getX() + 0.5 + back.getStepX() * 2.0,
                getBlockPos().getY() + 2.5, getBlockPos().getZ() + 0.5 + back.getStepZ() * 2.0);
    }

    public double massFraction() {
        return view().massFraction();
    }

    public StellarForgeState view() {
        StellarForgeState view = new StellarForgeState(getBlockPos().asLong());
        view.stage = stage();
        view.mass = mass();
        view.heat = heat();
        view.stability = stability();
        view.feed = feed;
        view.field = field;
        view.spin = spin;
        view.run = runSeed;
        view.age = runAge;
        view.eventCount = eventCount;
        view.eventKind = eventKind;
        return view;
    }

    private StellarForgeState state() {
        if (state == null) {
            state = new StellarForgeState(getBlockPos().asLong());
            state.stage = StellarForgeState.Stage.of(stageOrdinal);
            state.mass = massX100 / 100.0;
            state.heat = heatX10 / 10.0f;
            state.stability = stabilityX10 / 10.0f;
            state.feed = feed;
            state.field = field;
            state.spin = spin;
            state.collapseArmed = collapseArmed;
            state.run = runSeed;
            state.age = runAge;
            state.eventCount = eventCount;
            state.eventKind = eventKind;
        }
        return state;
    }

    public void setFeed(int value) {
        feed = state().feed = Math.max(0, Math.min(100, value));
        syncDataHolder.markClientSyncFieldDirty("feed");
    }

    public void setField(int value) {
        field = state().field = Math.max(0, Math.min(100, value));
        syncDataHolder.markClientSyncFieldDirty("field");
    }

    public void setSpin(int value) {
        spin = state().spin = Math.max(0, Math.min(100, value));
        syncDataHolder.markClientSyncFieldDirty("spin");
    }

    public void armCollapse() {
        StellarForgeState s = state();
        if (s.stage == StellarForgeState.Stage.STAR) {
            s.collapseArmed = !s.collapseArmed;
            collapseArmed = s.collapseArmed;
        }
    }

    private void pickPlasma(int index) {
        var list = StellarPlasma.all();
        plasmaId = index >= 1 && index <= list.size() ? list.get(index - 1).id().toString() : "";
        plasmaBuffer = 0.0;
        syncDataHolder.markClientSyncFieldDirty("plasmaId");
    }

    private void refreshPlasmaNote(StellarForgeState s, StellarPlasma.@Nullable Recipe exotic, boolean drawing,
                                   double rate) {
        String name;
        String note;
        if (plasmaId.isEmpty()) {
            name = "Oxygen plasma";
            note = "";
        } else if (exotic == null) {
            name = plasmaId;
            note = "no such plasma";
        } else {
            name = exotic.name();
            String why = exotic.whyNot(s);
            note = drawing ?
                    String.format(java.util.Locale.ROOT, "drawing %.1f mB/t, costs %.2f stability and %.2f M a tick",
                            rate, exotic.damageStability(), exotic.damageMass()) :
                    why != null ? why : "waiting (nothing to take yet)";
        }
        if (!name.equals(plasmaName)) {
            plasmaName = name;
            syncDataHolder.markClientSyncFieldDirty("plasmaName");
        }
        if (!note.equals(plasmaNote)) {
            plasmaNote = note;
            syncDataHolder.markClientSyncFieldDirty("plasmaNote");
        }
    }

    public String plasmaId() {
        return plasmaId;
    }

    public String plasmaName() {
        return plasmaName;
    }

    public String plasmaNote() {
        return plasmaNote;
    }

    public void disperse() {
        state().disperse();
        starTint = -1;
        holeTint = -1;
        syncDataHolder.markClientSyncFieldDirty("starTint");
        syncDataHolder.markClientSyncFieldDirty("holeTint");
        publish();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && getLevel().isClientSide) StellarForgeClient.add(this);
        tickHandler.initialize(getLevel());
    }

    @Override
    public void onUnload() {
        if (getLevel() != null && getLevel().isClientSide) StellarForgeClient.remove(this);
        tickHandler.unsubscribe();
        super.onUnload();
    }

    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);
        net.phoenix.core.PhoenixCore.LOGGER.debug("[Stellar] structure formed ({}), subscribing the tick",
                substructureName);
        tickHandler.updateSubscription();
    }

    @Override
    public void invalidateStructure(String substructureName) {
        super.invalidateStructure(substructureName);
        tickHandler.updateSubscription();
    }

    private List<FluidHatchPartMachine> hatches(IO io) {
        List<FluidHatchPartMachine> found = new ArrayList<>();
        for (var part : getParts()) {
            if (part instanceof FluidHatchPartMachine hatch && hatch.tank.getHandlerIO() == io) found.add(hatch);
        }
        return found;
    }

    private long energyAvailable() {
        long total = 0L;
        for (var part : getParts()) {
            if (part instanceof EnergyHatchPartMachine hatch && hatch.energyContainer.getHandlerIO() == IO.IN) {
                total += hatch.energyContainer.getEnergyStored();
            }
        }
        return total;
    }

    private void takeEnergy(long amount) {
        long left = amount;
        for (var part : getParts()) {
            if (left <= 0) break;
            if (part instanceof EnergyHatchPartMachine hatch && hatch.energyContainer.getHandlerIO() == IO.IN) {
                long take = Math.min(left, hatch.energyContainer.getEnergyStored());
                if (take > 0) {
                    hatch.energyContainer.changeEnergy(-take);
                    left -= take;
                }
            }
        }
    }

    private void forgeTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        StellarForgeState s = state();

        long need = s.energyPerTick();
        boolean ok = need <= 0 || energyAvailable() >= need;
        if (ok && need > 0) takeEnergy(need);

        double fed = 0.0;
        double want = s.stage == StellarForgeState.Stage.IDLE ?
                StellarForgeState.MAX_FEED_PER_TICK[1] * feed / 100.0 : s.feedPerTick();
        if (!ok && s.stage != StellarForgeState.Stage.IDLE) want = 0.0;
        feedBuffer += want;
        int whole = (int) feedBuffer;
        if (whole > 0) {
            for (FluidHatchPartMachine hatch : hatches(IO.IN)) {
                if (whole <= 0) break;
                for (int tank = 0; tank < hatch.tank.getTanks() && whole > 0; tank++) {
                    FluidStack inside = hatch.tank.getFluidInTank(tank);
                    double perMb = inside.isEmpty() ? 0.0 : massPerMb(inside);
                    if (perMb <= 0.0) continue;
                    FluidStack taken = hatch.tank.drainInternal(new FluidStack(inside.getFluid(), Math.min(whole,
                            inside.getAmount())), IFluidHandler.FluidAction.EXECUTE);
                    if (!taken.isEmpty()) {
                        fed += taken.getAmount() * perMb;
                        whole -= taken.getAmount();
                        feedBuffer -= taken.getAmount();
                    }
                }
            }

            feedBuffer = Math.min(feedBuffer, 2.0);
        }

        boolean blocked = false;
        double rate = s.plasmaPerTick();
        Fluid plasma = GTMaterials.Oxygen.getFluid(FluidStorageKeys.PLASMA);
        StellarPlasma.Recipe exotic = StellarPlasma.get(plasmaId);
        boolean drawingExotic = false;
        if (!plasmaId.isEmpty()) {

            rate = 0.0;
            plasma = null;
            if (exotic != null && exotic.eligible(s)) {
                Fluid wanted = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(exotic.fluid());
                if (wanted != net.minecraft.world.level.material.Fluids.EMPTY) {
                    plasma = wanted;
                    rate = exotic.rate() * (s.stability / 100.0);
                    drawingExotic = rate > 0.0;
                }
            }
        }
        if (rate > 0.0) {
            plasmaBuffer += rate;
            int out = (int) plasmaBuffer;
            if (out > 0) {
                int left = out;
                if (plasma != null) {
                    for (FluidHatchPartMachine hatch : hatches(IO.OUT)) {
                        if (left <= 0) break;
                        left -= hatch.tank.fillInternal(new FluidStack(plasma, left),
                                IFluidHandler.FluidAction.EXECUTE);
                    }
                }
                plasmaBuffer -= out - left;
                blocked = left > 0;
                plasmaBuffer = Math.min(plasmaBuffer, 64.0);
            }
        }
        if (drawingExotic && !blocked) {

            s.stability = Math.max(0.0f, s.stability - exotic.damageStability());
            s.mass = Math.max(0.0, s.mass - exotic.damageMass());
            s.heat = Math.max(0.0f, Math.min(100.0f, s.heat + exotic.damageHeat()));
        }
        if (getLevel().getGameTime() % 10 == 0) refreshPlasmaNote(s, exotic, drawingExotic, rate);

        if (getLevel().getGameTime() % 100 == 0) {
            var report = new StringBuilder();
            for (FluidHatchPartMachine hatch : hatches(IO.IN)) {
                for (int tank = 0; tank < hatch.tank.getTanks(); tank++) {
                    FluidStack inside = hatch.tank.getFluidInTank(tank);
                    if (!inside.isEmpty()) report.append(inside.getFluid().builtInRegistryHolder().key().location())
                            .append(" x").append(inside.getAmount()).append(" (").append(massPerMb(inside))
                            .append("/mB) ");
                }
            }
            net.phoenix.core.PhoenixCore.LOGGER.debug("[Stellar] tick: stage {} mass {} stability {} heat {} hold {} " +
                    "feed {} field {} (band {}) spin {} (band {}) want {} fed {} inHatches {} powered {} fluids: {}",
                    s.stage, s.mass, s.stability, s.heat, s.holdTicks, feed, field, s.fieldBand(), spin, s.spinBand(),
                    want, fed, hatches(IO.IN).size(), ok, report);
        }

        if (s.stage == StellarForgeState.Stage.IDLE) {
            runSeed = getLevel().random.nextInt();
            s.run = runSeed;
        }
        s.tick(fed, ok, blocked);
        if (getLevel().getGameTime() % 20 == 0) applyTints();
        if (s.stage != lastStage) {

            if (s.stage == StellarForgeState.Stage.BLACK_HOLE && lastStage == StellarForgeState.Stage.STAR &&
                    starTint >= 0) {
                holeTint = starTint;
                syncDataHolder.markClientSyncFieldDirty("holeTint");
            }
            lastStage = s.stage;
            String trigger = net.phoenix.core.integration.chronicles.PhoenixChroniclesTriggers.stellar(s.stage.name());
            if (trigger != null) {
                net.phoenix.core.integration.chronicles.ChroniclesLink.fire(getLevel().getServer(), getOwnerUUID(),
                        trigger, net.phoenix.core.integration.chronicles.ChroniclesLink.data("stage", s.stage.name()));
            }
        }
        powered = ok;
        energyUse = (int) Math.min(Integer.MAX_VALUE, need);
        plasmaRateX10 = Math.round((float) rate * 10.0f);
        publish();
    }

    private void publish() {
        StellarForgeState s = state();
        boolean push = getLevel() != null && getLevel().getGameTime() % 2 == 0;
        int stageNow = s.stage.ordinal();
        if (stageNow != stageOrdinal) {
            stageOrdinal = stageNow;
            syncDataHolder.markClientSyncFieldDirty("stageOrdinal");
            syncDataHolder.markClientSyncFieldDirty("runSeed");
            push = true;
        }
        runAge = s.age;
        if (eventCount != s.eventCount) {
            eventCount = s.eventCount;
            eventKind = s.eventKind;
            syncDataHolder.markClientSyncFieldDirty("eventCount");
            syncDataHolder.markClientSyncFieldDirty("eventKind");
        }
        massX100 = (int) Math.min(Integer.MAX_VALUE, Math.round(s.mass * 100.0));
        heatX10 = Math.round(s.heat * 10.0f);
        stabilityX10 = Math.round(s.stability * 10.0f);
        feed = s.feed;
        field = s.field;
        spin = s.spin;
        if (collapseArmed != s.collapseArmed) {
            collapseArmed = s.collapseArmed;
            syncDataHolder.markClientSyncFieldDirty("collapseArmed");
        }

        if (push) {
            for (String name : new String[] { "massX100", "heatX10", "stabilityX10", "feed", "field", "spin",
                    "powered", "energyUse", "plasmaRateX10", "runAge" }) {
                syncDataHolder.markClientSyncFieldDirty(name);
            }
        }
    }

    private static final int UI_WIDTH = 264;
    private static final int UI_HEIGHT = 276;

    private void control(int which, int value) {
        net.phoenix.core.PhoenixCore.LOGGER.debug("[Stellar] control {} = {} (stage {})", which, value, stage());
        switch (which) {
            case StellarControls.FEED -> setFeed(value);
            case StellarControls.FIELD -> setField(value);
            case StellarControls.SPIN -> setSpin(value);
            case StellarControls.ARM -> armCollapse();
            case StellarControls.DISPERSE -> disperse();
            case StellarControls.PLASMA -> pickPlasma(value);
            default -> {}
        }
    }

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerServerSyncedAction("stellar_control", buf -> {
            int which = buf.readVarInt();
            int value = buf.readVarInt();
            control(which, value);
        });
        TabStrip screen = null;
        if (getLevel() != null && getLevel().isClientSide) {
            StellarControls controls = new StellarControls() {

                @Override
                public void set(int which, int value) {
                    syncManager.callSyncedAction("stellar_control", buf -> {
                        buf.writeVarInt(which);
                        buf.writeVarInt(value);
                    });
                }

                @Override
                public void press(int action) {
                    set(action, 0);
                }
            };
            screen = new TabStrip(List.of(new TabStrip.Tab("H", new StellarHudPage(this, controls)),
                    new TabStrip.Tab("V", new net.phoenix.core.client.stellar.ui.StellarViewPage(this)),
                    new TabStrip.Tab("M", new StellarMathPage(this)),
                    new TabStrip.Tab("~", new StellarGraphsPage(this))),
                    net.phoenix.core.client.gui.hud.HudTheme.DEEP_SPACE);
        }
        final TabStrip tabs = screen;
        var panel = MachineUIPanelBuilder.panelBuilder(this).attachInventory(false).addTitleBar(false)
                .drawGTLogo(false)
                .mainContents(main -> {
                    if (tabs != null) main.child(new CanvasWidget(tabs).size(UI_WIDTH, UI_HEIGHT));
                    else main.size(UI_WIDTH, UI_HEIGHT);
                }).build(syncManager, settings);
        if (tabs != null) HudPanel.apply(panel, tabs);
        return panel;
    }
}
